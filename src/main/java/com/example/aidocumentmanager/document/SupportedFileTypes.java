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

    /**
     * Get file type description for display
     */
    public static String getFileTypeDescription(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return switch (extension) {
            case ".pdf" -> "PDF Document";
            case ".docx" -> "Word Document";
            case ".txt" -> "Text File";
            case ".md" -> "Markdown File";
            case ".java" -> "Java Source File";
            case ".xml" -> "XML File";
            case ".json" -> "JSON File";
            case ".yml", ".yaml" -> "YAML File";
            case ".properties" -> "Properties File";
            default -> "Document";
        };
    }

    /**
     * Check if file can be processed for embeddings
     */
    public static boolean canProcessForEmbeddings(String fileName) {
        return isSupportedFile(fileName);
    }
}
