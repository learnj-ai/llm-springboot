package com.techcorp.assistant.module03.controller;

import com.techcorp.assistant.module03.dto.ChatRequest;
import com.techcorp.assistant.module03.dto.ChatResponse;
import com.techcorp.assistant.module03.service.ToolOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI assistant with tool support.
 *
 * Provides endpoints for chat interactions where the LLM can
 * autonomously use tools to access databases and external APIs.
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

    private final ToolOrchestrator toolOrchestrator;

    public AssistantController(ToolOrchestrator toolOrchestrator) {
        this.toolOrchestrator = toolOrchestrator;
    }

    /**
     * Chat endpoint with automatic tool execution.
     *
     * The LLM will automatically invoke tools (CustomerDataTool, WeatherTool)
     * when needed to answer the user's question.
     *
     * Example queries:
     * - "What is customer 12345's email?"
     * - "Show me all open support tickets"
     * - "What's the weather in Boston?"
     * - "What open tickets does customer 12345 have?"
     *
     * @param request The user's chat message
     * @return AI-generated response, potentially augmented with tool data
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.message());

        try {
            String response = toolOrchestrator.processRequest(request.message());
            return ResponseEntity.ok(new ChatResponse(response));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("An error occurred processing your request"));
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Module 03: Tools & MCP - OK");
    }
}
