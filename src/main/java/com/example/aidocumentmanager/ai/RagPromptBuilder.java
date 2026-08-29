package com.example.aidocumentmanager.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * Assembles the grounded prompt: the retrieved excerpts, then the question.
 */
class RagPromptBuilder {

    /**
     * Prepend the retrieved excerpts to the question. The instruction to admit
     * when the answer is absent is what makes an off-topic excerpt harmless,
     * which is why retrieval falls back to the closest chunks rather than
     * returning nothing.
     */
    static String build(String userQuestion, List<EmbeddingMatch<TextSegment>> matches) {
        StringBuilder context = new StringBuilder();
        context.append("The following are the most relevant excerpts from the provided documents:\n\n");
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            context.append("--- Excerpt ").append(i + 1)
                    .append(" (relevance: ").append(String.format("%.2f", match.score())).append(") ---\n")
                    .append(match.embedded().text()).append("\n\n");
        }

        return "You are an AI assistant. Use the following context from the user's documents to answer the question accurately and concisely. "
                + "If the answer is not found in the context, say so clearly.\n\n"
                + context
                + "User Question: " + userQuestion;
    }
}
