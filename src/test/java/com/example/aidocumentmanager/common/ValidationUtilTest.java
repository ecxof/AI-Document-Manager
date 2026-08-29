package com.example.aidocumentmanager.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilTest {

    @Test
    void isNotEmptyDistinguishesBlankAndNull() {
        assertTrue(ValidationUtil.isNotEmpty("hello"));
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
        assertFalse(ValidationUtil.isNotEmpty(null));
    }

    @Test
    void isEmptyIsInverseOfIsNotEmpty() {
        assertTrue(ValidationUtil.isEmpty(null));
        assertTrue(ValidationUtil.isEmpty("  "));
        assertFalse(ValidationUtil.isEmpty("x"));
    }

    @Test
    void validatesEmailFormat() {
        assertTrue(ValidationUtil.isValidEmail("user@example.com"));
        assertFalse(ValidationUtil.isValidEmail("not-an-email"));
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    void validatesOpenAiApiKeyFormat() {
        assertTrue(ValidationUtil.isValidOpenAIApiKey("sk-" + "a".repeat(48)));
        assertFalse(ValidationUtil.isValidOpenAIApiKey("sk-short"));
        assertFalse(ValidationUtil.isValidOpenAIApiKey("no-prefix-1234567890"));
        assertFalse(ValidationUtil.isValidOpenAIApiKey(null));
    }

    @Test
    void validatesChatMessageAndSearchQuery() {
        assertTrue(ValidationUtil.isValidChatMessage("What is in my documents?"));
        assertFalse(ValidationUtil.isValidChatMessage(""));
        assertFalse(ValidationUtil.isValidChatMessage("   "));

        assertTrue(ValidationUtil.isValidSearchQuery("java"));
        assertFalse(ValidationUtil.isValidSearchQuery(""));
    }

    @Test
    void numericRangeHelpers() {
        assertTrue(ValidationUtil.isInRange(5, 1, 10));
        assertFalse(ValidationUtil.isInRange(11, 1, 10));
        assertTrue(ValidationUtil.isPositive(1));
        assertFalse(ValidationUtil.isPositive(0));
        assertTrue(ValidationUtil.isNonNegative(0));
        assertFalse(ValidationUtil.isNonNegative(-1));
    }

    @Test
    void fileSizeBoundsAreOneByteToTenMegabytes() {
        assertTrue(ValidationUtil.isValidFileSize(1024));
        assertFalse(ValidationUtil.isValidFileSize(0));
        assertFalse(ValidationUtil.isValidFileSize(11L * 1024 * 1024));
    }

    @Test
    void sanitizeInputStripsMarkupCharacters() {
        String cleaned = ValidationUtil.sanitizeInput("<script>alert('x')</script>");
        assertFalse(cleaned.contains("<"));
        assertFalse(cleaned.contains(">"));
        assertFalse(cleaned.contains("'"));
    }

    @Test
    void lengthValidationHandlesNull() {
        assertTrue(ValidationUtil.isValidLength("abc", 1, 5));
        assertFalse(ValidationUtil.isValidLength("abcdef", 1, 5));
        assertTrue(ValidationUtil.isValidLength(null, 0, 5));
        assertFalse(ValidationUtil.isValidLength(null, 1, 5));
    }
}
