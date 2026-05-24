package com.techcorp.assistant.module03.dto;

/**
 * Response DTO for the chat endpoint.
 *
 * @param response The AI-generated response (potentially tool-augmented).
 */
public record ChatResponse(String response) {
}
