package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFilterTest {

    private static DocumentEntry doc(String fileName, long size, boolean indexed) {
        DocumentEntry entry = new DocumentEntry(fileName, "/tmp/" + fileName, "content");
        entry.setFileSize(size);
        entry.setIndexed(indexed);
        return entry;
    }

    @Test
    void unfilteredAcceptsEverything() {
        assertTrue(DocumentFilter.unfiltered().matches(doc("a.pdf", 0, false)));
        assertTrue(DocumentFilter.unfiltered().matches(doc("b.txt", Long.MAX_VALUE, true)));
    }

    @Test
    void fileTypeFilterIsExclusive() {
        DocumentFilter pdfOnly = new DocumentFilter("PDF", DocumentFilter.ALL_STATUSES, 0, Long.MAX_VALUE);

        assertTrue(pdfOnly.matches(doc("report.pdf", 100, true)));
        assertFalse(pdfOnly.matches(doc("notes.txt", 100, true)));
    }

    @Test
    void statusFilterSeparatesIndexedFromNot() {
        DocumentFilter indexed = new DocumentFilter(DocumentFilter.ALL_TYPES, "INDEXED", 0, Long.MAX_VALUE);
        DocumentFilter notIndexed = new DocumentFilter(DocumentFilter.ALL_TYPES, "NOT INDEXED", 0, Long.MAX_VALUE);

        assertTrue(indexed.matches(doc("a.txt", 10, true)));
        assertFalse(indexed.matches(doc("a.txt", 10, false)));

        assertTrue(notIndexed.matches(doc("a.txt", 10, false)));
        assertFalse(notIndexed.matches(doc("a.txt", 10, true)));
    }

    @Test
    void sizeRangeIsInclusiveAtBothEnds() {
        DocumentFilter between = new DocumentFilter(
                DocumentFilter.ALL_TYPES, DocumentFilter.ALL_STATUSES, 1000, 2000);

        assertFalse(between.matches(doc("a.txt", 999, true)));
        assertTrue(between.matches(doc("a.txt", 1000, true)));
        assertTrue(between.matches(doc("a.txt", 2000, true)));
        assertFalse(between.matches(doc("a.txt", 2001, true)));
    }

    @Test
    void describeMentionsOnlyTheNarrowingFilters() {
        assertEquals("none", DocumentFilter.unfiltered().describe());

        DocumentFilter filter = new DocumentFilter("PDF", "INDEXED", 2048, 4096);
        assertEquals("Type=PDF Status=INDEXED MinSize=2KB MaxSize=4KB", filter.describe());
    }
}
