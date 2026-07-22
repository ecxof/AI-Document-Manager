package com.example.insightbaseai.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @Test
    void supportedFilesAreDetectedCaseInsensitively() {
        assertTrue(FileUtils.isSupportedFile("notes.txt"));
        assertTrue(FileUtils.isSupportedFile("REPORT.PDF"));
        assertTrue(FileUtils.isSupportedFile("Model.DOCX"));
        assertFalse(FileUtils.isSupportedFile("virus.exe"));
        assertFalse(FileUtils.isSupportedFile(""));
        assertFalse(FileUtils.isSupportedFile(null));
    }

    @Test
    void extractsFileExtension() {
        assertEquals(".txt", FileUtils.getFileExtension("notes.txt"));
        assertEquals(".gz", FileUtils.getFileExtension("archive.tar.gz"));
        assertEquals("", FileUtils.getFileExtension("noextension"));
        assertEquals("", FileUtils.getFileExtension(null));
    }

    @Test
    void formatsFileSizeWithUnits() {
        // Bytes have no decimal separator; the KB/MB branches use String.format,
        // whose decimal separator is locale-dependent, so normalize ',' to '.'.
        assertEquals("512 B", FileUtils.formatFileSize(512));
        assertEquals("1.0 KB", normalizeDecimal(FileUtils.formatFileSize(1024)));
        assertEquals("1.5 KB", normalizeDecimal(FileUtils.formatFileSize(1536)));
        assertEquals("1.0 MB", normalizeDecimal(FileUtils.formatFileSize(1024L * 1024)));
    }

    private static String normalizeDecimal(String formatted) {
        return formatted.replace(',', '.');
    }

    @Test
    void describesFileTypes() {
        assertEquals("PDF Document", FileUtils.getFileTypeDescription("a.pdf"));
        assertEquals("Markdown File", FileUtils.getFileTypeDescription("a.md"));
        assertEquals("Document", FileUtils.getFileTypeDescription("a.unknown"));
    }

    @Test
    void splitsTextIntoChunks() {
        assertTrue(FileUtils.splitTextIntoChunks(null, 100, 10).isEmpty());
        assertTrue(FileUtils.splitTextIntoChunks("", 100, 10).isEmpty());

        List<String> single = FileUtils.splitTextIntoChunks("short text", 100, 10);
        assertEquals(1, single.size());

        List<String> many = FileUtils.splitTextIntoChunks("a".repeat(300), 100, 20);
        assertTrue(many.size() > 1);
        for (String chunk : many) {
            assertFalse(chunk.isEmpty());
            assertTrue(chunk.length() <= 100);
        }
    }
}
