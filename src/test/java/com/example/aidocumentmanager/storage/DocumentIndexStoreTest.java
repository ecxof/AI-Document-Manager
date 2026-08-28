package com.example.aidocumentmanager.storage;

import com.example.aidocumentmanager.ai.AIService;
import com.example.aidocumentmanager.domain.DocumentEntry;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The index has to survive a restart: a document uploaded yesterday should still
 * answer questions today, without being uploaded again.
 */
class DocumentIndexStoreTest {

    private static final String POLICY_TEXT = """
            Employees may work remotely up to three days per week. Requests must be
            approved by a direct manager at least five business days in advance.
            """;

    private static final int DIMENSION = 384;

    private static EmbeddingModel embeddingModel;

    @TempDir
    Path indexDirectory;

    private DocumentIndexStore store;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private Map<String, List<String>> embeddingIds;
    private DocumentEntry policy;

    @BeforeAll
    static void loadModel() {
        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @BeforeEach
    void indexOneDocument() {
        store = new DocumentIndexStore(indexDirectory);
        embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingIds = new HashMap<>();

        policy = new DocumentEntry("policy.pdf", "/tmp/policy.pdf", POLICY_TEXT);
        policy.setFileSize(4096);
        policy.setHash("abc123");
        policy.setIndexed(true);

        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = splitter.split(Document.from(POLICY_TEXT));
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingIds.put(policy.getId(), new ArrayList<>(embeddingStore.addAll(embeddings, segments)));
        policy.setChunkCount(segments.size());
    }

    private void save() {
        store.save(List.of(policy), embeddingIds, embeddingStore, DIMENSION);
    }

    private boolean retrievable(DocumentIndexStore.Snapshot snapshot, String question, String expectedText) {
        List<EmbeddingMatch<TextSegment>> matches = snapshot.embeddingStore()
                .search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(embeddingModel.embed(question).content())
                        .maxResults(10)
                        .minScore(0.0)
                        .build())
                .matches();

        return matches.stream().anyMatch(m -> m.embedded().text().contains(expectedText));
    }

    @Test
    void restoresDocumentsAndTheirEmbeddings() {
        save();

        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(DIMENSION);

        assertEquals(1, restored.documents().size());
        assertTrue(retrievable(restored, "How many remote days per week?", "three days per week"),
                "a restored document must still answer questions");
    }

    @Test
    void restoresDocumentMetadataUsedByTheAdminPanel() {
        save();

        DocumentEntry restored = new DocumentIndexStore(indexDirectory).load(DIMENSION).documents().get(0);

        assertEquals(policy.getId(), restored.getId());
        assertEquals("policy.pdf", restored.getFileName());
        assertEquals(DocumentEntry.DocumentType.PDF, restored.getType());
        assertEquals(4096, restored.getFileSize());
        assertEquals("abc123", restored.getHash(), "the hash has to survive, or duplicate detection breaks");
        assertTrue(restored.isIndexed());
        assertEquals(policy.getChunkCount(), restored.getChunkCount());
        assertEquals(policy.getUploadedAt(), restored.getUploadedAt());
        assertTrue(restored.getContent().contains("three days per week"),
                "content is what the Admin panel preview shows");
    }

    @Test
    void restoresTheDocumentToEmbeddingMapping() {
        save();

        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(DIMENSION);

        assertEquals(embeddingIds.get(policy.getId()), restored.embeddingIds().get(policy.getId()),
                "without the mapping, a document restored from disk could never be deleted properly");
    }

    @Test
    void deletedDocumentDoesNotComeBackAfterRestart() {
        AIService.removeEmbeddings(embeddingStore, embeddingIds, policy);
        store.save(List.of(), embeddingIds, embeddingStore, DIMENSION);

        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(DIMENSION);

        assertTrue(restored.isEmpty());
        assertFalse(retrievable(restored, "How many remote days per week?", "three days per week"),
                "a document deleted before the restart must stay deleted");
    }

    @Test
    void discardsAnIndexBuiltWithADifferentEmbeddingModel() {
        save();

        // Vectors from another model have no meaning against this one, so the
        // index is dropped rather than silently returning nonsense matches.
        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(768);

        assertTrue(restored.isEmpty());
    }

    @Test
    void survivesACorruptIndexFile() throws Exception {
        save();
        Files.writeString(store.getIndexFile(), "{ this is not valid json");

        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(DIMENSION);

        assertTrue(restored.isEmpty(), "a corrupt index should cost a re-upload, not a crash");
        assertNotNull(restored.embeddingStore(), "callers still need a usable store");
    }

    @Test
    void missingIndexLoadsEmpty() {
        DocumentIndexStore.Snapshot restored = new DocumentIndexStore(indexDirectory).load(DIMENSION);

        assertTrue(restored.isEmpty());
        assertNotNull(restored.embeddingStore());
    }

    @Test
    void savingLeavesNoTemporaryFileBehind() throws Exception {
        save();

        try (var entries = Files.list(indexDirectory)) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "the temporary file should have been moved into place");
        }
    }
}
