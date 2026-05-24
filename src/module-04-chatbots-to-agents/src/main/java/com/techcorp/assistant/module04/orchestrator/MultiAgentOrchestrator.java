package com.techcorp.assistant.module04.orchestrator;

import com.techcorp.assistant.module04.agent.CustomerSupportAgent;
import com.techcorp.assistant.module04.agent.ProductExpertAgent;
import com.techcorp.assistant.module04.agent.SpecializedAgent;
import com.techcorp.assistant.module04.agent.TechnicalDocAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates multiple specialized agents.
 *
 * Provides two modes:
 * 1. Routing: Selects the best agent for a request
 * 2. Collaborative: Gathers perspectives from all agents
 */
@Service
public class MultiAgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(MultiAgentOrchestrator.class);

    private final ChatModel coordinatorModel;
    private final Map<String, SpecializedAgent> agents;

    public MultiAgentOrchestrator(
            ChatModel coordinatorModel,
            CustomerSupportAgent customerSupportAgent,
            TechnicalDocAgent technicalDocAgent,
            ProductExpertAgent productExpertAgent) {

        this.coordinatorModel = coordinatorModel;
        this.agents = new HashMap<>();
        this.agents.put("support", customerSupportAgent);
        this.agents.put("documentation", technicalDocAgent);
        this.agents.put("product", productExpertAgent);

        log.info("MultiAgentOrchestrator initialized with {} agents", agents.size());
    }

    /**
     * Routes request to the most appropriate specialized agent.
     *
     * Uses LLM to analyze the request and select the best agent.
     *
     * @param request User's request
     * @return Response from the selected agent
     */
    public String routeRequest(String request) {
        log.info("Routing request: {}", request);

        // Build routing prompt
        String agentDescriptions = agents.entrySet().stream()
                .map(entry -> String.format("- %s: %s",
                        entry.getKey(),
                        entry.getValue().getDescription()))
                .collect(Collectors.joining("\n"));

        String routingPrompt = String.format("""
                Analyze this user request and select the most appropriate agent to handle it.

                Available agents:
                %s

                User request: "%s"

                Respond with ONLY the agent key (support, documentation, or product).
                If uncertain, default to 'support'.
                """, agentDescriptions, request);

        String selectedKey = coordinatorModel.chat(routingPrompt).trim().toLowerCase();

        // Clean up response (remove quotes, extra words, etc.)
        selectedKey = selectedKey.replaceAll("[\"']", "").split("\\s+")[0];

        // Validate and default
        if (!agents.containsKey(selectedKey)) {
            log.warn("Invalid agent key '{}', defaulting to support", selectedKey);
            selectedKey = "support";
        }

        log.info("Selected agent: {}", selectedKey);
        SpecializedAgent selectedAgent = agents.get(selectedKey);

        return selectedAgent.process(request);
    }

    /**
     * Gathers perspectives from all agents and synthesizes a response.
     *
     * Useful for complex requests requiring cross-domain knowledge.
     *
     * @param request User's request
     * @return Synthesized response combining all agent perspectives
     */
    public String collaborativeRequest(String request) {
        log.info("Processing collaborative request: {}", request);

        // Gather responses from all agents
        Map<String, String> agentResponses = new HashMap<>();

        for (Map.Entry<String, SpecializedAgent> entry : agents.entrySet()) {
            String agentKey = entry.getKey();
            SpecializedAgent agent = entry.getValue();

            log.debug("Getting perspective from {}", agent.getName());
            String response = agent.process(request);
            agentResponses.put(agentKey, response);
        }

        // Synthesize responses
        String synthesisPrompt = String.format("""
                Multiple experts have provided perspectives on this question:

                Question: %s

                Customer Support perspective:
                %s

                Technical Documentation perspective:
                %s

                Product Expert perspective:
                %s

                Synthesize these perspectives into a single, comprehensive response.
                Highlight complementary insights and resolve any contradictions.
                Provide a unified answer that leverages all expert knowledge.
                """,
                request,
                agentResponses.get("support"),
                agentResponses.get("documentation"),
                agentResponses.get("product")
        );

        String synthesizedResponse = coordinatorModel.chat(synthesisPrompt);
        log.info("Synthesized collaborative response");

        return synthesizedResponse;
    }
}
