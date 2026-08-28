package com.example.aidocumentmanager.ai;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the retrieval step of the RAG pipeline: a question about an indexed
 * document has to come back with that document's text attached, otherwise the
 * model answers as though the document was never uploaded.
 */
class RagRetrievalTest {

    private static final String DOCUMENT_TEXT = """
            Employee Remote Work Policy

            Employees may work remotely up to three days per week. Requests must be
            approved by a direct manager at least five business days in advance.

            The company reimburses home office equipment up to 500 dollars per year.
            Receipts must be submitted through the expense portal within 30 days.
            """;

    private static final String QUESTION = "What does the document say about remote work days per week?";

    private static EmbeddingModel embeddingModel;
    private static EmbeddingStore<TextSegment> embeddingStore;
    private static Embedding queryEmbedding;

    @BeforeAll
    static void indexDocument() {
        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        embeddingStore = new InMemoryEmbeddingStore<>();

        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = splitter.split(Document.from(DOCUMENT_TEXT));
        assertFalse(segments.isEmpty(), "document should split into at least one chunk");

        embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
        queryEmbedding = embeddingModel.embed(QUESTION).content();
    }

    @Test
    void retrievesTheAnsweringChunkAtTheDefaultThreshold() {
        List<EmbeddingMatch<TextSegment>> matches = AIService.retrieveWithFallback(embeddingStore, queryEmbedding, 3,
                0.5);

        assertFalse(matches.isEmpty(), "a directly related question must retrieve context");
        assertTrue(matches.get(0).embedded().text().contains("three days per week"),
                "the chunk answering the question should rank first");
    }

    @Test
    void stillRetrievesContextWhenTheThresholdFiltersEverything() {
        // 0.99 stands in for any threshold set too high for the corpus - the point
        // is that the user still gets an answer grounded in their document rather
        // than a bare question sent to the model.
        List<EmbeddingMatch<TextSegment>> matches = AIService.retrieveWithFallback(embeddingStore, queryEmbedding, 3,
                0.99);

        assertFalse(matches.isEmpty(), "the fallback must return the closest chunks");
        assertTrue(matches.get(0).embedded().text().contains("three days per week"));
    }

    @Test
    void scoresAreRescaledCosineSimilarityNotRawCosine() {
        // The reason a 0.7 threshold was silently dropping every chunk: langchain4j
        // compares minScore against (cosineSimilarity + 1) / 2, so 0.7 really asks
        // for a cosine of 0.4 - a bar on-topic questions clear only narrowly.
        EmbeddingMatch<TextSegment> match = AIService
                .retrieveWithFallback(embeddingStore, queryEmbedding, 1, 0.0)
                .get(0);

        double cosine = CosineSimilarity.between(queryEmbedding,
                embeddingModel.embed(match.embedded()).content());

        assertEquals((cosine + 1) / 2, match.score(), 1e-6);
        assertTrue(cosine < 0.7, "an on-topic cosine score is nowhere near the threshold's face value");
    }
}
