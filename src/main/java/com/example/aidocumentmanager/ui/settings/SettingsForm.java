package com.example.aidocumentmanager.ui.settings;

import com.example.aidocumentmanager.config.ConfigurationManager;
import com.example.aidocumentmanager.ui.ThemeManager;

/**
 * The values behind the Settings panel, separated from the controls that show
 * them.
 *
 * <p>
 * The rules worth getting right live here rather than in the controller: which
 * model names are superseded, how the provider is spelled in the config file
 * versus the combo box, and what the defaults are.
 */
public record SettingsForm(
        String provider,
        String openAIApiKey,
        String openAIModel,
        String huggingFaceApiKey,
        String huggingFaceModel,
        int maxChatHistory,
        boolean loggingEnabled,
        boolean autoSaveEnabled,
        String theme,
        int chunkSize,
        int chunkOverlap,
        int maxRetrievalResults,
        double similarityThreshold) {

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_HUGGINGFACE = "huggingface";

    public static final String DISPLAY_OPENAI = "OpenAI";
    public static final String DISPLAY_HUGGINGFACE = "Hugging Face";

    public static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    public static final String DEFAULT_HUGGINGFACE_MODEL = "meta-llama/Llama-3.1-8B-Instruct";

    // Superseded defaults. Both shipped as defaults once and are now too weak to
    // be worth honouring, so a config still carrying either is treated as unset.
    private static final String SUPERSEDED_OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String SUPERSEDED_HUGGINGFACE_MODEL = "gpt2";

    /** What a fresh installation starts with. */
    public static SettingsForm defaults() {
        return new SettingsForm(
                PROVIDER_OPENAI, "", DEFAULT_OPENAI_MODEL, "", DEFAULT_HUGGINGFACE_MODEL,
                10, true, true, ThemeManager.THEME_SYSTEM,
                500, 100, 3, 0.5);
    }

    /** Read the currently saved settings. */
    public static SettingsForm from(ConfigurationManager config) {
        return new SettingsForm(
                config.getAIProvider(),
                config.getOpenAIApiKey(),
                config.getOpenAIModel(),
                config.getHuggingFaceApiKey(),
                config.getHuggingFaceModel(),
                config.getMaxChatHistory(),
                config.isLoggingEnabled(),
                config.isAutoSaveEnabled(),
                config.getTheme(),
                config.getChunkSize(),
                config.getChunkOverlap(),
                config.getMaxRetrievalResults(),
                config.getSimilarityThreshold());
    }

    /**
     * Replace blank or superseded model names with the current defaults, and
     * round the similarity threshold to the two decimals the slider shows.
     */
    public SettingsForm normalized() {
        return new SettingsForm(
                provider,
                openAIApiKey,
                resolveModel(openAIModel, SUPERSEDED_OPENAI_MODEL, DEFAULT_OPENAI_MODEL),
                huggingFaceApiKey,
                resolveModel(huggingFaceModel, SUPERSEDED_HUGGINGFACE_MODEL, DEFAULT_HUGGINGFACE_MODEL),
                maxChatHistory,
                loggingEnabled,
                autoSaveEnabled,
                theme,
                chunkSize,
                chunkOverlap,
                maxRetrievalResults,
                Math.round(similarityThreshold * 100.0) / 100.0);
    }

    private static String resolveModel(String configured, String superseded, String replacement) {
        if (configured == null || configured.trim().isEmpty()
                || superseded.equalsIgnoreCase(configured.trim())) {
            return replacement;
        }
        return configured;
    }

    /** Save these settings. */
    public void applyTo(ConfigurationManager config) {
        config.setAIProvider(provider);
        config.setOpenAIApiKey(openAIApiKey);
        config.setOpenAIModel(openAIModel);
        config.setHuggingFaceApiKey(huggingFaceApiKey);
        config.setHuggingFaceModel(huggingFaceModel);

        config.setMaxChatHistory(maxChatHistory);
        config.setLoggingEnabled(loggingEnabled);
        config.setAutoSaveEnabled(autoSaveEnabled);
        config.setTheme(theme);

        config.setChunkSize(chunkSize);
        config.setChunkOverlap(chunkOverlap);
        config.setMaxRetrievalResults(maxRetrievalResults);
        config.setSimilarityThreshold(similarityThreshold);
    }

    /** "Hugging Face" as typed in the combo box becomes "huggingface" on disk. */
    public static String providerKey(String displayName) {
        return displayName == null ? PROVIDER_OPENAI : displayName.toLowerCase().replace(" ", "");
    }

    public static String providerDisplayName(String provider) {
        return PROVIDER_OPENAI.equalsIgnoreCase(provider) ? DISPLAY_OPENAI : DISPLAY_HUGGINGFACE;
    }
}
