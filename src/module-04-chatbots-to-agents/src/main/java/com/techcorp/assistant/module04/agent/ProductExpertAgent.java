package com.techcorp.assistant.module04.agent;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Specialized agent for product-related queries.
 *
 * Handles questions about product features, capabilities, and specifications.
 */
@Component
public class ProductExpertAgent implements SpecializedAgent {

    private final ChatModel chatModel;

    public ProductExpertAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a product expert. Answer questions about:
                - Product features and capabilities
                - Feature comparisons and roadmap
                - Product specifications
                - Use cases and best practices

                User question: %s

                Provide a comprehensive response highlighting relevant features and benefits.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "ProductExpertAgent";
    }

    @Override
    public String getDescription() {
        return "Handles product features, specifications, and capability questions";
    }
}
