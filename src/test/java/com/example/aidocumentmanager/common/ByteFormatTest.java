package com.example.aidocumentmanager.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteFormatTest {

    @Test
    void formatsSizeWithUnits() {
        // Bytes have no decimal separator; the KB/MB branches use String.format,
        // whose decimal separator is locale-dependent, so normalize ',' to '.'.
        assertEquals("512 B", ByteFormat.format(512));
        assertEquals("1.0 KB", normalizeDecimal(ByteFormat.format(1024)));
        assertEquals("1.5 KB", normalizeDecimal(ByteFormat.format(1536)));
        assertEquals("1.0 MB", normalizeDecimal(ByteFormat.format(1024L * 1024)));
        assertEquals("1.0 GB", normalizeDecimal(ByteFormat.format(1024L * 1024 * 1024)));
    }

    private static String normalizeDecimal(String formatted) {
        return formatted.replace(',', '.');
    }
}
