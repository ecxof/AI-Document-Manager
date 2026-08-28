package com.example.aidocumentmanager.ai;

import com.example.aidocumentmanager.domain.DocumentEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end cover for the persistence wiring in AIService: uploading a document
 * writes it to disk, and a fresh service picks it back up.
 *
 * <p>
 * A restart is simulated by clearing the singleton, which re-runs the whole
 * constructor - initialization order included. That order matters here:
 * initializeStatistics resets the counters, so restoring before it would leave a
 * restored knowledge base reporting zero documents.
 *
 * <p>
 * Tests run against a sandboxed user.home (see the surefire configuration in the
 * pom), so this never touches a real installation's index.
 */
class AIServiceRestartTest {

    private static final String POLICY_TEXT = """
            Employees may work remotely up to three days per week. Requests must be
            approved by a direct manager at least five business days in advance.
            """;

    /** Drop the singleton so the next getInstance() rebuilds it from disk. */
    private static void simulateRestart() throws Exception {
        Field instance = AIService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static DocumentEntry newDocument(String fileName) {
        DocumentEntry document = new DocumentEntry(fileName, "/tmp/" + fileName, POLICY_TEXT);
        document.setFileSize(POLICY_TEXT.length());
        document.setHash("hash-" + fileName);
        return document;
    }

    @BeforeEach
    void startFromAnEmptyIndex() throws Exception {
        simulateRestart();
        AIService service = AIService.getInstance();

        for (DocumentEntry existing : service.getKnowledgeBase().getAllDocuments()) {
            service.removeDocument(existing.getId());
        }

        assertTrue(service.getKnowledgeBase().isEmpty(), "precondition: no documents carried over");
        simulateRestart();
    }

    @Test
    void anUploadedDocumentSurvivesARestart() throws Exception {
        DocumentEntry uploaded = newDocument("policy.pdf");
        AIService.getInstance().addDocument(uploaded);
        assertTrue(uploaded.getChunkCount() > 0, "precondition: the document was chunked");

        simulateRestart();
        AIService restarted = AIService.getInstance();

        DocumentEntry restored = restarted.getKnowledgeBase().getDocument(uploaded.getId());
        assertNotNull(restored, "the document should come back after a restart");
        assertEquals("policy.pdf", restored.getFileName());
        assertEquals(uploaded.getChunkCount(), restored.getChunkCount());
        assertTrue(restored.isIndexed());
    }

    @Test
    void restoredStatisticsSurviveInitializationOrder() throws Exception {
        DocumentEntry uploaded = newDocument("policy.pdf");
        AIService.getInstance().addDocument(uploaded);

        simulateRestart();
        Map<String, Object> statistics = AIService.getInstance().getStatistics();

        assertEquals(1, statistics.get("totalDocuments"),
                "restoring before initializeStatistics would zero this out");
        assertEquals(uploaded.getChunkCount(), statistics.get("totalChunks"));
    }

    @Test
    void aDeletedDocumentStaysDeletedAfterARestart() throws Exception {
        DocumentEntry uploaded = newDocument("policy.pdf");
        AIService service = AIService.getInstance();
        service.addDocument(uploaded);
        service.removeDocument(uploaded.getId());

        simulateRestart();

        assertTrue(AIService.getInstance().getKnowledgeBase().isEmpty(),
                "a deletion has to be persisted too, not just the uploads");
    }
}
