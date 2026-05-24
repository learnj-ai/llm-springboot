# Chapter 2: Understanding the ReAct Pattern

## Overview

The ReAct (Reason + Act) pattern is a fundamental approach to building AI agents that can solve complex problems through iterative reasoning and action. Unlike simple chatbots that generate a single response, ReAct agents think through problems step by step, using tools and gathering information until they reach a solution.

## Learning Objectives

- Understand the theoretical foundation of the ReAct pattern
- Learn how thought-action-observation cycles work
- Explore the ReActAgent implementation
- Master prompt engineering for agent reasoning
- Handle edge cases and iteration limits
- Debug agent reasoning processes

## The ReAct Pattern Explained

### Why ReAct?

Traditional LLMs have two limitations when solving problems:

1. **Limited Knowledge**: They only know what was in their training data
2. **No External Actions**: They can't fetch real-time data, query databases, or interact with APIs

The ReAct pattern solves both by enabling agents to:
- **Reason** about what information they need
- **Act** by calling tools and APIs
- **Observe** the results and adjust their approach

### The Thought-Action-Observation Cycle

```
┌─────────────────────────────────────────────────────┐
│                   ReAct Loop                        │
│                                                     │
│  ┌─────────┐        ┌─────────┐       ┌─────────┐ │
│  │ THOUGHT │───────▶│ ACTION  │──────▶│  TOOL   │ │
│  └─────────┘        └─────────┘       └────┬────┘ │
│       ▲                                     │      │
│       │                                     ▼      │
│       │              ┌────────────┐    ┌─────────┐│
│       └──────────────│   UPDATE   │◀───│  OBS.   ││
│                      │  HISTORY   │    └─────────┘│
│                      └────────────┘                │
│                           │                        │
│                           ▼                        │
│                    ┌─────────────┐                 │
│                    │Final Answer?│                 │
│                    └──────┬──────┘                 │
│                           │                        │
│                      Yes  │  No                    │
│                           │  (continue loop)       │
└───────────────────────────┼────────────────────────┘
                            ▼
                      Return Answer
```

### ReAct Flow Example

Let's trace a query through the ReAct pattern:

**User Query**: "What is the weather in Boston?"

**Iteration 1:**
- **THOUGHT**: "I need to get current weather information for Boston. I should use the weather tool."
- **ACTION**: `getCurrentWeather("Boston")`
- **OBSERVATION**: "Current Weather in Boston: Temperature: 18°C (64°F), Conditions: Partly cloudy, Humidity: 65%, Wind: 12 km/h NE"

**Iteration 2:**
- **THOUGHT**: "I have the weather information. I can now provide a complete answer to the user."
- **FINAL ANSWER**: "The current weather in Boston is 18°C (64°F) with partly cloudy conditions. The humidity is 65% and there's a wind of 12 km/h from the northeast."

## Exploring the Implementation

### The ReActAgent Class

Let's examine the core implementation:

```java
@Service
public class ReActAgent {
    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    private final ChatModel chatModel;
    private final CustomerDataTool customerDataTool;
    private final WeatherTool weatherTool;

    @Value("${agent.react.max-iterations:5}")
    private int maxIterations;

    // Main entry point
    public String solve(String question) {
        return solve(question, maxIterations);
    }

    public String solve(String question, int maxIter) {
        log.info("Starting ReAct agent to solve: {}", question);

        StringBuilder history = new StringBuilder();
        String currentPrompt = REACT_PROMPT
                .replace("{question}", question)
                .replace("{history}", "");

        for (int iteration = 1; iteration <= maxIter; iteration++) {
            // Generate thought and action
            String response = chatModel.chat(currentPrompt);

            // Check for final answer
            if (response.contains("FINAL ANSWER:")) {
                return extractFinalAnswer(response);
            }

            // Extract and execute action
            String thought = extractThought(response);
            String action = extractAction(response);
            String observation = executeAction(action);

            // Update history for next iteration
            history.append("THOUGHT: ").append(thought).append("\n");
            history.append("ACTION: ").append(action).append("\n");
            history.append("OBSERVATION: ").append(observation).append("\n\n");

            currentPrompt = REACT_PROMPT
                    .replace("{question}", question)
                    .replace("{history}", history.toString());
        }

        // Max iterations reached
        return "Reached max iterations without final answer";
    }
}
```

### Key Components Breakdown

#### 1. The ReAct Prompt

```java
private static final String REACT_PROMPT = """
    You are a helpful AI assistant that uses tools to answer questions.

    You have access to the following tools:
    - getCustomerInfo(customerId): Get customer details by ID
    - searchTickets(status): Search support tickets (open/pending/closed)
    - getCurrentWeather(city): Get current weather for a city

    For each step, use this exact format:

    THOUGHT: [your reasoning about what to do]
    ACTION: [tool_name(parameters)]

    After receiving an observation, either:
    - Continue with another THOUGHT/ACTION if you need more information
    - Or provide the FINAL ANSWER when you have enough information

    FINAL ANSWER: [your complete answer to the user]

    Question: {question}

    {history}
    """;
```

**Prompt Design Principles:**

1. **Clear Tool Descriptions**: The LLM needs to know what tools are available
2. **Structured Format**: THOUGHT/ACTION/OBSERVATION format ensures parseable output
3. **Termination Condition**: FINAL ANSWER signals completion
4. **History Inclusion**: Previous steps inform future decisions

#### 2. Thought Extraction

```java
private String extractThought(String response) {
    Pattern pattern = Pattern.compile(
        "THOUGHT:\\s*(.+?)(?=ACTION:|FINAL ANSWER:|$)",
        Pattern.DOTALL
    );
    Matcher matcher = pattern.matcher(response);
    if (matcher.find()) {
        return matcher.group(1).trim();
    }
    return "";
}
```

**Why Regular Expressions?**
- LLM output is free-form text
- Need to extract specific sections reliably
- DOTALL flag allows matching across multiple lines
- Lookahead `(?=...)` finds text between markers

#### 3. Action Extraction and Execution

```java
private String extractAction(String response) {
    Pattern pattern = Pattern.compile(
        "ACTION:\\s*(.+?)(?=OBSERVATION:|THOUGHT:|FINAL ANSWER:|$)",
        Pattern.DOTALL
    );
    Matcher matcher = pattern.matcher(response);
    return matcher.find() ? matcher.group(1).trim() : null;
}

private String executeAction(String action) {
    try {
        // Parse: tool_name(parameters)
        Pattern pattern = Pattern.compile("(\\w+)\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(action.trim());

        if (!matcher.find()) {
            return "Error: Invalid action format";
        }

        String toolName = matcher.group(1);
        String parameters = matcher.group(2)
            .replaceAll("['\"]", "")
            .trim();

        return switch (toolName) {
            case "getCustomerInfo" -> customerDataTool.getCustomerInfo(parameters);
            case "searchTickets" -> customerDataTool.searchTickets(parameters);
            case "getCurrentWeather" -> weatherTool.getCurrentWeather(parameters);
            default -> "Error: Unknown tool: " + toolName;
        };

    } catch (Exception e) {
        log.error("Error executing action: {}", action, e);
        return "Error executing action: " + e.getMessage();
    }
}
```

> **Why this parser is brittle, and what to use in production.**
>
> The regex above (`(\\w+)\\(([^)]*)\\)`) only handles single-line, comma-free
> calls with no nested parentheses. It breaks on:
>
> | Input the LLM produced                                          | What goes wrong                                                                              |
> |-----------------------------------------------------------------|----------------------------------------------------------------------------------------------|
> | `getCustomerInfo("O'Brien, Sean")`                              | The `replaceAll("['\"]", "")` strips the apostrophe, so the lookup runs on `OBrien, Sean`.   |
> | `getCurrentWeather("San José, Spain (region)")`                | The first `)` ends the match — you call `getCurrentWeather("San José, Spain (region")`.       |
> | `searchTickets({"status": "open", "owner": "alice"})`           | Curly-brace JSON arg looks fine to the LLM; the regex captures the whole `{...}` as one param. |
> | `getCurrentWeather('Boston')` with smart quotes (`'…'`)         | `replaceAll("['\"]", "")` keeps the smart quotes; lookup hits `'Boston'` literally.           |
>
> The fix is to stop parsing free-form text the LLM emitted and instead use a
> structured tool-call channel:
>
> 1. **Migrate to LangChain4J `@Tool` annotations + `AiServices`.** The OpenAI /
>    Anthropic / Bedrock backends return tool calls as structured JSON
>    (`{"name": "getCurrentWeather", "arguments": {"city": "San José, Spain"}}`).
>    LangChain4J dispatches them directly to your `@Tool`-annotated methods —
>    no regex, no quote-stripping. See `module-03-tools-mcp` for the pattern.
> 2. **Or, if you must keep a custom ReAct loop, ask the model for JSON.** Change
>    the action format from `tool_name(args)` to
>    `ACTION: {"tool": "getCurrentWeather", "args": {"city": "..."}}` and parse
>    with Jackson. JSON gives you proper string escaping, nested structures,
>    and a type-safe deserialization target.
>
> The regex-based parser in this chapter is fine *as a teaching tool* — it
> makes the Thought / Action / Observation loop visible without hiding it
> behind a framework. Just don't ship it.

**Action Execution Flow:**

```
ACTION: getCurrentWeather("Boston")
    │
    ├─> Parse tool name: "getCurrentWeather"
    ├─> Parse parameters: "Boston"
    ├─> Route to WeatherTool
    └─> Return observation
```

#### 4. History Management

The agent maintains a running history of all thoughts, actions, and observations:

```java
history.append("THOUGHT: ").append(thought).append("\n");
history.append("ACTION: ").append(action).append("\n");
history.append("OBSERVATION: ").append(observation).append("\n\n");

currentPrompt = REACT_PROMPT
        .replace("{question}", question)
        .replace("{history}", history.toString());
```

**Why History Matters:**

- **Context**: Each iteration builds on previous ones
- **Error Correction**: Agent can adjust if an action didn't work
- **Reasoning Chain**: Demonstrates agent's thought process
- **Debugging**: Helps understand how the agent reached its conclusion

## Advanced ReAct Patterns

### Multi-Step Reasoning

Some queries require multiple tool calls:

**Query**: "Find all open tickets for customer CUST001"

```
ITERATION 1:
THOUGHT: I need to get customer information first to verify the customer exists
ACTION: getCustomerInfo("CUST001")
OBSERVATION: Customer Information: ID: CUST001, Name: Alice Johnson...

ITERATION 2:
THOUGHT: Customer exists. Now I need to search for their open tickets
ACTION: searchTickets("open")
OBSERVATION: Found 2 open tickets...

ITERATION 3:
THOUGHT: I have the information needed. I can provide the final answer
FINAL ANSWER: Customer Alice Johnson (CUST001) has 2 open tickets: ...
```

### Error Recovery

The agent can recover from errors:

```
ITERATION 1:
THOUGHT: I'll search for the weather in Bostun
ACTION: getCurrentWeather("Bostun")
OBSERVATION: Error: City not found

ITERATION 2:
THOUGHT: I made a typo. Let me try "Boston" instead
ACTION: getCurrentWeather("Boston")
OBSERVATION: Current Weather in Boston: ...
```

### Iteration Limits

Maximum iterations prevent infinite loops:

```java
@Value("${agent.react.max-iterations:5}")
private int maxIterations;
```

**Best Practices:**
- **5 iterations** is usually sufficient for most queries
- Too few: Agent may not complete complex tasks
- Too many: Costs increase, slower responses

## Debugging Agent Reasoning

### Enable Debug Logging

```properties
logging.level.com.techcorp.assistant.module04.agent=DEBUG
logging.level.dev.langchain4j=DEBUG
```

### Analyzing Agent Traces

Sample log output:

```
2024-05-08 10:15:23 INFO  ReActAgent - Starting ReAct agent to solve: What's the weather in Boston?
2024-05-08 10:15:23 DEBUG ReActAgent - Iteration 1/5
2024-05-08 10:15:24 DEBUG ReActAgent - LLM Response:
THOUGHT: I need to use the weather tool to get current weather for Boston
ACTION: getCurrentWeather("Boston")

2024-05-08 10:15:24 DEBUG ReActAgent - THOUGHT: I need to use the weather tool...
2024-05-08 10:15:24 DEBUG ReActAgent - ACTION: getCurrentWeather("Boston")
2024-05-08 10:15:24 DEBUG WeatherTool - Tool invoked: getCurrentWeather(Boston)
2024-05-08 10:15:24 DEBUG ReActAgent - OBSERVATION: Current Weather in Boston: ...
2024-05-08 10:15:25 INFO  ReActAgent - Reached final answer after 2 iterations
```

### Common Issues and Solutions

#### Issue: Agent Loops Without Terminating

**Symptom**: Agent reaches max iterations without providing FINAL ANSWER

**Causes:**
- Prompt doesn't clearly specify when to finish
- Agent gets stuck in analysis paralysis
- Conflicting observations

**Solution:**
```java
// Add explicit termination hints to the prompt
"""
If you have enough information to answer the question, provide the FINAL ANSWER.
Do not continue gathering information unnecessarily.
"""
```

#### Issue: Invalid Action Format

**Symptom**: `Error: Invalid action format`

**Causes:**
- LLM didn't follow the `tool_name(parameters)` format
- Extra characters or formatting

**Solution:**
```java
// More robust parsing
private String executeAction(String action) {
    // Clean the action string
    action = action.trim()
        .replaceAll("^ACTION:\\s*", "")
        .replaceAll("[`'\"]", "");

    // Try multiple patterns
    // ...
}
```

#### Issue: Tool Not Found

**Symptom**: `Error: Unknown tool: toolName`

**Causes:**
- Tool name in action doesn't match registered tools
- Typo in tool name

**Solution:**
```java
// Case-insensitive matching
String normalizedToolName = toolName.toLowerCase();
return switch (normalizedToolName) {
    case "getcustomerinfo", "customer_info" -> customerDataTool.getCustomerInfo(parameters);
    // ...
};
```

## Practice Exercises

### Exercise 1: Trace a ReAct Execution

Enable debug logging and execute this query:

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find customer CUST002 and check their subscription plan",
    "mode": "react"
  }'
```

**Tasks:**
1. Count how many iterations the agent used
2. Identify what tools were called
3. Note the thought process at each step

<details>
<summary>Solution Analysis</summary>

Expected trace:
- **Iteration 1**:
  - THOUGHT: Need customer information
  - ACTION: getCustomerInfo("CUST002")
  - OBSERVATION: Customer data including subscription_plan: "standard"
- **Iteration 2**:
  - THOUGHT: I have the subscription plan information
  - FINAL ANSWER: Customer CUST002 (Bob Smith) has a standard subscription plan

Total iterations: 2
Tools called: getCustomerInfo
</details>

### Exercise 2: Force Multi-Step Reasoning

Create a query that requires multiple tool calls:

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find all pending tickets and tell me the weather in Boston",
    "mode": "react"
  }'
```

**Questions:**
1. Did the agent call both tools?
2. In what order were they called?
3. How did the agent combine the information?

<details>
<summary>Expected Behavior</summary>

The agent should:
1. Call `searchTickets("pending")`
2. Call `getCurrentWeather("Boston")`
3. Synthesize both results into a coherent answer

Order may vary depending on the LLM's reasoning, but both tools should be invoked.
</details>

### Exercise 3: Test Iteration Limits

Modify the max iterations to 2 and test with a complex query:

```properties
agent.react.max-iterations=2
```

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find customer CUST001, check their tickets, and get weather in their city",
    "mode": "react"
  }'
```

**Observe:**
- Does the agent complete the task?
- What happens when it hits the limit?

<details>
<summary>Expected Result</summary>

With only 2 iterations, the agent likely won't complete all three subtasks:
1. Get customer info
2. Search tickets

The response will indicate "Reached max iterations without final answer" and include the partial progress in the history.

**Lesson**: Match max iterations to task complexity.
</details>

### Exercise 4: Implement a Custom Tool

Add a new tool to the ReActAgent. Create a `OrderTool` that can check order status:

```java
@Component
public class OrderTool {
    @Tool("Retrieves order status by order ID")
    public String getOrderStatus(@P("Order ID to check") String orderId) {
        // Mock implementation
        return "Order " + orderId + ": Shipped, Expected delivery: 2 days";
    }
}
```

**Steps:**
1. Create the OrderTool class
2. Inject it into ReActAgent
3. Add it to the executeAction switch statement
4. Update the REACT_PROMPT to include the new tool
5. Test with a query like "What's the status of order ORD123?"

<details>
<summary>Solution Code</summary>

```java
// In ReActAgent.java
private final OrderTool orderTool;

public ReActAgent(
        ChatModel chatModel,
        CustomerDataTool customerDataTool,
        WeatherTool weatherTool,
        OrderTool orderTool) {  // Add parameter
    this.chatModel = chatModel;
    this.customerDataTool = customerDataTool;
    this.weatherTool = weatherTool;
    this.orderTool = orderTool;  // Assign
}

// Update REACT_PROMPT
private static final String REACT_PROMPT = """
    ...
    You have access to the following tools:
    - getCustomerInfo(customerId): Get customer details by ID
    - searchTickets(status): Search support tickets
    - getCurrentWeather(city): Get current weather
    - getOrderStatus(orderId): Get order status by ID
    ...
    """;

// Update executeAction
return switch (toolName) {
    case "getCustomerInfo" -> customerDataTool.getCustomerInfo(parameters);
    case "searchTickets" -> customerDataTool.searchTickets(parameters);
    case "getCurrentWeather" -> weatherTool.getCurrentWeather(parameters);
    case "getOrderStatus" -> orderTool.getOrderStatus(parameters);  // Add
    default -> "Error: Unknown tool: " + toolName;
};
```

</details>

## Performance Considerations

### Token Usage

Each iteration consumes tokens:
- **Input tokens**: Prompt + history (grows each iteration)
- **Output tokens**: LLM response (thought + action)

**Optimization strategies:**
- Set appropriate max iterations
- Summarize history if it gets too long
- Use smaller models for simple queries

### Response Time

ReAct agents are slower than simple chatbots due to:
- Multiple LLM calls (one per iteration)
- Tool execution time
- Network latency

**Typical timings:**
- Simple query (1-2 iterations): 2-4 seconds
- Complex query (3-5 iterations): 5-10 seconds

### Cost Management

**Costs per query:**
- Each iteration = 1 LLM API call
- 5 iterations = 5x the cost of a single chatbot response

**Optimization:**
- Cache frequent tool results
- Use cheaper models when possible
- Implement query complexity detection

## Key Takeaways

- **ReAct = Reason + Act**: Iterative cycle of thinking, acting, and observing
- **Structured Prompts**: Clear formatting enables reliable parsing
- **History Matters**: Each iteration builds on previous context
- **Iteration Limits**: Prevent infinite loops while allowing complex reasoning
- **Tool Integration**: External tools give agents real-world capabilities
- **Error Handling**: Robust parsing and execution error management is critical
- **Performance Trade-offs**: More capability comes with higher latency and cost

## Further Reading

- [ReAct: Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629) - Original research paper
- [LangChain4j Agent Documentation](https://docs.langchain4j.dev/tutorials/agents)
- [Prompt Engineering Guide](https://www.promptingguide.ai/)

## What's Next?

Now that you understand how ReAct agents work, the next chapter explores how to build and integrate your own custom tools to extend agent capabilities.

Continue to [Chapter 3: Integrating External Tools](03-external-tools.md) to learn tool development.

---

**Previous**: [Chapter 1: Getting Started](01-getting-started.md) | **Next**: [Chapter 3: Integrating External Tools](03-external-tools.md)
