# Metrics and Monitoring: Observability in Production

You can't improve what you don't measure. In production, **metrics** give you real-time visibility into system health, performance, and user behavior. How many queries per second? What's the 95th percentile latency? How many LLM tokens are you burning through? This chapter shows you how to collect, expose, and analyze production metrics using Prometheus and Micrometer.

## What are Metrics?

**Metrics** are numerical measurements collected over time. Unlike logs (discrete events) or traces (request flows), metrics aggregate data into time series:

- **Counters**: Ever-increasing values (total requests, total errors)
- **Gauges**: Point-in-time values (current memory usage, active connections)
- **Histograms**: Distribution of values (latency percentiles, response sizes)
- **Timers**: Specialized histograms for measuring duration

## Why Metrics Matter for RAG

RAG systems have unique monitoring needs:

**Performance metrics**:
- Query throughput (queries/second)
- Response latency (p50, p95, p99)
- Cache hit rates

**Cost metrics**:
- Total LLM tokens used
- API calls per minute
- Estimated cost per query

**Quality metrics**:
- Average evaluation scores
- Error rates
- Retrieval relevance

Without metrics, you're flying blind.

## Architecture

```mermaid
graph LR
    A[Application] --> B[Micrometer]
    B --> C[MeterRegistry]
    C --> D[Prometheus Exporter]
    D --> E[/actuator/prometheus]
    F[Prometheus Server] -->|scrapes| E
    F --> G[Grafana Dashboards]
```

**Components**:
- **Micrometer**: Vendor-neutral metrics facade (like SLF4J for metrics)
- **MeterRegistry**: Stores metrics in memory
- **Prometheus exporter**: Exposes metrics in Prometheus format
- **Prometheus server** (external): Scrapes and stores time series
- **Grafana** (external): Visualizes metrics in dashboards

## Code Deep Dive

### MetricsCollector

Custom service for RAG-specific metrics:

```java
@Service
public class MetricsCollector {

    private final Counter queryCounter;
    private final Timer responseTimer;
    private final AtomicLong tokensUsed;

    public MetricsCollector(MeterRegistry meterRegistry) {
        // Counter for total queries
        this.queryCounter = Counter.builder("rag.queries.total")
                .description("Total number of RAG queries processed")
                .tag("type", "rag")
                .register(meterRegistry);

        // Timer for response time
        this.responseTimer = Timer.builder("rag.response.time")
                .description("RAG query response time (Prometheus exports as `_seconds`)")
                .tag("type", "rag")
                .register(meterRegistry);

        // Gauge for token usage
        this.tokensUsed = new AtomicLong(0);
        Gauge.builder("rag.tokens.used", tokensUsed, AtomicLong::get)
                .description("Total tokens used by RAG queries")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    public void recordQuery() {
        queryCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordResponseTime(Timer.Sample sample) {
        sample.stop(responseTimer);
    }

    public void recordTokens(long tokens) {
        tokensUsed.addAndGet(tokens);
    }
}
```

**Key patterns**:
- **Counter**: Use `increment()` for events (queries, errors, cache hits)
- **Timer**: Use `Timer.start()` / `sample.stop()` pattern for latency
- **Gauge**: Use `AtomicLong` or `Supplier` for current values

### Integration with RAG Service

Instrument your RAG service:

```java
@Service
public class SimpleRAGService {

    private final MetricsCollector metrics;
    private final ChatLanguageModel chatModel;

    public RAGResponse query(String query) {
        // Start timer
        Timer.Sample sample = metrics.startTimer();
        metrics.recordQuery();

        try {
            // Execute RAG
            String response = chatModel.generate(query);
            int tokensUsed = estimateTokens(response);

            // Record metrics
            metrics.recordTokens(tokensUsed);
            metrics.recordResponseTime(sample);

            return RAGResponse.success(response, tokensUsed);

        } catch (Exception e) {
            metrics.recordError(e.getClass().getSimpleName());
            throw e;
        }
    }
}
```

### Viewing Metrics

Start the application and visit: http://localhost:8086/actuator/prometheus

**Example output**:

```prometheus
# HELP rag_queries_total Total number of RAG queries processed
# TYPE rag_queries_total counter
rag_queries_total{type="rag"} 523.0

# HELP rag_response_time_seconds RAG query response time
# TYPE rag_response_time_seconds histogram
rag_response_time_seconds_bucket{type="rag",le="0.1"} 12.0
rag_response_time_seconds_bucket{type="rag",le="0.5"} 145.0
rag_response_time_seconds_bucket{type="rag",le="1.0"} 387.0
rag_response_time_seconds_bucket{type="rag",le="2.0"} 501.0
rag_response_time_seconds_bucket{type="rag",le="+Inf"} 523.0
rag_response_time_seconds_sum{type="rag"} 612.34
rag_response_time_seconds_count{type="rag"} 523.0

# HELP rag_tokens_used Total tokens used by RAG queries
# TYPE rag_tokens_used gauge
rag_tokens_used{type="rag"} 78456.0
```

**Interpreting results**:
- **Total queries**: 523
- **P95 latency**: ~1.7s (between 1.0 and 2.0 buckets)
- **Average latency**: 612.34 / 523 = 1.17s
- **Total tokens**: 78,456
- **Average tokens/query**: 78,456 / 523 = 150

## Common Metrics

### Performance Metrics

```java
// Throughput
Counter.builder("rag.queries.total")
    .tag("status", "success")
    .register(registry);

// Latency distribution
Timer.builder("rag.latency")
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(registry);

// Cache effectiveness
Counter.builder("cache.hits").register(registry);
Counter.builder("cache.misses").register(registry);
```

### Cost Metrics

```java
// Token usage by operation
Counter.builder("llm.tokens.total")
    .tag("operation", "embedding")
    .register(registry);

Counter.builder("llm.tokens.total")
    .tag("operation", "generation")
    .register(registry);

// Estimated cost (assuming $0.03/1K tokens)
Gauge.builder("llm.cost.estimated", () -> {
    long tokens = tokensUsed.get();
    return (tokens / 1000.0) * 0.03;
}).register(registry);
```

### Quality Metrics

```java
// Evaluation scores
Gauge.builder("eval.faithfulness.avg", () -> {
    return evaluationService.getAverageFaithfulness();
}).register(registry);

// Error rates
Counter.builder("rag.errors.total")
    .tag("error_type", "llm_timeout")
    .register(registry);
```

## Practice Exercise

Create a custom metric for monitoring evaluation quality over time.

### Task: Track Evaluation Pass Rate

1. **Add metric to DokimosEvaluationService**:

```java
@Service
public class DokimosEvaluationService {

    private final AtomicReference<Double> lastPassRate = new AtomicReference<>(0.0);

    public DokimosEvaluationService(MeterRegistry meterRegistry, ...) {
        // ... existing code

        Gauge.builder("eval.pass_rate", lastPassRate, AtomicReference::get)
            .description("Most recent evaluation pass rate")
            .register(meterRegistry);
    }

    public ExperimentResult runExperiment(List<String> evaluatorFilter) {
        ExperimentResult result = experiment.run();

        // Update pass rate metric
        lastPassRate.set(result.passRate());

        return result;
    }
}
```

2. **Run evaluations** and check metrics:

```bash
curl -X POST http://localhost:8086/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{"datasetName": "eval-golden-set"}'
```

3. **View the metric**:

```bash
curl http://localhost:8086/actuator/prometheus | grep eval_pass_rate
# eval_pass_rate 0.85
```

**Expected Outcome**: The pass rate metric updates after each evaluation, allowing you to track quality trends over time.

## Scraping Metrics in OpenShift

Locally we let Prometheus scrape the app directly (see `docker/prometheus/prometheus.yml`). In OpenShift the recommended path is to **enable user-workload monitoring** and declare a `ServiceMonitor`/`PodMonitor` — the cluster-managed Prometheus then picks up your pod via label selector, and you don't run your own Prometheus instance.

### 1. Enable user-workload monitoring (cluster-admin)

OpenShift 4.6+ ships a built-in monitoring stack but only scrapes platform components by default. Enable user workloads by patching the cluster monitoring config:

```yaml
# cluster-monitoring-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
```

```bash
oc apply -f cluster-monitoring-config.yaml
oc -n openshift-user-workload-monitoring get pods   # should show prometheus-user-workload-* running
```

### 2. Expose the `/actuator/prometheus` endpoint behind a Service port

Make sure the Service that fronts your deployment exposes the actuator port (8086 in this module) and the pod template carries a label the `ServiceMonitor` can match — e.g. `app: module-06-production`.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: module-06-production
  labels:
    app: module-06-production
spec:
  selector:
    app: module-06-production
  ports:
    - name: metrics            # the ServiceMonitor matches this name, not the number
      port: 8086
      targetPort: 8086
```

### 3. Declare a `ServiceMonitor` in the application namespace

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: module-06-production
  namespace: rag-prod            # same namespace as the Service above
  labels:
    app: module-06-production
spec:
  selector:
    matchLabels:
      app: module-06-production
  endpoints:
    - port: metrics              # matches the Service port name
      path: /actuator/prometheus
      interval: 30s
      scheme: http
      # If you've enabled TLS on the actuator port, set scheme: https and add tlsConfig.
```

```bash
oc apply -f servicemonitor.yaml
oc -n openshift-user-workload-monitoring port-forward svc/prometheus-user-workload 9090
# In the Prometheus UI's Targets page, your endpoint should now be UP.
```

### 4. PodMonitor as a lighter alternative

If you don't want a Service (e.g. a Job that should be scraped while it runs), declare a `PodMonitor` on the pod label instead:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: module-06-production
  namespace: rag-prod
spec:
  selector:
    matchLabels:
      app: module-06-production
  podMetricsEndpoints:
    - port: metrics
      path: /actuator/prometheus
      interval: 30s
```

### Notes

- The Prometheus instance scraping your endpoints is `prometheus-user-workload`, not the cluster `prometheus-k8s`. Grafana / dashboards must be pointed at the user-workload Prometheus datasource (`Thanos Querier`'s "user" tenant works too) — see the OpenShift docs for [Querying metrics for user-defined projects](https://docs.openshift.com/container-platform/latest/observability/monitoring/accessing-third-party-monitoring-apis.html).
- If you've also installed the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator) on vanilla Kubernetes, the same `ServiceMonitor`/`PodMonitor` manifests apply unchanged — that's the upstream API OpenShift adopted.
- The starter Grafana dashboard shipped under `docker/grafana/dashboards/rag-dashboard.json` queries the same metric names (`rag_response_time_seconds`, `rag_queries_total`, `rag_tokens_used`), so once the user-workload Prometheus is wired into Grafana, you can import the dashboard as-is.

---

## Navigation

👈 **[Previous: Caching Strategies: Performance and Cost Optimization](05-caching-strategies.md)**

👉 **[Next: Token Optimization: Reducing Costs and Latency](07-token-optimization.md)**
