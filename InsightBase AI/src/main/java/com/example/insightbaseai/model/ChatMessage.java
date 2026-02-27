package com.example.insightbaseai.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String id;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String source; // For AI responses, indicates document source
    
    public enum MessageType {
        USER, AI, SYSTEM
    }
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.id = generateId();
    }
    
    public ChatMessage(String content, MessageType type) {
        this();
        this.content = content;
        this.type = type;
    }
    
    public ChatMessage(String content, MessageType type, String source) {
        this(content, type);
        this.source = source;
    }
    
    private String generateId() {
        return "msg_" + System.currentTimeMillis() + "_" + hashCode();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp.toString(), type, content);
    }
}