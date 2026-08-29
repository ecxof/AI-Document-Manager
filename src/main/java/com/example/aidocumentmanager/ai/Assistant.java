package com.example.aidocumentmanager.ai;

/**
 * The chat backend, whether that is a real provider or the demo stand-in.
 */
public interface Assistant {
    String chat(String message);
}
