package com.techcorp.assistant.module06.service;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simplified RAG service for production features demonstration.
 */
@Service
public class SimpleRAGService {

    private static final Logger log = LoggerFactory.getLogger(SimpleRAGService.class);
    private final ChatModel chatModel;

    public SimpleRAGService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public RAGResponse query(String query) {
        log.debug("Processing RAG query: {}", query);

        // Simulate document retrieval
        List<String> documents = List.of(
                "Our product offers enterprise-grade security with encryption at rest and in transit.",
                "We provide 24/7 customer support via phone, email, and chat.",
                "Pricing starts at $99/month for the basic plan."
        );

        // Build context
        String context = String.join("\n\n", documents);

        // Generate response
        String prompt = buildPrompt(query, context);
        String response = chatModel.chat(prompt);

        log.debug("Generated response: {}", response);

        return new RAGResponse(response, documents);
    }

    private String buildPrompt(String query, String context) {
        return """
                Context:
                %s

                Question: %s

                Instructions: Answer based only on the context provided.
                """.formatted(context, query);
    }

    public record RAGResponse(String response, List<String> sourceDocuments) {}
}
