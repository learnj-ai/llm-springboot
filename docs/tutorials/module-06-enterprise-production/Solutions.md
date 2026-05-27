# Module 06: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 06: Enterprise Production.

## How to Run and Test Solutions

### Prerequisites
- Java 21+
- Maven 3.9+
- OpenAI API key (or compatible API)
- Docker & Docker Compose (for observability stack)
- Kubernetes cluster (for deployment exercises) - Minikube or kind for local testing

### Project Location
```bash
cd src/module-06-enterprise-production/
```

### Setup
```bash
# 1. Set environment variables
export OPENAI_API_KEY=your-api-key-here
export OPENAI_MODEL_NAME=gpt-4o-mini

# 2. Start all infrastructure services (includes observability stack)
docker compose up -d

# This starts:
# - Redis (port 6379) - for caching
# - Prometheus (port 9090) - for metrics
# - Grafana (port 3000) - for dashboards
# - Jaeger (port 16686) - for distributed tracing

# 3. Verify all services are running
docker ps
curl http://localhost:9090/-/healthy    # Prometheus
curl http://localhost:16686/            # Jaeger UI
```

### Configuration
Update `application.yml` for production features:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,trace

spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=1h
```

### Running the Application
```bash
# Development mode
mvn clean spring-boot:run

# Production mode
mvn clean package
java -jar target/module-06-enterprise-production-1.0.0.jar \
  --spring.profiles.active=prod
```

### Running Tests
```bash
# Run all tests including evaluation tests
mvn test

# Run evaluation framework tests
mvn test -Dtest=RagEvaluatorTest

# Run with test reports
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Testing Evaluation Framework
```bash
# Run evaluation on test dataset
curl -X POST http://localhost:8080/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{
    "datasetName": "qa_test_set",
    "evaluators": ["completeness", "accuracy", "relevance"]
  }'

# Get evaluation results
curl http://localhost:8080/api/v1/eval/results/latest

# Compare strategies
curl -X POST http://localhost:8080/api/v1/eval/compare \
  -H "Content-Type: application/json" \
  -d '{
    "strategies": ["vector-only", "hybrid", "hybrid-reranked"]
  }'
```

### Viewing Distributed Traces
```bash
# Open Jaeger UI
open http://localhost:16686

# Search for traces
# 1. Select service: "rag-service"
# 2. Look for operation: "rag.full_pipeline"
# 3. Click on a trace to see the waterfall view
# 4. Inspect individual spans for timing and attributes
```

### Monitoring Metrics
```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Query specific metrics
curl 'http://localhost:9090/api/v1/query?query=rag_request_total'
curl 'http://localhost:9090/api/v1/query?query=rag_request_duration_seconds'

# Grafana dashboards
open http://localhost:3000
# Default credentials: admin/admin
# Import dashboard: config/grafana-dashboard.json
```

### Testing Cache Performance
```bash
# First request (cache miss)
time curl -X POST http://localhost:8080/api/v1/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is vector search?"}'

# Second request (cache hit - should be much faster)
time curl -X POST http://localhost:8080/api/v1/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is vector search?"}'

# Check cache stats
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8080/actuator/metrics/cache.puts
```

### Adding Your Solution Code
1. **Evaluation framework**: `src/main/java/com/example/production/eval/`
   - `RagEvaluator.java`
   - `CompletenessEvaluator.java`
   - `FactualAccuracyEvaluator.java`
   - `CompositeEvaluator.java`

2. **Tracing**: `src/main/java/com/example/production/tracing/`
   - `TracingConfiguration.java`
   - `TracedRagService.java`

3. **Caching**: `src/main/java/com/example/production/cache/`
   - `CachedEmbeddingService.java`
   - `SemanticCacheService.java`
   - `MultiLevelCacheService.java`

4. **Metrics**: `src/main/java/com/example/production/metrics/`
   - `RagMetricsInterceptor.java`
   - `CustomMetrics.java`

### Running Kubernetes Deployment
```bash
# Start local Kubernetes (Minikube)
minikube start

# Build Docker image
docker build -t rag-service:latest .

# Load image into Minikube
minikube image load rag-service:latest

# Apply Kubernetes manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# Check deployment
kubectl get pods -n rag-production
kubectl logs -f deployment/rag-service -n rag-production

# Test service
kubectl port-forward svc/rag-service 8080:8080 -n rag-production
curl http://localhost:8080/actuator/health
```

### Load Testing
```bash
# Install k6 (load testing tool)
brew install k6  # macOS
# or download from k6.io

# Run load test
k6 run scripts/load-test.js

# Expected output shows:
# - Requests per second
# - Response times (p95, p99)
# - Error rate
# - Throughput
```

### Continuous Evaluation
```bash
# View evaluation history
curl http://localhost:8080/api/v1/eval/history?days=7

# Trigger manual evaluation
curl -X POST http://localhost:8080/api/v1/eval/run-now

# Check for quality degradation alerts
curl http://localhost:8080/api/v1/eval/alerts
```

### Verifying Production Solutions
1. **Traces appear in Jaeger**: All RAG requests create spans
2. **Metrics in Prometheus**: Gauges and counters update in real-time
3. **Cache improves performance**: Second identical query is 10x+ faster
4. **Evaluation runs successfully**: Pass rate > 80% on test dataset
5. **Kubernetes pods are healthy**: All pods in Running state
6. **HPA scales pods**: Load test triggers autoscaling

### Performance Benchmarks
```bash
# Run benchmark suite
mvn test -Dtest=PerformanceBenchmarkTest

# Expected results:
# - Cold start (no cache): ~500-1000ms
# - Warm cache: ~50-100ms
# - Throughput: 50-100 req/sec (single instance)
# - P95 latency: < 1000ms
# - P99 latency: < 2000ms
```

### Analyzing Evaluation Results
```bash
# Generate evaluation report
curl http://localhost:8080/api/v1/eval/report > evaluation-report.json

# Visualize results
python scripts/visualize_eval.py evaluation-report.json

# Compare with baseline
curl -X POST http://localhost:8080/api/v1/eval/compare-baseline \
  -H "Content-Type: application/json" \
  -d '{"baselineId": "v1.0", "currentId": "v1.1"}'
```

### Troubleshooting
- **API key errors**: Verify `OPENAI_API_KEY` is set correctly
- **No traces in Jaeger**: Check `management.tracing.enabled=true` in config
- **Metrics not exported**: Verify Prometheus scrapes `actuator/prometheus` endpoint
- **Cache not working**: Check Redis connection, verify cache configuration is loaded
- **Evaluation fails**: Ensure test dataset exists and API is accessible
- **K8s pods crash**: Check logs with `kubectl logs`, verify resource limits and environment variables

### Production Checklist
- [ ] Distributed tracing configured and validated
- [ ] Metrics exported to Prometheus
- [ ] Caching enabled and tested
- [ ] Evaluation framework runs hourly
- [ ] Alerts configured for quality degradation
- [ ] Kubernetes manifests tested
- [ ] HPA configured for autoscaling
- [ ] Load tests pass performance targets
- [ ] Documentation updated

---

## Chapter 2: Dokimos Evaluation Framework - Solutions

### Exercise 1: Create custom evaluation dataset

**Solution:**

```java
@Service
public class EvaluationDatasetBuilder {
    
    public List<EvaluationCase> buildDataset() {
        return List.of(
            new EvaluationCase(
                "How do I reset my password?",
                "To reset your password, click the 'Forgot Password' link on the login page.",
                List.of("password", "reset", "login"),
                0.9
            ),
            new EvaluationCase(
                "What are the API rate limits?",
                "The API has a rate limit of 100 requests per minute for free tier.",
                List.of("API", "rate limit", "100"),
                0.85
            ),
            new EvaluationCase(
                "How do I configure authentication?",
                "Configure authentication by setting the auth.provider property in application.yml",
                List.of("authentication", "configuration", "application.yml"),
                0.9
            )
        );
    }
    
    public record EvaluationCase(
        String query,
        String expectedAnswer,
        List<String> requiredKeywords,
        double minimumRelevanceScore
    ) {}
}
```

### Exercise 2: Implement automated evaluation

**Solution:**

```java
@Service
public class RagEvaluator {
    
    private final RagService ragService;
    private final ChatLanguageModel judgeModel;
    
    public EvaluationReport evaluate(List<EvaluationCase> dataset) {
        List<CaseResult> results = new ArrayList<>();
        
        for (EvaluationCase testCase : dataset) {
            String actualAnswer = ragService.answer(testCase.query(), 10);
            
            double relevanceScore = calculateRelevance(
                testCase.query(),
                actualAnswer,
                testCase.expectedAnswer()
            );
            
            boolean hasKeywords = testCase.requiredKeywords().stream()
                .allMatch(keyword -> actualAnswer.toLowerCase()
                    .contains(keyword.toLowerCase()));
            
            boolean passed = relevanceScore >= testCase.minimumRelevanceScore() 
                          && hasKeywords;
            
            results.add(new CaseResult(
                testCase.query(),
                actualAnswer,
                relevanceScore,
                hasKeywords,
                passed
            ));
        }
        
        return new EvaluationReport(results, calculateMetrics(results));
    }
    
    private double calculateRelevance(String query, String actual, String expected) {
        String prompt = String.format("""
            Rate how well the actual answer addresses the query, compared to the expected answer.
            
            Query: %s
            Expected: %s
            Actual: %s
            
            Return a score from 0.0 to 1.0.
            """, query, expected, actual);
        
        String response = judgeModel.generate(prompt);
        
        try {
            return Double.parseDouble(response.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private Metrics calculateMetrics(List<CaseResult> results) {
        long passed = results.stream().filter(CaseResult::passed).count();
        double passRate = (double) passed / results.size();
        
        double avgRelevance = results.stream()
            .mapToDouble(CaseResult::relevanceScore)
            .average()
            .orElse(0.0);
        
        return new Metrics(passRate, avgRelevance, results.size());
    }
    
    public record EvaluationReport(List<CaseResult> results, Metrics metrics) {}
    public record CaseResult(
        String query,
        String answer,
        double relevanceScore,
        boolean hasKeywords,
        boolean passed
    ) {}
    public record Metrics(double passRate, double avgRelevance, int totalCases) {}
}
```

### Exercise 3 (Bonus): A/B test different RAG strategies

**Solution:**

```java
@Service
public class RagStrategyComparison {
    
    private final Map<String, RagConfiguration> strategies;
    
    public ComparisonReport compareStrategies(List<EvaluationCase> dataset) {
        Map<String, EvaluationReport> results = new HashMap<>();
        
        for (Map.Entry<String, RagConfiguration> entry : strategies.entrySet()) {
            String strategyName = entry.getKey();
            RagConfiguration config = entry.getValue();
            
            // Configure RAG service with this strategy
            configureRagService(config);
            
            // Evaluate
            EvaluationReport report = evaluate(dataset);
            results.put(strategyName, report);
        }
        
        return new ComparisonReport(results, findBestStrategy(results));
    }
    
    private String findBestStrategy(Map<String, EvaluationReport> results) {
        return results.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().metrics().passRate()))
            .map(Map.Entry::getKey)
            .orElse("none");
    }
    
    public record RagConfiguration(
        double vectorWeight,
        double keywordWeight,
        int retrievalLimit,
        boolean useReranking
    ) {}
    
    public record ComparisonReport(
        Map<String, EvaluationReport> results,
        String bestStrategy
    ) {}
}
```

### Exercise 4 (Challenge): Continuous evaluation pipeline

**Solution:**

```java
@Service
public class ContinuousEvaluationPipeline {
    
    private final RagEvaluator evaluator;
    private final MetricsRegistry metricsRegistry;
    
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void runEvaluation() {
        logger.info("Starting continuous evaluation");
        
        try {
            List<EvaluationCase> dataset = loadDataset();
            EvaluationReport report = evaluator.evaluate(dataset);
            
            // Record metrics
            recordMetrics(report);
            
            // Check for degradation
            if (report.metrics().passRate() < 0.8) {
                alertOnDegradation(report);
            }
            
            // Store results
            storeEvaluationResults(report);
            
        } catch (Exception e) {
            logger.error("Evaluation failed", e);
        }
    }
    
    private void recordMetrics(EvaluationReport report) {
        metricsRegistry.gauge("rag.evaluation.pass_rate", 
            report.metrics().passRate());
        metricsRegistry.gauge("rag.evaluation.avg_relevance", 
            report.metrics().avgRelevance());
    }
    
    private void alertOnDegradation(EvaluationReport report) {
        logger.error("RAG quality degradation detected! Pass rate: {}", 
            report.metrics().passRate());
        
        // Send alert (email, Slack, PagerDuty, etc.)
        alertService.send(new Alert(
            "RAG Quality Degradation",
            "Pass rate dropped to " + report.metrics().passRate(),
            AlertSeverity.HIGH
        ));
    }
    
    private void storeEvaluationResults(EvaluationReport report) {
        // Store in database for trend analysis
        evaluationRepository.save(new EvaluationRun(
            Instant.now(),
            report.metrics().passRate(),
            report.metrics().avgRelevance(),
            report.results().size()
        ));
    }
}
```

---

## Chapter 3: Custom Evaluators - Solutions

### Exercise 1: Create answer completeness evaluator

**Solution:**

```java
@Component
public class CompletenessEvaluator implements CustomEvaluator {
    
    private final ChatLanguageModel judgeModel;
    
    @Override
    public EvaluationResult evaluate(String query, String answer, String context) {
        String prompt = String.format("""
            Evaluate if this answer completely addresses all parts of the query.
            
            Query: %s
            Answer: %s
            
            Score the completeness from 0.0 to 1.0:
            - 1.0: Fully answers all aspects of the query
            - 0.5: Partially answers the query
            - 0.0: Does not answer the query
            
            Return JSON:
            {
                "score": 0.0-1.0,
                "missing_aspects": ["list of missing information"],
                "explanation": "brief explanation"
            }
            """, query, answer);
        
        String response = judgeModel.generate(prompt);
        CompletenessScore score = parseScore(response);
        
        return new EvaluationResult(
            "completeness",
            score.score(),
            score.explanation(),
            score.missingAspects()
        );
    }
    
    record CompletenessScore(double score, List<String> missingAspects, String explanation) {}
}
```

### Exercise 2: Create factual accuracy evaluator

**Solution:**

```java
@Component
public class FactualAccuracyEvaluator implements CustomEvaluator {
    
    private final ChatLanguageModel judgeModel;
    
    @Override
    public EvaluationResult evaluate(String query, String answer, String context) {
        String prompt = String.format("""
            Verify the factual accuracy of this answer against the provided context.
            
            Context (source of truth):
            %s
            
            Answer to verify:
            %s
            
            Check each claim in the answer:
            - Is it supported by the context?
            - Are there any hallucinations (facts not in context)?
            - Are there any contradictions?
            
            Return JSON:
            {
                "score": 0.0-1.0,
                "supported_claims": ["list"],
                "hallucinations": ["list"],
                "contradictions": ["list"]
            }
            """, context, answer);
        
        String response = judgeModel.generate(prompt);
        AccuracyAnalysis analysis = parseAnalysis(response);
        
        return new EvaluationResult(
            "factual_accuracy",
            analysis.score(),
            formatExplanation(analysis),
            analysis.hallucinations()
        );
    }
    
    private String formatExplanation(AccuracyAnalysis analysis) {
        if (analysis.hallucinations().isEmpty() && analysis.contradictions().isEmpty()) {
            return "All claims are supported by the context";
        }
        
        return String.format("Found %d hallucinations and %d contradictions",
            analysis.hallucinations().size(),
            analysis.contradictions().size()
        );
    }
    
    record AccuracyAnalysis(
        double score,
        List<String> supportedClaims,
        List<String> hallucinations,
        List<String> contradictions
    ) {}
}
```

### Exercise 3 (Bonus): Create citation accuracy evaluator

**Solution:**

```java
@Component
public class CitationAccuracyEvaluator implements CustomEvaluator {
    
    @Override
    public EvaluationResult evaluate(String query, String answer, String context) {
        // Extract citations from answer (e.g., [1], [2])
        List<Citation> citations = extractCitations(answer);
        
        if (citations.isEmpty()) {
            return new EvaluationResult(
                "citation_accuracy",
                0.0,
                "No citations found in answer",
                List.of()
            );
        }
        
        // Verify each citation
        int correctCitations = 0;
        List<String> errors = new ArrayList<>();
        
        for (Citation citation : citations) {
            if (verifyCitation(citation, context)) {
                correctCitations++;
            } else {
                errors.add("Citation [" + citation.number() + "] is incorrect or unsupported");
            }
        }
        
        double score = (double) correctCitations / citations.size();
        
        return new EvaluationResult(
            "citation_accuracy",
            score,
            String.format("%d/%d citations are accurate", correctCitations, citations.size()),
            errors
        );
    }
    
    private List<Citation> extractCitations(String answer) {
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(answer);
        
        List<Citation> citations = new ArrayList<>();
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            int start = matcher.start();
            citations.add(new Citation(number, start));
        }
        
        return citations;
    }
    
    private boolean verifyCitation(Citation citation, String context) {
        // Extract the claim associated with this citation
        // Verify it against the corresponding source in context
        // Simplified implementation
        return true;
    }
    
    record Citation(int number, int position) {}
}
```

### Exercise 4 (Challenge): Composite evaluator

**Solution:**

```java
@Service
public class CompositeEvaluator {
    
    private final List<CustomEvaluator> evaluators;
    private final Map<String, Double> weights;
    
    public CompositeEvaluator(List<CustomEvaluator> evaluators) {
        this.evaluators = evaluators;
        this.weights = Map.of(
            "completeness", 0.3,
            "factual_accuracy", 0.4,
            "citation_accuracy", 0.2,
            "relevance", 0.1
        );
    }
    
    public CompositeResult evaluate(String query, String answer, String context) {
        List<EvaluationResult> results = evaluators.stream()
            .map(evaluator -> evaluator.evaluate(query, answer, context))
            .toList();
        
        double weightedScore = results.stream()
            .mapToDouble(result -> 
                result.score() * weights.getOrDefault(result.dimension(), 1.0))
            .sum();
        
        // Normalize by sum of weights
        double totalWeight = results.stream()
            .mapToDouble(result -> weights.getOrDefault(result.dimension(), 1.0))
            .sum();
        
        double finalScore = weightedScore / totalWeight;
        
        return new CompositeResult(finalScore, results, determineGrade(finalScore));
    }
    
    private String determineGrade(double score) {
        if (score >= 0.9) return "A";
        if (score >= 0.8) return "B";
        if (score >= 0.7) return "C";
        if (score >= 0.6) return "D";
        return "F";
    }
    
    public record CompositeResult(
        double overallScore,
        List<EvaluationResult> dimensionScores,
        String grade
    ) {}
}
```

---

## Chapter 4: Distributed Tracing - Solutions

### Exercise 1: Add OpenTelemetry instrumentation

**Solution:**

```java
@Configuration
public class TracingConfiguration {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "rag-service",
                ResourceAttributes.SERVICE_VERSION, "1.0.0"
            )));
        
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint("http://localhost:4317")
                    .build()
            ).build())
            .setResource(resource)
            .build();
        
        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .buildAndRegisterGlobal();
    }
}

@Service
public class TracedRagService {
    
    private final Tracer tracer;
    private final RagService ragService;
    
    public TracedRagService(OpenTelemetry openTelemetry, RagService ragService) {
        this.tracer = openTelemetry.getTracer("rag-service");
        this.ragService = ragService;
    }
    
    public String answer(String query) {
        Span span = tracer.spanBuilder("rag.answer")
            .setAttribute("query.length", query.length())
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            String answer = ragService.answer(query, 10);
            
            span.setAttribute("answer.length", answer.length());
            span.setStatus(StatusCode.OK);
            
            return answer;
            
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
            
        } finally {
            span.end();
        }
    }
}
```

### Exercise 2: Trace RAG pipeline stages

**Solution:**

```java
public String answerWithTracing(String query, int limit) {
    Span parentSpan = tracer.spanBuilder("rag.full_pipeline")
        .setAttribute("query", query)
        .startSpan();
    
    try (var parentScope = parentSpan.makeCurrent()) {
        
        // Stage 1: Retrieval
        List<SearchResult> results = traceRetrieval(query, limit);
        
        // Stage 2: Reranking
        List<SearchResult> reranked = traceReranking(query, results);
        
        // Stage 3: Generation
        String answer = traceGeneration(query, reranked);
        
        parentSpan.setAttribute("answer.length", answer.length());
        parentSpan.setStatus(StatusCode.OK);
        
        return answer;
        
    } finally {
        parentSpan.end();
    }
}

private List<SearchResult> traceRetrieval(String query, int limit) {
    Span span = tracer.spanBuilder("rag.retrieval")
        .setAttribute("retrieval.limit", limit)
        .startSpan();
    
    try (var scope = span.makeCurrent()) {
        var results = searchService.search(query, limit);
        span.setAttribute("retrieval.results_count", results.size());
        return results;
    } finally {
        span.end();
    }
}

private List<SearchResult> traceReranking(String query, List<SearchResult> results) {
    Span span = tracer.spanBuilder("rag.reranking")
        .setAttribute("reranking.input_count", results.size())
        .startSpan();
    
    try (var scope = span.makeCurrent()) {
        var reranked = reRankingService.rerank(query, results, 5);
        span.setAttribute("reranking.output_count", reranked.size());
        return reranked;
    } finally {
        span.end();
    }
}
```

### Exercise 3 (Bonus): Custom span attributes

**Solution:**

```java
public class RichSpanAttributes {
    
    public static void addQueryAttributes(Span span, String query) {
        span.setAttribute("query.text", query);
        span.setAttribute("query.word_count", query.split("\\s+").length);
        span.setAttribute("query.char_count", query.length());
        span.setAttribute("query.has_question_mark", query.contains("?"));
    }
    
    public static void addRetrievalAttributes(Span span, List<SearchResult> results) {
        span.setAttribute("retrieval.count", results.size());
        
        if (!results.isEmpty()) {
            span.setAttribute("retrieval.top_score", results.get(0).score());
            span.setAttribute("retrieval.avg_score", 
                results.stream().mapToDouble(SearchResult::score).average().orElse(0.0));
        }
    }
    
    public static void addGenerationAttributes(Span span, String answer, long durationMs) {
        span.setAttribute("generation.answer_length", answer.length());
        span.setAttribute("generation.word_count", answer.split("\\s+").length);
        span.setAttribute("generation.duration_ms", durationMs);
        span.setAttribute("generation.words_per_second", 
            (answer.split("\\s+").length * 1000.0) / durationMs);
    }
}
```

### Exercise 4 (Challenge): Trace visualization dashboard

**Solution:**

```java
@RestController
@RequestMapping("/api/v1/traces")
public class TraceController {
    
    private final TraceRepository traceRepository;
    
    @GetMapping("/recent")
    public List<TraceSummary> getRecentTraces(
            @RequestParam(defaultValue = "10") int limit) {
        
        return traceRepository.findRecent(limit).stream()
            .map(this::toSummary)
            .toList();
    }
    
    @GetMapping("/{traceId}")
    public TraceDetails getTraceDetails(@PathVariable String traceId) {
        Trace trace = traceRepository.findById(traceId)
            .orElseThrow(() -> new NotFoundException("Trace not found"));
        
        return new TraceDetails(
            trace.traceId(),
            trace.timestamp(),
            trace.duration(),
            buildSpanTree(trace.spans()),
            extractAttributes(trace)
        );
    }
    
    @GetMapping("/stats")
    public TraceStatistics getStatistics(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        
        List<Trace> traces = traceRepository.findBetween(from, to);
        
        return new TraceStatistics(
            traces.size(),
            calculateAverageDuration(traces),
            calculateP95Duration(traces),
            calculateErrorRate(traces),
            groupByEndpoint(traces)
        );
    }
    
    private SpanNode buildSpanTree(List<Span> spans) {
        // Build hierarchical span tree
        Map<String, SpanNode> nodes = new HashMap<>();
        
        for (Span span : spans) {
            nodes.put(span.spanId(), new SpanNode(span, new ArrayList<>()));
        }
        
        SpanNode root = null;
        for (Span span : spans) {
            SpanNode node = nodes.get(span.spanId());
            
            if (span.parentSpanId() == null) {
                root = node;
            } else {
                SpanNode parent = nodes.get(span.parentSpanId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        
        return root;
    }
    
    record SpanNode(Span span, List<SpanNode> children) {}
    record TraceSummary(String traceId, Instant timestamp, long duration, String status) {}
    record TraceDetails(
        String traceId,
        Instant timestamp,
        long duration,
        SpanNode spanTree,
        Map<String, Object> attributes
    ) {}
}
```

---

## Chapter 5: Caching Strategies - Solutions

### Exercise 1: Implement embedding cache

**Solution:**

```java
@Service
public class CachedEmbeddingService {
    
    private final EmbeddingService embeddingService;
    private final Cache<String, float[]> cache;
    
    public CachedEmbeddingService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();
    }
    
    public float[] getVector(String text) {
        return cache.get(text, key -> embeddingService.getVector(key));
    }
    
    public CacheStats getStats() {
        return cache.stats();
    }
}
```

### Exercise 2: Test cache effectiveness

**Solution:**

```java
@Test
void shouldCacheEmbeddings() {
    CachedEmbeddingService service = new CachedEmbeddingService(embeddingService);
    
    String text = "test query";
    
    // First call - cache miss
    float[] result1 = service.getVector(text);
    
    // Second call - cache hit
    float[] result2 = service.getVector(text);
    
    assertThat(result1).isEqualTo(result2);
    
    verify(embeddingService, times(1)).getVector(text);
    
    CacheStats stats = service.getStats();
    assertThat(stats.hitRate()).isGreaterThan(0.0);
}
```

### Exercise 3 (Bonus): Multi-level cache

**Solution:**

```java
@Service
public class MultiLevelCacheService {
    
    private final Cache<String, String> l1Cache; // In-memory
    private final Cache<String, String> l2Cache; // Redis
    private final RagService ragService;
    
    public MultiLevelCacheService(RagService ragService, RedisTemplate<String, String> redis) {
        this.ragService = ragService;
        
        // L1: Fast in-memory cache
        this.l1Cache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
        
        // L2: Larger Redis cache
        this.l2Cache = CaffeineCache.build(
            redis,
            Duration.ofHours(1),
            1000
        );
    }
    
    public String getAnswer(String query) {
        // Try L1 cache
        String answer = l1Cache.getIfPresent(query);
        if (answer != null) {
            logger.debug("L1 cache hit for: {}", query);
            return answer;
        }
        
        // Try L2 cache
        answer = l2Cache.getIfPresent(query);
        if (answer != null) {
            logger.debug("L2 cache hit for: {}", query);
            l1Cache.put(query, answer); // Promote to L1
            return answer;
        }
        
        // Cache miss - compute
        logger.debug("Cache miss for: {}", query);
        answer = ragService.answer(query, 10);
        
        // Store in both caches
        l1Cache.put(query, answer);
        l2Cache.put(query, answer);
        
        return answer;
    }
}
```

### Exercise 4 (Challenge): Semantic cache

**Solution:**

```java
@Service
public class SemanticCacheService {
    
    private final EmbeddingService embeddingService;
    private final List<CacheEntry> cache = new CopyOnWriteArrayList<>();
    private final double similarityThreshold = 0.95;
    
    public Optional<String> get(String query) {
        float[] queryVector = embeddingService.getVector(query);
        
        return cache.stream()
            .filter(entry -> {
                double similarity = cosineSimilarity(queryVector, entry.queryVector());
                return similarity >= similarityThreshold;
            })
            .max(Comparator.comparingDouble(entry -> 
                cosineSimilarity(queryVector, entry.queryVector())))
            .map(CacheEntry::answer);
    }
    
    public void put(String query, String answer) {
        float[] queryVector = embeddingService.getVector(query);
        cache.add(new CacheEntry(query, queryVector, answer, Instant.now()));
        
        // Evict old entries
        if (cache.size() > 1000) {
            cache.sort(Comparator.comparing(CacheEntry::timestamp));
            cache.subList(0, 200).clear();
        }
    }
    
    public String getOrCompute(String query, Supplier<String> supplier) {
        return get(query).orElseGet(() -> {
            String answer = supplier.get();
            put(query, answer);
            return answer;
        });
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    record CacheEntry(String query, float[] queryVector, String answer, Instant timestamp) {}
}
```

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Workshop Complete!](../README.md)**
