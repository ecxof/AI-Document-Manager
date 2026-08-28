package com.example.aidocumentmanager.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits document text into overlapping chunks. This is the fallback chunker;
 * the RAG pipeline itself splits with langchain4j's recursive splitter.
 */
public class TextChunker {

    /**
     * Clean text content for processing
     */
    public static String cleanText(String text) {
        if (text == null)
            return "";

        return text
                .replaceAll("\\s+", " ") // Replace multiple whitespace with single space
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "") // Remove control characters except newline and tab
                .trim();
    }

    /**
     * Split text into chunks for better processing
     */
    public static List<String> splitTextIntoChunks(String text, int chunkSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            // Try to end at a word boundary if possible
            if (end < textLength) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start && lastSpace < end) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // The final chunk reached the end of the text, so there is nothing
            // left to emit. Without this the loop falls back to start + 1 and
            // walks forward one character at a time, producing one chunk per
            // remaining character.
            if (end >= textLength) {
                break;
            }

            // Move to next chunk with overlap
            start = Math.max(start + 1, end - overlapSize);
        }

        return chunks;
    }

    /**
     * Extract text and split into chunks for better processing
     */
    public static List<String> extractTextChunks(File file, int chunkSize, int overlapSize) throws IOException {
        String fullText = DocumentTextExtractor.extractTextFromFile(file);
        return splitTextIntoChunks(fullText, chunkSize, overlapSize);
    }
}
