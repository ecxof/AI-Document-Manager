package com.example.insightbaseai.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration manager for application settings
 * Handles loading, saving, and managing application configuration
 */
public class ConfigurationManager {

    private static volatile ConfigurationManager instance;
    private static final String CONFIG_DIR = ".insightbaseai";
    private static final String CONFIG_FILE = "config.properties";
    private static final String DOCUMENTS_DIR = "documents";
    private static final String LOGS_DIR = "logs";

    private final Properties properties;
    private final Path configPath;
    private final Path documentsPath;
    private final Path logsPath;
    private final LoggerUtil logger = LoggerUtil.getInstance();

    private ConfigurationManager() throws IOException {
        this.properties = new Properties();

        // Initialize paths
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path appDir = userHome.resolve(CONFIG_DIR);

        this.configPath = appDir.resolve(CONFIG_FILE);
        this.documentsPath = appDir.resolve(DOCUMENTS_DIR);
        this.logsPath = appDir.resolve(LOGS_DIR);

        // Create directories if they don't exist
        Files.createDirectories(appDir);
        Files.createDirectories(documentsPath);
        Files.createDirectories(logsPath);

        // Load existing configuration
        loadConfiguration();

        // Set default values if not present
        setDefaultConfiguration();

        // Save configuration to ensure all defaults are persisted
        saveConfiguration();
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    try {
                        instance = new ConfigurationManager();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to initialize configuration manager", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Load configuration from file
     */
    private void loadConfiguration() {
        try {
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    properties.load(input);
                }
                logger.info("Configuration loaded from: " + configPath);
            } else {
                logger.info("No existing configuration found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            ErrorHandler.getInstance().handleException("Configuration Loading", e);
        }
    }

    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        try {
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "InsightBase AI Configuration");
            }
            logger.info("Configuration saved to: " + configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
            ErrorHandler.getInstance().handleConfigurationError("Save Configuration", e.getMessage());
        }
    }

    /**
     * Set default configuration values
     */
    private void setDefaultConfiguration() {
        // API Configuration
        setDefaultProperty("openai.api.key", "");
        setDefaultProperty("openai.model", "gpt-4o-mini");
        setDefaultProperty("openai.temperature", "0.7");
        setDefaultProperty("openai.max.tokens", "2000");

        // Hugging Face Configuration
        setDefaultProperty("huggingface.api.key", "");
        setDefaultProperty("huggingface.model", "meta-llama/Llama-3.1-8B-Instruct");
        setDefaultProperty("ai.provider", "openai");

        // Application Settings
        setDefaultProperty("app.theme", "light");
        setDefaultProperty("app.max.chat.history", "10");
        setDefaultProperty("app.enable.logging", "true");
        setDefaultProperty("app.auto.save", "true");
        setDefaultProperty("app.language", "en");

        // RAG Settings
        setDefaultProperty("rag.chunk.size", "500");
        setDefaultProperty("rag.chunk.overlap", "100");
        setDefaultProperty("rag.max.retrieval.results", "3");
        setDefaultProperty("rag.similarity.threshold", "0.7");

        // File Processing Settings
        setDefaultProperty("file.max.size.mb", "50");
        setDefaultProperty("file.supported.formats", "txt,pdf,docx,md,java,xml,json,yml,yaml,properties");

        // Window Settings
        setDefaultProperty("window.width", "1200");
        setDefaultProperty("window.height", "800");
        setDefaultProperty("window.maximized", "false");

        // Performance Settings
        setDefaultProperty("performance.thread.pool.size", "4");
        setDefaultProperty("performance.cache.size", "100");
    }

    /**
     * Set default property if not already set
     */
    private void setDefaultProperty(String key, String defaultValue) {
        if (!properties.containsKey(key)) {
            properties.setProperty(key, defaultValue);
        }
    }

    // Configuration Getters

    public String getOpenAIApiKey() {
        return getProperty("openai.api.key", "");
    }

    public void setOpenAIApiKey(String apiKey) {
        setProperty("openai.api.key", apiKey != null ? apiKey : "");
    }

    public String getOpenAIModel() {
        String model = getProperty("openai.model", "gpt-4o-mini");
        if (model == null || model.trim().isEmpty()) {
            return "gpt-4o-mini";
        }
        return model.trim();
    }

    public void setOpenAIModel(String model) {
        setProperty("openai.model", model);
    }

    public String getHuggingFaceApiKey() {
        return getProperty("huggingface.api.key", "");
    }

    public void setHuggingFaceApiKey(String apiKey) {
        setProperty("huggingface.api.key", apiKey != null ? apiKey : "");
    }

    public String getHuggingFaceModel() {
        String model = getProperty("huggingface.model", "meta-llama/Llama-3.1-8B-Instruct");
        if (model == null || model.trim().isEmpty()) {
            return "meta-llama/Llama-3.1-8B-Instruct";
        }
        return model.trim();
    }

    public void setHuggingFaceModel(String model) {
        setProperty("huggingface.model", model);
    }

    public String getAIProvider() {
        return getProperty("ai.provider", "openai");
    }

    public void setAIProvider(String provider) {
        setProperty("ai.provider", provider);
    }

    public double getTemperature() {
        return getDoubleProperty("openai.temperature", 0.7);
    }

    public void setTemperature(double temperature) {
        setProperty("openai.temperature", String.valueOf(temperature));
    }

    public int getMaxTokens() {
        return getIntProperty("openai.max.tokens", 2000);
    }

    public void setMaxTokens(int maxTokens) {
        setProperty("openai.max.tokens", String.valueOf(maxTokens));
    }

    public String getTheme() {
        return getProperty("app.theme", "light");
    }

    public void setTheme(String theme) {
        setProperty("app.theme", theme);
    }

    public int getMaxChatHistory() {
        return getIntProperty("app.max.chat.history", 10);
    }

    public void setMaxChatHistory(int maxHistory) {
        setProperty("app.max.chat.history", String.valueOf(maxHistory));
    }

    public boolean isLoggingEnabled() {
        return getBooleanProperty("app.enable.logging", true);
    }

    public void setLoggingEnabled(boolean enabled) {
        setProperty("app.enable.logging", String.valueOf(enabled));
    }

    public boolean isAutoSaveEnabled() {
        return getBooleanProperty("app.auto.save", true);
    }

    public void setAutoSaveEnabled(boolean enabled) {
        setProperty("app.auto.save", String.valueOf(enabled));
    }

    public int getChunkSize() {
        return getIntProperty("rag.chunk.size", 500);
    }

    public void setChunkSize(int chunkSize) {
        setProperty("rag.chunk.size", String.valueOf(chunkSize));
    }

    public int getChunkOverlap() {
        return getIntProperty("rag.chunk.overlap", 100);
    }

    public void setChunkOverlap(int overlap) {
        setProperty("rag.chunk.overlap", String.valueOf(overlap));
    }

    public int getMaxRetrievalResults() {
        return getIntProperty("rag.max.retrieval.results", 3);
    }

    public void setMaxRetrievalResults(int maxResults) {
        setProperty("rag.max.retrieval.results", String.valueOf(maxResults));
    }

    public double getSimilarityThreshold() {
        return getDoubleProperty("rag.similarity.threshold", 0.7);
    }

    public void setSimilarityThreshold(double threshold) {
        setProperty("rag.similarity.threshold", String.valueOf(threshold));
    }

    public int getMaxFileSizeMB() {
        return getIntProperty("file.max.size.mb", 50);
    }

    public void setMaxFileSizeMB(int maxSize) {
        setProperty("file.max.size.mb", String.valueOf(maxSize));
    }

    public int getWindowWidth() {
        return getIntProperty("window.width", 1200);
    }

    public void setWindowWidth(int width) {
        setProperty("window.width", String.valueOf(width));
    }

    public int getWindowHeight() {
        return getIntProperty("window.height", 800);
    }

    public void setWindowHeight(int height) {
        setProperty("window.height", String.valueOf(height));
    }

    public boolean isWindowMaximized() {
        return getBooleanProperty("window.maximized", false);
    }

    public void setWindowMaximized(boolean maximized) {
        setProperty("window.maximized", String.valueOf(maximized));
    }

    // Path Getters

    public Path getDocumentsPath() {
        return documentsPath;
    }

    public Path getLogsPath() {
        return logsPath;
    }

    public Path getConfigPath() {
        return configPath;
    }

    // Utility Methods

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        if (isAutoSaveEnabled()) {
            saveConfiguration();
        }
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer property: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    public double getDoubleProperty(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid double property: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Reset configuration to defaults
     */
    public void resetToDefaults() {
        properties.clear();
        setDefaultConfiguration();
        saveConfiguration();
        logger.info("Configuration reset to defaults");
    }

    /**
     * Export configuration to file
     */
    public void exportConfiguration(Path exportPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(exportPath)) {
            properties.store(output, "InsightBase AI Configuration Export");
        }
        logger.info("Configuration exported to: " + exportPath);
    }

    /**
     * Import configuration from file
     */
    public void importConfiguration(Path importPath) throws IOException {
        Properties importedProps = new Properties();
        try (InputStream input = Files.newInputStream(importPath)) {
            importedProps.load(input);
        }

        // Merge imported properties
        for (String key : importedProps.stringPropertyNames()) {
            properties.setProperty(key, importedProps.getProperty(key));
        }

        saveConfiguration();
        logger.info("Configuration imported from: " + importPath);
    }

    /**
     * Validate configuration
     */
    public boolean validateConfiguration() {
        boolean valid = true;

        // Validate API key format
        String apiKey = getOpenAIApiKey();
        if (!apiKey.isEmpty() && (!apiKey.startsWith("sk-") || apiKey.length() < 50)) {
            logger.warn("Invalid OpenAI API key format");
            valid = false;
        }

        // Validate numeric ranges
        if (getChunkSize() < 100 || getChunkSize() > 2000) {
            logger.warn("Chunk size out of recommended range (100-2000)");
            valid = false;
        }

        if (getChunkOverlap() < 0 || getChunkOverlap() >= getChunkSize()) {
            logger.warn("Invalid chunk overlap value");
            valid = false;
        }

        if (getMaxRetrievalResults() < 1 || getMaxRetrievalResults() > 10) {
            logger.warn("Max retrieval results out of range (1-10)");
            valid = false;
        }

        return valid;
    }

    /**
     * Get system information for diagnostics
     */
    public String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("InsightBase AI Configuration\n");
        info.append("============================\n\n");

        info.append("System Information:\n");
        info.append("- Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("- Operating System: ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append("\n");
        info.append("- User Home: ").append(System.getProperty("user.home")).append("\n");
        info.append("- Available Memory: ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append(" MB\n");

        info.append("\nApplication Paths:\n");
        info.append("- Config Path: ").append(configPath).append("\n");
        info.append("- Documents Path: ").append(documentsPath).append("\n");
        info.append("- Logs Path: ").append(logsPath).append("\n");

        info.append("\nConfiguration Summary:\n");
        info.append("- Model: ").append(getOpenAIModel()).append("\n");
        info.append("- Theme: ").append(getTheme()).append("\n");
        info.append("- Chunk Size: ").append(getChunkSize()).append("\n");
        info.append("- Max Chat History: ").append(getMaxChatHistory()).append("\n");
        info.append("- Logging Enabled: ").append(isLoggingEnabled()).append("\n");

        return info.toString();
    }
}
