package com.example.aidocumentmanager.ai;

import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.config.ConfigurationManager;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Builds the chat model for the configured provider, and wraps it in an
 * Assistant. Both providers are reached through the OpenAI client; Hugging Face
 * exposes an OpenAI-compatible router endpoint.
 */
class ChatModelFactory {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    /**
     * Build the model for the configured provider, or return null when no API
     * key is set, which is what puts the application into demo mode.
     */
    static ChatLanguageModel createChatModel() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        String provider = config.getAIProvider();

        if ("huggingface".equalsIgnoreCase(provider)) {
            String apiKey = config.getHuggingFaceApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Hugging Face API Key not found. Using mock responses.");
                return null;
            }
            String modelName = config.getHuggingFaceModel();
            if (modelName == null || modelName.trim().isEmpty() || "gpt2".equalsIgnoreCase(modelName.trim())) {
                modelName = "meta-llama/Llama-3.1-8B-Instruct";
            }
            return OpenAiChatModel.builder()
                    .baseUrl("https://router.huggingface.co/v1")
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();
        }

        String apiKey = config.getOpenAIApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("OpenAI API Key not found. Using mock responses.");
            return null;
        }
        String modelName = config.getOpenAIModel();
        if (modelName == null || modelName.trim().isEmpty() || "gpt-3.5-turbo".equals(modelName.trim())) {
            modelName = "gpt-4o-mini";
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /**
     * Wrap a model in an Assistant that reports generation failures as an answer
     * rather than throwing, or fall back to the demo assistant when there is no
     * model to wrap.
     */
    static Assistant createAssistant(ChatLanguageModel chatModel) {
        if (chatModel == null) {
            return new MockAssistant();
        }

        return message -> {
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
    }
}
