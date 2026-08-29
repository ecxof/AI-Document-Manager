package com.example.aidocumentmanager.ai;

import com.example.aidocumentmanager.domain.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation so far. Handed out as copies so callers cannot mutate it.
 */
class ChatHistory {

    private final List<ChatMessage> messages = new ArrayList<>();

    void add(ChatMessage message) {
        messages.add(message);
    }

    List<ChatMessage> all() {
        return new ArrayList<>(messages);
    }

    List<ChatMessage> recent(int limit) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    void clear() {
        messages.clear();
    }

    int size() {
        return messages.size();
    }
}
