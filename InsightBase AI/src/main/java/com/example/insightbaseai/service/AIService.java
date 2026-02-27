package com.example.insightbaseai.service;

import com.example.insightbaseai.model.ChatMessage;
import com.example.insightbaseai.model.DocumentEntry;
import com.example.insightbaseai.model.KnowledgeBase;
import com.example.insightbaseai.util.ConfigurationManager;
import com.example.insightbaseai.util.LoggerUtil;
import com.example.insightbaseai.util.ValidationUtil; // This import is used and should not be commented out

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.ArrayList; // Added as it's used for ArrayList
import java.util.HashMap; // Added as it's used for HashMap
import java.util.List; // Added as it's used for List
import java.util.Map; // Added as it's used for Map
import java.util.Random; // Added as it's used in MockAssistant
import java.util.concurrent.ConcurrentHashMap; // Added as it's used for ConcurrentHashMap

public class AIService {

    private static final LoggerUtil logger = LoggerUtil.getInstance();
    private static AIService instance;

    public interface Assistant {
        String chat(String message);
    }

    private Assistant assistant;
    private ChatLanguageModel chatModel;
    private KnowledgeBase knowledgeBase;
    // private ChatMemory chatMemory;

    // Statistics and monitoring
    private final Map<String, Object> statistics;
    private final List<ChatMessage> chatHistory;

    private AIService() {
        this.statistics = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
        this.knowledgeBase = new KnowledgeBase("Default Knowledge Base", "Default knowledge base for InsightBase AI");

        initializeModels();
        initializeStatistics();

        logger.logAIService("Initialized", "AIService initialized successfully");
    }

    /**
     * Get singleton instance of AIService
     */
    public static AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    private void initializeModels() {
        try {
            // Initialize chat model using ConfigurationManager
            ConfigurationManager config = ConfigurationManager.getInstance();
            String provider = config.getAIProvider();

            if ("huggingface".equalsIgnoreCase(provider)) {
                String apiKey = config.getHuggingFaceApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    logger.warn("Hugging Face API Key not found. Using mock responses.");
                    this.chatModel = null;
                } else {
                    String modelName = config.getHuggingFaceModel();
                    if (modelName == null || modelName.trim().isEmpty() || "gpt2".equalsIgnoreCase(modelName.trim())) {
                        modelName = "meta-llama/Llama-3.1-8B-Instruct";
                    }
                    // Hugging Face inference API is OpenAI-compatible
                    this.chatModel = OpenAiChatModel.builder()
                            .baseUrl("https://router.huggingface.co/v1")
                            .apiKey(apiKey)
                            .modelName(modelName)
                            .build();
                }
            } else {
                // Default to OpenAI
                String apiKey = config.getOpenAIApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    logger.warn("OpenAI API Key not found. Using mock responses.");
                    this.chatModel = null;
                } else {
                    String modelName = config.getOpenAIModel();
                    if (modelName == null || modelName.trim().isEmpty() || "gpt-3.5-turbo".equals(modelName.trim())) {
                        modelName = "gpt-4o-mini";
                    }
                    this.chatModel = OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .modelName(modelName)
                            .build();
                }
            }

            /*
             * // Initialize chat memory
             * this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);
             */

            // Build AI assistant
            buildAssistant();

        } catch (Exception e) {
            logger.error("Failed to initialize AI models", e);
            throw new RuntimeException("Failed to initialize AI Service", e);
        }
    }

    private void buildAssistant() {
        if (chatModel != null) {
            this.assistant = message -> {
                try {
                    return chatModel.generate(message);
                } catch (Exception e) {
                    logger.error("AI Generation failed", e);
                    ConfigurationManager config = ConfigurationManager.getInstance();
                    String provider = config.getAIProvider();
                    String model = "openai".equalsIgnoreCase(provider) ? config.getOpenAIModel()
                            : config.getHuggingFaceModel();
                    return "Sorry, I encountered an error while processing your message (provider=" + provider
                            + ", model=" + model + "): " + e.getMessage();
                }
            };
        } else {
            // Mock assistant for when API key is not available
            this.assistant = new MockAssistant();
        }
    }

    private void initializeStatistics() {
        statistics.put("totalQueries", 0);
        statistics.put("totalDocuments", 0);
        statistics.put("totalChunks", 0);
        statistics.put("averageResponseTime", 0.0);
        statistics.put("lastQueryTime", 0L);
    }

    public String getResponse(String message) {
        if (!ValidationUtil.isValidChatMessage(message)) {
            return "Please enter a valid message.";
        }

        long startTime = System.currentTimeMillis();

        try {
            // Log user message
            ChatMessage userMessage = new ChatMessage(message, ChatMessage.MessageType.USER);
            chatHistory.add(userMessage);
            logger.logUserAction("Chat Message", "User sent: " + message.substring(0, Math.min(50, message.length())));

            // Get AI response
            String response;
            if (assistant != null) {
                response = assistant.chat(message);
            } else {
                ConfigurationManager config = ConfigurationManager.getInstance();
                String provider = config.getAIProvider();
                String providerName = "openai".equalsIgnoreCase(provider) ? "OpenAI" : "Hugging Face";
                response = "AI service is not properly configured. Please check your " + providerName + " API key.";
            }

            // Log AI response
            ChatMessage aiMessage = new ChatMessage(response, ChatMessage.MessageType.AI);
            chatHistory.add(aiMessage);

            // Update statistics
            updateStatistics(startTime);

            return response;

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            logger.error("Error processing chat message: " + errorMsg, e);

            if (errorMsg != null && (errorMsg.contains("401") || errorMsg.contains("API key"))) {
                return "Error: Invalid or missing API key. Please check your settings.";
            } else if (errorMsg != null && errorMsg.contains("404")) {
                return "Error: The selected model was not found. Please try a different model in settings.";
            } else if (errorMsg != null && errorMsg.contains("quota")) {
                return "Error: OpenAI quota exceeded. Please check your billing.";
            }

            return "I'm sorry, I encountered an error while processing your message: " +
                    (errorMsg != null ? errorMsg : "Unknown error") + ". Please try again.";
        }
    }

    public void addDocument(DocumentEntry document) {
        if (document == null || document.getContent() == null || document.getContent().trim().isEmpty()) {
            logger.warn("Attempted to add null or empty document");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            // Add to knowledge base
            knowledgeBase.addDocument(document);

            // Simple chunking - split by paragraphs
            String content = document.getContent();
            String[] paragraphs = content.split("\n\n");
            int chunkCount = Math.max(1, paragraphs.length);

            // Update document with chunk count
            document.setChunkCount(chunkCount);
            document.setIndexed(true);

            // Update statistics
            statistics.put("totalDocuments", (Integer) statistics.get("totalDocuments") + 1);
            statistics.put("totalChunks", (Integer) statistics.get("totalChunks") + chunkCount);

            logger.logDocumentProcessing(document.getFileName(), "Added and indexed");
            logger.logPerformance("Document Indexing", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Failed to add document: " + document.getFileName(), e);
            document.setIndexed(false);
        }
    }

    public void removeDocument(String documentId) {
        DocumentEntry document = knowledgeBase.getDocument(documentId);
        if (document != null) {
            knowledgeBase.removeDocument(documentId);

            // Note: InMemoryEmbeddingStore doesn't support removal by metadata
            // In a production system, you'd want a more sophisticated embedding store
            logger.logDocumentProcessing(document.getFileName(), "Removed from knowledge base");

            // Update statistics
            statistics.put("totalDocuments", Math.max(0, (Integer) statistics.get("totalDocuments") - 1));
        }
    }

    public List<DocumentEntry> searchDocuments(String query) {
        if (!ValidationUtil.isValidSearchQuery(query)) {
            return new ArrayList<>();
        }

        return knowledgeBase.searchDocuments(query);
    }

    public List<ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    public List<ChatMessage> getRecentChatHistory(int limit) {
        int size = chatHistory.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(chatHistory.subList(fromIndex, size));
    }

    public void clearChatHistory() {
        chatHistory.clear();
        // this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        buildAssistant();
        logger.logUserAction("Clear History", "Chat history cleared");
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> currentStats = new HashMap<>(statistics);
        currentStats.put("knowledgeBaseStats", knowledgeBase.getDetailedStatistics());
        currentStats.put("chatHistorySize", chatHistory.size());
        return currentStats;
    }

    private void updateStatistics(long startTime) {
        int totalQueries = (Integer) statistics.get("totalQueries") + 1;
        long responseTime = System.currentTimeMillis() - startTime;
        double avgResponseTime = (Double) statistics.get("averageResponseTime");

        // Update average response time
        avgResponseTime = (avgResponseTime * (totalQueries - 1) + responseTime) / totalQueries;

        statistics.put("totalQueries", totalQueries);
        statistics.put("averageResponseTime", avgResponseTime);
        statistics.put("lastQueryTime", responseTime);

        logger.logPerformance("Chat Response", responseTime);
    }

    public boolean isConfigured() {
        return chatModel != null;
    }

    public String getConfigurationStatus() {
        if (chatModel != null) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            String provider = config.getAIProvider();
            String providerName = "openai".equalsIgnoreCase(provider) ? "OpenAI" : "Hugging Face";
            return "Fully configured with " + providerName + " API";
        } else {
            return "Running in demo mode - AI API key not configured";
        }
    }

    /**
     * Reinitialize the AI models with updated configuration
     * Call this method after updating the API key in settings
     */
    public void refreshConfiguration() {
        logger.logAIService("Configuration Refresh", "Refreshing AI service configuration");
        initializeModels();
        logger.logAIService("Configuration Refresh", "AI service configuration refreshed successfully");
    }

    // Mock assistant for when API key is not available
    private static class MockAssistant implements Assistant {
        private final Random random = new Random();

        private final String[] mockResponses = {
                "I'd be happy to help with that! However, I need an OpenAI API key to provide intelligent responses. Please configure your API key in the settings.",
                "This is a demonstration response. To get real AI-powered answers, please set up your OpenAI API key.",
                "I understand your question, but I'm running in demo mode. Configure the OpenAI API key to unlock full functionality.",
                "That's an interesting question! For actual AI responses based on your documents, please add your OpenAI API key to the settings."
        };

        @Override
        public String chat(String message) {
            // Simple pattern matching for demo
            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
                return "Hello! I'm InsightBase AI running in demo mode. Please configure your OpenAI API key for full functionality.";
            }

            if (lowerMessage.contains("help")) {
                return "I can help you search through your documents and answer questions. To enable full AI capabilities, please set up your OpenAI API key in the settings.";
            }

            if (lowerMessage.contains("document") || lowerMessage.contains("file")) {
                return "I can work with your uploaded documents once you configure the OpenAI API key. Currently running in demonstration mode.";
            }

            // Return random mock response
            return mockResponses[random.nextInt(mockResponses.length)];
        }
    }
}
