package com.example.aidocumentmanager.ui.settings;

import com.example.aidocumentmanager.config.ConfigurationManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JSON-shaped settings export.
 *
 * <p>
 * The two API keys are replaced with a placeholder. The export is a file the
 * user is likely to share when asking for help, so the keys must never be in
 * it; this is the only place that decides so.
 */
public class SettingsExporter {

    public static final String REDACTED = "[REDACTED]";

    private static final String APP_VERSION = "1.0-SNAPSHOT";

    /**
     * @param exportedAt ISO timestamp to stamp into the export
     */
    public static Map<String, Object> export(ConfigurationManager config, String exportedAt) {
        Map<String, Object> settingsMap = new LinkedHashMap<>();
        settingsMap.put("exportedAt", exportedAt);
        settingsMap.put("appVersion", APP_VERSION);

        Map<String, Object> apiSection = new LinkedHashMap<>();
        apiSection.put("provider", config.getAIProvider());
        apiSection.put("openAIModel", config.getOpenAIModel());
        apiSection.put("huggingFaceModel", config.getHuggingFaceModel());
        apiSection.put("openAIApiKey", REDACTED);
        apiSection.put("huggingFaceApiKey", REDACTED);
        settingsMap.put("api", apiSection);

        Map<String, Object> appSection = new LinkedHashMap<>();
        appSection.put("maxChatHistory", config.getMaxChatHistory());
        appSection.put("theme", config.getTheme());
        appSection.put("loggingEnabled", config.isLoggingEnabled());
        appSection.put("autoSave", config.isAutoSaveEnabled());
        settingsMap.put("application", appSection);

        Map<String, Object> ragSection = new LinkedHashMap<>();
        ragSection.put("chunkSize", config.getChunkSize());
        ragSection.put("chunkOverlap", config.getChunkOverlap());
        ragSection.put("maxRetrievalResults", config.getMaxRetrievalResults());
        settingsMap.put("rag", ragSection);

        return settingsMap;
    }
}
