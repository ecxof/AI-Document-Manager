package com.example.aidocumentmanager.ai;

import java.util.Random;

/**
 * Stands in for a real provider when no API key is configured, so the rest of
 * the application still works in demo mode.
 */
class MockAssistant implements Assistant {

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
