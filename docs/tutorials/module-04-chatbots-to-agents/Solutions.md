# Module 04: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 04: From Chatbots to Agents.

---

## Chapter 2: Understanding the ReAct Pattern - Solutions

### Exercise 1: Implement basic ReAct loop

**Solution:**

```java
@Service
public class ReactAgent {
    
    private final ChatLanguageModel chatModel;
    private final List<Tool> tools;
    
    public String process(String userQuery) {
        String thought = "";
        int maxIterations = 5;
        
        for (int i = 0; i < maxIterations; i++) {
            // Thought: Reason about what to do
            thought = generateThought(userQuery, thought);
            logger.info("Thought: {}", thought);
            
            // Action: Decide on action
            Action action = decideAction(thought);
            logger.info("Action: {}", action);
            
            if (action.isAnswer()) {
                return action.content();
            }
            
            // Observation: Execute tool and observe result
            String observation = executeTool(action);
            logger.info("Observation: {}", observation);
            
            thought = thought + "\nObservation: " + observation;
        }
        
        return "Could not complete task within iteration limit";
    }
    
    private String generateThought(String query, String history) {
        String prompt = String.format("""
            User query: %s
            
            Previous thoughts: %s
            
            What should I do next to answer this query?
            Think step by step.
            """, query, history);
        
        return chatModel.generate(prompt);
    }
    
    record Action(String type, String tool, Map<String, Object> params, String content) {
        boolean isAnswer() {
            return "answer".equals(type);
        }
    }
}
```

### Exercise 2: Test ReAct iterations

**Solution:**

```java
@Test
void shouldIterateUntilAnswer() {
    String query = "What's the weather in London?";
    
    when(chatModel.generate(contains("Think step by step")))
        .thenReturn("I need to use the weather tool")
        .thenReturn("Based on the weather data, I can answer");
    
    String result = reactAgent.process(query);
    
    assertThat(result).isNotBlank();
    verify(chatModel, atLeast(2)).generate(anyString());
}
```

### Exercise 3 (Bonus): Add thought logging

**Solution:**

```java
@Service
public class ReactAgent {
    
    private final List<ThoughtRecord> thoughtHistory = new ArrayList<>();
    
    public String processWithLogging(String userQuery) {
        thoughtHistory.clear();
        
        String thought = "";
        for (int i = 0; i < 5; i++) {
            thought = generateThought(userQuery, thought);
            
            ThoughtRecord record = new ThoughtRecord(
                i + 1,
                thought,
                Instant.now()
            );
            thoughtHistory.add(record);
            logger.info("Iteration {}: {}", i + 1, thought);
            
            Action action = decideAction(thought);
            
            if (action.isAnswer()) {
                return action.content();
            }
            
            String observation = executeTool(action);
            thought = thought + "\nObservation: " + observation;
        }
        
        return "Task incomplete";
    }
    
    public List<ThoughtRecord> getThoughtHistory() {
        return new ArrayList<>(thoughtHistory);
    }
    
    public record ThoughtRecord(int iteration, String thought, Instant timestamp) {}
}
```

### Exercise 4 (Challenge): Self-correction mechanism

**Solution:**

```java
private String processWithCorrection(String userQuery) {
    String thought = "";
    List<String> failedActions = new ArrayList<>();
    
    for (int i = 0; i < 5; i++) {
        thought = generateThoughtWithFailures(userQuery, thought, failedActions);
        Action action = decideAction(thought);
        
        if (action.isAnswer()) {
            return action.content();
        }
        
        try {
            String observation = executeTool(action);
            thought = thought + "\nObservation: " + observation;
            
        } catch (ToolExecutionException e) {
            logger.warn("Tool execution failed: {}", e.getMessage());
            failedActions.add(action.tool() + ": " + e.getMessage());
            
            thought = thought + "\nError: " + e.getMessage() + 
                     "\nI need to try a different approach.";
        }
    }
    
    return "Could not complete task";
}

private String generateThoughtWithFailures(String query, String history, List<String> failures) {
    String failureContext = failures.isEmpty() ? 
        "" : "\nPrevious failed attempts:\n" + String.join("\n", failures);
    
    String prompt = String.format("""
        User query: %s
        
        Previous thoughts: %s
        %s
        
        What should I do next? Consider what has failed before.
        """, query, history, failureContext);
    
    return chatModel.generate(prompt);
}
```

---

## Chapter 3: Integrating External Tools - Solutions

### Exercise 1: Add calculator tool

**Solution:**

```java
@Component
public class CalculatorTool implements Tool {
    
    @Override
    public String name() {
        return "calculator";
    }
    
    @Override
    public String description() {
        return "Perform basic arithmetic operations: add, subtract, multiply, divide";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        String operation = (String) params.get("operation");
        double a = ((Number) params.get("a")).doubleValue();
        double b = ((Number) params.get("b")).doubleValue();
        
        double result = switch (operation) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> {
                if (b == 0) {
                    throw new ToolExecutionException("Division by zero");
                }
                yield a / b;
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
        
        return ToolResult.success(String.valueOf(result));
    }
}
```

### Exercise 2: Test calculator tool

**Solution:**

```java
@Test
void shouldPerformBasicArithmetic() {
    CalculatorTool calc = new CalculatorTool();
    
    Map<String, Object> params = Map.of(
        "operation", "add",
        "a", 5,
        "b", 3
    );
    
    ToolResult result = calc.execute(params);
    
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isEqualTo("8.0");
}

@Test
void shouldHandleDivisionByZero() {
    CalculatorTool calc = new CalculatorTool();
    
    Map<String, Object> params = Map.of(
        "operation", "divide",
        "a", 10,
        "b", 0
    );
    
    assertThatThrownBy(() -> calc.execute(params))
        .isInstanceOf(ToolExecutionException.class)
        .hasMessageContaining("Division by zero");
}
```

### Exercise 3 (Bonus): File search tool

**Solution:**

```java
@Component
public class FileSearchTool implements Tool {
    
    private final Path basePath;
    
    public FileSearchTool(@Value("${file.search.base-path}") String basePath) {
        this.basePath = Paths.get(basePath);
    }
    
    @Override
    public String name() {
        return "search_files";
    }
    
    @Override
    public String description() {
        return "Search for files by name pattern in the configured directory";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        String pattern = (String) params.get("pattern");
        
        try (var stream = Files.walk(basePath)) {
            List<String> matches = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains(pattern))
                .map(Path::toString)
                .limit(20)
                .toList();
            
            if (matches.isEmpty()) {
                return ToolResult.success("No files found matching: " + pattern);
            }
            
            return ToolResult.success("Found files:\n" + String.join("\n", matches));
            
        } catch (IOException e) {
            throw new ToolExecutionException("File search failed: " + e.getMessage());
        }
    }
}
```

### Exercise 4 (Challenge): Web scraper tool

**Solution:**

```java
@Component
public class WebScraperTool implements Tool {
    
    private final RestTemplate restTemplate;
    
    @Override
    public String name() {
        return "scrape_webpage";
    }
    
    @Override
    public String description() {
        return "Extract text content from a webpage URL";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        String url = (String) params.get("url");
        validateUrl(url);
        
        try {
            String html = restTemplate.getForObject(url, String.class);
            String text = extractText(html);
            
            return ToolResult.success(text);
            
        } catch (Exception e) {
            throw new ToolExecutionException("Failed to scrape URL: " + e.getMessage());
        }
    }
    
    private void validateUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL scheme");
        }
    }
    
    private String extractText(String html) {
        // Remove HTML tags and extract text
        return html
            .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
            .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
            .replaceAll("<[^>]+>", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
```

---

## Chapter 4: Implementing Conversation Memory - Solutions

### Exercise 1: Implement conversation history

**Solution:**

```java
@Service
public class ConversationMemory {
    
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();
    
    public void addMessage(String conversationId, Message message) {
        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>())
            .add(message);
    }
    
    public List<Message> getHistory(String conversationId) {
        return conversations.getOrDefault(conversationId, List.of());
    }
    
    public List<Message> getRecentHistory(String conversationId, int limit) {
        List<Message> history = getHistory(conversationId);
        int start = Math.max(0, history.size() - limit);
        return history.subList(start, history.size());
    }
    
    public void clearHistory(String conversationId) {
        conversations.remove(conversationId);
    }
    
    public record Message(String role, String content, Instant timestamp) {}
}
```

### Exercise 2: Test memory persistence

**Solution:**

```java
@Test
void shouldStoreAndRetrieveMessages() {
    ConversationMemory memory = new ConversationMemory();
    String convId = "test-123";
    
    memory.addMessage(convId, new Message("user", "Hello", Instant.now()));
    memory.addMessage(convId, new Message("assistant", "Hi there", Instant.now()));
    
    List<Message> history = memory.getHistory(convId);
    
    assertThat(history).hasSize(2);
    assertThat(history.get(0).content()).isEqualTo("Hello");
    assertThat(history.get(1).content()).isEqualTo("Hi there");
}

@Test
void shouldLimitRecentHistory() {
    ConversationMemory memory = new ConversationMemory();
    String convId = "test-456";
    
    for (int i = 0; i < 10; i++) {
        memory.addMessage(convId, new Message("user", "Message " + i, Instant.now()));
    }
    
    List<Message> recent = memory.getRecentHistory(convId, 3);
    
    assertThat(recent).hasSize(3);
    assertThat(recent.get(0).content()).isEqualTo("Message 7");
    assertThat(recent.get(2).content()).isEqualTo("Message 9");
}
```

### Exercise 3 (Bonus): Sliding window memory

**Solution:**

```java
public class SlidingWindowMemory {
    
    private final int maxMessages;
    private final Map<String, Deque<Message>> conversations = new ConcurrentHashMap<>();
    
    public SlidingWindowMemory(int maxMessages) {
        this.maxMessages = maxMessages;
    }
    
    public void addMessage(String conversationId, Message message) {
        Deque<Message> history = conversations.computeIfAbsent(
            conversationId, 
            k -> new ArrayDeque<>(maxMessages)
        );
        
        if (history.size() >= maxMessages) {
            history.removeFirst();
        }
        
        history.addLast(message);
    }
    
    public List<Message> getHistory(String conversationId) {
        return new ArrayList<>(
            conversations.getOrDefault(conversationId, new ArrayDeque<>())
        );
    }
}
```

### Exercise 4 (Challenge): Summary-based memory

**Solution:**

```java
@Service
public class SummaryBasedMemory {
    
    private final ChatLanguageModel chatModel;
    private final Map<String, ConversationState> states = new ConcurrentHashMap<>();
    
    public void addMessage(String conversationId, Message message) {
        ConversationState state = states.computeIfAbsent(
            conversationId, 
            k -> new ConversationState()
        );
        
        state.recentMessages.add(message);
        
        // Summarize when we hit threshold
        if (state.recentMessages.size() >= 10) {
            summarizeAndCompress(conversationId, state);
        }
    }
    
    private void summarizeAndCompress(String conversationId, ConversationState state) {
        String conversationText = state.recentMessages.stream()
            .map(m -> m.role() + ": " + m.content())
            .collect(Collectors.joining("\n"));
        
        String prompt = String.format("""
            Summarize this conversation in 2-3 sentences, preserving key facts and context:
            
            %s
            
            Summary:
            """, conversationText);
        
        String summary = chatModel.generate(prompt);
        
        state.summaries.add(summary);
        state.recentMessages.clear();
        
        logger.info("Summarized conversation {}: {}", conversationId, summary);
    }
    
    public String getContextForPrompt(String conversationId) {
        ConversationState state = states.get(conversationId);
        if (state == null) return "";
        
        StringBuilder context = new StringBuilder();
        
        // Add summaries
        if (!state.summaries.isEmpty()) {
            context.append("Previous conversation summary:\n");
            context.append(String.join("\n", state.summaries));
            context.append("\n\n");
        }
        
        // Add recent messages
        if (!state.recentMessages.isEmpty()) {
            context.append("Recent messages:\n");
            state.recentMessages.forEach(m -> 
                context.append(m.role()).append(": ").append(m.content()).append("\n")
            );
        }
        
        return context.toString();
    }
    
    static class ConversationState {
        List<String> summaries = new ArrayList<>();
        List<Message> recentMessages = new ArrayList<>();
    }
}
```

---

## Chapter 5: Building Specialized Agents - Solutions

### Exercise 1: Create a research agent

**Solution:**

```java
@Service
public class ResearchAgent extends BaseAgent {
    
    private final WebScraperTool webScraper;
    private final FileSearchTool fileSearch;
    
    public String research(String topic) {
        logger.info("Starting research on: {}", topic);
        
        // 1. Search local knowledge base
        String localResults = searchLocal(topic);
        
        // 2. Search web if needed
        String webResults = searchWeb(topic);
        
        // 3. Synthesize findings
        return synthesizeFindings(topic, localResults, webResults);
    }
    
    private String searchLocal(String topic) {
        Map<String, Object> params = Map.of("pattern", topic);
        ToolResult result = fileSearch.execute(params);
        return result.data();
    }
    
    private String searchWeb(String topic) {
        // Generate search queries
        List<String> queries = generateSearchQueries(topic);
        
        StringBuilder results = new StringBuilder();
        for (String query : queries) {
            String url = "https://search-engine.com?q=" + query;
            Map<String, Object> params = Map.of("url", url);
            
            try {
                ToolResult result = webScraper.execute(params);
                results.append(result.data()).append("\n\n");
            } catch (Exception e) {
                logger.warn("Web search failed for: {}", query, e);
            }
        }
        
        return results.toString();
    }
    
    private String synthesizeFindings(String topic, String local, String web) {
        String prompt = String.format("""
            Research topic: %s
            
            Local knowledge base results:
            %s
            
            Web search results:
            %s
            
            Provide a comprehensive summary of the key findings.
            """, topic, local, web);
        
        return chatModel.generate(prompt);
    }
}
```

### Exercise 2: Create a code reviewer agent

**Solution:**

```java
@Service
public class CodeReviewAgent extends BaseAgent {
    
    public CodeReview review(String code, String language) {
        logger.info("Reviewing {} code", language);
        
        List<Issue> issues = new ArrayList<>();
        
        // Check various aspects
        issues.addAll(checkSecurity(code, language));
        issues.addAll(checkPerformance(code, language));
        issues.addAll(checkStyle(code, language));
        issues.addAll(checkBugs(code, language));
        
        return new CodeReview(code, language, issues, calculateScore(issues));
    }
    
    private List<Issue> checkSecurity(String code, String language) {
        String prompt = String.format("""
            Review this %s code for security vulnerabilities:
            
            ```%s
            %s
            ```
            
            List any security issues found. For each issue:
            - Line number (if applicable)
            - Severity (high/medium/low)
            - Description
            - Recommendation
            
            Return as JSON array.
            """, language, language, code);
        
        String response = chatModel.generate(prompt);
        return parseIssues(response, "security");
    }
    
    private double calculateScore(List<Issue> issues) {
        long highSeverity = issues.stream()
            .filter(i -> i.severity().equals("high"))
            .count();
        
        long mediumSeverity = issues.stream()
            .filter(i -> i.severity().equals("medium"))
            .count();
        
        // Start at 100, deduct points
        double score = 100.0;
        score -= highSeverity * 20;
        score -= mediumSeverity * 10;
        score -= (issues.size() - highSeverity - mediumSeverity) * 5;
        
        return Math.max(0, score);
    }
    
    public record CodeReview(String code, String language, List<Issue> issues, double score) {}
    public record Issue(String category, String severity, String description, String line) {}
}
```

### Exercise 3 (Bonus): Create a data analyst agent

**Solution:**

```java
@Service
public class DataAnalystAgent extends BaseAgent {
    
    private final DatabaseQueryTool dbTool;
    private final CalculatorTool calculator;
    
    public AnalysisReport analyze(String question) {
        logger.info("Analyzing: {}", question);
        
        // 1. Generate SQL query
        String query = generateQuery(question);
        logger.info("Generated query: {}", query);
        
        // 2. Execute query
        List<Map<String, Object>> data = executeQuery(query);
        
        // 3. Perform statistical analysis
        Statistics stats = calculateStatistics(data);
        
        // 4. Generate insights
        String insights = generateInsights(question, data, stats);
        
        return new AnalysisReport(question, query, data, stats, insights);
    }
    
    private String generateQuery(String question) {
        String prompt = String.format("""
            Generate a SQL query to answer this question:
            "%s"
            
            Available tables:
            - users (id, name, email, created_at)
            - orders (id, user_id, amount, created_at)
            - products (id, name, price, category)
            
            Return only the SQL query.
            """, question);
        
        return chatModel.generate(prompt).trim();
    }
    
    private Statistics calculateStatistics(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return new Statistics(0, 0, 0, 0, 0);
        }
        
        // Assuming numeric data in first column
        List<Double> values = data.stream()
            .map(row -> row.values().iterator().next())
            .map(v -> ((Number) v).doubleValue())
            .toList();
        
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / values.size();
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        return new Statistics(values.size(), sum, mean, min, max);
    }
    
    public record AnalysisReport(
        String question,
        String query,
        List<Map<String, Object>> data,
        Statistics stats,
        String insights
    ) {}
    
    public record Statistics(int count, double sum, double mean, double min, double max) {}
}
```

### Exercise 4 (Challenge): Agent collaboration framework

**Solution:**

```java
@Service
public class AgentCollaborationFramework {
    
    private final Map<String, BaseAgent> agents = new ConcurrentHashMap<>();
    
    public void registerAgent(String name, BaseAgent agent) {
        agents.put(name, agent);
        logger.info("Registered agent: {}", name);
    }
    
    public String solveComplexTask(String task) {
        // 1. Analyze task and determine required agents
        List<String> requiredAgents = determineRequiredAgents(task);
        logger.info("Task requires agents: {}", requiredAgents);
        
        // 2. Break down task into subtasks
        Map<String, String> subtasks = decomposeTask(task, requiredAgents);
        
        // 3. Execute subtasks in parallel
        Map<String, String> results = new ConcurrentHashMap<>();
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<Void>> tasks = subtasks.entrySet().stream()
                .map(entry -> scope.fork(() -> {
                    BaseAgent agent = agents.get(entry.getKey());
                    String result = agent.process(entry.getValue());
                    results.put(entry.getKey(), result);
                    return null;
                }))
                .toList();
            
            scope.join();
            scope.throwIfFailed();
            
        } catch (Exception e) {
            throw new RuntimeException("Agent collaboration failed", e);
        }
        
        // 4. Synthesize final result
        return synthesizeResults(task, results);
    }
    
    private List<String> determineRequiredAgents(String task) {
        // Use LLM to determine which agents are needed
        String prompt = String.format("""
            Available agents: %s
            
            Task: %s
            
            Which agents are needed to complete this task?
            Return as comma-separated list.
            """, String.join(", ", agents.keySet()), task);
        
        String response = chatModel.generate(prompt);
        return Arrays.stream(response.split(","))
            .map(String::trim)
            .toList();
    }
}
```

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Next Module: Security and Guardrails](../module-05-security-guardrails/README.md)**
