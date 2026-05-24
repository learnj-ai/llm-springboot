## ADDED Requirements

### Requirement: LLM evaluation framework
The system SHALL provide an evaluation service that runs test cases from a golden dataset and calculates accuracy, relevance, and faithfulness metrics.

#### Scenario: Load evaluation dataset
- **WHEN** evaluation runs with path to eval-golden-set.json
- **THEN** system loads all EvalCase objects with id, query, context, and expectedAnswer

#### Scenario: Calculate accuracy metric
- **WHEN** comparing expected and actual responses
- **THEN** system calculates cosine similarity between embeddings as accuracy score (0.0-1.0)

#### Scenario: Calculate relevance metric
- **WHEN** evaluating response against source contexts
- **THEN** system measures embedding similarity between response and combined context

#### Scenario: Calculate faithfulness metric
- **WHEN** checking if response is grounded in context
- **THEN** system uses LLM-as-judge to rate faithfulness (0.0-1.0)

#### Scenario: Generate evaluation report
- **WHEN** all test cases complete
- **THEN** system returns EvaluationReport with per-case results and metric averages

### Requirement: Distributed tracing
The system SHALL instrument the RAG pipeline with OpenTelemetry spans to trace requests across components.

#### Scenario: Trace method execution
- **WHEN** method annotated with @Traced executes
- **THEN** system creates span with method name, component attribute, and duration

#### Scenario: Record successful execution
- **WHEN** traced method completes successfully
- **THEN** span status is set to OK

#### Scenario: Record exceptions in spans
- **WHEN** traced method throws exception
- **THEN** span status is ERROR, exception is recorded with message and stack trace

#### Scenario: Maintain span context
- **WHEN** nested methods are traced
- **THEN** child spans inherit parent context creating a trace hierarchy

### Requirement: Metrics collection
The system SHALL collect and expose metrics for query count, response time, and token usage via Micrometer.

#### Scenario: Count total queries
- **WHEN** RAG query executes
- **THEN** counter "rag.queries.total" increments

#### Scenario: Record response time
- **WHEN** RAG query completes
- **THEN** timer "rag.response.time" records duration in milliseconds

#### Scenario: Expose token usage
- **WHEN** Prometheus scrapes /actuator/prometheus endpoint
- **THEN** gauge "rag.tokens.used" shows current token consumption

### Requirement: Response caching
The system SHALL cache RAG responses by exact query match with 24-hour TTL using Redis.

#### Scenario: Cache miss
- **WHEN** query is not in cache
- **THEN** getCachedResponse returns null triggering RAG execution

#### Scenario: Cache hit
- **WHEN** exact query exists in cache
- **THEN** system returns cached response without calling LLM

#### Scenario: Cache population
- **WHEN** RAG generates new response
- **THEN** cacheResponse stores it in Redis with 24-hour expiration

### Requirement: Semantic caching
The system SHALL cache responses by embedding similarity to reduce costs for semantically similar queries.

#### Scenario: Find semantically similar cached query
- **WHEN** new query embedding has >0.95 cosine similarity to cached query
- **THEN** system returns the cached response

#### Scenario: No similar query in cache
- **WHEN** no cached query exceeds similarity threshold
- **THEN** semanticCacheGet returns null

#### Scenario: Store with semantic key
- **WHEN** caching new response
- **THEN** semanticCachePut stores with query as key for later similarity matching

### Requirement: Token optimization
The system SHALL optimize context assembly to fit within token budgets while preserving quality.

#### Scenario: Fit segments within token budget
- **WHEN** optimizeContext receives segments exceeding maxTokens
- **THEN** system selects highest-relevance segments that fit within budget

#### Scenario: Token estimation
- **WHEN** processing text segments
- **THEN** system uses OpenAiTokenizer to estimate token count per segment

#### Scenario: Compress system prompts
- **WHEN** compressPrompt receives verbose prompt
- **THEN** system removes redundant whitespace and filler words

### Requirement: Health checks
The system SHALL expose liveness and readiness probes for Kubernetes orchestration.

#### Scenario: Liveness probe succeeds
- **WHEN** GET /actuator/health
- **THEN** returns HTTP 200 with status UP

#### Scenario: Readiness probe succeeds
- **WHEN** GET /actuator/health/readiness
- **THEN** returns HTTP 200 when all dependencies (Redis, PostgreSQL) are reachable

#### Scenario: Readiness probe fails
- **WHEN** critical dependency is unavailable
- **THEN** returns HTTP 503 signaling Kubernetes should not route traffic

### Requirement: OpenShift deployment
The system SHALL provide Kubernetes manifests for deploying to OpenShift with 3 replicas, resource limits, and TLS termination.

#### Scenario: Deploy application
- **WHEN** kubectl apply -f deployment.yaml
- **THEN** OpenShift creates Deployment with 3 replicas running UBI-based container

#### Scenario: Configure resource limits
- **WHEN** pod starts
- **THEN** container has requests (1Gi memory, 500m CPU) and limits (2Gi memory, 1000m CPU)

#### Scenario: Expose via Route
- **WHEN** Route resource is created
- **THEN** application is accessible via HTTPS with edge TLS termination

#### Scenario: Load secrets from environment
- **WHEN** container starts
- **THEN** OPENAI_API_KEY is loaded from OpenShift Secret, not hardcoded

### Requirement: Monitoring integration
The system SHALL integrate with Prometheus for metrics scraping and Grafana for visualization.

#### Scenario: Prometheus scrape configuration
- **WHEN** Prometheus scrapes /actuator/prometheus
- **THEN** receives metrics in OpenMetrics format

#### Scenario: Grafana dashboard
- **WHEN** Grafana connects to Prometheus data source
- **THEN** visualizations display RAG query rates, latencies, and error rates

### Requirement: Container image build
The system SHALL use Red Hat UBI-based OpenJDK 21 image for secure enterprise container builds.

#### Scenario: Build container
- **WHEN** docker build runs Dockerfile
- **THEN** resulting image uses registry.access.redhat.com/ubi9/openjdk-21 as base

#### Scenario: Run application in container
- **WHEN** container starts
- **THEN** Java process executes with JAR located at /deployments/app.jar

#### Scenario: Expose application port
- **WHEN** container is running
- **THEN** port 8080 is exposed for HTTP traffic
