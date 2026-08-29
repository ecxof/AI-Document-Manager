package com.example.aidocumentmanager.document;

import java.io.File;
import java.io.IOException;

/**
 * Checks that a file is something the application can actually ingest, as a
 * precondition before it is read.
 */
public class FileValidator {

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

}
