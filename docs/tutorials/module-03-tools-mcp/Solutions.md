# Module 03: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 03: Tools and Model Context Protocol.

## How to Run and Test Solutions

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL database (for database tools)
- Weather API key (for weather tool exercises)

### Project Location
```bash
cd src/module-03-tools-mcp/
```

### Database Setup
```bash
# Start PostgreSQL with Docker
docker run --name postgres-mcp \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=toolsdb \
  -p 5432:5432 \
  -d postgres:15

# Create test table
docker exec -it postgres-mcp psql -U postgres -d toolsdb -c \
  "CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(100));"

# Insert test data
docker exec -it postgres-mcp psql -U postgres -d toolsdb -c \
  "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com');"
```

### Configuration
Update `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/toolsdb
    username: postgres
    password: password

weather:
  api:
    key: YOUR_API_KEY_HERE  # Get from openweathermap.org
```

### Running the Application
```bash
mvn clean spring-boot:run
```

### Running Tests
```bash
# Run all tests
mvn test

# Run with Testcontainers (automatically starts PostgreSQL)
mvn test -Dtest=DatabaseQueryToolTest

# Run integration tests
mvn verify
```

### Testing Tool Endpoints
```bash
# Execute a database query
curl -X POST http://localhost:8080/api/v1/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT * FROM users LIMIT 10"
  }'

# Get weather information
curl -X POST http://localhost:8080/api/v1/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the weather in London?"
  }'

# List available tools
curl http://localhost:8080/api/v1/tools/list
```

### Adding Your Solution Code
1. **Tool implementations**: `src/main/java/com/example/toolsmcp/tools/`
   - `DatabaseQueryTool.java`
   - `WeatherApiTool.java`
   - Custom tools you create

2. **MCP configuration**: `src/main/java/com/example/toolsmcp/config/`
   - `McpServerConfiguration.java`

3. **Orchestrator**: `src/main/java/com/example/toolsmcp/service/`
   - `ToolOrchestrator.java`

4. **Controller**: `src/main/java/com/example/toolsmcp/controller/`
   - `ToolController.java`

### Verifying Solutions
1. **Tool registration**: Check logs for "Registered tool: tool_name"
2. **Database queries**: Verify safe queries execute, dangerous ones are blocked
3. **API calls**: Weather tool returns valid temperature data
4. **Error handling**: Invalid inputs return proper error messages

### Security Testing
```bash
# This should be REJECTED (dangerous query)
curl -X POST http://localhost:8080/api/v1/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "DROP TABLE users"
  }'

# Expected response: {"error": "Query contains forbidden keyword: DROP"}
```

### Troubleshooting
- **Database connection fails**: Verify PostgreSQL is running and credentials are correct
- **Weather API errors**: Check API key is valid and has quota remaining
- **Tool not found**: Ensure tool is registered in `McpServerConfiguration`

---

## Chapter 2: Database Tools - Solutions

### Exercise 1: Implement database query tool

**Solution:**

```java
@Component
public class DatabaseQueryTool {
    
    private final JdbcTemplate jdbcTemplate;
    
    @ToolDefinition(
        name = "query_database",
        description = "Execute a SQL query against the database"
    )
    public List<Map<String, Object>> queryDatabase(
            @ToolParameter(description = "SQL query to execute") String query) {
        
        try {
            return jdbcTemplate.queryForList(query);
        } catch (DataAccessException e) {
            throw new ToolExecutionException("Failed to execute query: " + e.getMessage());
        }
    }
}
```

### Exercise 2: Add query validation

**Solution:**

```java
public List<Map<String, Object>> queryDatabase(String query) {
    validateQuery(query);
    
    try {
        return jdbcTemplate.queryForList(query);
    } catch (DataAccessException e) {
        throw new ToolExecutionException("Failed to execute query: " + e.getMessage());
    }
}

private void validateQuery(String query) {
    String upperQuery = query.trim().toUpperCase();
    
    // Only allow SELECT queries
    if (!upperQuery.startsWith("SELECT")) {
        throw new IllegalArgumentException("Only SELECT queries are allowed");
    }
    
    // Prevent dangerous operations
    List<String> forbidden = List.of("DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE");
    for (String keyword : forbidden) {
        if (upperQuery.contains(keyword)) {
            throw new IllegalArgumentException("Query contains forbidden keyword: " + keyword);
        }
    }
    
    // Prevent multiple statements
    if (query.contains(";") && !query.trim().endsWith(";")) {
        throw new IllegalArgumentException("Multiple statements not allowed");
    }
}
```

### Exercise 3 (Bonus): Add pagination

**Solution:**

```java
@ToolDefinition(
    name = "query_database_paged",
    description = "Execute a paginated SQL query"
)
public PagedResult queryDatabasePaged(
        @ToolParameter(description = "SQL query") String query,
        @ToolParameter(description = "Page number (0-indexed)") int page,
        @ToolParameter(description = "Page size") int pageSize) {
    
    validateQuery(query);
    
    int offset = page * pageSize;
    String pagedQuery = query + " LIMIT " + pageSize + " OFFSET " + offset;
    
    List<Map<String, Object>> results = jdbcTemplate.queryForList(pagedQuery);
    int totalCount = getTotalCount(query);
    
    return new PagedResult(results, page, pageSize, totalCount);
}

private int getTotalCount(String query) {
    String countQuery = "SELECT COUNT(*) FROM (" + query + ") AS total";
    return jdbcTemplate.queryForObject(countQuery, Integer.class);
}

public record PagedResult(
    List<Map<String, Object>> data,
    int page,
    int pageSize,
    int total
) {}
```

### Exercise 4 (Challenge): Schema introspection tool

**Solution:**

```java
@ToolDefinition(
    name = "describe_table",
    description = "Get schema information for a database table"
)
public TableSchema describeTable(
        @ToolParameter(description = "Table name") String tableName) {
    
    String query = """
        SELECT column_name, data_type, is_nullable, column_default
        FROM information_schema.columns
        WHERE table_name = ?
        ORDER BY ordinal_position
        """;
    
    List<ColumnInfo> columns = jdbcTemplate.query(query, 
        (rs, rowNum) -> new ColumnInfo(
            rs.getString("column_name"),
            rs.getString("data_type"),
            rs.getString("is_nullable").equals("YES"),
            rs.getString("column_default")
        ),
        tableName
    );
    
    return new TableSchema(tableName, columns);
}

public record ColumnInfo(String name, String type, boolean nullable, String defaultValue) {}
public record TableSchema(String tableName, List<ColumnInfo> columns) {}
```

---

## Chapter 3: External API Tools - Solutions

### Exercise 1: Implement weather API tool

**Solution:**

```java
@Component
public class WeatherApiTool {
    
    private final RestTemplate restTemplate;
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    @ToolDefinition(
        name = "get_weather",
        description = "Get current weather for a location"
    )
    public WeatherData getWeather(
            @ToolParameter(description = "City name") String city) {
        
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric",
            city, apiKey
        );
        
        try {
            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
            
            return new WeatherData(
                city,
                response.main().temp(),
                response.weather().get(0).description(),
                response.main().humidity()
            );
        } catch (Exception e) {
            throw new ToolExecutionException("Failed to fetch weather: " + e.getMessage());
        }
    }
    
    public record WeatherData(String city, double temperature, String description, int humidity) {}
    
    record WeatherResponse(MainData main, List<WeatherInfo> weather) {}
    record MainData(double temp, int humidity) {}
    record WeatherInfo(String description) {}
}
```

### Exercise 2: Add error handling and retries

**Solution:**

```java
@Component
public class WeatherApiTool {
    
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    
    public WeatherApiTool(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(1000)
            .retryOn(RestClientException.class)
            .build();
    }
    
    public WeatherData getWeather(String city) {
        return retryTemplate.execute(context -> {
            logger.info("Fetching weather for {} (attempt {})", 
                city, context.getRetryCount() + 1);
            
            try {
                WeatherResponse response = restTemplate.getForObject(
                    buildUrl(city), 
                    WeatherResponse.class
                );
                
                if (response == null) {
                    throw new ToolExecutionException("No weather data returned");
                }
                
                return mapToWeatherData(city, response);
                
            } catch (HttpClientErrorException.NotFound e) {
                throw new ToolExecutionException("City not found: " + city);
            } catch (RestClientException e) {
                logger.warn("API call failed: {}", e.getMessage());
                throw e; // Retry
            }
        });
    }
}
```

### Exercise 3 (Bonus): Cache API responses

**Solution:**

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("weatherCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, 
                    WeatherData.class,
                    ResourcePoolsBuilder.heap(100))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
            )
            .build(true);
    }
}

@Component
public class WeatherApiTool {
    
    @Cacheable(value = "weatherCache", key = "#city")
    public WeatherData getWeather(String city) {
        logger.info("Cache miss - fetching weather for {}", city);
        // ... API call
    }
}
```

### Exercise 4 (Challenge): Multi-location weather comparison

**Solution:**

```java
@ToolDefinition(
    name = "compare_weather",
    description = "Compare weather across multiple cities"
)
public WeatherComparison compareWeather(
        @ToolParameter(description = "List of cities") List<String> cities) {
    
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        
        List<Subtask<WeatherData>> tasks = cities.stream()
            .map(city -> scope.fork(() -> getWeather(city)))
            .toList();
        
        scope.join();
        scope.throwIfFailed();
        
        List<WeatherData> results = tasks.stream()
            .map(Subtask::get)
            .toList();
        
        return new WeatherComparison(results, findWarmest(results), findColdest(results));
        
    } catch (Exception e) {
        throw new ToolExecutionException("Failed to compare weather: " + e.getMessage());
    }
}

public record WeatherComparison(
    List<WeatherData> cities,
    WeatherData warmest,
    WeatherData coldest
) {}
```

---

## Chapter 4: MCP Server Configuration - Solutions

### Exercise 1: Create MCP server configuration

**Solution:**

```java
@Configuration
public class McpServerConfiguration {
    
    @Bean
    public McpServer mcpServer(List<Tool> tools) {
        return McpServer.builder()
            .serverInfo(new ServerInfo(
                "java-llm-workshop-mcp",
                "1.0.0"
            ))
            .tools(tools)
            .transport(StdioTransport.create())
            .build();
    }
    
    @Bean
    public List<Tool> registeredTools(
            DatabaseQueryTool dbTool,
            WeatherApiTool weatherTool) {
        
        return List.of(
            Tool.builder()
                .name("query_database")
                .description("Query the database")
                .handler(dbTool::queryDatabase)
                .build(),
            Tool.builder()
                .name("get_weather")
                .description("Get weather information")
                .handler(weatherTool::getWeather)
                .build()
        );
    }
}
```

### Exercise 2: Add tool validation

**Solution:**

```java
@Component
public class ToolValidator {
    
    public void validateTool(Tool tool) {
        if (tool.name() == null || tool.name().isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be blank");
        }
        
        if (tool.description() == null || tool.description().isBlank()) {
            throw new IllegalArgumentException("Tool description cannot be blank");
        }
        
        if (tool.handler() == null) {
            throw new IllegalArgumentException("Tool handler cannot be null");
        }
        
        // Validate name format (lowercase, underscores only)
        if (!tool.name().matches("^[a-z][a-z0-9_]*$")) {
            throw new IllegalArgumentException(
                "Tool name must be lowercase with underscores only: " + tool.name()
            );
        }
    }
    
    @Bean
    public List<Tool> registeredTools(List<Tool> tools, ToolValidator validator) {
        tools.forEach(validator::validateTool);
        return tools;
    }
}
```

### Exercise 3 (Bonus): Dynamic tool registration

**Solution:**

```java
@Service
public class DynamicToolRegistry {
    
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final McpServer mcpServer;
    
    public void registerTool(Tool tool) {
        validateTool(tool);
        
        tools.put(tool.name(), tool);
        mcpServer.addTool(tool);
        
        logger.info("Registered tool: {}", tool.name());
    }
    
    public void unregisterTool(String toolName) {
        Tool removed = tools.remove(toolName);
        
        if (removed != null) {
            mcpServer.removeTool(toolName);
            logger.info("Unregistered tool: {}", toolName);
        }
    }
    
    public List<Tool> listTools() {
        return new ArrayList<>(tools.values());
    }
    
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
}
```

### Exercise 4 (Challenge): Tool permissions system

**Solution:**

```java
@Service
public class ToolPermissionService {
    
    private final Map<String, Set<String>> toolPermissions = new ConcurrentHashMap<>();
    
    public void grantPermission(String userId, String toolName) {
        toolPermissions.computeIfAbsent(userId, k -> new HashSet<>()).add(toolName);
        logger.info("Granted {} permission to use {}", userId, toolName);
    }
    
    public void revokePermission(String userId, String toolName) {
        Set<String> userTools = toolPermissions.get(userId);
        if (userTools != null) {
            userTools.remove(toolName);
        }
    }
    
    public boolean hasPermission(String userId, String toolName) {
        Set<String> userTools = toolPermissions.get(userId);
        return userTools != null && userTools.contains(toolName);
    }
    
    public ToolExecutionResult executeWithPermission(
            String userId, 
            String toolName, 
            Map<String, Object> params) {
        
        if (!hasPermission(userId, toolName)) {
            return ToolExecutionResult.error(
                "User " + userId + " does not have permission to use " + toolName
            );
        }
        
        // Execute tool
        Tool tool = toolRegistry.getTool(toolName)
            .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));
        
        return tool.execute(params);
    }
}
```

---

## Chapter 5: Tool Orchestrator - Solutions

### Exercise 1: Build tool orchestrator

**Solution:**

```java
@Service
public class ToolOrchestrator {
    
    private final Map<String, Tool> tools;
    private final ChatLanguageModel chatModel;
    
    public String processRequest(String userRequest) {
        // 1. Determine which tool to use
        ToolSelection selection = selectTool(userRequest);
        
        // 2. Extract parameters
        Map<String, Object> params = extractParameters(userRequest, selection.tool());
        
        // 3. Execute tool
        ToolExecutionResult result = executeTool(selection.tool(), params);
        
        // 4. Format response
        return formatResponse(userRequest, result);
    }
    
    private ToolSelection selectTool(String request) {
        String prompt = String.format("""
            Given the user request: "%s"
            
            Available tools:
            %s
            
            Which tool should be used? Respond with just the tool name.
            """, request, formatToolList());
        
        String toolName = chatModel.generate(prompt).trim();
        
        Tool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalStateException("Invalid tool selected: " + toolName);
        }
        
        return new ToolSelection(tool, toolName);
    }
    
    record ToolSelection(Tool tool, String name) {}
}
```

### Exercise 2: Test tool selection

**Solution:**

```java
@Test
void shouldSelectCorrectToolForWeatherQuery() {
    String request = "What's the weather in London?";
    
    ToolSelection selection = orchestrator.selectTool(request);
    
    assertThat(selection.name()).isEqualTo("get_weather");
}

@Test
void shouldSelectCorrectToolForDatabaseQuery() {
    String request = "Show me all users in the database";
    
    ToolSelection selection = orchestrator.selectTool(request);
    
    assertThat(selection.name()).isEqualTo("query_database");
}
```

### Exercise 3 (Bonus): Multi-tool orchestration

**Solution:**

```java
public String processComplexRequest(String userRequest) {
    // 1. Analyze request and plan tool sequence
    List<ToolStep> plan = planToolExecution(userRequest);
    
    // 2. Execute tools in sequence, passing results forward
    Map<String, Object> context = new HashMap<>();
    
    for (ToolStep step : plan) {
        Map<String, Object> params = resolveParameters(step, context);
        ToolExecutionResult result = executeTool(step.tool(), params);
        
        context.put(step.name(), result.data());
    }
    
    // 3. Synthesize final answer from all results
    return synthesizeAnswer(userRequest, context);
}

private List<ToolStep> planToolExecution(String request) {
    String prompt = String.format("""
        Break down this request into a sequence of tool calls:
        "%s"
        
        Available tools: %s
        
        Return a JSON array of tool steps with dependencies.
        """, request, formatToolList());
    
    String response = chatModel.generate(prompt);
    return parseToolPlan(response);
}

record ToolStep(String name, Tool tool, Map<String, String> paramSources) {}
```

### Exercise 4 (Challenge): Parallel tool execution

**Solution:**

```java
public String processRequestParallel(String userRequest) {
    List<ToolStep> plan = planToolExecution(userRequest);
    
    // Group independent tools for parallel execution
    List<List<ToolStep>> batches = groupIndependentTools(plan);
    
    Map<String, Object> results = new HashMap<>();
    
    for (List<ToolStep> batch : batches) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            List<Subtask<ToolExecutionResult>> tasks = batch.stream()
                .map(step -> scope.fork(() -> {
                    Map<String, Object> params = resolveParameters(step, results);
                    return executeTool(step.tool(), params);
                }))
                .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            for (int i = 0; i < batch.size(); i++) {
                results.put(batch.get(i).name(), tasks.get(i).get().data());
            }
            
        } catch (Exception e) {
            throw new ToolExecutionException("Parallel execution failed", e);
        }
    }
    
    return synthesizeAnswer(userRequest, results);
}

private List<List<ToolStep>> groupIndependentTools(List<ToolStep> steps) {
    // Build dependency graph and group tools by level
    // Tools with no dependencies can run in parallel
    // Implementation depends on dependency analysis
    return List.of(steps); // Simplified
}
```

---

## Chapter 6: REST Controller - Solutions

### Exercise 1: Create tool execution endpoint

**Solution:**

```java
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {
    
    private final ToolOrchestrator orchestrator;
    
    @PostMapping("/execute")
    public ResponseEntity<ToolResponse> executeTool(
            @RequestBody @Valid ToolRequest request) {
        
        try {
            String result = orchestrator.processRequest(request.query());
            return ResponseEntity.ok(new ToolResponse(result, "Success", null));
            
        } catch (Exception e) {
            logger.error("Tool execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ToolResponse(null, "Error", e.getMessage()));
        }
    }
    
    public record ToolRequest(@NotBlank String query) {}
    public record ToolResponse(String result, String status, String error) {}
}
```

### Exercise 2: Add input validation

**Solution:**

```java
public record ToolRequest(
    @NotBlank(message = "Query is required")
    @Size(max = 1000, message = "Query too long")
    String query,
    
    @Pattern(regexp = "sync|async", message = "Mode must be sync or async")
    String mode
) {
    public ToolRequest {
        if (mode == null) {
            mode = "sync";
        }
    }
}

@PostMapping("/execute")
public ResponseEntity<ToolResponse> executeTool(@RequestBody @Valid ToolRequest request) {
    if (request.query().contains("DROP") || request.query().contains("DELETE")) {
        return ResponseEntity.badRequest()
            .body(new ToolResponse(null, "Error", "Dangerous operation detected"));
    }
    
    // ... execute
}
```

### Exercise 3 (Bonus): List available tools endpoint

**Solution:**

```java
@GetMapping("/list")
public ResponseEntity<List<ToolInfo>> listTools() {
    List<ToolInfo> tools = toolRegistry.listTools().stream()
        .map(tool -> new ToolInfo(
            tool.name(),
            tool.description(),
            tool.parameters().stream()
                .map(p -> new ParameterInfo(p.name(), p.type(), p.required()))
                .toList()
        ))
        .toList();
    
    return ResponseEntity.ok(tools);
}

public record ToolInfo(
    String name, 
    String description, 
    List<ParameterInfo> parameters
) {}

public record ParameterInfo(String name, String type, boolean required) {}
```

### Exercise 4 (Challenge): Async tool execution

**Solution:**

```java
@Service
public class AsyncToolExecutor {
    
    private final Map<String, CompletableFuture<String>> executions = new ConcurrentHashMap<>();
    
    public String submitExecution(String requestId, String query) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
            orchestrator.processRequest(query)
        );
        
        executions.put(requestId, future);
        return requestId;
    }
    
    public ExecutionStatus getStatus(String requestId) {
        CompletableFuture<String> future = executions.get(requestId);
        
        if (future == null) {
            return new ExecutionStatus("not_found", null, null);
        }
        
        if (!future.isDone()) {
            return new ExecutionStatus("running", null, null);
        }
        
        try {
            String result = future.get();
            return new ExecutionStatus("completed", result, null);
        } catch (Exception e) {
            return new ExecutionStatus("failed", null, e.getMessage());
        }
    }
}

@PostMapping("/execute/async")
public ResponseEntity<AsyncResponse> executeAsync(@RequestBody ToolRequest request) {
    String requestId = UUID.randomUUID().toString();
    asyncExecutor.submitExecution(requestId, request.query());
    
    return ResponseEntity.accepted()
        .body(new AsyncResponse(requestId, "/api/v1/tools/status/" + requestId));
}

@GetMapping("/status/{requestId}")
public ResponseEntity<ExecutionStatus> getStatus(@PathVariable String requestId) {
    return ResponseEntity.ok(asyncExecutor.getStatus(requestId));
}
```

---

## Chapter 7: Testing - Solutions

### Exercise 1: Test database tool

**Solution:**

```java
@SpringBootTest
@Testcontainers
class DatabaseQueryToolTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private DatabaseQueryTool dbTool;
    
    @Test
    void shouldExecuteSimpleQuery() {
        List<Map<String, Object>> results = dbTool.queryDatabase("SELECT 1 as num");
        
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("num")).isEqualTo(1);
    }
    
    @Test
    void shouldRejectDangerousQueries() {
        assertThatThrownBy(() -> dbTool.queryDatabase("DROP TABLE users"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("forbidden");
    }
}
```

### Exercise 2: Mock external APIs

**Solution:**

```java
@SpringBootTest
class WeatherApiToolTest {
    
    @Autowired
    private WeatherApiTool weatherTool;
    
    @MockBean
    private RestTemplate restTemplate;
    
    @Test
    void shouldReturnWeatherData() {
        WeatherResponse mockResponse = new WeatherResponse(
            new MainData(20.5, 65),
            List.of(new WeatherInfo("Clear sky"))
        );
        
        when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
            .thenReturn(mockResponse);
        
        WeatherData result = weatherTool.getWeather("London");
        
        assertThat(result.temperature()).isEqualTo(20.5);
        assertThat(result.description()).isEqualTo("Clear sky");
    }
    
    @Test
    void shouldHandleApiErrors() {
        when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
            .thenThrow(new RestClientException("API error"));
        
        assertThatThrownBy(() -> weatherTool.getWeather("Invalid"))
            .isInstanceOf(ToolExecutionException.class);
    }
}
```

### Exercise 3 (Bonus): Integration test for orchestrator

**Solution:**

```java
@SpringBootTest
class ToolOrchestratorIntegrationTest {
    
    @Autowired
    private ToolOrchestrator orchestrator;
    
    @MockBean
    private ChatLanguageModel chatModel;
    
    @Test
    void shouldOrchestrateWeatherQuery() {
        when(chatModel.generate(contains("Which tool")))
            .thenReturn("get_weather");
        
        when(chatModel.generate(contains("Extract parameters")))
            .thenReturn("{\"city\": \"London\"}");
        
        String result = orchestrator.processRequest("What's the weather in London?");
        
        assertThat(result).contains("London");
    }
}
```

### Exercise 4 (Challenge): Property-based testing

**Solution:**

```java
@Property
void toolShouldHandleArbitraryQueries(@ForAll String query) {
    // Tool should never crash, even with random input
    assertThatCode(() -> {
        try {
            orchestrator.processRequest(query);
        } catch (ToolExecutionException e) {
            // Expected for invalid queries
        }
    }).doesNotThrowAnyException();
}

@Property
void databaseToolShouldRejectAllNonSelectQueries(
        @ForAll @From("dangerousQueries") String query) {
    
    assertThatThrownBy(() -> dbTool.queryDatabase(query))
        .isInstanceOf(IllegalArgumentException.class);
}

@Provide
Arbitrary<String> dangerousQueries() {
    return Arbitraries.of(
        "DROP TABLE users",
        "DELETE FROM users",
        "UPDATE users SET password = 'hacked'",
        "INSERT INTO users VALUES ('hacker')"
    );
}
```

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Next Module: From Chatbots to Agents](../module-04-chatbots-to-agents/README.md)**
