package com.example.aidocumentmanager.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportedFileTypesTest {

    @Test
    void supportedFilesAreDetectedCaseInsensitively() {
        assertTrue(SupportedFileTypes.isSupportedFile("notes.txt"));
        assertTrue(SupportedFileTypes.isSupportedFile("REPORT.PDF"));
        assertTrue(SupportedFileTypes.isSupportedFile("Model.DOCX"));
        assertFalse(SupportedFileTypes.isSupportedFile("virus.exe"));
        assertFalse(SupportedFileTypes.isSupportedFile(""));
        assertFalse(SupportedFileTypes.isSupportedFile(null));
    }

    @Test
    void extractsFileExtension() {
        assertEquals(".txt", SupportedFileTypes.getFileExtension("notes.txt"));
        assertEquals(".gz", SupportedFileTypes.getFileExtension("archive.tar.gz"));
        assertEquals("", SupportedFileTypes.getFileExtension("noextension"));
        assertEquals("", SupportedFileTypes.getFileExtension(null));
    }

    @Test
    void describesFileTypes() {
        assertEquals("PDF Document", SupportedFileTypes.getFileTypeDescription("a.pdf"));
        assertEquals("Markdown File", SupportedFileTypes.getFileTypeDescription("a.md"));
        assertEquals("Document", SupportedFileTypes.getFileTypeDescription("a.unknown"));
    }
}
