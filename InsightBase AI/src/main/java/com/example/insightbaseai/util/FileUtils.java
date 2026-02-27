package com.example.insightbaseai.util;

import com.example.insightbaseai.model.DocumentEntry;
import org.apache.commons.io.IOUtils;

// PDFBox 3.x
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

// Apache POI – DOCX
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtils {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    // Supported file extensions (enhanced with PDF and DOCX)
    private static final String[] SUPPORTED_EXTENSIONS = { ".txt", ".pdf", ".docx", ".md", ".java", ".xml", ".json",
            ".yml", ".yaml", ".properties" };

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
     * Extract text content from various document types
     */
    public static String extractTextFromFile(File file) throws IOException {
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File does not exist or cannot be read: " + file.getAbsolutePath());
        }

        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".java") ||
                fileName.endsWith(".xml") || fileName.endsWith(".json") || fileName.endsWith(".yml") ||
                fileName.endsWith(".yaml") || fileName.endsWith(".properties")) {
            return extractFromTXT(file);
        } else if (fileName.endsWith(".pdf")) {
            return extractFromPDF(file);
        } else if (fileName.endsWith(".docx")) {
            return extractFromDOCX(file);
        } else {
            throw new IOException("Unsupported file format: " + fileName +
                    ". Supported formats: TXT, PDF, DOCX, MD, Java, XML, JSON, YAML, Properties");
        }
    }

    /**
     * Extract text from TXT files
     */
    private static String extractFromTXT(File file) throws IOException {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback to system default encoding
            try (FileInputStream fis = new FileInputStream(file)) {
                return IOUtils.toString(fis, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("Failed to extract text from TXT: " + file.getName(), ex);
                throw new IOException("Failed to process TXT file: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Extract text from PDF files using Apache PDFBox 3.x
     */
    private static String extractFromPDF(File file) throws IOException {
        logger.info("Extracting text from PDF: " + file.getName());
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.getNumberOfPages() == 0) {
                logger.warn("PDF has no pages: " + file.getName());
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            logger.info("Extracting text from " + pageCount + " pages in PDF: " + file.getName());

            String text = stripper.getText(document);
            logger.info("Finished PDF extraction for: " + file.getName());

            if (text == null || text.trim().isEmpty()) {
                logger.warn("No text could be extracted from PDF (may be image-only): " + file.getName());
                throw new IOException(
                        "No text could be extracted from '" + file.getName() + "'. "
                                + "The PDF may contain only scanned images. "
                                + "Please use a text-based or OCR-processed PDF.");
            }

            logger.info("Successfully extracted " + text.length() + " characters from PDF: " + file.getName());
            return text;
        } catch (IOException e) {
            // Re-throw IOExceptions we throw ourselves, or genuine read errors
            throw e;
        } catch (Exception e) {
            logger.error("Failed to extract text from PDF: " + file.getName(), e);
            throw new IOException("Failed to process PDF file '" + file.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from DOCX files using Apache POI
     */
    private static String extractFromDOCX(File file) throws IOException {
        logger.info("Extracting text from DOCX: " + file.getName());
        try (FileInputStream fis = new FileInputStream(file);
                XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();

            // Extract text from all paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isEmpty()) {
                    sb.append(paraText).append("\n");
                }
            }

            // Extract text from all tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isEmpty()) {
                            sb.append(cellText).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }

            String result = sb.toString();
            if (result.trim().isEmpty()) {
                logger.warn("No text could be extracted from DOCX: " + file.getName());
                throw new IOException(
                        "No text could be extracted from '" + file.getName() + "'. "
                                + "The document may be empty or contain only non-text content.");
            }

            logger.info("Successfully extracted " + result.length() + " characters from DOCX: " + file.getName());
            return result;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to extract text from DOCX: " + file.getName(), e);
            throw new IOException("Failed to process DOCX file '" + file.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Calculate MD5 hash of file for duplicate detection
     */
    public static String calculateFileHash(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;

            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }

            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Create DocumentEntry from file
     */
    public static DocumentEntry createDocumentEntry(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        if (!isSupportedFile(file.getName())) {
            throw new IOException("Unsupported file format: " + file.getName());
        }

        String content = extractTextFromFile(file);
        DocumentEntry entry = new DocumentEntry(file.getName(), file.getAbsolutePath(), content);

        entry.setFileSize(file.length());
        entry.setHash(calculateFileHash(file));

        return entry;
    }

    /**
     * Copy file to application data directory
     */
    public static Path copyToDataDirectory(File sourceFile, String dataDir) throws IOException {
        Path dataPath = Paths.get(dataDir);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }

        Path targetPath = dataPath.resolve(sourceFile.getName());

        // Handle duplicate file names
        int counter = 1;
        while (Files.exists(targetPath)) {
            String name = sourceFile.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                String baseName = name.substring(0, dotIndex);
                String extension = name.substring(dotIndex);
                targetPath = dataPath.resolve(baseName + "_" + counter + extension);
            } else {
                targetPath = dataPath.resolve(name + "_" + counter);
            }
            counter++;
        }

        return Files.copy(sourceFile.toPath(), targetPath);
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
     * Format file size for display
     */
    public static String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Create application data directory if it doesn't exist
     */
    public static Path ensureDataDirectory(String dirName) throws IOException {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path appDir = userHome.resolve(".insightbaseai").resolve(dirName);

        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
        }

        return appDir;
    }

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

            // Move to next chunk with overlap
            start = Math.max(start + 1, end - overlapSize);
        }

        return chunks;
    }

    /**
     * Extract text and split into chunks for better processing
     */
    public static List<String> extractTextChunks(File file, int chunkSize, int overlapSize) throws IOException {
        String fullText = extractTextFromFile(file);
        return splitTextIntoChunks(fullText, chunkSize, overlapSize);
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
     * Get estimated processing time based on file size and type
     */
    public static String getEstimatedProcessingTime(File file) {
        try {
            long size = file.length();
            String extension = getFileExtension(file.getName()).toLowerCase();

            // Base processing time estimates (in seconds)
            long baseTime = switch (extension) {
                case ".pdf" -> size / (100 * 1024); // ~100KB per second
                case ".docx" -> size / (200 * 1024); // ~200KB per second
                default -> size / (500 * 1024); // ~500KB per second for text
            };

            if (baseTime < 1)
                return "< 1 second";
            if (baseTime < 60)
                return baseTime + " seconds";
            return (baseTime / 60) + " minutes";

        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Check if file can be processed for embeddings
     */
    public static boolean canProcessForEmbeddings(String fileName) {
        return isSupportedFile(fileName);
    }

    /**
     * Validate file with detailed feedback
     */
    public static FileValidationResult validateFileDetailed(File file) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (file == null) {
            errors.add("File is null");
            return new FileValidationResult(false, errors, warnings);
        }

        if (!file.exists()) {
            errors.add("File does not exist");
            return new FileValidationResult(false, errors, warnings);
        }

        if (!file.isFile()) {
            errors.add("Path is not a file");
            return new FileValidationResult(false, errors, warnings);
        }

        if (!file.canRead()) {
            errors.add("File is not readable");
            return new FileValidationResult(false, errors, warnings);
        }

        String extension = getFileExtension(file.getName()).toLowerCase();
        if (!isSupportedFile(file.getName())) {
            errors.add("Unsupported file type: " + extension +
                    ". Supported formats: TXT, MD, Java, XML, JSON, YAML, Properties");
            return new FileValidationResult(false, errors, warnings);
        }

        try {
            long size = file.length();
            if (size == 0) {
                warnings.add("File is empty");
            } else if (size > 50 * 1024 * 1024) { // 50MB
                warnings.add("Very large file (>50MB) - processing may be slow");
            } else if (size > 10 * 1024 * 1024) { // 10MB
                warnings.add("Large file (>10MB) - processing may take some time");
            }

            // Additional validation for specific file types
            if (extension.equals(".pdf") && size < 100) {
                warnings.add("PDF file seems very small - may be corrupted");
            } else if (extension.equals(".docx") && size < 1000) {
                warnings.add("DOCX file seems very small - may be empty or corrupted");
            }

        } catch (Exception e) {
            errors.add("Could not determine file size");
        }

        return new FileValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * File validation result with detailed feedback
     */
    public static class FileValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public FileValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public String getWarningMessage() {
            return String.join("; ", warnings);
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Validate file before processing
     */
    public static void validateFile(File file) throws IOException {
        if (file == null) {
            throw new IOException("File is null");
        }

        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IOException("File is not readable: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
            throw new IOException("Path is a directory, not a file: " + file.getAbsolutePath());
        }

        if (file.length() == 0) {
            throw new IOException("File is empty: " + file.getAbsolutePath());
        }

        if (file.length() > 10 * 1024 * 1024) { // 10MB limit for now
            throw new IOException("File is too large (>10MB): " + file.getAbsolutePath());
        }

        if (!isSupportedFile(file.getName())) {
            throw new IOException(
                    "Unsupported file format. Supported formats: TXT, MD, Java, XML, JSON, YAML, Properties (PDF and DOCX coming soon): "
                            + file.getName());
        }
    }
}