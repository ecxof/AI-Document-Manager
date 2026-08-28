package com.example.aidocumentmanager.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentEntryTest {

    @Test
    void constructorDerivesTypeAndTitleFromFileName() {
        DocumentEntry entry = new DocumentEntry("report.pdf", "/tmp/report.pdf", "content");
        assertEquals(DocumentEntry.DocumentType.PDF, entry.getType());
        assertEquals("report", entry.getTitle());
        assertFalse(entry.isIndexed());
    }

    @Test
    void documentTypeIsResolvedFromExtension() {
        assertEquals(DocumentEntry.DocumentType.PDF, DocumentEntry.DocumentType.fromFileName("a.pdf"));
        assertEquals(DocumentEntry.DocumentType.DOCX, DocumentEntry.DocumentType.fromFileName("a.docx"));
        assertEquals(DocumentEntry.DocumentType.DOC, DocumentEntry.DocumentType.fromFileName("a.doc"));
        assertEquals(DocumentEntry.DocumentType.TXT, DocumentEntry.DocumentType.fromFileName("a.txt"));
        assertEquals(DocumentEntry.DocumentType.UNKNOWN, DocumentEntry.DocumentType.fromFileName("a.py"));
    }

    @Test
    void formattedFileSizeUsesHumanReadableUnits() {
        DocumentEntry entry = new DocumentEntry("a.txt", "/tmp/a.txt", "x");
        entry.setFileSize(2048);
        // Decimal separator is locale-dependent (String.format), so normalize it.
        assertEquals("2.0 KB", entry.getFormattedFileSize().replace(',', '.'));
    }
}
