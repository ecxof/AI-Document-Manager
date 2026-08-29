package com.example.aidocumentmanager.document;

import com.example.aidocumentmanager.common.LoggerUtil;

import org.apache.commons.io.IOUtils;

// PDFBox 3.x
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

// Apache POI - DOCX
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Pulls plain text out of the document formats the application accepts.
 */
public class DocumentTextExtractor {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    /**
     * Extract text content from various document types
     */
    public static String extractTextFromFile(File file) throws IOException {
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File does not exist or cannot be read: " + file.getAbsolutePath());
        }

        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".java") ||
                fileName.endsWith(".xml") || fileName.endsWith(".json") || fileName.endsWith(".yml") ||
                fileName.endsWith(".yaml") || fileName.endsWith(".properties")) {
            return extractFromTXT(file);
        } else if (fileName.endsWith(".pdf")) {
            return extractFromPDF(file);
        } else if (fileName.endsWith(".docx")) {
            return extractFromDOCX(file);
        } else {
            throw new IOException("Unsupported file format: " + fileName +
                    ". " + SupportedFileTypes.SUPPORTED_FORMATS_DESCRIPTION);
        }
    }

    /**
     * Extract text from TXT files
     */
    private static String extractFromTXT(File file) throws IOException {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback to system default encoding
            try (FileInputStream fis = new FileInputStream(file)) {
                return IOUtils.toString(fis, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("Failed to extract text from TXT: " + file.getName(), ex);
                throw new IOException("Failed to process TXT file: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Extract text from PDF files using Apache PDFBox 3.x
     */
    private static String extractFromPDF(File file) throws IOException {
        logger.info("Extracting text from PDF: " + file.getName());
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.getNumberOfPages() == 0) {
                logger.warn("PDF has no pages: " + file.getName());
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            logger.info("Extracting text from " + pageCount + " pages in PDF: " + file.getName());

            String text = stripper.getText(document);
            logger.info("Finished PDF extraction for: " + file.getName());

            if (text == null || text.trim().isEmpty()) {
                logger.warn("No text could be extracted from PDF (may be image-only): " + file.getName());
                throw new IOException(
                        "No text could be extracted from '" + file.getName() + "'. "
                                + "The PDF may contain only scanned images. "
                                + "Please use a text-based or OCR-processed PDF.");
            }

            logger.info("Successfully extracted " + text.length() + " characters from PDF: " + file.getName());
            return text;
        } catch (IOException e) {
            // Re-throw IOExceptions we throw ourselves, or genuine read errors
            throw e;
        } catch (Exception e) {
            logger.error("Failed to extract text from PDF: " + file.getName(), e);
            throw new IOException("Failed to process PDF file '" + file.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from DOCX files using Apache POI
     */
    private static String extractFromDOCX(File file) throws IOException {
        logger.info("Extracting text from DOCX: " + file.getName());
        try (FileInputStream fis = new FileInputStream(file);
                XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();

            // Extract text from all paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isEmpty()) {
                    sb.append(paraText).append("\n");
                }
            }

            // Extract text from all tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isEmpty()) {
                            sb.append(cellText).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }

            String result = sb.toString();
            if (result.trim().isEmpty()) {
                logger.warn("No text could be extracted from DOCX: " + file.getName());
                throw new IOException(
                        "No text could be extracted from '" + file.getName() + "'. "
                                + "The document may be empty or contain only non-text content.");
            }

            logger.info("Successfully extracted " + result.length() + " characters from DOCX: " + file.getName());
            return result;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to extract text from DOCX: " + file.getName(), e);
            throw new IOException("Failed to process DOCX file '" + file.getName() + "': " + e.getMessage(), e);
        }
    }
}
