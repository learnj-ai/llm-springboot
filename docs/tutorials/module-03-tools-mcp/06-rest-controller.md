# REST Controller: The API Gateway

In this chapter, you'll learn how to expose the tool-enabled AI assistant through REST APIs, design request/response DTOs, implement proper error handling, and build production-ready endpoints that clients can consume.

## Why REST APIs for AI Assistants?

REST APIs provide a universal interface for AI assistants, enabling:

- **Web applications** - Chat interfaces, dashboards, admin panels
- **Mobile apps** - iOS and Android native apps
- **Third-party integrations** - Zapier, Make, custom webhooks
- **Microservices** - Other services in your architecture
- **Testing and monitoring** - Automated health checks and integration tests

By exposing the ToolOrchestrator through REST, you decouple the AI logic from the presentation layer.

## The AssistantController Class

Here's the complete implementation:

```java
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
```

## Key Components Explained

### 1. Spring REST Annotations

```java
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
```

- **@RestController** - Combines `@Controller` and `@ResponseBody`, indicating this class handles HTTP requests and returns JSON
- **@RequestMapping** - Base path for all endpoints in this controller (`/api/v1/assistant`)

**Why versioning (`/v1/`)?**
- Allows breaking changes in `/v2/` without affecting existing clients
- Enables gradual migration
- Follows REST API best practices

### 2. Endpoint Definition

```java
@PostMapping("/chat")
public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
```

- **@PostMapping** - Handles POST requests to `/api/v1/assistant/chat`
- **@RequestBody** - Deserializes JSON request body to `ChatRequest` object
- **ResponseEntity<T>** - Allows control over HTTP status codes and headers

**Why POST instead of GET?**
- Chat messages can be long (exceed URL length limits)
- POST is semantically correct for state-changing operations
- Request body can include complex structures (metadata, context, etc.)

### 3. Error Handling

```java
try {
    String response = toolOrchestrator.processRequest(request.message());
    return ResponseEntity.ok(new ChatResponse(response));

} catch (Exception e) {
    log.error("Error processing chat request", e);
    return ResponseEntity.internalServerError()
            .body(new ChatResponse("An error occurred processing your request"));
}
```

Always catch exceptions at the controller level:
- **Prevents stack traces leaking to clients**
- **Logs errors for debugging**
- **Returns user-friendly error messages**
- **Uses appropriate HTTP status codes** (500 for server errors)

### 4. Health Check Endpoint

```java
@GetMapping("/health")
public ResponseEntity<String> health() {
    return ResponseEntity.ok("Module 03: Tools & MCP - OK");
}
```

Essential for:
- Load balancer health checks
- Kubernetes liveness/readiness probes
- Monitoring systems (Prometheus, Datadog)
- Quick verification during deployment

## Request and Response DTOs

### ChatRequest

```java
package com.techcorp.assistant.module03.dto;

/**
 * Request DTO for chat endpoint.
 *
 * @param message The user's message/query
 */
public record ChatRequest(String message) {
    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
    }
}
```

**Record features**:
- **Compact syntax** - No boilerplate getters, equals, hashCode, toString
- **Immutable by default** - Fields are final
- **Compact constructor** - Validation logic in compact constructor block

**Validation** ensures:
- Message is not null
- Message is not empty or just whitespace
- Fail fast with clear error message

### ChatResponse

```java
package com.techcorp.assistant.module03.dto;

/**
 * Response DTO for the chat endpoint.
 *
 * @param response The AI-generated response (potentially tool-augmented).
 */
public record ChatResponse(String response) {
}
```

**Design notes**:
- Single field: the model's textual answer.
- Tool-call observability (which tools fired and with what arguments) is not exposed by `ToolOrchestrator` today — the orchestrator only returns the final `String` answer, so any `toolsUsed`/`metadata` field would be permanently empty. To surface tool calls, wrap the LangChain4J `ToolExecutor` with an interceptor that captures the calls and thread them up to the controller. That's worth doing as a follow-up exercise; until then, the DTO stays single-field rather than promising metadata it can't deliver.

## API Usage Examples

### Basic Chat Request

```bash
curl -X POST http://localhost:8083/api/v1/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is customer 12345'\''s email address?"
  }'
```

**Response**:
```json
{
  "response": "The email address for customer 12345 (Alice Johnson) is alice.johnson@example.com."
}
```

### Error Handling

**Invalid request** (empty message):
```bash
curl -X POST http://localhost:8083/api/v1/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": ""}'
```

**Response** (400 Bad Request):
```json
{
  "timestamp": "2024-05-08T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Message cannot be null or empty"
}
```

### Health Check

```bash
curl http://localhost:8083/api/v1/assistant/health
```

**Response** (200 OK):
```
Module 03: Tools & MCP - OK
```

## Production-Ready Enhancements

### 1. Global Exception Handler

The module ships a `@RestControllerAdvice` (see `controller/GlobalExceptionHandler.java`) that turns uncaught exceptions into structured JSON error responses instead of Spring's default error page:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse("Validation Error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal Error", "An unexpected error occurred"));
    }

    public record ErrorResponse(String error, String message) {}
}
```

`@RestControllerAdvice` is `@ControllerAdvice` + `@ResponseBody`, so the returned `ErrorResponse` is serialized to JSON automatically. Note that `AssistantController.chat()` already wraps its happy-path call in a `try/catch` and returns a `ChatResponse`, so the advice mainly catches `IllegalArgumentException` from validation and unhandled errors from any new endpoints you add.

### 2. Request Validation with Bean Validation

```java
public record ChatRequest(
    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
    String message
) {}

@PostMapping("/chat")
public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
    // Spring automatically validates before method execution
}
```

### 3. Rate Limiting

Prevent abuse with rate limiting:

```java
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 req/sec

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (!rateLimiter.tryAcquire()) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ChatResponse("Rate limit exceeded. Please try again later."));
        }

        // Process request...
    }
}
```

### 4. CORS Configuration

Enable cross-origin requests for web frontends:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "https://app.techcorp.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### 5. Request/Response Logging

Log all requests for debugging and auditing:

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Request: {} {} - Status: {} - Duration: {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration);
    }
}
```

### 6. Async Processing

For long-running tool chains, use async processing:

```java
@PostMapping("/chat")
public CompletableFuture<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        String response = toolOrchestrator.processRequest(request.message());
        return ResponseEntity.ok(new ChatResponse(response));
    });
}
```

Or use Spring's `DeferredResult` for more control:

```java
@PostMapping("/chat")
public DeferredResult<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest request) {
    DeferredResult<ResponseEntity<ChatResponse>> result = new DeferredResult<>(30000L); // 30s timeout

    CompletableFuture.supplyAsync(() -> toolOrchestrator.processRequest(request.message()))
        .whenComplete((response, error) -> {
            if (error != null) {
                result.setErrorResult(ResponseEntity.internalServerError()
                    .body(new ChatResponse("Error processing request")));
            } else {
                result.setResult(ResponseEntity.ok(new ChatResponse(response)));
            }
        });

    return result;
}
```

## API Documentation with OpenAPI/Swagger

Add Swagger UI for interactive API documentation:

### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2. Configure OpenAPI

```java
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI assistantAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TechCorp AI Assistant API")
                .description("Tool-enabled AI assistant for customer support and data retrieval")
                .version("1.0.0")
                .contact(new Contact()
                    .name("TechCorp Engineering")
                    .email("engineering@techcorp.com")))
            .servers(List.of(
                new Server().url("http://localhost:8083").description("Development"),
                new Server().url("https://api.techcorp.com").description("Production")
            ));
    }
}
```

### 3. Annotate Endpoints

```java
@RestController
@RequestMapping("/api/v1/assistant")
@Tag(name = "AI Assistant", description = "Tool-enabled AI assistant endpoints")
public class AssistantController {

    @Operation(
        summary = "Chat with AI assistant",
        description = "Send a message to the AI assistant. The assistant can autonomously use tools to access customer data, support tickets, and weather information."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User's chat message",
            required = true,
            content = @Content(schema = @Schema(implementation = ChatRequest.class))
        )
        @RequestBody ChatRequest request) {
        // Implementation...
    }
}
```

Access Swagger UI at: `http://localhost:8083/swagger-ui.html`

## Practice Exercises

### Exercise 1: Add Conversation History Endpoint

Create an endpoint to retrieve conversation history:

```java
@GetMapping("/conversations/{sessionId}/history")
public ResponseEntity<List<Message>> getHistory(@PathVariable String sessionId) {
    // TODO: Implement conversation history retrieval
    // Return list of messages with timestamps
}
```

### Exercise 2: Implement Streaming Responses

Create an endpoint that streams responses in real-time:

```java
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
    // TODO: Return server-sent events as tokens are generated
}
```

### Exercise 3: Add Authentication

Secure endpoints with JWT authentication:

```java
@PostMapping("/chat")
public ResponseEntity<ChatResponse> chat(
    @RequestBody ChatRequest request,
    @AuthenticationPrincipal User user) {
    // TODO: Verify user authentication
    // Log user ID for auditing
}
```

### Exercise 4: Create Batch Processing Endpoint

Process multiple messages in one request:

```java
@PostMapping("/chat/batch")
public ResponseEntity<List<ChatResponse>> chatBatch(
    @RequestBody List<ChatRequest> requests) {
    // TODO: Process multiple messages
    // Return responses in same order
}
```

## Key Takeaways

- **@RestController exposes AI assistant as REST API** for universal access
- **DTOs (records) provide type-safe request/response handling** with validation
- **ResponseEntity allows control over HTTP status codes** and headers
- **Global exception handling prevents stack traces from leaking** to clients
- **Health checks are essential for production monitoring** and orchestration
- **CORS configuration enables web frontend integration**
- **Rate limiting prevents abuse** and protects backend resources
- **OpenAPI/Swagger provides interactive documentation** for API consumers
- **Async processing improves scalability** for long-running tool chains

---

## Navigation

[← Back to Tool Orchestrator](05-tool-orchestrator.md) | [Next: Testing →](07-testing.md)
