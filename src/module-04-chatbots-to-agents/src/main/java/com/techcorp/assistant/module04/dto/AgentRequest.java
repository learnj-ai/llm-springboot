package com.techcorp.assistant.module04.dto;

/**
 * Request DTO for agent endpoints.
 *
 * @param message User's message or task
 * @param sessionId Optional session ID for stateful conversations
 * @param mode Agent mode: "react", "multiagent", or "decompose"
 */
public record AgentRequest(
        String message,
        String sessionId,
        String mode
) {
    public AgentRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (mode == null || mode.isBlank()) {
            mode = "react"; // default mode
        }
    }

    public AgentRequest(String message) {
        this(message, null, "react");
    }
}
