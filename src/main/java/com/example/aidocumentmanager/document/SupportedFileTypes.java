package com.example.aidocumentmanager.document;

/**
 * The file formats the application will ingest, and the naming questions that
 * follow from an extension.
 */
public class SupportedFileTypes {

    // Supported file extensions (enhanced with PDF and DOCX)
    private static final String[] SUPPORTED_EXTENSIONS = { ".txt", ".pdf", ".docx", ".md", ".java", ".xml", ".json",
            ".yml", ".yaml", ".properties" };

    /** Human-readable list of the supported formats, for error messages. */
    public static final String SUPPORTED_FORMATS_DESCRIPTION =
            "Supported formats: TXT, PDF, DOCX, MD, Java, XML, JSON, YAML, Properties";

    /**
     * Check if file extension is supported
     */
    public static boolean isSupportedFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

}
