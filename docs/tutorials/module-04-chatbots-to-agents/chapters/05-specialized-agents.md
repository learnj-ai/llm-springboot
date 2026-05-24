# Chapter 5: Building Specialized Agents

## Overview

Different problems require different expertise. Just as human organizations have specialists for customer support, technical documentation, and product knowledge, multi-agent systems benefit from specialized agents that excel in their respective domains. This chapter explores how to design, implement, and deploy specialized agents.

## Learning Objectives

- Understand the benefits of agent specialization
- Design the SpecializedAgent interface
- Implement domain-specific agents
- Configure agent system prompts effectively
- Test specialized agent behavior
- Balance breadth vs. depth in agent design

## The Case for Specialization

### Monolithic Agent Problems

A single "do everything" agent faces challenges:

- **Diluted Expertise**: Jack of all trades, master of none
- **Prompt Confusion**: Too many responsibilities in one prompt
- **Difficult to Optimize**: Can't tune for specific use cases
- **Hard to Test**: Many failure modes to cover
- **Poor Performance**: One agent type can't excel at everything

### Specialized Agent Benefits

- **Focused Expertise**: Each agent masters its domain
- **Clearer Prompts**: Specific instructions for specific tasks
- **Easier Testing**: Narrower scope, clearer success criteria
- **Better Performance**: Optimized for specific use cases
- **Maintainability**: Changes isolated to specific agents

## Architecture Pattern

```
                    ┌─────────────────┐
                    │  Orchestrator   │
                    │  (Router/       │
                    │   Coordinator)  │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│Customer Support │ │Technical Doc    │ │Product Expert   │
│     Agent       │ │     Agent       │ │     Agent       │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│• Customer data  │ │• API docs       │ │• Features       │
│• Ticket support │ │• Integration    │ │• Roadmap        │
│• Account issues │ │• Technical spec │ │• Use cases      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

## The SpecializedAgent Interface

Define a common contract for all specialized agents:

```java
package com.techcorp.assistant.module04.agent;

/**
 * Interface for specialized agents in the multi-agent system.
 *
 * Each specialized agent handles a specific domain of queries.
 */
public interface SpecializedAgent {

    /**
     * Processes a request within this agent's domain of expertise.
     *
     * @param request The user's request
     * @return The agent's response
     */
    String process(String request);

    /**
     * Returns the name of this agent.
     *
     * @return Agent name
     */
    String getName();

    /**
     * Returns a description of this agent's capabilities.
     *
     * @return Agent description
     */
    String getDescription();
}
```

**Why This Interface?**

- **Polymorphism**: Treat all agents uniformly in orchestrators
- **Discoverability**: Standard getName/getDescription for routing
- **Testability**: Mock implementations for testing
- **Extensibility**: Easy to add new specialized agents

## Implementing Specialized Agents

### 1. CustomerSupportAgent

Handles customer service queries:

```java
@Component
public class CustomerSupportAgent implements SpecializedAgent {

    private final ChatModel chatModel;
    private final CustomerDataTool customerDataTool;

    public CustomerSupportAgent(
            ChatModel chatModel,
            CustomerDataTool customerDataTool) {
        this.chatModel = chatModel;
        this.customerDataTool = customerDataTool;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a customer support specialist. Answer questions about:
                - Customer accounts and information
                - Support tickets and their status
                - Account issues and troubleshooting

                Available tools:
                - Look up customer information by ID
                - Search support tickets by status

                User question: %s

                Provide a helpful and professional response.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "CustomerSupportAgent";
    }

    @Override
    public String getDescription() {
        return "Handles customer account queries, support tickets, and account-related issues";
    }
}
```

**Prompt Design:**
- **Role Definition**: "You are a customer support specialist"
- **Scope**: Clearly defines what this agent handles
- **Tone**: "helpful and professional"
- **Context**: Available tools listed

### 2. TechnicalDocAgent

Specializes in technical documentation:

```java
@Component
public class TechnicalDocAgent implements SpecializedAgent {

    private final ChatModel chatModel;

    public TechnicalDocAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a technical documentation specialist. Answer questions about:
                - API documentation and usage
                - Integration guides
                - Technical specifications
                - Development best practices

                User question: %s

                Provide a clear, technically accurate response with examples when appropriate.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "TechnicalDocAgent";
    }

    @Override
    public String getDescription() {
        return "Handles technical documentation, API usage, and integration questions";
    }
}
```

**Characteristics:**
- **Technical Focus**: API docs, integration guides
- **Example-Driven**: "with examples when appropriate"
- **Accuracy**: "technically accurate"

### 3. ProductExpertAgent

Product features and capabilities specialist:

```java
@Component
public class ProductExpertAgent implements SpecializedAgent {

    private final ChatModel chatModel;

    public ProductExpertAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a product expert. Answer questions about:
                - Product features and capabilities
                - Feature comparisons and roadmap
                - Product specifications
                - Use cases and best practices

                User question: %s

                Provide a comprehensive response highlighting relevant features and benefits.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "ProductExpertAgent";
    }

    @Override
    public String getDescription() {
        return "Handles product features, specifications, and capability questions";
    }
}
```

## Advanced Specialized Agent Patterns

### Agent with RAG (Retrieval-Augmented Generation)

```java
@Component
public class DocumentationAgent implements SpecializedAgent {

    private final ChatModel chatModel;
    private final EmbeddingStore<TextSegment> documentStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public String process(String request) {
        // 1. Retrieve relevant documentation
        List<EmbeddingMatch<TextSegment>> relevant = findRelevantDocs(request);

        // 2. Build context from retrieved docs
        String context = relevant.stream()
            .map(match -> match.embedded().text())
            .collect(Collectors.joining("\n\n"));

        // 3. Generate response with context
        String prompt = String.format("""
                Based on the following documentation:

                %s

                Answer this question: %s
                """, context, request);

        return chatModel.chat(prompt);
    }

    private List<EmbeddingMatch<TextSegment>> findRelevantDocs(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        return documentStore.findRelevant(queryEmbedding, 3);
    }

    // getName() and getDescription() implementations
}
```

### Agent with Tool Access

```java
@Component
public class AnalyticsAgent implements SpecializedAgent {

    private final ChatModel chatModel;
    private final AnalyticsTool analyticsTool;
    private final ReportingTool reportingTool;

    @Override
    public String process(String request) {
        // This agent has access to specialized analytics tools
        String prompt = String.format("""
                You are a data analytics specialist with access to:
                - Analytics data queries
                - Report generation
                - Metric calculations

                Available tools:
                - getMetrics(timeRange, metricType)
                - generateReport(reportType)
                - calculateGrowth(metric, period)

                User question: %s

                Analyze the data and provide insights.
                """, request);

        // Could integrate with ReAct pattern for tool use
        return chatModel.chat(prompt);
    }
}
```

### Agent with Fine-Tuned Model

```java
@Component
public class LegalAgent implements SpecializedAgent {

    private final ChatModel fineTunedLegalModel;

    public LegalAgent(@Qualifier("legalModel") ChatModel fineTunedLegalModel) {
        this.fineTunedLegalModel = fineTunedLegalModel;
    }

    @Override
    public String process(String request) {
        // Use fine-tuned model for domain-specific accuracy
        String prompt = String.format("""
                Provide legal guidance on: %s

                Note: This is not legal advice. Consult a licensed attorney.
                """, request);

        return fineTunedLegalModel.chat(prompt);
    }
}
```

## Prompt Engineering for Specialists

### 1. Role and Persona

Define who the agent is:

```java
"""
You are [role] with [X] years of experience in [domain].
Your expertise includes [specific areas].
"""
```

Example:
```java
"""
You are a senior DevOps engineer with 10 years of experience in
cloud infrastructure, CI/CD, and Kubernetes deployments.
"""
```

### 2. Scope and Boundaries

What the agent should and shouldn't handle:

```java
"""
You specialize in:
- [Topic A]
- [Topic B]
- [Topic C]

You should defer to other specialists for:
- [Out of scope topic A]
- [Out of scope topic B]
"""
```

### 3. Tone and Style

How the agent communicates:

```java
// Customer support: Empathetic and helpful
"""
Be warm, empathetic, and solution-focused.
Acknowledge frustrations and provide clear next steps.
"""

// Technical docs: Precise and example-driven
"""
Be precise and technically accurate.
Provide code examples where applicable.
Use proper terminology.
"""

// Product expert: Enthusiastic and benefit-focused
"""
Be enthusiastic about product capabilities.
Highlight benefits and use cases.
Compare features when relevant.
"""
```

### 4. Output Format

Structure responses consistently:

```java
"""
Format your response as:
1. Brief summary
2. Detailed explanation
3. Relevant examples
4. Next steps or recommendations
"""
```

## Testing Specialized Agents

### Unit Tests

```java
@SpringBootTest
class CustomerSupportAgentTest {

    @Autowired
    private CustomerSupportAgent agent;

    @Test
    void testHandlesCustomerQuery() {
        String response = agent.process("How do I upgrade my subscription?");

        assertThat(response)
            .isNotBlank()
            .containsAnyOf("upgrade", "subscription", "plan");
    }

    @Test
    void testAgentMetadata() {
        assertThat(agent.getName()).isEqualTo("CustomerSupportAgent");
        assertThat(agent.getDescription()).contains("customer");
    }
}
```

### Integration Tests

```java
@SpringBootTest
class SpecializedAgentIntegrationTest {

    @Autowired
    private List<SpecializedAgent> allAgents;

    @Test
    void testAllAgentsRegistered() {
        assertThat(allAgents).hasSize(3);

        List<String> agentNames = allAgents.stream()
            .map(SpecializedAgent::getName)
            .toList();

        assertThat(agentNames).containsExactlyInAnyOrder(
            "CustomerSupportAgent",
            "TechnicalDocAgent",
            "ProductExpertAgent"
        );
    }

    @Test
    void testAgentsHaveUniqueDescriptions() {
        Set<String> descriptions = allAgents.stream()
            .map(SpecializedAgent::getDescription)
            .collect(Collectors.toSet());

        // Each agent should have unique description
        assertThat(descriptions).hasSize(allAgents.size());
    }
}
```

### Behavior Tests

```java
@SpringBootTest
class AgentBehaviorTest {

    @Autowired
    private TechnicalDocAgent techAgent;

    @Autowired
    private ProductExpertAgent productAgent;

    @Test
    void testTechnicalAgentProvidesCodeExamples() {
        String response = techAgent.process("How do I call the API?");

        // Technical agent should provide code examples
        assertThat(response).containsAnyOf("```", "curl", "http", "GET", "POST");
    }

    @Test
    void testProductAgentHighlightsBenefits() {
        String response = productAgent.process("Tell me about feature X");

        // Product agent should focus on benefits
        assertThat(response).containsAnyOf("benefit", "advantage", "helps", "enables");
    }
}
```

## Agent Configuration

### Externalized Prompts

Store prompts in configuration:

```yaml
# application.yml
agents:
  customer-support:
    system-prompt: |
      You are a customer support specialist.
      Answer questions about customer accounts and support tickets.
    temperature: 0.7
    max-tokens: 500

  technical-doc:
    system-prompt: |
      You are a technical documentation specialist.
      Provide accurate technical information with examples.
    temperature: 0.5
    max-tokens: 800
```

```java
@Component
public class CustomerSupportAgent implements SpecializedAgent {

    @Value("${agents.customer-support.system-prompt}")
    private String systemPrompt;

    @Value("${agents.customer-support.temperature:0.7}")
    private double temperature;

    @Override
    public String process(String request) {
        String prompt = String.format("%s\n\nUser question: %s",
            systemPrompt, request);

        return chatModel.chat(prompt);
    }
}
```

### Dynamic Agent Configuration

```java
@Configuration
public class AgentConfig {

    @Bean
    public Map<String, SpecializedAgent> agentRegistry(
            CustomerSupportAgent customerSupport,
            TechnicalDocAgent technicalDoc,
            ProductExpertAgent productExpert) {

        Map<String, SpecializedAgent> registry = new HashMap<>();
        registry.put("support", customerSupport);
        registry.put("docs", technicalDoc);
        registry.put("product", productExpert);

        return registry;
    }
}
```

## Practice Exercises

### Exercise 1: Create a SalesAgent

Implement a new specialized agent for sales inquiries:

**Requirements:**
- Handle pricing questions
- Provide product comparisons
- Suggest appropriate plans
- Friendly, persuasive tone

<details>
<summary>Solution Template</summary>

```java
@Component
public class SalesAgent implements SpecializedAgent {

    private final ChatModel chatModel;

    public SalesAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a sales specialist. Help potential customers with:
                - Pricing information
                - Product comparisons
                - Choosing the right plan
                - Understanding value propositions

                User question: %s

                Be friendly, consultative, and focus on customer value.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "SalesAgent";
    }

    @Override
    public String getDescription() {
        return "Handles sales inquiries, pricing, and product selection guidance";
    }
}
```
</details>

### Exercise 2: Add Tool Integration to ProductExpertAgent

Extend ProductExpertAgent to use a ProductCatalogTool:

**Steps:**
1. Create ProductCatalogTool with methods like `searchProducts()`, `getProductDetails()`
2. Inject the tool into ProductExpertAgent
3. Update the prompt to mention available tools
4. Test with queries that require product data

<details>
<summary>Hint</summary>

```java
@Component
public class ProductExpertAgent implements SpecializedAgent {

    private final ChatModel chatModel;
    private final ProductCatalogTool catalogTool;

    @Override
    public String process(String request) {
        // Check if request needs product data
        if (needsProductData(request)) {
            String productData = catalogTool.searchProducts(extractKeywords(request));
            // Include in prompt context
        }

        // Generate response
    }
}
```
</details>

### Exercise 3: Implement Agent Performance Metrics

Track how well each specialized agent performs:

**Metrics to track:**
- Response time
- Success rate
- User satisfaction (if feedback available)
- Most common query types

<details>
<summary>Implementation Approach</summary>

```java
@Component
public class MetricsTrackingAgent implements SpecializedAgent {

    private final SpecializedAgent delegate;
    private final MeterRegistry meterRegistry;

    @Override
    public String process(String request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            String response = delegate.process(request);

            sample.stop(Timer.builder("agent.response.time")
                .tag("agent", delegate.getName())
                .register(meterRegistry));

            meterRegistry.counter("agent.requests.success",
                "agent", delegate.getName()).increment();

            return response;

        } catch (Exception e) {
            meterRegistry.counter("agent.requests.failure",
                "agent", delegate.getName()).increment();
            throw e;
        }
    }

    // Delegate other methods to wrapped agent
}
```
</details>

## Design Guidelines

### When to Create a New Specialized Agent

**Create a new agent when:**
- Domain requires specific expertise
- Prompt would become too complex in a general agent
- Different tools/data sources are needed
- Response tone/style should differ significantly
- Performance optimization is needed for specific queries

**Don't create a new agent when:**
- Differences are minor (use prompt variations instead)
- Queries are very rare
- Capabilities overlap significantly with existing agents
- Maintenance burden outweighs benefits

### Balancing Agent Count

**Too few agents:**
- Agents try to do too much
- Prompts become complex and fragile
- Hard to optimize performance

**Too many agents:**
- Routing becomes complex
- Maintenance burden increases
- User confusion about which agent to use
- Overlapping responsibilities

**Sweet spot:** 3-7 specialized agents for most applications.

## Key Takeaways

- **Specialization Improves Performance**: Focused agents outperform generalists
- **Interface Standardization**: SpecializedAgent interface enables polymorphism
- **Prompt Engineering**: Each agent needs carefully crafted system prompts
- **Clear Boundaries**: Define what each agent handles and what it doesn't
- **Tool Integration**: Agents can have domain-specific tools
- **Testing**: Verify both functionality and behavioral characteristics
- **Configuration**: Externalize prompts for easier tuning
- **Metrics**: Track agent performance to guide optimization

## What's Next?

With specialized agents implemented, the next chapter explores how to coordinate them through multi-agent orchestration patterns.

Continue to [Chapter 6: Multi-Agent Orchestration](06-multi-agent-orchestration.md).

---

**Previous**: [Chapter 4: Implementing Conversation Memory](04-conversation-memory.md) | **Next**: [Chapter 6: Multi-Agent Orchestration](06-multi-agent-orchestration.md)
