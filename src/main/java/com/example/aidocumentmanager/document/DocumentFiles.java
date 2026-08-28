package com.example.aidocumentmanager.document;

import com.example.aidocumentmanager.domain.DocumentEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns a file on disk into the application's own representation of a document.
 */
public class DocumentFiles {

    /**
     * Calculate MD5 hash of file for duplicate detection
     */
    private static String calculateFileHash(File file) throws IOException {
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

        if (!SupportedFileTypes.isSupportedFile(file.getName())) {
            throw new IOException("Unsupported file format: " + file.getName());
        }

        String content = DocumentTextExtractor.extractTextFromFile(file);
        DocumentEntry entry = new DocumentEntry(file.getName(), file.getAbsolutePath(), content);

        entry.setFileSize(file.length());
        entry.setHash(calculateFileHash(file));

        return entry;
    }

}
