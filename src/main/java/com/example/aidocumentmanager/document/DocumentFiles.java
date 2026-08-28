package com.example.aidocumentmanager.document;

import com.example.aidocumentmanager.domain.DocumentEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns files on disk into the application's own representation of a document,
 * and owns the application data directory those files are copied into.
 */
public class DocumentFiles {

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

        if (!SupportedFileTypes.isSupportedFile(file.getName())) {
            throw new IOException("Unsupported file format: " + file.getName());
        }

        String content = DocumentTextExtractor.extractTextFromFile(file);
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
     * Create the application data directory if it is not there yet
     */
    public static Path ensureDataDirectory(String dirName) throws IOException {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path appDir = userHome.resolve(".aidocumentmanager").resolve(dirName);

        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
        }

        return appDir;
    }

    /**
     * Get estimated processing time based on file size and type
     */
    public static String getEstimatedProcessingTime(File file) {
        try {
            long size = file.length();
            String extension = SupportedFileTypes.getFileExtension(file.getName()).toLowerCase();

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
}
