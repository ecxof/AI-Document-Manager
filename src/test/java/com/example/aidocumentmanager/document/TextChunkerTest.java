package com.example.aidocumentmanager.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitsTextIntoChunks() {
        assertTrue(TextChunker.splitTextIntoChunks(null, 100, 10).isEmpty());
        assertTrue(TextChunker.splitTextIntoChunks("", 100, 10).isEmpty());

        List<String> single = TextChunker.splitTextIntoChunks("short text", 100, 10);
        assertEquals(1, single.size());

        List<String> many = TextChunker.splitTextIntoChunks("a".repeat(300), 100, 20);
        assertTrue(many.size() > 1);
        for (String chunk : many) {
            assertFalse(chunk.isEmpty());
            assertTrue(chunk.length() <= 100);
        }
    }
}
