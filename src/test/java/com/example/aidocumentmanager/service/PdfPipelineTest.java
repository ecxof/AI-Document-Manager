package com.example.aidocumentmanager.service;

import com.example.aidocumentmanager.model.DocumentEntry;
import com.example.aidocumentmanager.util.ConfigurationManager;
import com.example.aidocumentmanager.util.FileUtils;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Walks a real PDF through the whole pipeline the way an upload does: parse it,
 * chunk it, embed it, persist it, and ask a question that only the PDF can
 * answer. This is the path that was silently returning nothing.
 */
class PdfPipelineTest {

    private static final List<String> PDF_LINES = List.of(
            "Field Operations Handbook",
            "",
            "Section 4. Equipment checks.",
            "The turbine inspection interval is 380 operating hours.",
            "Inspections must be logged by the on-site engineer within 24 hours.",
            "",
            "Section 5. Site access.",
            "Visitors require a level 2 badge and a signed escort form.");

    @TempDir
    Path uploadDirectory;

    private File pdfFile;

    @BeforeEach
    void createPdfAndClearIndex() throws Exception {
        pdfFile = uploadDirectory.resolve("handbook.pdf").toFile();
        writePdf(pdfFile);

        AIService service = AIService.getInstance();
        for (DocumentEntry existing : service.getKnowledgeBase().getAllDocuments()) {
            service.removeDocument(existing.getId());
        }
    }

    private static void writePdf(File target) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.setLeading(16);
                content.newLineAtOffset(60, 700);
                for (String line : PDF_LINES) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }

            document.save(target);
        }
    }

    @Test
    void aPdfUploadIsParsedIndexedPersistedAndRetrievable() {
        // 1. Parse - the same call the Admin panel upload makes.
        DocumentEntry document = assertDoesNotThrow(() -> FileUtils.createDocumentEntry(pdfFile));

        assertEquals(DocumentEntry.DocumentType.PDF, document.getType());
        assertTrue(document.getContent().contains("380 operating hours"),
                "PDF text extraction should recover the document body");

        // 2. Index.
        AIService.getInstance().addDocument(document);

        assertTrue(document.isIndexed(), "the Admin panel status column reads this");
        assertTrue(document.getChunkCount() > 0, "the Admin panel chunk count reads this");

        // 3. Persist, then reload the way a restart would.
        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(
                ConfigurationManager.getInstance().getIndexPath()).load(384);

        assertFalse(restored.isEmpty(), "the upload should have been written to disk");
        assertTrue(restored.documents().stream().anyMatch(d -> "handbook.pdf".equals(d.getFileName())));

        // 4. Ask a question only the PDF can answer, against the reloaded index.
        EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        List<EmbeddingMatch<TextSegment>> matches = AIService.retrieveWithFallback(
                restored.embeddingStore(),
                embeddingModel.embed("How often should the turbine be inspected?").content(),
                3,
                ConfigurationManager.getInstance().getSimilarityThreshold());

        assertFalse(matches.isEmpty(), "the question must retrieve context, not fall through empty");
        assertTrue(matches.stream().anyMatch(m -> m.embedded().text().contains("380 operating hours")),
                "the retrieved context has to contain the answer the model will be asked to use");
    }
}
