package com.example.aidocumentmanager.common;

/**
 * Renders a byte count for display. Document sizes and JVM memory readings were
 * each formatted by their own identical copy of this before it was pulled out.
 */
public class ByteFormat {

    public static String format(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
