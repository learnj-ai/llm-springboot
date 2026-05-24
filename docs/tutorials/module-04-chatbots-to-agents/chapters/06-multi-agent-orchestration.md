# Chapter 6: Multi-Agent Orchestration

## Overview

Multi-agent orchestration coordinates specialized agents to handle complex requests. This chapter explores routing patterns, collaboration strategies, and building effective multi-agent systems.

## Learning Objectives

- Understand orchestration patterns (routing vs. collaboration)
- Implement the MultiAgentOrchestrator
- Design LLM-based routing logic
- Synthesize responses from multiple agents
- Handle edge cases and fallbacks

## Orchestration Patterns

### 1. Routing Pattern

Direct requests to the single best agent:

```
User Request → Orchestrator → [Select Best Agent] → Single Agent → Response
```

### 2. Collaboration Pattern

Gather perspectives from multiple agents:

```
User Request → Orchestrator → All Agents → Synthesize → Response
```

## MultiAgentOrchestrator Implementation

```java
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
    }

    public String routeRequest(String request) {
        String agentDescriptions = agents.entrySet().stream()
                .map(entry -> String.format("- %s: %s",
                        entry.getKey(),
                        entry.getValue().getDescription()))
                .collect(Collectors.joining("\n"));

        String routingPrompt = String.format("""
                Analyze this user request and select the most appropriate agent.

                Available agents:
                %s

                User request: "%s"

                Respond with ONLY the agent key.
                """, agentDescriptions, request);

        String selectedKey = coordinatorModel.chat(routingPrompt)
            .trim().toLowerCase();

        if (!agents.containsKey(selectedKey)) {
            selectedKey = "support"; // Default fallback
        }

        return agents.get(selectedKey).process(request);
    }

    public String collaborativeRequest(String request) {
        Map<String, String> agentResponses = new HashMap<>();

        for (Map.Entry<String, SpecializedAgent> entry : agents.entrySet()) {
            String response = entry.getValue().process(request);
            agentResponses.put(entry.getKey(), response);
        }

        String synthesisPrompt = String.format("""
                Synthesize these expert perspectives:

                Question: %s

                Customer Support: %s
                Technical Docs: %s
                Product Expert: %s

                Provide a unified, comprehensive answer.
                """,
                request,
                agentResponses.get("support"),
                agentResponses.get("documentation"),
                agentResponses.get("product")
        );

        return coordinatorModel.chat(synthesisPrompt);
    }
}
```

## Advanced Orchestration Patterns

### Conditional Routing

```java
public String conditionalRoute(String request, Map<String, Object> context) {
    // Route based on user role, history, complexity, etc.
    if (context.get("userRole").equals("developer")) {
        return agents.get("documentation").process(request);
    } else if (context.get("userRole").equals("customer")) {
        return agents.get("support").process(request);
    }
    // Fall back to LLM routing
    return routeRequest(request);
}
```

### Sequential Processing

```java
public String sequentialProcessing(String request) {
    // Agent 1: Gather information
    String info = agents.get("support").process("Get context for: " + request);

    // Agent 2: Analyze with context
    String analysis = agents.get("product").process(
        "Based on this context: " + info + "\nAnswer: " + request
    );

    return analysis;
}
```

### Voting/Consensus

```java
public String consensusDecision(String request) {
    Map<String, String> responses = new HashMap<>();

    // Get all agent responses
    agents.forEach((key, agent) -> {
        responses.put(key, agent.process(request));
    });

    // Synthesize with voting logic
    String prompt = String.format("""
        Multiple experts provided answers. Identify the consensus:
        %s

        Provide the most agreed-upon answer.
        """, formatResponses(responses));

    return coordinatorModel.chat(prompt);
}
```

## Practice Exercises

### Exercise 1: Test Routing Accuracy

Send queries and verify correct routing:

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{"message": "How do I integrate with your API?", "mode": "multiagent"}'
```

Expected: Routes to TechnicalDocAgent

### Exercise 2: Implement Fallback Logic

Add fallback when agent selection fails:

```java
public String routeWithFallback(String request) {
    try {
        return routeRequest(request);
    } catch (Exception e) {
        log.warn("Routing failed, using general agent", e);
        return generalAgent.process(request);
    }
}
```

### Exercise 3: Add Routing Metrics

Track which agents are selected most often:

```java
@Autowired
private MeterRegistry meterRegistry;

private String trackRouting(String agentKey, String request) {
    meterRegistry.counter("agent.routing",
        "agent", agentKey).increment();

    return agents.get(agentKey).process(request);
}
```

## Key Takeaways

- **Routing**: Select single best agent for efficiency
- **Collaboration**: Combine multiple perspectives for complex queries
- **LLM-Based Routing**: Use AI to make routing decisions
- **Synthesis**: Combine agent outputs intelligently
- **Fallbacks**: Handle routing failures gracefully

## What's Next?

Continue to [Chapter 7: Complex Task Decomposition](07-task-decomposition.md).

---

**Previous**: [Chapter 5: Building Specialized Agents](05-specialized-agents.md) | **Next**: [Chapter 7: Complex Task Decomposition](07-task-decomposition.md)
