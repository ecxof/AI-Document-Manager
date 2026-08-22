package com.example.aidocumentmanager.service;

import com.example.aidocumentmanager.model.DocumentEntry;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deleting a document has to take its chunks out of the vector store as well.
 * Otherwise the chunks stay searchable and the assistant keeps answering from a
 * document the user deleted.
 */
class EmbeddingDeletionTest {

    private static final String POLICY_TEXT = """
            Employees may work remotely up to three days per week. Requests must be
            approved by a direct manager at least five business days in advance.
            """;

    private static final String MENU_TEXT = """
            The cafeteria serves lentil soup on Tuesdays and grilled salmon on
            Fridays. Hot meals are available between 11am and 2pm.
            """;

    private static EmbeddingModel embeddingModel;

    private EmbeddingStore<TextSegment> store;
    private Map<String, List<String>> embeddingIdsByDocument;
    private DocumentEntry policy;
    private DocumentEntry menu;

    @BeforeAll
    static void loadModel() {
        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @BeforeEach
    void indexTwoDocuments() {
        store = new InMemoryEmbeddingStore<>();
        embeddingIdsByDocument = new ConcurrentHashMap<>();

        policy = new DocumentEntry("policy.pdf", "/tmp/policy.pdf", POLICY_TEXT);
        menu = new DocumentEntry("menu.pdf", "/tmp/menu.pdf", MENU_TEXT);

        index(policy);
        index(menu);
    }

    private void index(DocumentEntry document) {
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = splitter.split(Document.from(document.getContent()));
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingIdsByDocument.put(document.getId(), new ArrayList<>(store.addAll(embeddings, segments)));
    }

    private List<EmbeddingMatch<TextSegment>> search(String question) {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(question).content())
                .maxResults(10)
                .minScore(0.0)
                .build()).matches();
    }

    private boolean anyMatchContains(String question, String text) {
        return search(question).stream().anyMatch(m -> m.embedded().text().contains(text));
    }

    @Test
    void deletedDocumentStopsBeingRetrievable() {
        assertTrue(anyMatchContains("How many remote days per week?", "three days per week"),
                "precondition: the policy is retrievable before deletion");

        int removed = AIService.removeEmbeddings(store, embeddingIdsByDocument, policy);

        assertTrue(removed > 0, "deleting a document should report the chunks it dropped");
        assertFalse(anyMatchContains("How many remote days per week?", "three days per week"),
                "a deleted document must not come back as context");
    }

    @Test
    void deletingOneDocumentLeavesTheOthersIntact() {
        AIService.removeEmbeddings(store, embeddingIdsByDocument, policy);

        assertTrue(anyMatchContains("What is for lunch on Friday?", "grilled salmon"),
                "the remaining document must still be retrievable");
        assertTrue(embeddingIdsByDocument.containsKey(menu.getId()));
        assertFalse(embeddingIdsByDocument.containsKey(policy.getId()),
                "the deleted document's ids should not be tracked any more");
    }

    @Test
    void deletingTwiceIsHarmless() {
        int first = AIService.removeEmbeddings(store, embeddingIdsByDocument, policy);
        int second = AIService.removeEmbeddings(store, embeddingIdsByDocument, policy);

        assertTrue(first > 0);
        assertEquals(0, second, "a second delete has nothing left to remove");
    }

    @Test
    void neverEmbeddedDocumentIsSkipped() {
        DocumentEntry unindexed = new DocumentEntry("scan.pdf", "/tmp/scan.pdf", "");

        assertEquals(0, AIService.removeEmbeddings(store, new HashMap<>(), unindexed));
    }
}
