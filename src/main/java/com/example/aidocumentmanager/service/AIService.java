package com.example.aidocumentmanager.service;

import com.example.aidocumentmanager.model.ChatMessage;
import com.example.aidocumentmanager.model.DocumentEntry;
import com.example.aidocumentmanager.model.KnowledgeBase;
import com.example.aidocumentmanager.util.ConfigurationManager;
import com.example.aidocumentmanager.util.LoggerUtil;
import com.example.aidocumentmanager.util.ValidationUtil;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AIService {

    private static final LoggerUtil logger = LoggerUtil.getInstance();
    private static AIService instance;

    public interface Assistant {
        String chat(String message);
    }

    private Assistant assistant;
    private ChatLanguageModel chatModel;
    private KnowledgeBase knowledgeBase;

    // RAG components
    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private DocumentSplitter documentSplitter;

    // Embedding store ids per document, so a deleted document's chunks can be
    // dropped from the vector store too - the store deletes by embedding id and
    // knows nothing about our documents.
    private final Map<String, List<String>> documentEmbeddingIds;

    // Statistics and monitoring
    private final Map<String, Object> statistics;
    private final List<ChatMessage> chatHistory;

    private AIService() {
        this.statistics = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
        this.documentEmbeddingIds = new ConcurrentHashMap<>();
        this.knowledgeBase = new KnowledgeBase("Default Knowledge Base", "Default knowledge base for AI Document Manager");

        initializeEmbedding();
        initializeModels();
        initializeStatistics();

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

    private void initializeEmbedding() {
        try {
            logger.info("Initializing local embedding model (All-MiniLM-L6-v2)...");
            this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            this.documentSplitter = createDocumentSplitter();
            logger.info("Embedding model initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize embedding model — RAG will be disabled.", e);
            this.embeddingModel = null;
            this.embeddingStore = null;
        }
    }

    /**
     * Build a document splitter using the chunk size / overlap configured in the
     * Settings panel, so the RAG sliders actually take effect. Falls back to sane
     * defaults if the configured values are out of range.
     */
    private DocumentSplitter createDocumentSplitter() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        // Guard against invalid combinations that would break the splitter.
        if (chunkSize < 100) {
            chunkSize = 500;
        }
        if (overlap < 0 || overlap >= chunkSize) {
            overlap = Math.min(50, chunkSize / 10);
        }

        logger.info("Configuring document splitter: chunkSize=" + chunkSize + ", overlap=" + overlap);
        return DocumentSplitters.recursive(chunkSize, overlap);
    }

    private void initializeModels() {
        try {
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
                    this.chatModel = OpenAiChatModel.builder()
                            .baseUrl("https://router.huggingface.co/v1")
                            .apiKey(apiKey)
                            .modelName(modelName)
                            .build();
                }
            } else {
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

    /**
     * Retrieve relevant context chunks from the embedding store and prepend them
     * to the user message to form a grounded RAG prompt.
     */
    private String buildRagPrompt(String userQuestion) {
        if (embeddingModel == null || embeddingStore == null || knowledgeBase.isEmpty()) {
            return userQuestion;
        }

        try {
            logger.info("Performing vector similarity search for: "
                    + userQuestion.substring(0, Math.min(60, userQuestion.length())));

            ConfigurationManager config = ConfigurationManager.getInstance();
            int maxResults = Math.max(1, config.getMaxRetrievalResults());
            double minScore = Math.max(0.0, Math.min(1.0, config.getSimilarityThreshold()));

            Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();

            List<EmbeddingMatch<TextSegment>> matches = retrieveWithFallback(embeddingStore, queryEmbedding,
                    maxResults, minScore);

            if (matches.isEmpty()) {
                logger.info("Knowledge base returned no chunks at all. Answering without RAG context.");
                return userQuestion;
            }

            StringBuilder context = new StringBuilder();
            context.append("The following are the most relevant excerpts from the provided documents:\n\n");
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                context.append("--- Excerpt ").append(i + 1)
                        .append(" (relevance: ").append(String.format("%.2f", match.score())).append(") ---\n")
                        .append(match.embedded().text()).append("\n\n");
            }

            logger.info("Injecting " + matches.size() + " context chunks into prompt (top score: "
                    + String.format("%.3f", matches.get(0).score()) + ").");

            return "You are an AI assistant. Use the following context from the user's documents to answer the question accurately and concisely. "
                    + "If the answer is not found in the context, say so clearly.\n\n"
                    + context
                    + "User Question: " + userQuestion;

        } catch (Exception e) {
            logger.error("RAG retrieval failed, falling back to no-context response.", e);
            return userQuestion;
        }
    }

    /**
     * Search the store for the chunks closest to the query, retrying without the
     * similarity threshold when it filters out everything.
     *
     * <p>
     * Note on {@code minScore}: langchain4j does not compare against raw cosine
     * similarity. It rescales with {@code (cosineSimilarity + 1) / 2}, so 0.5
     * means "cosine 0.0" and 0.7 means "cosine 0.4". On-topic questions score
     * around 0.70-0.83 against real prose, so a 0.7 threshold sits right on the
     * cliff: rephrase the question and every chunk of a correctly indexed
     * document drops out, leaving the model to answer with no context at all.
     *
     * <p>
     * Falling back to the closest chunks is the safer failure mode. The prompt
     * tells the model to say when the answer is not in the context, so an
     * off-topic excerpt costs little, while dropping the context guarantees an
     * "I don't have that information" answer about a document we did index.
     */
    static List<EmbeddingMatch<TextSegment>> retrieveWithFallback(EmbeddingStore<TextSegment> store,
            Embedding queryEmbedding, int maxResults, double minScore) {

        List<EmbeddingMatch<TextSegment>> matches = search(store, queryEmbedding, maxResults, minScore);

        if (matches.isEmpty() && minScore > 0.0) {
            logger.info("No chunks scored at or above the similarity threshold (" + minScore
                    + "). Falling back to the closest " + maxResults + " chunks.");
            matches = search(store, queryEmbedding, maxResults, 0.0);
        }

        return matches;
    }

    private static List<EmbeddingMatch<TextSegment>> search(EmbeddingStore<TextSegment> store,
            Embedding queryEmbedding, int maxResults, double minScore) {

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        return searchResult.matches();
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
            int chunkCount = 0;
            if (embeddingModel != null && embeddingStore != null) {
                chunkCount = embedDocument(document);
            } else {
                // Fallback: paragraph-based chunk count without embedding
                String[] paragraphs = document.getContent().split("\n\n");
                chunkCount = Math.max(1, paragraphs.length);
            }

            document.setChunkCount(chunkCount);
            document.setIndexed(true);

            statistics.put("totalDocuments", (Integer) statistics.get("totalDocuments") + 1);
            statistics.put("totalChunks", (Integer) statistics.get("totalChunks") + chunkCount);

            logger.logDocumentProcessing(document.getFileName(), "Added and indexed (" + chunkCount + " chunks)");
            logger.logPerformance("Document Indexing + Embedding", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Failed to add document: " + document.getFileName(), e);
            document.setIndexed(false);
        }
    }

    /**
     * Split document content into chunks and store embeddings in the vector store.
     * 
     * @return number of chunks created
     */
    private int embedDocument(DocumentEntry docEntry) {
        try {
            Document langchainDoc = Document.from(docEntry.getContent());
            List<TextSegment> segments = documentSplitter.split(langchainDoc);

            if (segments.isEmpty()) {
                logger.warn("No segments produced for document: " + docEntry.getFileName());
                return 0;
            }

            logger.info("Embedding " + segments.size() + " chunks for: " + docEntry.getFileName());

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            List<String> embeddingIds = embeddingStore.addAll(embeddings, segments);
            documentEmbeddingIds.put(docEntry.getId(), new ArrayList<>(embeddingIds));

            logger.info("Successfully embedded " + segments.size() + " chunks for: " + docEntry.getFileName());
            return segments.size();

        } catch (Exception e) {
            logger.error("Failed to embed document: " + docEntry.getFileName(), e);
            return 1; // Fallback count
        }
    }

    public void removeDocument(String documentId) {
        DocumentEntry document = knowledgeBase.getDocument(documentId);
        if (document == null) {
            return;
        }

        knowledgeBase.removeDocument(documentId);
        int removedChunks = removeEmbeddings(embeddingStore, documentEmbeddingIds, document);

        logger.logDocumentProcessing(document.getFileName(),
                "Removed from knowledge base (" + removedChunks + " chunks dropped from the vector store)");

        statistics.put("totalDocuments", Math.max(0, (Integer) statistics.get("totalDocuments") - 1));
        // Mirror addDocument, which counts the document's own chunk count - that
        // is a paragraph estimate rather than an embedding count when embedding
        // was unavailable, and the two must cancel out.
        statistics.put("totalChunks",
                Math.max(0, (Integer) statistics.get("totalChunks") - document.getChunkCount()));
    }

    /**
     * Drop a document's chunks from the vector store. Without this the chunks
     * stay searchable after the document is deleted, so the assistant keeps
     * answering from a document the user removed.
     *
     * @return number of chunks removed
     */
    static int removeEmbeddings(EmbeddingStore<TextSegment> store, Map<String, List<String>> embeddingIdsByDocument,
            DocumentEntry document) {

        List<String> embeddingIds = embeddingIdsByDocument.remove(document.getId());

        if (embeddingIds == null || embeddingIds.isEmpty()) {
            // Never embedded - the model failed to load, or embedding threw and
            // the document was indexed by paragraph count only.
            return 0;
        }

        if (store == null) {
            return 0;
        }

        try {
            store.removeAll(embeddingIds);
            return embeddingIds.size();
        } catch (Exception e) {
            logger.error("Failed to remove embeddings for: " + document.getFileName(), e);
            // Put them back so a later delete, or a store swap, can retry.
            embeddingIdsByDocument.put(document.getId(), embeddingIds);
            return 0;
        }
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
        return new ArrayList<>(chatHistory);
    }

    public List<ChatMessage> getRecentChatHistory(int limit) {
        int size = chatHistory.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(chatHistory.subList(fromIndex, size));
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
        Map<String, Object> currentStats = new HashMap<>(statistics);
        currentStats.put("knowledgeBaseStats", knowledgeBase.getDetailedStatistics());
        currentStats.put("chatHistorySize", chatHistory.size());
        currentStats.put("ragEnabled", embeddingModel != null);
        return currentStats;
    }

    private void updateStatistics(long startTime) {
        int totalQueries = (Integer) statistics.get("totalQueries") + 1;
        long responseTime = System.currentTimeMillis() - startTime;
        double avgResponseTime = (Double) statistics.get("averageResponseTime");
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
            return "Fully configured with " + providerName + " API" + (embeddingModel != null ? " + RAG" : "");
        } else {
            return "Running in demo mode - AI API key not configured";
        }
    }

    public void refreshConfiguration() {
        logger.logAIService("Configuration Refresh", "Refreshing AI service configuration");
        initializeModels();
        // Pick up any changes to the RAG chunk size / overlap for documents added
        // from now on (already-embedded documents keep their original chunking).
        if (embeddingModel != null) {
            this.documentSplitter = createDocumentSplitter();
        }
        logger.logAIService("Configuration Refresh", "AI service configuration refreshed successfully");
    }

    // =========================================================================
    // Mock Assistant
    // =========================================================================

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
            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
                return "Hello! I'm AI Document Manager running in demo mode. Please configure your OpenAI API key for full functionality.";
            }
            if (lowerMessage.contains("help")) {
                return "I can help you search through your documents and answer questions. To enable full AI capabilities, please set up your OpenAI API key in the settings.";
            }
            if (lowerMessage.contains("document") || lowerMessage.contains("file")) {
                return "I can work with your uploaded documents once you configure the OpenAI API key. Currently running in demonstration mode.";
            }

            return mockResponses[random.nextInt(mockResponses.length)];
        }
    }
}
