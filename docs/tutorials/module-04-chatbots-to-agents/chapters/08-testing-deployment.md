# Chapter 8: Testing and Production Deployment

Building reliable agent systems requires comprehensive testing and careful production deployment. This chapter covers testing strategies, deployment patterns, monitoring, and operational best practices for intelligent agents.

## Learning Objectives

- Write effective unit tests for agents
- Implement integration tests for multi-agent systems
- Mock LLM calls for deterministic testing
- Deploy agents to production environments
- Monitor agent performance and behavior
- Handle failures and implement circuit breakers
- Manage API costs and rate limits

## Testing Strategy Overview

Agent systems present unique testing challenges:

1. **Non-deterministic behavior**: LLMs produce varying responses
2. **External dependencies**: APIs, databases, LLMs
3. **Complex state**: Conversation memory, multi-step reasoning
4. **Asynchronous operations**: Parallel tool execution
5. **Cost considerations**: API calls during testing

## Unit Testing Agents

### Testing ReAct Agent Logic

```java
@SpringBootTest
class ReActAgentTest {

    @Autowired
    private ReActAgent reActAgent;

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private CustomerDataTool customerDataTool;

    @MockBean
    private WeatherTool weatherTool;

    @Test
    void testSimpleReActLoop() {
        // Arrange
        when(chatModel.chat(anyString()))
            .thenReturn("""
                THOUGHT: I need to get customer information
                ACTION: getCustomerInfo(12345)
                """)
            .thenReturn("""
                FINAL ANSWER: Customer John Doe is on the Premium plan.
                """);

        when(customerDataTool.getCustomerInfo("12345"))
            .thenReturn("""
                Customer Information:
                - ID: 12345
                - Name: John Doe
                - Subscription Plan: Premium
                """);

        // Act
        String result = reActAgent.solve(
            "What subscription plan is customer 12345 on?"
        );

        // Assert
        assertThat(result).contains("Premium plan");
        verify(chatModel, times(2)).chat(anyString());
        verify(customerDataTool, times(1)).getCustomerInfo("12345");
    }

    @Test
    void testMaxIterationsReached() {
        // Arrange: LLM never provides FINAL ANSWER
        when(chatModel.chat(anyString()))
            .thenReturn("""
                THOUGHT: Thinking...
                ACTION: getCustomerInfo(12345)
                """);

        when(customerDataTool.getCustomerInfo(anyString()))
            .thenReturn("Customer data");

        // Act
        String result = reActAgent.solve("Test question", 3);

        // Assert
        assertThat(result).contains("Reached max iterations");
        verify(chatModel, times(3)).chat(anyString());
    }

    @Test
    void testInvalidActionFormat() {
        // Arrange
        when(chatModel.chat(anyString()))
            .thenReturn("""
                THOUGHT: Getting data
                ACTION: invalid-format-here
                """)
            .thenReturn("""
                FINAL ANSWER: Unable to complete task.
                """);

        // Act
        String result = reActAgent.solve("Test question");

        // Assert
        assertThat(result).isNotNull();
        verify(chatModel, atLeast(1)).chat(anyString());
    }
}
```

### Testing Conversation Memory

```java
@SpringBootTest
class ConversationMemoryServiceTest {

    @Autowired
    private ConversationMemoryService memoryService;

    @MockBean
    private RedisChatMemoryStore memoryStore;

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID().toString();
        when(memoryStore.getMessages(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void testAddAndRetrieveMessages() {
        // Add user message
        memoryService.addMessage(sessionId, "Hello");

        // Add AI message
        memoryService.addAiMessage(sessionId, "Hi there!");

        // Retrieve history
        List<ChatMessage> history = memoryService.getHistory(sessionId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0)).isInstanceOf(UserMessage.class);
        assertThat(history.get(1)).isInstanceOf(AiMessage.class);
    }

    @Test
    void testClearMemory() {
        // Add messages
        memoryService.addMessage(sessionId, "Test message");

        // Clear memory
        memoryService.clearMemory(sessionId);

        // Verify cleared
        int count = memoryService.getMessageCount(sessionId);
        assertThat(count).isZero();
    }

    @Test
    void testMemoryWindowLimit() {
        // Add more messages than window size
        for (int i = 0; i < 25; i++) {
            memoryService.addMessage(sessionId, "Message " + i);
        }

        // Verify window limit is enforced (max 20)
        int count = memoryService.getMessageCount(sessionId);
        assertThat(count).isLessThanOrEqualTo(20);
    }
}
```

### Testing Multi-Agent Orchestrator

```java
@SpringBootTest
class MultiAgentOrchestratorTest {

    @Autowired
    private MultiAgentOrchestrator orchestrator;

    @MockBean
    private ChatModel coordinatorModel;

    @MockBean
    private CustomerSupportAgent supportAgent;

    @MockBean
    private TechnicalDocAgent techAgent;

    @MockBean
    private ProductExpertAgent productAgent;

    @Test
    void testRoutingToCorrectAgent() {
        // Arrange
        when(coordinatorModel.chat(anyString()))
            .thenReturn("documentation");

        when(techAgent.process(anyString()))
            .thenReturn("Technical response");

        when(techAgent.getDescription())
            .thenReturn("Handles technical docs");

        // Act
        String result = orchestrator.routeRequest("How do I use the API?");

        // Assert
        verify(techAgent, times(1)).process(anyString());
        verify(supportAgent, never()).process(anyString());
        verify(productAgent, never()).process(anyString());
    }

    @Test
    void testCollaborativeMode() {
        // Arrange
        when(supportAgent.process(anyString()))
            .thenReturn("Support perspective");
        when(techAgent.process(anyString()))
            .thenReturn("Technical perspective");
        when(productAgent.process(anyString()))
            .thenReturn("Product perspective");
        when(coordinatorModel.chat(contains("Synthesize")))
            .thenReturn("Combined response");

        // Act
        String result = orchestrator.collaborativeRequest(
            "What are the benefits of your product?"
        );

        // Assert
        verify(supportAgent, times(1)).process(anyString());
        verify(techAgent, times(1)).process(anyString());
        verify(productAgent, times(1)).process(anyString());
        assertThat(result).contains("Combined response");
    }

    @Test
    void testRoutingFallback() {
        // Arrange: coordinator returns invalid agent key
        when(coordinatorModel.chat(anyString()))
            .thenReturn("invalid-agent-key");

        when(supportAgent.process(anyString()))
            .thenReturn("Fallback response");

        // Act
        String result = orchestrator.routeRequest("Some question");

        // Assert: should fall back to support agent
        verify(supportAgent, times(1)).process(anyString());
    }
}
```

### Testing Task Decomposition

```java
@SpringBootTest
class TaskDecomposerTest {

    @Autowired
    private TaskDecomposer taskDecomposer;

    @MockBean
    private ChatModel chatModel;

    @Test
    void testTaskDecomposition() {
        // Arrange: Mock LLM to return structured subtasks
        when(chatModel.chat(contains("Decompose")))
            .thenReturn("""
                SUBTASK id=task1 deps=[]
                description: Research the topic

                SUBTASK id=task2 deps=[task1]
                description: Write an outline based on research

                SUBTASK id=task3 deps=[task2]
                description: Write the full content
                """);

        // Mock subtask execution responses
        when(chatModel.chat(contains("Research the topic")))
            .thenReturn("Research completed");
        when(chatModel.chat(contains("Write an outline")))
            .thenReturn("Outline created");
        when(chatModel.chat(contains("Write the full content")))
            .thenReturn("Content written");
        when(chatModel.chat(contains("Provide a comprehensive summary")))
            .thenReturn("Final summary");

        // Act
        TaskDecomposer.TaskExecutionResult result =
            taskDecomposer.executeComplexTask("Write a blog post");

        // Assert
        assertThat(result.subtaskResults()).hasSize(3);
        assertThat(result.summary()).isEqualTo("Final summary");

        // Verify execution order (task1 -> task2 -> task3)
        List<TaskDecomposer.SubtaskResult> results = result.subtaskResults();
        assertThat(results.get(0).subtaskId()).isEqualTo("task1");
        assertThat(results.get(1).subtaskId()).isEqualTo("task2");
        assertThat(results.get(2).subtaskId()).isEqualTo("task3");
    }

    @Test
    void testFailedSubtaskHandling() {
        // Arrange
        when(chatModel.chat(contains("Decompose")))
            .thenReturn("""
                SUBTASK id=task1 deps=[]
                description: First task

                SUBTASK id=task2 deps=[task1]
                description: Second task
                """);

        when(chatModel.chat(contains("First task")))
            .thenThrow(new RuntimeException("LLM error"));

        // Act & Assert
        assertThatThrownBy(() ->
            taskDecomposer.executeComplexTask("Test task")
        ).isInstanceOf(RuntimeException.class);
    }
}
```

## Integration Testing

### Controller Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReActAgent reActAgent;

    @MockBean
    private MultiAgentOrchestrator orchestrator;

    @MockBean
    private TaskDecomposer taskDecomposer;

    @MockBean
    private ConversationMemoryService memoryService;

    @Test
    void testExecuteReActMode() throws Exception {
        // Arrange
        when(reActAgent.solve(anyString()))
            .thenReturn("Agent response");

        // Act & Assert
        mockMvc.perform(post("/api/v1/agent/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "message": "Test question",
                        "mode": "react"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Agent response"))
            .andExpect(jsonPath("$.mode").value("react"));

        verify(reActAgent, times(1)).solve("Test question");
    }

    @Test
    void testExecuteWithSession() throws Exception {
        // Arrange
        String sessionId = "test-session-123";
        when(orchestrator.routeRequest(anyString()))
            .thenReturn("Routed response");

        // Act & Assert
        mockMvc.perform(post("/api/v1/agent/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "message": "Test question",
                        "sessionId": "%s",
                        "mode": "multiagent"
                    }
                    """.formatted(sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId));

        verify(memoryService, times(1))
            .addMessage(sessionId, "Test question");
        verify(memoryService, times(1))
            .addAiMessage(eq(sessionId), anyString());
    }

    @Test
    void testClearSession() throws Exception {
        String sessionId = "test-session";

        mockMvc.perform(delete("/api/v1/agent/session/" + sessionId))
            .andExpect(status().isOk())
            .andExpect(content().string("Session cleared"));

        verify(memoryService, times(1)).clearMemory(sessionId);
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/agent/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Module 04")));
    }
}
```

### Database Integration Tests

```java
@SpringBootTest
@Testcontainers
class CustomerDataToolIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CustomerDataTool customerDataTool;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create test schema
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS customers (
                customer_id VARCHAR(50) PRIMARY KEY,
                name VARCHAR(255),
                email VARCHAR(255),
                subscription_plan VARCHAR(50),
                created_at TIMESTAMP
            )
            """);

        // Insert test data
        jdbcTemplate.execute("""
            INSERT INTO customers VALUES
            ('12345', 'John Doe', 'john@example.com', 'Premium', NOW())
            """);
    }

    @Test
    void testGetCustomerInfo() {
        String result = customerDataTool.getCustomerInfo("12345");

        assertThat(result)
            .contains("John Doe")
            .contains("john@example.com")
            .contains("Premium");
    }

    @Test
    void testCustomerNotFound() {
        String result = customerDataTool.getCustomerInfo("99999");

        assertThat(result).contains("Customer not found");
    }
}
```

## Deployment Strategies

### Containerization with Docker

Create a `Dockerfile` for the module:

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/module-04-chatbots-to-agents-1.0.0-SNAPSHOT.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/v1/agent/health || exit 1

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose for Local Development

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: workshop_module04
      POSTGRES_USER: workshop
      POSTGRES_PASSWORD: workshop123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  module-04:
    build: .
    ports:
      - "8084:8084"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/workshop_module04
      SPRING_DATASOURCE_USERNAME: workshop
      SPRING_DATASOURCE_PASSWORD: workshop123
      SPRING_DATA_REDIS_HOST: redis
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
  redis_data:
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agent-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: agent-service
  template:
    metadata:
      labels:
        app: agent-service
    spec:
      containers:
      - name: agent-service
        image: techcorp/module-04-agents:latest
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-credentials
              key: api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/v1/agent/health
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/agent/health
            port: 8084
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: agent-service
spec:
  selector:
    app: agent-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8084
  type: LoadBalancer
```

## Monitoring and Observability

### Metrics with Micrometer

```java
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

Add metrics to agent methods:

```java
@Service
public class ReActAgent {

    @Autowired
    private MeterRegistry meterRegistry;

    @Timed(value = "agent.react.solve", description = "Time to solve with ReAct")
    public String solve(String question) {
        Counter.builder("agent.react.invocations")
            .tag("type", "solve")
            .register(meterRegistry)
            .increment();

        try {
            String result = solveInternal(question);

            meterRegistry.counter("agent.react.success").increment();
            return result;

        } catch (Exception e) {
            meterRegistry.counter("agent.react.errors",
                "error", e.getClass().getSimpleName()
            ).increment();
            throw e;
        }
    }
}
```

### Logging Best Practices

```java
@Service
public class ReActAgent {
    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    public String solve(String question) {
        // Use structured logging
        log.info("ReAct agent invoked",
            kv("question_length", question.length()),
            kv("max_iterations", maxIterations));

        try {
            String result = solveInternal(question);

            log.info("ReAct agent completed successfully",
                kv("iterations_used", iterationsUsed),
                kv("tools_called", toolsCalled));

            return result;

        } catch (Exception e) {
            log.error("ReAct agent failed",
                kv("question", question),
                kv("error", e.getMessage()),
                e);
            throw e;
        }
    }
}
```

### Distributed Tracing

Add OpenTelemetry or Sleuth for distributed tracing:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

## Error Handling and Resilience

### Circuit Breaker Pattern

Use Resilience4j for circuit breaker:

```java
@Service
public class ResilientReActAgent {

    private final ReActAgent reActAgent;
    private final CircuitBreaker circuitBreaker;

    public ResilientReActAgent(
        ReActAgent reActAgent,
        CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.reActAgent = reActAgent;
        this.circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("reactAgent");
    }

    public String solve(String question) {
        return circuitBreaker.executeSupplier(() ->
            reActAgent.solve(question)
        );
    }
}
```

Configuration:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      reactAgent:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

### Retry Logic

```java
@Service
public class RetryableAgent {

    @Retry(name = "agentRetry", fallbackMethod = "fallbackSolve")
    public String solve(String question) {
        return reActAgent.solve(question);
    }

    private String fallbackSolve(String question, Exception e) {
        log.warn("Agent failed, using fallback response", e);
        return "I'm experiencing technical difficulties. " +
               "Please try again in a moment.";
    }
}
```

```yaml
resilience4j:
  retry:
    instances:
      agentRetry:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

### Rate Limiting

Protect against API abuse:

```java
@Service
public class RateLimitedAgent {

    private final RateLimiter rateLimiter;

    @RateLimiter(name = "agentRateLimit")
    public String solve(String question) {
        return reActAgent.solve(question);
    }
}
```

```yaml
resilience4j:
  ratelimiter:
    instances:
      agentRateLimit:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0s
```

## Cost Management

### Token Usage Tracking

```java
@Service
public class CostTrackingAgent {

    @Autowired
    private MeterRegistry meterRegistry;

    public String solve(String question) {
        int inputTokens = estimateTokens(question);

        String response = reActAgent.solve(question);

        int outputTokens = estimateTokens(response);
        int totalTokens = inputTokens + outputTokens;

        // Track token usage
        meterRegistry.counter("agent.tokens.used",
            "type", "total"
        ).increment(totalTokens);

        // Estimate cost (example: gpt-4o-mini pricing)
        double cost = (inputTokens * 0.00015 / 1000) +
                     (outputTokens * 0.0006 / 1000);

        meterRegistry.summary("agent.cost.usd").record(cost);

        return response;
    }

    private int estimateTokens(String text) {
        // Rough estimate: 1 token ≈ 4 characters
        return text.length() / 4;
    }
}
```

### Caching Strategies

Cache LLM responses for common queries:

```java
@Service
@CacheConfig(cacheNames = "agentResponses")
public class CachedAgent {

    @Cacheable(key = "#question.hashCode()")
    public String solve(String question) {
        return reActAgent.solve(question);
    }

    @CacheEvict(allEntries = true)
    @Scheduled(fixedRate = 3600000) // Clear every hour
    public void evictCache() {
        log.info("Evicting agent response cache");
    }
}
```

## Practice Exercises

### Exercise 1: Write End-to-End Tests

Create tests that verify the complete flow:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AgentE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCompleteAgentFlow() {
        // Execute agent request
        AgentRequest request = new AgentRequest(
            "Test question",
            null,
            "react"
        );

        ResponseEntity<AgentResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/agent/execute",
            request,
            AgentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isNotBlank();
        assertThat(response.getBody().sessionId()).isNotNull();
    }
}
```

### Exercise 2: Add Performance Tests

Use JMeter or Gatling for load testing:

```scala
// Gatling scenario
class AgentLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8084")
    .acceptHeader("application/json")

  val scn = scenario("Agent Load Test")
    .exec(
      http("execute agent")
        .post("/api/v1/agent/execute")
        .body(StringBody("""{"message":"Test","mode":"react"}"""))
        .check(status.is(200))
    )

  setUp(
    scn.inject(
      rampUsersPerSec(1) to 10 during (30 seconds),
      constantUsersPerSec(10) during (60 seconds)
    )
  ).protocols(httpProtocol)
}
```

### Exercise 3: Implement Health Checks

Comprehensive health indicators:

```java
@Component
public class AgentHealthIndicator implements HealthIndicator {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private RedisConnectionFactory redisFactory;

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try {
            // Check LLM connectivity
            chatModel.chat("health check");

            // Check Redis
            redisFactory.getConnection().ping();

            // Check database
            try (Connection conn = dataSource.getConnection()) {
                conn.isValid(1);
            }

            return Health.up()
                .withDetail("llm", "available")
                .withDetail("redis", "connected")
                .withDetail("database", "connected")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Key Takeaways

- **Unit testing** validates individual components with mocked dependencies
- **Integration testing** verifies interactions between components
- **Mocking LLMs** enables deterministic, cost-effective tests
- **Containerization** simplifies deployment and scaling
- **Monitoring** provides visibility into agent behavior and performance
- **Circuit breakers** protect against cascading failures
- **Rate limiting** prevents abuse and controls costs
- **Caching** reduces redundant LLM calls
- **Health checks** enable automated recovery and alerting

## Production Checklist

Before deploying to production:

- [ ] All tests passing (unit, integration, e2e)
- [ ] Performance benchmarks met
- [ ] Security review completed
- [ ] API keys stored in secrets manager
- [ ] Monitoring and alerting configured
- [ ] Circuit breakers and retries implemented
- [ ] Rate limiting in place
- [ ] Cost tracking enabled
- [ ] Documentation updated
- [ ] Runbook for common issues created
- [ ] Rollback plan documented
- [ ] Load testing completed

## What's Next?

Continue to [Conclusion and Next Steps](09-conclusion.md) to review what you've learned and explore advanced topics.

---

**Previous**: [Chapter 7: Complex Task Decomposition](07-task-decomposition.md) | **Next**: [Conclusion and Next Steps](09-conclusion.md)
