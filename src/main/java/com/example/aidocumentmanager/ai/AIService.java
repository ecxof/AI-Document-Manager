package com.example.aidocumentmanager.ai;

import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.common.ValidationUtil;
import com.example.aidocumentmanager.config.ConfigurationManager;
import com.example.aidocumentmanager.domain.ChatMessage;
import com.example.aidocumentmanager.domain.DocumentEntry;
import com.example.aidocumentmanager.domain.KnowledgeBase;
import com.example.aidocumentmanager.storage.DocumentIndexStore;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The application's entry point into retrieval-augmented chat: it owns the
 * knowledge base and drives the embedding index, the prompt builder, the
 * assistant, the chat history, and the statistics.
 */
public class AIService {

    private static final LoggerUtil logger = LoggerUtil.getInstance();
    private static AIService instance;

    private Assistant assistant;
    private ChatLanguageModel chatModel;
    private final KnowledgeBase knowledgeBase;

    private final EmbeddingIndex embeddingIndex;
    private final ChatHistory chatHistory = new ChatHistory();
    private final ServiceStatistics statistics = new ServiceStatistics();

    private AIService() {
        this.knowledgeBase = new KnowledgeBase("Default Knowledge Base",
                "Default knowledge base for AI Document Manager");

        this.embeddingIndex = new EmbeddingIndex();
        initializeModels();

        // After the statistics are constructed, which would otherwise reset the
        // counts of whatever was reloaded from the previous session.
        restoreIndex();

        logger.logAIService("Initialized", "AIService initialized successfully with RAG support");
    }

    public static AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    /**
     * Reload the documents and embeddings written by the previous session, so an
     * uploaded document does not have to be uploaded again after a restart.
     */
    private void restoreIndex() {
        DocumentIndexStore.Snapshot snapshot = embeddingIndex.restore();
        if (snapshot.isEmpty()) {
            return;
        }

        int totalChunks = 0;
        for (DocumentEntry document : snapshot.documents()) {
            knowledgeBase.addDocument(document);
            totalChunks += document.getChunkCount();
        }

        statistics.restored(snapshot.documents().size(), totalChunks);

        logger.logAIService("Index Restored",
                snapshot.documents().size() + " documents (" + totalChunks + " chunks) reloaded from disk");
    }

    private void initializeModels() {
        try {
            this.chatModel = ChatModelFactory.createChatModel();
            buildAssistant();
        } catch (Exception e) {
            logger.error("Failed to initialize AI models", e);
            throw new RuntimeException("Failed to initialize AI Service", e);
        }
    }

    private void buildAssistant() {
        this.assistant = ChatModelFactory.createAssistant(chatModel);
    }

    // =========================================================================
    // Chat / Response
    // =========================================================================

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

            // Build the prompt with RAG context
            String prompt = buildRagPrompt(message);

            // Get AI response
            String response;
            if (assistant != null) {
                response = assistant.chat(prompt);
            } else {
                ConfigurationManager config = ConfigurationManager.getInstance();
                String provider = config.getAIProvider();
                String providerName = "openai".equalsIgnoreCase(provider) ? "OpenAI" : "Hugging Face";
                response = "AI service is not properly configured. Please check your " + providerName + " API key.";
            }

            // Log AI response
            ChatMessage aiMessage = new ChatMessage(response, ChatMessage.MessageType.AI);
            chatHistory.add(aiMessage);

            logger.logPerformance("Chat Response", statistics.queryCompleted(startTime));

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

    /**
     * Retrieve relevant context chunks from the embedding index and prepend them
     * to the user message to form a grounded RAG prompt. Falls back to the bare
     * question whenever retrieval is unavailable or turns up nothing.
     */
    private String buildRagPrompt(String userQuestion) {
        if (!embeddingIndex.isUsable() || knowledgeBase.isEmpty()) {
            return userQuestion;
        }

        try {
            logger.info("Performing vector similarity search for: "
                    + userQuestion.substring(0, Math.min(60, userQuestion.length())));

            ConfigurationManager config = ConfigurationManager.getInstance();
            int maxResults = Math.max(1, config.getMaxRetrievalResults());
            double minScore = Math.max(0.0, Math.min(1.0, config.getSimilarityThreshold()));

            List<EmbeddingMatch<TextSegment>> matches = embeddingIndex.retrieve(userQuestion, maxResults, minScore);

            if (matches.isEmpty()) {
                logger.info("Knowledge base returned no chunks at all. Answering without RAG context.");
                return userQuestion;
            }

            logger.info("Injecting " + matches.size() + " context chunks into prompt (top score: "
                    + String.format("%.3f", matches.get(0).score()) + ").");

            return RagPromptBuilder.build(userQuestion, matches);

        } catch (Exception e) {
            logger.error("RAG retrieval failed, falling back to no-context response.", e);
            return userQuestion;
        }
    }

    // =========================================================================
    // Document Management
    // =========================================================================

    public void addDocument(DocumentEntry document) {
        if (document == null || document.getContent() == null || document.getContent().trim().isEmpty()) {
            logger.warn("Attempted to add null or empty document");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            // Add to knowledge base
            knowledgeBase.addDocument(document);

            // Split into chunks and embed
            int chunkCount;
            if (embeddingIndex.isUsable()) {
                chunkCount = embeddingIndex.embedDocument(document);
            } else {
                // Fallback: paragraph-based chunk count without embedding
                String[] paragraphs = document.getContent().split("\n\n");
                chunkCount = Math.max(1, paragraphs.length);
            }

            document.setChunkCount(chunkCount);
            document.setIndexed(true);

            statistics.documentAdded(chunkCount);

            embeddingIndex.persist(knowledgeBase.getAllDocuments());

            logger.logDocumentProcessing(document.getFileName(), "Added and indexed (" + chunkCount + " chunks)");
            logger.logPerformance("Document Indexing + Embedding", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Failed to add document: " + document.getFileName(), e);
            document.setIndexed(false);
        }
    }

    public void removeDocument(String documentId) {
        DocumentEntry document = knowledgeBase.getDocument(documentId);
        if (document == null) {
            return;
        }

        knowledgeBase.removeDocument(documentId);
        int removedChunks = embeddingIndex.removeDocument(document);

        logger.logDocumentProcessing(document.getFileName(),
                "Removed from knowledge base (" + removedChunks + " chunks dropped from the vector store)");

        statistics.documentRemoved(document.getChunkCount());

        embeddingIndex.persist(knowledgeBase.getAllDocuments());
    }

    public List<DocumentEntry> searchDocuments(String query) {
        if (!ValidationUtil.isValidSearchQuery(query)) {
            return new ArrayList<>();
        }
        return knowledgeBase.searchDocuments(query);
    }

    // =========================================================================
    // Utility
    // =========================================================================

    public List<ChatMessage> getChatHistory() {
        return chatHistory.all();
    }

    public List<ChatMessage> getRecentChatHistory(int limit) {
        return chatHistory.recent(limit);
    }

    public void clearChatHistory() {
        chatHistory.clear();
        buildAssistant();
        logger.logUserAction("Clear History", "Chat history cleared");
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> currentStats = statistics.snapshot();
        currentStats.put("knowledgeBaseStats", knowledgeBase.getDetailedStatistics());
        currentStats.put("chatHistorySize", chatHistory.size());
        currentStats.put("ragEnabled", embeddingIndex.isModelAvailable());
        return currentStats;
    }

    public boolean isConfigured() {
        return chatModel != null;
    }

    public String getConfigurationStatus() {
        if (chatModel != null) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            String provider = config.getAIProvider();
            String providerName = "openai".equalsIgnoreCase(provider) ? "OpenAI" : "Hugging Face";
            return "Fully configured with " + providerName + " API" + (embeddingIndex.isModelAvailable() ? " + RAG" : "");
        } else {
            return "Running in demo mode - AI API key not configured";
        }
    }

    public void refreshConfiguration() {
        logger.logAIService("Configuration Refresh", "Refreshing AI service configuration");
        initializeModels();
        // Pick up any changes to the RAG chunk size / overlap for documents added
        // from now on (already-embedded documents keep their original chunking).
        embeddingIndex.refreshSplitter();
        logger.logAIService("Configuration Refresh", "AI service configuration refreshed successfully");
    }
}
