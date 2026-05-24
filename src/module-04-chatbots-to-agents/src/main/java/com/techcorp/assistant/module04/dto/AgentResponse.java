package com.techcorp.assistant.module04.dto;

/**
 * Response DTO for agent endpoints.
 *
 * @param response The agent's response
 * @param sessionId Session ID for continuing the conversation
 * @param mode The mode that was used
 */
public record AgentResponse(
        String response,
        String sessionId,
        String mode
) {}
