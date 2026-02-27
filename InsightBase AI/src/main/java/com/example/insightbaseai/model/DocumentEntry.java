package com.example.insightbaseai.model;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentEntry {
    private String id;
    private String fileName;
    private String filePath;
    private String title;
    private String content;
    private DocumentType type;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private LocalDateTime lastModified;
    private String hash; // For duplicate detection
    private boolean isIndexed;
    private List<String> tags;
    private String category;
    private int chunkCount; // Number of chunks created from this document
    
    public enum DocumentType {
        PDF(".pdf"),
        DOCX(".docx"),
        DOC(".doc"),
        TXT(".txt"),
        UNKNOWN("");
        
        private final String extension;
        
        DocumentType(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() { return extension; }
        
        public static DocumentType fromFileName(String fileName) {
            String lower = fileName.toLowerCase();
            for (DocumentType type : values()) {
                if (lower.endsWith(type.getExtension()) && !type.equals(UNKNOWN)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
    
    public DocumentEntry() {
        this.uploadedAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.id = generateId();
        this.isIndexed = false;
    }
    
    public DocumentEntry(String fileName, String filePath, String content) {
        this();
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = content;
        this.type = DocumentType.fromFileName(fileName);
        this.title = extractTitleFromFileName(fileName);
    }
    
    private String generateId() {
        return "doc_" + System.currentTimeMillis() + "_" + hashCode();
    }
    
    private String extractTitleFromFileName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { 
        this.fileName = fileName;
        this.type = DocumentType.fromFileName(fileName);
        if (this.title == null || this.title.isEmpty()) {
            this.title = extractTitleFromFileName(fileName);
        }
    }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    public boolean isIndexed() { return isIndexed; }
    public void setIndexed(boolean indexed) { this.isIndexed = indexed; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    public String toString() {
        return String.format("Document{id='%s', title='%s', type=%s, size=%s, indexed=%s}", 
                           id, title, type, getFormattedFileSize(), isIndexed);
    }
}