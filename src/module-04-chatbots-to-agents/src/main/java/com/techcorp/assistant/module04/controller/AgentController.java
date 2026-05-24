package com.techcorp.assistant.module04.controller;

import com.techcorp.assistant.module04.agent.ReActAgent;
import com.techcorp.assistant.module04.dto.AgentRequest;
import com.techcorp.assistant.module04.dto.AgentResponse;
import com.techcorp.assistant.module04.memory.ConversationMemoryService;
import com.techcorp.assistant.module04.orchestrator.MultiAgentOrchestrator;
import com.techcorp.assistant.module04.service.TaskDecomposer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for agent-based interactions.
 *
 * Supports multiple agent modes:
 * - ReAct: Iterative reasoning with tool use
 * - MultiAgent: Routing to specialized agents
 * - Decompose: Breaking down complex tasks
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ReActAgent reActAgent;
    private final MultiAgentOrchestrator multiAgentOrchestrator;
    private final TaskDecomposer taskDecomposer;
    private final ConversationMemoryService memoryService;

    public AgentController(
            ReActAgent reActAgent,
            MultiAgentOrchestrator multiAgentOrchestrator,
            TaskDecomposer taskDecomposer,
            ConversationMemoryService memoryService) {
        this.reActAgent = reActAgent;
        this.multiAgentOrchestrator = multiAgentOrchestrator;
        this.taskDecomposer = taskDecomposer;
        this.memoryService = memoryService;
    }

    /**
     * Main agent execution endpoint.
     *
     * Supports stateful conversations via session management.
     * Different agent modes can be selected via the mode parameter.
     *
     * @param request Agent request with message, sessionId, and mode
     * @return Agent response
     */
    @PostMapping("/execute")
    public ResponseEntity<AgentResponse> execute(@RequestBody AgentRequest request) {
        log.info("Agent execute request - mode: {}, session: {}", request.mode(), request.sessionId());

        try {
            // Get or create session
            String sessionId = request.sessionId() != null && !request.sessionId().isBlank()
                    ? request.sessionId()
                    : UUID.randomUUID().toString();

            // Resolve prior turns for the session BEFORE we add the new user message;
            // otherwise the history we hand to the agent would include the question
            // it's about to answer (and the agent would see its own prompt twice).
            String priorTurns = renderHistory(memoryService.getHistory(sessionId));
            memoryService.addMessage(sessionId, request.message());

            // Inject the prior-turn transcript into the message so the agent has
            // conversational context. Without this, follow-ups like "what plan are
            // they on?" lose the antecedent (which customer "they" refers to) and
            // the ReAct loop spins asking for clarification.
            String messageWithHistory = priorTurns.isEmpty()
                    ? request.message()
                    : """
                            Previous conversation (prior user/assistant turns):
                            %s

                            Current user question:
                            %s
                            """.formatted(priorTurns, request.message());

            // Execute based on mode
            String response = switch (request.mode().toLowerCase()) {
                case "react" -> reActAgent.solve(messageWithHistory);
                case "multiagent" -> multiAgentOrchestrator.routeRequest(messageWithHistory);
                case "collaborative" -> multiAgentOrchestrator.collaborativeRequest(messageWithHistory);
                case "decompose" -> {
                    TaskDecomposer.TaskExecutionResult result =
                            taskDecomposer.executeComplexTask(messageWithHistory);
                    yield result.summary();
                }
                default -> "Unknown mode: " + request.mode();
            };

            // Add AI response to memory
            memoryService.addAiMessage(sessionId, response);

            return ResponseEntity.ok(new AgentResponse(response, sessionId, request.mode()));

        } catch (Exception e) {
            log.error("Error executing agent request", e);
            return ResponseEntity.internalServerError()
                    .body(new AgentResponse(
                            "An error occurred processing your request",
                            request.sessionId(),
                            request.mode()
                    ));
        }
    }

    /**
     * Clear session memory.
     *
     * @param sessionId Session to clear
     * @return Success message
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<String> clearSession(@PathVariable String sessionId) {
        log.info("Clearing session: {}", sessionId);
        memoryService.clearMemory(sessionId);
        return ResponseEntity.ok("Session cleared");
    }

    private String renderHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        return history.stream()
                .map(m -> switch (m) {
                    case UserMessage u -> "User: " + u.singleText();
                    case AiMessage a   -> "Assistant: " + a.text();
                    default            -> "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Module 04: Agents - OK");
    }
}
