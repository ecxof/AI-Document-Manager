package com.example.aidocumentmanager.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Checks that a file is something the application can actually ingest, either
 * as a hard precondition or as detailed feedback for the user.
 */
public class FileValidator {

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

        String extension = SupportedFileTypes.getFileExtension(file.getName()).toLowerCase();
        if (!SupportedFileTypes.isSupportedFile(file.getName())) {
            errors.add("Unsupported file type: " + extension +
                    ". " + SupportedFileTypes.SUPPORTED_FORMATS_DESCRIPTION);
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

        if (!SupportedFileTypes.isSupportedFile(file.getName())) {
            throw new IOException(
                    "Unsupported file format. " + SupportedFileTypes.SUPPORTED_FORMATS_DESCRIPTION + ": "
                            + file.getName());
        }
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
}
