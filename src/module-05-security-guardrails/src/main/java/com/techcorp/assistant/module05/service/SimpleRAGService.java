package com.techcorp.assistant.module05.service;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simplified RAG service for security demonstration.
 * In production, this would integrate with vector stores and hybrid search.
 */
@Service
public class SimpleRAGService {

    private static final Logger log = LoggerFactory.getLogger(SimpleRAGService.class);
    private final ChatModel chatModel;

    public SimpleRAGService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public RAGResponse query(String query, String userId, List<String> userRoles, String department) {
        log.debug("Processing RAG query for user: {}, roles: {}, dept: {}", userId, userRoles, department);

        // Simulate document retrieval
        List<RetrievedDocument> documents = retrieveDocuments(query);

        // Build context from documents
        String context = buildContext(documents);

        // Generate response using LLM
        String prompt = buildPrompt(query, context);
        String response = chatModel.chat(prompt);

        log.debug("Generated response: {}", response);

        return new RAGResponse(response, documents);
    }

    private List<RetrievedDocument> retrieveDocuments(String query) {
        // Simplified retrieval - in production would use vector search
        return List.of(
                new RetrievedDocument(
                        "doc1",
                        "Our product offers enterprise-grade security features including encryption at rest and in transit.",
                        0.95,
                        new DocumentMetadata(null, null)
                ),
                new RetrievedDocument(
                        "doc2",
                        "Customer support is available 24/7 via phone, email, and live chat.",
                        0.87,
                        new DocumentMetadata("support", null)
                )
        );
    }

    private String buildContext(List<RetrievedDocument> documents) {
        StringBuilder context = new StringBuilder();
        for (RetrievedDocument doc : documents) {
            context.append(doc.content()).append("\n\n");
        }
        return context.toString();
    }

    private String buildPrompt(String query, String context) {
        return """
                Context:
                %s

                Question: %s

                Instructions: Answer the question based only on the context provided.
                If the answer is not in the context, say "I don't have enough information to answer that."
                """.formatted(context, query);
    }

    public record RAGResponse(String response, List<RetrievedDocument> sourceDocuments) {}

    public record RetrievedDocument(
            String id,
            String content,
            double score,
            DocumentMetadata metadata
    ) {}

    public record DocumentMetadata(String department, String requiredRole) {}
}
