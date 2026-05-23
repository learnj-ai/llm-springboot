# RAG Controller: Building the API

Now that you've built a sophisticated RAG pipeline, how do you expose it to the world? REST APIs are the standard interface for AI services, and this chapter explores how `RAGController` provides clean, validated endpoints for RAG queries and search comparisons. You'll learn about request validation, error handling, API design best practices, and how to make your RAG system production-ready.

## API Design Principles

Before diving into code, let's establish what makes a good RAG API:

### 1. Simple and Intuitive

```http
POST /api/v1/rag/query
Content-Type: application/json

{
  "question": "How do I reset my password?",
  "useQueryExpansion": true
}
```

**Why this works:**
- Clear endpoint name: `/rag/query`
- Minimal required fields: Just `question`
- Optional parameters have sensible defaults

### 2. Validated Inputs

- **Required fields**: Fail fast with clear error messages
- **Type safety**: Use DTOs (Data Transfer Objects) with validation annotations
- **Constraints**: Enforce limits (e.g., question length, topK range)

### 3. Consistent Responses

```json
{
  "answer": "To reset your password, navigate to...",
  "sources": [],
  "transformedQueries": ["How do I reset my password?"],
  "elapsedMs": 1420,
  "status": "ANSWERED"
}
```

Every endpoint returns a predictable structure—no surprises.

### 4. Error Handling

```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "question",
      "message": "must not be blank"
    }
  ]
}
```

Errors include context to help developers debug.

## Endpoints Overview

The `RAGController` exposes two endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/rag/query` | POST | Full RAG pipeline (retrieve + generate answer) |
| `/api/v1/rag/compare` | POST | Compare vector, keyword, and hybrid search results |

## Code Deep Dive

Let's explore the `RAGController` implementation in detail.

### Controller Class Structure

```java
@RestController
@RequestMapping("/api/v1/rag")
public class RAGController {

    private final RAGService ragService;
    private final HybridSearchService hybridSearchService;

    public RAGController(RAGService ragService, HybridSearchService hybridSearchService) {
        this.ragService = ragService;
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/query")
    public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
        // ...
    }

    @PostMapping("/compare")
    public ResponseEntity<SearchComparisonResponse> compareSearchMethods(
            @Valid @RequestBody CompareRequest request) {
        // ...
    }
}
```

**Annotations breakdown:**
- **`@RestController`**: Combines `@Controller` + `@ResponseBody` (all methods return JSON)
- **`@RequestMapping("/api/v1/rag")`**: Base path for all endpoints in this controller
- **Constructor injection**: Spring provides `RAGService` and `HybridSearchService`

### Endpoint 1: RAG Query

The main RAG endpoint that generates answers:

```java
@PostMapping("/query")
public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
    RAGService.RagAnswer result = ragService.queryWithSources(
            request.question(), request.useQueryExpansion());
    return ResponseEntity.ok(new RAGResponse(
            result.answer(),
            result.sources(),
            result.transformedQueries(),
            result.elapsedMs(),
            result.status()));
}
```

**Simple but powerful:**
1. **`@PostMapping("/query")`**: Maps to `POST /api/v1/rag/query`
2. **`@Valid @RequestBody RAGRequest`**: Validates the request DTO before processing
3. **Call RAGService**: Delegate to the service layer, which returns a `RagAnswer` with the answer text, citations, the query variants that ran, elapsed time, and a `RagStatus` so the client can distinguish a grounded answer from a degraded one.
4. **Wrap response**: Map `RagAnswer` to `RAGResponse` DTO (same fields, exposed over JSON).
5. **`ResponseEntity.ok()`**: Returns HTTP 200 with JSON body

**Why `ResponseEntity`?** Allows control over HTTP status codes, headers, etc. You could return `RAGResponse` directly, but `ResponseEntity` is more flexible.

### Request DTO: RAGRequest

```java
// Defined in RAGRequest.java
record RAGRequest(
        @NotBlank
        String question,

        Boolean useQueryExpansion
) {
    // Compact constructor for defaults
    public RAGRequest {
        useQueryExpansion = (useQueryExpansion == null) ? true : useQueryExpansion;
    }
}
```

**Using Java records:**
- **Immutable**: Fields are `final` by default
- **Concise**: No need for getters, equals, hashCode, toString
- **Validation**: `@NotBlank` ensures `question` is provided and non-empty
- **Defaults**: Compact constructor sets `useQueryExpansion = true` if not provided

**Why records for DTOs?** They're perfect for data transfer—immutable, concise, and validation-friendly.

### Response DTO: RAGResponse

```java
public record RAGResponse(
        String answer,
        List<RAGService.Source> sources,
        List<String> transformedQueries,
        long elapsedMs,
        RAGService.RagStatus status) {

    public RAGResponse(String answer) {
        this(answer, List.of(), List.of(), 0L, RAGService.RagStatus.INSUFFICIENT_CONTEXT);
    }
}
```

The response exposes the generated text plus the exact source list and query trace that `RAGService.RagAnswer` produced. The one-argument constructor is only a convenience for legacy answer-only callers; the controller's normal path uses the five-argument shape.

### Validation in Action

What happens if the client sends invalid data?

**Request:**
```json
{
  "question": "",
  "useQueryExpansion": true
}
```

**Response (HTTP 400):**
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "question",
      "message": "must not be blank"
    }
  ]
}
```

This is handled automatically by Spring's `@Valid` annotation in combination with `@NotBlank` on the field.

### Endpoint 2: Search Method Comparison

This endpoint compares vector, keyword, and hybrid search side-by-side:

```java
@PostMapping("/compare")
public ResponseEntity<SearchComparisonResponse> compareSearchMethods(
        @Valid @RequestBody CompareRequest request) {
    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<List<String>>allSuccessfulOrThrow())) {
        var vectorTask = scope.fork(() -> hybridSearchService.vectorOnlySearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        var keywordTask = scope.fork(() -> hybridSearchService.keywordOnlySearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        var hybridTask = scope.fork(() -> hybridSearchService.hybridSearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        scope.join();

        return ResponseEntity.ok(new SearchComparisonResponse(
                request.query(),
                vectorTask.get(),
                keywordTask.get(),
                hybridTask.get()));
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Search comparison interrupted", e);
    }
}
```

**This is dense—let's break it down:**

1. **`StructuredTaskScope.open(...)`**: Creates a structured concurrency scope (covered in Chapter 8)
2. **`scope.fork(...)`**: Launches three searches **in parallel**:
   - Vector-only search
   - Keyword-only search
   - Hybrid search
3. **`scope.join()`**: Waits for all tasks to complete
4. **`vectorTask.get()`**: Retrieves results from each task
5. **Error handling**: If any task fails or is interrupted, the entire scope fails (structured concurrency principle)

**Why structured concurrency?** Ensures clean resource management—if one search fails, all others are cancelled. More on this in Chapter 8.

### Compare Request and Response

**CompareRequest:**
```java
record CompareRequest(
        @NotBlank String query,
        @Min(1) @Max(20) int topK
) {
    CompareRequest {
        topK = (topK == 0) ? 5 : topK;  // Default to 5 if not provided
    }
}
```

**Validation:**
- **`@NotBlank String query`**: Query must not be empty
- **`@Min(1) @Max(20) int topK`**: Limit results between 1 and 20 (prevents abuse)

**SearchComparisonResponse:**
```java
record SearchComparisonResponse(
    String query,
    List<String> vectorResults,
    List<String> keywordResults,
    List<String> hybridResults
) {}
```

**Example response:**
```json
{
  "query": "VPN setup",
  "vectorResults": [
    "To configure VPN access, download the TechCorp VPN client...",
    "Remote workers must use the corporate VPN..."
  ],
  "keywordResults": [
    "VPN Configuration Guide: Step 1: Download the VPN client...",
    "SEV1 Incident: VPN outage affecting remote users..."
  ],
  "hybridResults": [
    "To configure VPN access, download the TechCorp VPN client...",
    "VPN Configuration Guide: Step 1: Download the VPN client..."
  ]
}
```

## Global Exception Handling

The module includes a `GlobalExceptionHandler` to catch validation errors and other exceptions:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", (Object) error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "details", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed request body"));
    }
}
```

**What it does:**
- **`@RestControllerAdvice`**: Global exception handler for all controllers
- **`handleValidation`**: Catches `@Valid` validation failures, returns HTTP 400 with field errors
- **`handleUnreadable`**: Catches malformed JSON/request bodies, returns HTTP 400 with a compact message

**Production enhancements:**
- **Don't leak stack traces**: Log them server-side, return generic messages to clients
- **Custom exception types**: `DocumentNotFoundException`, `RAGPipelineException`, etc.
- **Correlation IDs**: Include request ID for tracing errors across logs

## API Testing with curl

Let's test the endpoints:

### Test 1: Valid RAG Query

```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How do I reset my password?",
    "useQueryExpansion": true
  }'
```

**Expected Response (HTTP 200):**
```json
{
  "answer": "To reset your password, navigate to the IT Portal and click 'Forgot Password'...",
  "sources": [
    {
      "number": 1,
      "title": "password-reset.md",
      "text": "...",
      "score": 0.0478,
      "sourceQuery": "hybrid: How do I reset my password?"
    }
  ],
  "transformedQueries": [
    "How do I reset my password?",
    "What is the process for password recovery?"
  ],
  "elapsedMs": 1420,
  "status": "ANSWERED"
}
```

### Test 2: Invalid Request (Blank Question)

```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "",
    "useQueryExpansion": true
  }'
```

**Expected Response (HTTP 400):**
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "question",
      "message": "must not be blank"
    }
  ]
}
```

### Test 3: Compare Search Methods

```bash
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{
    "query": "VPN troubleshooting",
    "topK": 3
  }'
```

**Expected Response (HTTP 200):**
```json
{
  "query": "VPN troubleshooting",
  "vectorResults": ["...", "...", "..."],
  "keywordResults": ["...", "...", "..."],
  "hybridResults": ["...", "...", "..."]
}
```

### Test 4: Invalid topK (Out of Range)

```bash
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{
    "query": "VPN",
    "topK": 50
  }'
```

**Expected Response (HTTP 400):**
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "topK",
      "message": "must be less than or equal to 20"
    }
  ]
}
```

## Production API Enhancements

For production systems, consider these enhancements:

### 1. Rate Limiting

Prevent abuse with request rate limits:

```java
@RestController
@RequestMapping("/api/v1/rag")
@RateLimiter(name = "rag", fallbackMethod = "rateLimitFallback")
public class RAGController {
    // ...
}
```

Using **Resilience4j** for rate limiting (e.g., 10 requests per minute per API key).

### 2. Authentication and Authorization

Secure endpoints with API keys or OAuth:

```java
@PostMapping("/query")
@PreAuthorize("hasRole('RAG_USER')")
public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
    // ...
}
```

### 3. Request Tracing

Add correlation IDs for distributed tracing:

```java
@PostMapping("/query")
public ResponseEntity<RAGResponse> query(
        @Valid @RequestBody RAGRequest request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

    if (correlationId == null) {
        correlationId = UUID.randomUUID().toString();
    }

    MDC.put("correlationId", correlationId);
    log.info("Processing RAG query: {}", request.question());

    RAGService.RagAnswer result = ragService.queryWithSources(
            request.question(), request.useQueryExpansion());

    return ResponseEntity.ok()
            .header("X-Correlation-ID", correlationId)
            .body(new RAGResponse(
                    result.answer(), result.sources(),
                    result.transformedQueries(), result.elapsedMs(), result.status()));
}
```

### 4. Streaming Responses

Stream LLM answers as they're generated (Server-Sent Events):

```java
@PostMapping("/query-stream")
public SseEmitter queryStream(@Valid @RequestBody RAGRequest request) {
    SseEmitter emitter = new SseEmitter();

    // Run RAG pipeline asynchronously
    CompletableFuture.runAsync(() -> {
        try {
            ragService.queryStream(request.question(), token -> {
                emitter.send(SseEmitter.event().data(token));
            });
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });

    return emitter;
}
```

### 5. Metrics and Monitoring

Expose metrics for observability:

```java
@Autowired
private MeterRegistry meterRegistry;

@PostMapping("/query")
public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
    Timer.Sample sample = Timer.start(meterRegistry);

    RAGService.RagAnswer result = ragService.queryWithSources(
            request.question(), request.useQueryExpansion());

    sample.stop(Timer.builder("rag.query.duration")
            .tag("expansion", String.valueOf(request.useQueryExpansion()))
            .tag("status", result.status().name())
            .register(meterRegistry));

    meterRegistry.counter("rag.query.total").increment();

    return ResponseEntity.ok(new RAGResponse(
            result.answer(), result.sources(),
            result.transformedQueries(), result.elapsedMs(), result.status()));
}
```

Track metrics like:
- Request count
- Latency distribution
- Error rate
- Query expansion usage

## Practice Exercises

### Exercise 1: Add Request Logging

Log all incoming requests with details:

```java
@PostMapping("/query")
public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
    log.info("Received RAG query: question='{}', expansion={}", request.question(), request.useQueryExpansion());

    RAGService.RagAnswer result = ragService.queryWithSources(
            request.question(), request.useQueryExpansion());

    log.info("RAG query completed: status={}, answerLength={}, sources={}",
            result.status(), result.answer().length(), result.sources().size());

    return ResponseEntity.ok(new RAGResponse(
            result.answer(), result.sources(),
            result.transformedQueries(), result.elapsedMs(), result.status()));
}
```

**Questions to explore:**
- What patterns emerge in the logs?
- Are most users enabling or disabling query expansion?

### Exercise 2: Render Citation Sources

The response already includes source citations. Build a client-side view model that maps citation markers in the answer back to `sources`:

```java
record CitationView(
    int number,
    String title,
    String preview,
    double score,
    String provenance
) {}
```

Use `Source.number()` to match `[Source N]`, `Source.title()` for the document label, and `Source.sourceQuery()` to show which query variants surfaced the chunk.

### Exercise 3: Implement Pagination for Compare

Allow users to request different pages of results:

```java
record CompareRequest(
    @NotBlank String query,
    @Min(1) @Max(20) int topK,
    @Min(0) int offset
) {}
```

Modify `hybridSearchService` to support offset:
```java
public List<TextSegment> vectorOnlySearch(String query, int topK, int offset) {
    return vectorStore.searchSegments(query, topK + offset)
            .stream()
            .skip(offset)
            .limit(topK)
            .toList();
}
```

### Exercise 4: Add Health Check Endpoint

Expose a health check for monitoring:

```java
@GetMapping("/health")
public ResponseEntity<Map<String, String>> health() {
    Map<String, String> status = new HashMap<>();
    status.put("status", "UP");
    status.put("vectorStore", vectorStore.isInitialized() ? "UP" : "DOWN");
    status.put("llm", isLLMAvailable() ? "UP" : "DOWN");
    return ResponseEntity.ok(status);
}
```

## Key Takeaways

- **`RAGController` exposes RAG capabilities via REST APIs**—two endpoints for query and compare
- **Use DTOs with validation** (`@Valid`, `@NotBlank`, `@Min`, `@Max`) for type safety and clear errors
- **Java records are ideal for DTOs**—concise, immutable, validation-friendly
- **`GlobalExceptionHandler` centralizes error handling**—consistent error responses across all endpoints
- **Structured concurrency** (covered in Chapter 8) enables safe parallel search execution
- **Production enhancements**: Rate limiting, auth, tracing, streaming, metrics
- **Separation of concerns**: Controller handles HTTP, service handles business logic

---

## Navigation

⬅️ **[Previous: RAG Service: The Complete Pipeline](06-rag-service.md)**
➡️ **[Next: Structured Concurrency: Modern Java Parallelism](08-structured-concurrency.md)**
