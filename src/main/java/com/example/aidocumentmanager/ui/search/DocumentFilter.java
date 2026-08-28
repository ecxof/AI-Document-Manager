package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;

/**
 * The advanced filters applied on top of a text search: file type, index
 * status, and a size range in bytes.
 */
public record DocumentFilter(String fileType, String status, long minBytes, long maxBytes) {

    public static final String ALL_TYPES = "All Types";
    public static final String ALL_STATUSES = "All Statuses";

    /** Lets everything through. */
    public static DocumentFilter unfiltered() {
        return new DocumentFilter(ALL_TYPES, ALL_STATUSES, 0, Long.MAX_VALUE);
    }

    public boolean matches(DocumentEntry doc) {
        // File type filter
        if (!ALL_TYPES.equals(fileType)) {
            String docType = doc.getType() != null ? doc.getType().toString().toUpperCase() : "";
            if (!docType.equalsIgnoreCase(fileType))
                return false;
        }

        // Status filter
        if (!ALL_STATUSES.equals(status)) {
            boolean indexed = doc.isIndexed();
            if ("INDEXED".equals(status) && !indexed)
                return false;
            if ("NOT INDEXED".equals(status) && indexed)
                return false;
        }

        // Size filter
        long fileSizeBytes = doc.getFileSize();
        if (fileSizeBytes < minBytes || fileSizeBytes > maxBytes)
            return false;

        return true;
    }

    /** Describes only the filters that are actually narrowing anything. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (!ALL_TYPES.equals(fileType))
            sb.append("Type=").append(fileType).append(" ");
        if (!ALL_STATUSES.equals(status))
            sb.append("Status=").append(status).append(" ");
        if (minBytes > 0)
            sb.append("MinSize=").append(minBytes / 1024).append("KB ");
        if (maxBytes < Long.MAX_VALUE)
            sb.append("MaxSize=").append(maxBytes / 1024).append("KB ");
        return sb.length() == 0 ? "none" : sb.toString().trim();
    }
}
