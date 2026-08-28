package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The matching rules were reachable only through a JavaFX Task before, so none
 * of this was covered.
 */
class DocumentSearchTest {

    private static DocumentEntry doc(String fileName, String content) {
        return new DocumentEntry(fileName, "/tmp/" + fileName, content);
    }

    private static DocumentSearch.Criteria criteria(String query, String mode, boolean caseSensitive,
            boolean wholeWords) {
        return new DocumentSearch.Criteria(query, mode, caseSensitive, wholeWords);
    }

    @Test
    void contentSearchIgnoresCaseUnlessAsked() {
        List<DocumentEntry> docs = List.of(doc("notes.txt", "The Quarterly Report is ready"));

        assertEquals(1, DocumentSearch.search(docs,
                criteria("quarterly", DocumentSearch.CONTENT_SEARCH, false, false),
                DocumentFilter.unfiltered()).size());

        assertEquals(0, DocumentSearch.search(docs,
                criteria("quarterly", DocumentSearch.CONTENT_SEARCH, true, false),
                DocumentFilter.unfiltered()).size());
    }

    @Test
    void filenameSearchDoesNotLookAtContent() {
        List<DocumentEntry> docs = List.of(doc("notes.txt", "budget figures"));

        assertEquals(0, DocumentSearch.search(docs,
                criteria("budget", DocumentSearch.FILENAME_SEARCH, false, false),
                DocumentFilter.unfiltered()).size());

        assertEquals(1, DocumentSearch.search(docs,
                criteria("notes", DocumentSearch.FILENAME_SEARCH, false, false),
                DocumentFilter.unfiltered()).size());
    }

    @Test
    void fullTextSearchAlsoMatchesTheTitle() {
        // The title is derived from the file name, so search on a term that is
        // in neither the content nor the extension-bearing name.
        List<DocumentEntry> docs = List.of(doc("roadmap.txt", "nothing relevant here"));

        assertEquals(1, DocumentSearch.search(docs,
                criteria("roadmap", DocumentSearch.FULL_TEXT_SEARCH, false, false),
                DocumentFilter.unfiltered()).size());
    }

    @Test
    void wholeWordsRejectsPartialMatches() {
        assertTrue(DocumentSearch.matchesText("the cat sat", "cat", false, true));
        assertFalse(DocumentSearch.matchesText("concatenate", "cat", false, true));
        // Without the whole-words option the substring is a match again.
        assertTrue(DocumentSearch.matchesText("concatenate", "cat", false, false));
    }

    @Test
    void missingTextNeverMatches() {
        assertFalse(DocumentSearch.matchesText(null, "anything", false, false));
        assertFalse(DocumentSearch.matchesText("", "anything", false, false));
    }

    @Test
    void contentSearchSkipsDocumentsWithoutContent() {
        DocumentEntry empty = doc("empty.txt", null);

        assertEquals(0, DocumentSearch.search(List.of(empty),
                criteria("anything", DocumentSearch.CONTENT_SEARCH, false, false),
                DocumentFilter.unfiltered()).size());
    }

    @Test
    void relevanceRisesWithTheNumberOfHits() {
        assertEquals("-", DocumentSearch.relevance(doc("a.txt", "text"), ""));
        assertEquals("Low", DocumentSearch.relevance(doc("a.txt", "nothing here"), "missing"));
        assertEquals("Medium", DocumentSearch.relevance(doc("a.txt", "hit hit"), "hit"));
        assertEquals("High", DocumentSearch.relevance(doc("a.txt", "hit ".repeat(6)), "hit"));
    }

    @Test
    void filtersNarrowTheResults() {
        DocumentEntry small = doc("small.txt", "match");
        small.setFileSize(500);
        DocumentEntry large = doc("large.txt", "match");
        large.setFileSize(50_000);

        List<DocumentEntry> docs = List.of(small, large);
        DocumentSearch.Criteria criteria = criteria("match", DocumentSearch.CONTENT_SEARCH, false, false);

        DocumentFilter upTo1KB = new DocumentFilter(
                DocumentFilter.ALL_TYPES, DocumentFilter.ALL_STATUSES, 0, 1024);

        List<DocumentEntry> results = DocumentSearch.search(docs, criteria, upTo1KB);
        assertEquals(1, results.size());
        assertEquals("small.txt", results.get(0).getFileName());
    }
}
