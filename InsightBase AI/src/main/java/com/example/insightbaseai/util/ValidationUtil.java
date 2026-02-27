package com.example.insightbaseai.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    // Common validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._\\-\\s()]+$");

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "^sk-([a-zA-Z0-9_-]+)?[a-zA-Z0-9]{20,}$");

    /**
     * Validate if string is not null or empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Validate if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate filename format
     */
    public static boolean isValidFileName(String fileName) {
        if (isEmpty(fileName)) {
            return false;
        }

        // Check for invalid characters and length
        return fileName.length() <= 255 &&
                FILENAME_PATTERN.matcher(fileName).matches() &&
                !fileName.equals(".") &&
                !fileName.equals("..");
    }

    /**
     * Validate OpenAI API key format
     */
    public static boolean isValidOpenAIApiKey(String apiKey) {
        return isNotEmpty(apiKey) && API_KEY_PATTERN.matcher(apiKey).matches();
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength == 0;
        }

        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate numeric range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validate numeric range for long values
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * Validate if value is positive
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }

    /**
     * Validate if value is positive for long values
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }

    /**
     * Validate if value is non-negative
     */
    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    /**
     * Validate if value is non-negative for long values
     */
    public static boolean isNonNegative(long value) {
        return value >= 0;
    }

    /**
     * Sanitize input string by removing potentially harmful characters
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("[<>\"'&]", "") // Remove potential XSS characters
                .replaceAll("\\p{Cntrl}", "") // Remove control characters
                .trim();
    }

    /**
     * Validate document content
     */
    public static boolean isValidDocumentContent(String content) {
        if (isEmpty(content)) {
            return false;
        }

        // Check if content has minimum meaningful length
        String trimmed = content.trim();
        return trimmed.length() >= 10 && trimmed.length() <= 10_000_000; // 10MB text limit
    }

    /**
     * Validate category name
     */
    public static boolean isValidCategoryName(String category) {
        if (isEmpty(category)) {
            return false;
        }

        return category.length() <= 50 &&
                Pattern.matches("^[a-zA-Z0-9\\s\\-_]+$", category);
    }

    /**
     * Validate tag name
     */
    public static boolean isValidTagName(String tag) {
        if (isEmpty(tag)) {
            return false;
        }

        return tag.length() <= 30 &&
                Pattern.matches("^[a-zA-Z0-9\\-_]+$", tag);
    }

    /**
     * Validate document title
     */
    public static boolean isValidDocumentTitle(String title) {
        if (isEmpty(title)) {
            return false;
        }

        return isValidLength(title, 1, 200) &&
                !title.matches(".*[<>\"'&].*");
    }

    /**
     * Validate knowledge base name
     */
    public static boolean isValidKnowledgeBaseName(String name) {
        if (isEmpty(name)) {
            return false;
        }

        return isValidLength(name, 3, 100) &&
                Pattern.matches("^[a-zA-Z0-9\\s\\-_.]+$", name);
    }

    /**
     * Validate description
     */
    public static boolean isValidDescription(String description) {
        if (description == null) {
            return true; // Description can be null
        }

        return description.length() <= 1000;
    }

    /**
     * Validate chat message
     */
    public static boolean isValidChatMessage(String message) {
        if (isEmpty(message)) {
            return false;
        }

        return isValidLength(message, 1, 4000); // Reasonable chat message limit
    }

    /**
     * Validate search query
     */
    public static boolean isValidSearchQuery(String query) {
        if (isEmpty(query)) {
            return false;
        }

        return isValidLength(query.trim(), 1, 500);
    }

    /**
     * Validate file size
     */
    public static boolean isValidFileSize(long sizeBytes) {
        return isInRange(sizeBytes, 1, 10 * 1024 * 1024); // 1 byte to 10MB
    }

    /**
     * Get validation error message for common validation failures
     */
    public static String getValidationErrorMessage(String fieldName, String value, String validationType) {
        switch (validationType.toLowerCase()) {
            case "required" -> {
                return fieldName + " is required and cannot be empty.";
            }
            case "email" -> {
                return fieldName + " must be a valid email address.";
            }
            case "filename" -> {
                return fieldName + " contains invalid characters or is too long.";
            }
            case "apikey" -> {
                return fieldName + " must be a valid OpenAI API key (starts with 'sk-' and 48 characters long).";
            }
            case "length" -> {
                return fieldName + " length is not within the allowed range.";
            }
            case "filesize" -> {
                return "File size must be between 1 byte and 10MB.";
            }
            default -> {
                return fieldName + " is not valid.";
            }
        }
    }

    /**
     * Comprehensive validation for file upload
     */
    public static ValidationResult validateFileUpload(String fileName, long fileSize, String content) {
        ValidationResult result = new ValidationResult();

        if (!isValidFileName(fileName)) {
            result.addError("Invalid file name: " + fileName);
        }

        if (!isValidFileSize(fileSize)) {
            result.addError("Invalid file size: " + FileUtils.formatFileSize(fileSize));
        }

        if (!FileUtils.isSupportedFile(fileName)) {
            result.addError("Unsupported file format: " + FileUtils.getFileExtension(fileName));
        }

        if (!isValidDocumentContent(content)) {
            result.addError("Document content is invalid or too short/long");
        }

        return result;
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}