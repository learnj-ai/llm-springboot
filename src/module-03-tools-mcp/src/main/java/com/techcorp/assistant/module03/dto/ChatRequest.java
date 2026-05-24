package com.techcorp.assistant.module03.dto;

/**
 * Request DTO for chat endpoint.
 *
 * @param message The user's message/query
 */
public record ChatRequest(String message) {
    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
    }
}
