package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultsCsvWriterTest {

    private static DocumentEntry doc(String fileName, String content) {
        return new DocumentEntry(fileName, "/tmp/" + fileName, content);
    }

    @Test
    void quotesEveryFieldAndDoublesInternalQuotes() {
        String row = SearchResultsCsvWriter.row(doc("the \"good\" report.txt", "body"), "");

        assertTrue(row.startsWith("\"the \"\"good\"\" report.txt\","),
                "a quote in the file name must be doubled, not left to close the field early: " + row);
    }

    @Test
    void aCommaInAFieldDoesNotShiftTheColumns() {
        String row = SearchResultsCsvWriter.row(doc("a.txt", "one, two, three"), "");

        // Six quoted fields, so five separators sit outside quotes.
        assertEquals(6, row.split("\",\"").length, row);
    }

    @Test
    void newlinesAreFlattenedSoARowStaysOneLine() {
        String row = SearchResultsCsvWriter.row(doc("a.txt", "first\nsecond"), "");

        assertFalse(row.contains("\n"), row);
        assertTrue(row.contains("first second"), row);
    }

    @Test
    void previewIsTruncated() {
        String row = SearchResultsCsvWriter.row(doc("a.txt", "x".repeat(500)), "");

        assertTrue(row.contains("x".repeat(200)), "the first 200 characters should survive");
        assertFalse(row.contains("x".repeat(201)), "anything past 200 characters should be dropped");
    }

    @Test
    void nullContentBecomesAnEmptyField() {
        String row = SearchResultsCsvWriter.row(doc("a.txt", null), "");

        assertTrue(row.endsWith(",\"\""), row);
    }

    @Test
    void writesAHeaderAndOneRowPerResult() {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            SearchResultsCsvWriter.write(writer, List.of(doc("a.txt", "one"), doc("b.txt", "two")), "one");
        }

        List<String> lines = out.toString().lines().toList();
        assertEquals(3, lines.size());
        assertEquals(SearchResultsCsvWriter.HEADER, lines.get(0));
    }
}
