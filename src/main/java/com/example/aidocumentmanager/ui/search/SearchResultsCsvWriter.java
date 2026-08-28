package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

import java.io.PrintWriter;
import java.util.List;

/**
 * Renders search results as CSV. Every field is quoted and internal quotes are
 * doubled, so a document whose name or content contains a comma or a quote does
 * not shift the remaining columns.
 */
public class SearchResultsCsvWriter {

    public static final String HEADER = "File Name,Type,Size,Status,Relevance,Content Preview";

    private static final int PREVIEW_LENGTH = 200;

    /** Write the header and one row per result. */
    public static void write(PrintWriter writer, List<DocumentEntry> results, String query) {
        writer.println(HEADER);
        for (DocumentEntry doc : results) {
            writer.println(row(doc, query));
        }
    }

    static String row(DocumentEntry doc, String query) {
        String name = escapeCsv(doc.getFileName());
        String type = escapeCsv(doc.getType() != null ? doc.getType().toString() : "");
        String size = escapeCsv(doc.getFormattedFileSize());
        String status = escapeCsv(doc.isIndexed() ? "INDEXED" : "NOT INDEXED");
        String relevance = escapeCsv(DocumentSearch.relevance(doc, query));
        String preview = escapeCsv(contentPreview(doc));

        return name + "," + type + "," + size + "," + status + "," + relevance + "," + preview;
    }

    private static String contentPreview(DocumentEntry doc) {
        if (doc.getContent() == null) {
            return "";
        }
        return doc.getContent()
                .substring(0, Math.min(PREVIEW_LENGTH, doc.getContent().length()))
                .replace("\n", " ");
    }

    static String escapeCsv(String value) {
        if (value == null)
            return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
