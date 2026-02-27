package com.example.insightbaseai.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KnowledgeBase {
    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private Map<String, DocumentEntry> documents;
    private Set<String> categories;
    private Map<String, Integer> statistics;
    private boolean isActive;
    
    public KnowledgeBase() {
        this.id = generateId();
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.documents = new ConcurrentHashMap<>();
        this.categories = new HashSet<>();
        this.statistics = new ConcurrentHashMap<>();
        this.isActive = true;
        initializeStatistics();
    }
    
    public KnowledgeBase(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    private String generateId() {
        return "kb_" + System.currentTimeMillis() + "_" + hashCode();
    }
    
    private void initializeStatistics() {
        statistics.put("totalDocuments", 0);
        statistics.put("indexedDocuments", 0);
        statistics.put("totalChunks", 0);
        statistics.put("totalSize", 0);
    }
    
    public void addDocument(DocumentEntry document) {
        if (document != null && document.getId() != null) {
            documents.put(document.getId(), document);
            if (document.getCategory() != null && !document.getCategory().isEmpty()) {
                categories.add(document.getCategory());
            }
            updateStatistics();
            this.lastUpdated = LocalDateTime.now();
        }
    }
    
    public void removeDocument(String documentId) {
        DocumentEntry removed = documents.remove(documentId);
        if (removed != null) {
            updateStatistics();
            this.lastUpdated = LocalDateTime.now();
        }
    }
    
    public DocumentEntry getDocument(String documentId) {
        return documents.get(documentId);
    }
    
    public List<DocumentEntry> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }
    
    public List<DocumentEntry> getDocumentsByCategory(String category) {
        return documents.values().stream()
                .filter(doc -> category.equals(doc.getCategory()))
                .toList();
    }
    
    public List<DocumentEntry> getDocumentsByType(DocumentEntry.DocumentType type) {
        return documents.values().stream()
                .filter(doc -> type.equals(doc.getType()))
                .toList();
    }
    
    public List<DocumentEntry> searchDocuments(String query) {
        String lowerQuery = query.toLowerCase();
        return documents.values().stream()
                .filter(doc -> 
                    doc.getTitle().toLowerCase().contains(lowerQuery) ||
                    doc.getFileName().toLowerCase().contains(lowerQuery) ||
                    (doc.getContent() != null && doc.getContent().toLowerCase().contains(lowerQuery)) ||
                    (doc.getTags() != null && doc.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))))
                .toList();
    }
    
    private void updateStatistics() {
        int totalDocs = documents.size();
        int indexedDocs = (int) documents.values().stream().filter(DocumentEntry::isIndexed).count();
        int totalChunks = documents.values().stream().mapToInt(DocumentEntry::getChunkCount).sum();
        long totalSize = documents.values().stream().mapToLong(DocumentEntry::getFileSize).sum();
        
        statistics.put("totalDocuments", totalDocs);
        statistics.put("indexedDocuments", indexedDocs);
        statistics.put("totalChunks", totalChunks);
        statistics.put("totalSize", (int) (totalSize / 1024)); // Store in KB
    }
    
    public Map<String, Object> getDetailedStatistics() {
        Map<String, Object> detailed = new HashMap<>();
        detailed.put("id", id);
        detailed.put("name", name);
        detailed.put("totalDocuments", statistics.get("totalDocuments"));
        detailed.put("indexedDocuments", statistics.get("indexedDocuments"));
        detailed.put("totalChunks", statistics.get("totalChunks"));
        detailed.put("totalSizeKB", statistics.get("totalSize"));
        detailed.put("categories", new ArrayList<>(categories));
        detailed.put("createdAt", createdAt);
        detailed.put("lastUpdated", lastUpdated);
        detailed.put("isActive", isActive);
        
        // Document type breakdown
        Map<DocumentEntry.DocumentType, Long> typeCount = documents.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    DocumentEntry::getType, 
                    java.util.stream.Collectors.counting()));
        detailed.put("documentTypes", typeCount);
        
        return detailed;
    }
    
    public boolean isEmpty() {
        return documents.isEmpty();
    }
    
    public void clear() {
        documents.clear();
        categories.clear();
        initializeStatistics();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Map<String, DocumentEntry> getDocuments() { return new HashMap<>(documents); }
    
    public Set<String> getCategories() { return new HashSet<>(categories); }
    
    public Map<String, Integer> getStatistics() { return new HashMap<>(statistics); }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    @Override
    public String toString() {
        return String.format("KnowledgeBase{id='%s', name='%s', documents=%d, indexed=%d}", 
                           id, name, statistics.get("totalDocuments"), statistics.get("indexedDocuments"));
    }
}