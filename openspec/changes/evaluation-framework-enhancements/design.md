## Context

Module 06 now has Dokimos evaluation framework integrated with 5 evaluators (3 built-in + 2 custom). While this provides a solid foundation for RAG system assessment, production deployments require additional capabilities for cost optimization, continuous monitoring, and domain-specific evaluation.

Current state:
- Dokimos framework v0.14.2 with Spring AI v1.0.0-M5
- 5 evaluators: faithfulness, hallucination, contextual-relevance, exact-match, response-length
- REST API for running evaluations
- JUnit integration for CI/CD
- Spring Boot 4.0.5, Java 25, Langchain4J 1.11.0

Constraints:
- Must maintain compatibility with existing Dokimos integration
- Redis already present in Module 06 for caching (unused for evaluation)
- Grafana/Prometheus deployment assumed for production environments
- CI/CD platforms: GitHub Actions, GitLab CI, Jenkins (common in enterprise)
- Educational focus: students should understand production patterns

Stakeholders:
- Workshop students learning production LLM application patterns
- DevOps teams deploying evaluation pipelines
- Product teams monitoring RAG system quality over time

## Goals / Non-Goals

**Goals:**
- Provide 5 production-ready custom evaluators for common domain needs (citation quality, source diversity, answer completeness, semantic coherence, bias detection)
- Reduce LLM API costs through intelligent caching of expensive evaluations
- Enable automated quality gates in CI/CD pipelines with configurable thresholds
- Provide real-time visibility into evaluation metrics via Grafana dashboards
- Maintain educational clarity—students should understand each enhancement

**Non-Goals:**
- Building a general-purpose evaluation framework (Dokimos already provides this)
- Real-time streaming evaluation (batch processing is sufficient)
- Multi-tenant evaluation isolation (single-tenant workshop scope)
- Automated model retraining based on evaluation results (future enhancement)
- Supporting CI/CD platforms beyond GitHub Actions, GitLab CI, Jenkins
- Custom Grafana plugin development (use standard panels)

## Decisions

### Decision 1: Custom Evaluator Implementation Strategy

**Choice:** Implement all custom evaluators as standalone classes extending `BaseEvaluator` with Spring bean registration, following the same pattern as `ResponseLengthEvaluator`.

**Rationale:**
- **Consistency**: Matches existing pattern established in Dokimos integration
- **Discoverability**: Spring beans auto-wire into `DokimosEvaluationService`
- **Configuration**: Leverage Spring Boot's `@ConfigurationProperties` for thresholds
- **Testability**: Each evaluator is independently testable

**Alternatives Considered:**
- **Evaluator factory pattern**: Rejected—adds complexity without benefit
- **Dynamic evaluator loading via SPI**: Rejected—overkill for 5 evaluators
- **Inline evaluators in service class**: Rejected—violates single responsibility

**Implementation:**
```java
@Configuration
public class CustomEvaluatorConfig {
    @Bean
    public CitationQualityEvaluator citationQualityEvaluator(
            @Value("${dokimos.evaluators.citation-quality.min-citations:2}") int minCitations) {
        return new CitationQualityEvaluator(minCitations, 1.0);
    }
    // ... other evaluators
}
```

### Decision 2: LLM-as-Judge vs Rule-Based for Custom Evaluators

**Choice:** Use rule-based logic for all 5 custom evaluators to avoid increasing LLM API costs.

**Rationale:**
- **Cost**: LLM-as-judge evaluators are expensive; already have 3 built-in
- **Determinism**: Rule-based evaluators provide consistent, reproducible scores
- **Speed**: Fast execution enables quick feedback loops in CI/CD
- **Transparency**: Students can understand exact evaluation logic

**Breakdown:**
- **Citation Quality**: Pattern matching for [1], [2] format (rule-based)
- **Source Diversity**: Count distinct sources in context (rule-based)
- **Answer Completeness**: Keyword/phrase matching against query (rule-based with heuristics)
- **Semantic Coherence**: Sentence structure analysis + contradiction detection (rule-based with NLP)
- **Bias Detection**: Keyword lists for loaded language (rule-based with word lists)

**Alternatives Considered:**
- **All LLM-as-judge**: Rejected—doubles LLM API costs
- **Hybrid (2-3 LLM-as-judge)**: Considered—would provide higher quality but conflicts with cost goal
- **Embedding similarity**: Considered—adds complexity, less transparent

**Trade-off:** Rule-based evaluators may miss nuanced issues that LLM-as-judge would catch, but cost/speed benefits outweigh this for these specific metrics.

### Decision 3: Caching Architecture

**Choice:** Use Spring Cache abstraction with Redis backend, caching at the `EvalResult` level with cache keys based on evaluator name + example input + task outputs.

**Rationale:**
- **Spring Integration**: Leverages `@Cacheable` annotations, minimal code
- **Flexibility**: Cache abstraction allows switching backends (Redis → Caffeine for local dev)
- **Granularity**: `EvalResult` level balances hit rate with complexity
- **Correctness**: Including task outputs in key prevents stale results when RAG system changes

**Alternatives Considered:**
- **Cache at Experiment level**: Rejected—too coarse, low hit rate
- **Cache at Example level (all evaluators)**: Rejected—misses partial re-evaluation opportunities
- **Custom caching logic**: Rejected—Spring Cache provides needed features

**Implementation:**
```java
public class CachingEvaluatorProxy implements Evaluator {
    @Cacheable(value = "evaluation-results",
               key = "#evaluator.name() + ':' + #testCase.input() + ':' + #testCase.actualOutput().hashCode()")
    public EvalResult evaluate(Evaluator evaluator, EvalTestCase testCase) {
        return evaluator.evaluate(testCase);
    }
}
```

**Cache Key Strategy:**
- Format: `{evaluatorName}:{exampleInputHash}:{taskOutputHash}`
- Example: `faithfulness:abc123:def456`
- Hash functions ensure fixed-length keys
- Collision risk mitigated by including both input and output

### Decision 4: Selective Caching by Evaluator Type

**Choice:** Only cache LLM-as-judge evaluators (faithfulness, hallucination, contextual-relevance) by default; allow configuration override.

**Rationale:**
- **Cost Focus**: LLM-as-judge evaluators are expensive (gpt-4 API calls)
- **Fast Evaluators**: Rule-based evaluators (exact-match, response-length) execute in milliseconds—caching overhead not worth it
- **Configurable**: `dokimos.cache.evaluators` property allows overriding defaults

**Configuration:**
```yaml
dokimos:
  cache:
    enabled: true
    evaluators:
      - faithfulness
      - hallucination
      - contextual-relevance
    ttl:
      default: 24h
      golden-set: 0  # Never expire
```

### Decision 5: CI/CD Pipeline Strategy

**Choice:** Provide 3 complete pipeline templates (GitHub Actions, GitLab CI, Jenkins) as standalone files in `cicd/` directory, not integrated into Module 06 repository directly.

**Rationale:**
- **Separation**: Workshop repository shouldn't have production CI/CD in root
- **Templates**: Students copy/adapt templates to their own repositories
- **Multiple Platforms**: Enterprise uses vary; supporting top 3 covers 90% of users
- **Maintainability**: Separate files easier to update independently

**Template Structure:**
```
cicd/
├── github-actions/
│   ├── evaluation-pr.yml         # PR quality gate
│   ├── evaluation-main.yml       # Main branch full evaluation
│   └── README.md                 # Setup instructions
├── gitlab-ci/
│   ├── .gitlab-ci.yml            # Complete pipeline
│   └── README.md
└── jenkins/
    ├── Jenkinsfile               # Declarative pipeline
    └── README.md
```

### Decision 6: Threshold Configuration Strategy

**Choice:** Support environment-specific thresholds via Spring profiles (dev, staging, prod) with defaults suitable for each.

**Rationale:**
- **Flexibility**: Different environments have different quality bars
- **Spring Patterns**: Leverages familiar Spring Boot configuration
- **Defaults**: Sensible defaults prevent misconfiguration

**Configuration:**
```yaml
# application.yml
dokimos:
  evaluators:
    faithfulness:
      threshold: 0.7  # Default

---
# application-prod.yml
spring.config.activate.on-profile: prod
dokimos:
  evaluators:
    faithfulness:
      threshold: 0.85  # Stricter for production
```

**Pipeline Integration:**
- PR checks use dev profile (lenient: 70%)
- Main branch uses staging profile (moderate: 75%)
- Production deployments use prod profile (strict: 85%)

### Decision 7: Grafana Dashboard Design

**Choice:** Create 3 separate dashboards (Overview, Evaluator Details, Operations) as importable JSON files, using Prometheus as datasource.

**Rationale:**
- **Separation of Concerns**: Different audiences need different views
- **Prometheus Standard**: Already integrated in Module 06 for metrics
- **JSON Import**: Simple deployment (no Terraform/API complexity)
- **Pre-configured**: Students can import and immediately use

**Dashboard Breakdown:**
1. **Overview Dashboard**: Pass rates, trends, recent regressions
   - Audience: Product teams, stakeholders
   - Refresh: 30s
   - Panels: Current pass rate, 7-day trend, evaluator summary

2. **Evaluator Details**: Per-evaluator scores, distributions, examples
   - Audience: ML engineers, developers
   - Refresh: 1m
   - Panels: Score histograms, min/max ranges, failed examples

3. **Operations Dashboard**: Execution metrics, cache performance, cost tracking
   - Audience: DevOps, SREs
   - Refresh: 30s
   - Panels: Cache hit rate, API call count, estimated costs, execution duration

### Decision 8: Metrics Instrumentation

**Choice:** Add Micrometer metrics in `DokimosEvaluationService` using Spring Boot Actuator, exposing custom metrics via `/actuator/prometheus`.

**Rationale:**
- **Spring Integration**: Micrometer is Spring Boot standard
- **Prometheus Compatible**: Already configured in Module 06
- **Minimal Code**: Annotations + counters/gauges

**Metrics to Add:**
```java
@Timed(value = "evaluation.execution.time", description = "Evaluation execution duration")
public ExperimentResult runExperiment(List<String> evaluatorFilter) {
    // Implementation
}

@Counted(value = "evaluation.cache.hits", description = "Cache hit count")
@Counted(value = "evaluation.cache.misses", description = "Cache miss count")
public EvalResult getCachedResult(String key) {
    // Cache lookup
}

meterRegistry.gauge("evaluation.pass.rate", this, service -> service.getLatestPassRate());
meterRegistry.counter("evaluation.llm.api.calls").increment();
meterRegistry.gauge("evaluation.estimated.cost.usd", this, service -> service.calculateCost());
```

## Risks / Trade-offs

### Risk: Cache Invalidation Complexity
**Risk:** Cached results become stale when RAG system or evaluator logic changes
**Mitigation:**
- Include task output hash in cache key (catches RAG changes)
- Provide manual cache invalidation API endpoint
- Document cache TTL recommendations
- Consider cache versioning if evaluator logic changes

### Risk: Rule-Based Evaluators Limited Accuracy
**Risk:** Rule-based custom evaluators may miss nuanced issues vs LLM-as-judge
**Mitigation:**
- Clearly document evaluator limitations in lab materials
- Provide guidance on when to use LLM-as-judge custom evaluators
- Include "building LLM-as-judge custom evaluator" as advanced exercise
- Built-in LLM-as-judge evaluators still available for semantic quality

### Risk: CI/CD Pipeline Template Drift
**Risk:** Pipeline templates become outdated as GitHub Actions/GitLab CI/Jenkins evolve
**Mitigation:**
- Version templates (v1, v2) with migration guides
- Include "last tested with" versions in README
- Provide basic template that works long-term vs cutting-edge features

### Risk: Grafana Dashboard Compatibility
**Risk:** Dashboard JSON may not work with older/newer Grafana versions
**Mitigation:**
- Target Grafana 10.x (current stable)
- Document required Grafana version in setup guide
- Provide manual panel configuration instructions as fallback

### Risk: Redis Dependency for Caching
**Risk:** Teams without Redis cannot use caching feature
**Mitigation:**
- Caching is optional (graceful degradation if Redis unavailable)
- Document how to use in-memory cache (Caffeine) for local development
- Provide Docker Compose for easy local Redis setup

### Trade-off: Educational vs Production Optimality
**Trade-off:** Some production optimizations (e.g., distributed caching, advanced alerting) omitted for simplicity
**Decision:** Prioritize educational clarity—students grasp concepts before advanced patterns
**Mitigation:** Include "Production Considerations" section in docs pointing to advanced topics

## Migration Plan

### Phase 1: Custom Evaluators (Low Risk)
1. Add 5 custom evaluator classes to `module06.dokimos` package
2. Register beans in `CustomEvaluatorConfig`
3. Update `DokimosEvaluationService.buildEvaluatorList()` to include new evaluators
4. Add configuration properties to `application.yml`
5. Run tests to verify registration

**Rollback:** Remove beans from config; evaluators are optional

### Phase 2: Caching Layer (Medium Risk)
1. Create `EvaluationCacheConfig` with Redis configuration
2. Implement `CachingEvaluatorProxy` wrapper
3. Add cache metrics to `DokimosEvaluationService`
4. Update documentation with Redis setup guide
5. Test cache hit/miss behavior

**Rollback:** Set `dokimos.cache.enabled=false`; system works without caching

### Phase 3: CI/CD Templates (No Risk)
1. Create `cicd/` directory with templates
2. Write setup guides for each platform
3. Test templates in separate repository
4. Document threshold configuration patterns

**Rollback:** N/A (templates don't affect runtime)

### Phase 4: Grafana Dashboards (No Risk)
1. Create dashboard JSON files
2. Add Prometheus metrics to evaluation service
3. Write dashboard setup guide
4. Test import in Grafana 10.x

**Rollback:** N/A (dashboards don't affect runtime)

## Open Questions

1. **Custom Evaluator Extensibility**: Should we provide base classes for common patterns (keyword-based, pattern-matching)? Or keep each evaluator standalone?
   - **Decision needed by**: Design complete
   - **Recommendation**: Keep standalone initially; refactor common patterns if 3+ evaluators share logic

2. **Cache Warming Strategy**: Should we provide a way to pre-populate cache with golden dataset results?
   - **Decision needed by**: Implementation
   - **Recommendation**: Add `/api/v1/eval/cache/warm` endpoint that runs golden set

3. **Dashboard Alerting**: Should alerts be configured in JSON or require manual setup?
   - **Decision needed by**: Implementation
   - **Recommendation**: Manual setup (alerts need Slack webhooks, email config)

4. **Metrics Retention**: What's the recommended Prometheus retention for evaluation metrics?
   - **Decision needed by**: Documentation
   - **Recommendation**: 30 days (balance detail vs storage cost)

5. **CI/CD Artifact Storage**: Where should evaluation artifacts be stored long-term?
   - **Decision needed by**: Documentation
   - **Recommendation**: Document options (S3, Artifactory, GitHub Releases) but don't mandate
