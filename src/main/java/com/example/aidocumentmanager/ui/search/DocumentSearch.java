package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matching documents against a query. Kept clear of JavaFX so the search rules
 * can be exercised without a scene graph.
 */
public class DocumentSearch {

    public static final String CONTENT_SEARCH = "Content Search";
    public static final String FILENAME_SEARCH = "Filename Search";
    public static final String FULL_TEXT_SEARCH = "Full Text Search";

    /**
     * What the user asked for. {@link #effectiveQuery()} is what the matchers
     * actually compare against: a case-insensitive search lowercases the query
     * once here, because the text it is matched against is lowercased too.
     */
    public record Criteria(String query, String mode, boolean caseSensitive, boolean wholeWords) {

        public String effectiveQuery() {
            return caseSensitive ? query : query.toLowerCase();
        }
    }

    /** Documents matching the query, then narrowed by the advanced filters. */
    public static List<DocumentEntry> search(List<DocumentEntry> documents, Criteria criteria, DocumentFilter filter) {
        String searchQuery = criteria.effectiveQuery();

        List<DocumentEntry> results = new ArrayList<>();
        for (DocumentEntry doc : documents) {
            // Text match
            if (!matchesCriteria(doc, searchQuery, criteria))
                continue;
            // Advanced filters
            if (!filter.matches(doc))
                continue;
            results.add(doc);
        }
        return results;
    }

    static boolean matchesCriteria(DocumentEntry document, String query, Criteria criteria) {
        boolean caseSensitive = criteria.caseSensitive();
        boolean wholeWords = criteria.wholeWords();

        switch (criteria.mode()) {
            case FILENAME_SEARCH:
                return matchesText(document.getFileName(), query, caseSensitive, wholeWords);

            case CONTENT_SEARCH:
                if (document.getContent() == null)
                    return false;
                return matchesText(document.getContent(), query, caseSensitive, wholeWords);

            case FULL_TEXT_SEARCH:
            default:
                return matchesText(document.getFileName(), query, caseSensitive, wholeWords) ||
                        matchesText(document.getTitle(), query, caseSensitive, wholeWords) ||
                        (document.getContent() != null &&
                                matchesText(document.getContent(), query, caseSensitive, wholeWords));
        }
    }

    static boolean matchesText(String text, String query, boolean caseSensitive, boolean wholeWords) {
        if (text == null || text.isEmpty())
            return false;

        String searchText = caseSensitive ? text : text.toLowerCase();

        if (wholeWords) {
            String pattern = "\\b" + Pattern.quote(query) + "\\b";
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(pattern, flags).matcher(searchText).find();
        } else {
            return searchText.contains(query);
        }
    }

    /**
     * A simple relevance score based on how many times the query terms appear in
     * the document.
     */
    public static String relevance(DocumentEntry doc, String query) {
        if (query == null || query.isBlank() || doc.getContent() == null)
            return "-";
        String content = doc.getContent().toLowerCase();
        String[] terms = query.toLowerCase().split("\\s+");
        int hits = 0;
        for (String term : terms) {
            int idx = 0;
            while ((idx = content.indexOf(term, idx)) != -1) {
                hits++;
                idx += term.length();
            }
        }
        if (hits == 0)
            return "Low";
        if (hits < 5)
            return "Medium";
        return "High";
    }
}
