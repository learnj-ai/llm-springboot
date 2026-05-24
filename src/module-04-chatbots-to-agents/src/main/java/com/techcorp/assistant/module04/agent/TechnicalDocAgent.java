package com.techcorp.assistant.module04.agent;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Specialized agent for technical documentation queries.
 *
 * Handles questions about APIs, integrations, and technical documentation.
 */
@Component
public class TechnicalDocAgent implements SpecializedAgent {

    private final ChatModel chatModel;

    public TechnicalDocAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a technical documentation specialist. Answer questions about:
                - API documentation and usage
                - Integration guides
                - Technical specifications
                - Development best practices

                User question: %s

                Provide a clear, technically accurate response with examples when appropriate.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "TechnicalDocAgent";
    }

    @Override
    public String getDescription() {
        return "Handles technical documentation, API usage, and integration questions";
    }
}
