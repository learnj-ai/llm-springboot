## ADDED Requirements

### Requirement: Evaluation Result Caching
The system SHALL cache evaluation results in Redis to avoid redundant expensive LLM-as-judge evaluations.

#### Scenario: Cache hit on identical evaluation
- **WHEN** same example and evaluator are run twice within TTL period
- **THEN** second execution returns cached result without calling LLM API

#### Scenario: Cache miss on first evaluation
- **WHEN** example is evaluated for the first time
- **THEN** evaluation executes normally and result is cached

#### Scenario: Cache expiration after TTL
- **WHEN** cached result exceeds configured TTL (e.g., 24 hours)
- **THEN** next evaluation executes fresh evaluation and updates cache

### Requirement: Cache Key Generation
The system SHALL generate cache keys based on example input, evaluator name, and evaluator configuration to ensure correctness.

#### Scenario: Same input different evaluators
- **WHEN** same example is evaluated by faithfulness and hallucination evaluators
- **THEN** separate cache entries are created with distinct keys

#### Scenario: Same evaluator different thresholds
- **WHEN** faithfulness evaluator runs with threshold 0.7 then threshold 0.8
- **THEN** separate cache entries are created (threshold affects key)

#### Scenario: Cache key includes task outputs
- **WHEN** same query produces different RAG responses (non-deterministic LLM)
- **THEN** different cache keys are generated based on actual response

### Requirement: Selective Caching
The system SHALL allow selective caching by evaluator type, enabling caching only for expensive LLM-as-judge evaluators.

#### Scenario: LLM-as-judge evaluators cached
- **WHEN** faithfulness, hallucination, or contextual-relevance evaluators run
- **THEN** results are cached in Redis

#### Scenario: Rule-based evaluators not cached
- **WHEN** exact-match or response-length evaluators run
- **THEN** results are NOT cached (fast execution, no benefit)

#### Scenario: Caching configurable per evaluator
- **WHEN** application.yml sets dokimos.cache.evaluators=[faithfulness, hallucination]
- **THEN** only specified evaluators use caching

### Requirement: Cache Statistics
The system SHALL track and expose cache hit/miss rates via Prometheus metrics for monitoring.

#### Scenario: Cache hit rate metric exposed
- **WHEN** Prometheus scrapes /actuator/prometheus endpoint
- **THEN** metric evaluation_cache_hit_rate is available

#### Scenario: Cache size metric exposed
- **WHEN** monitoring dashboard queries cache size
- **THEN** metric evaluation_cache_entries shows current cached result count

#### Scenario: LLM cost savings metric
- **WHEN** evaluations use cached results
- **THEN** metric evaluation_cache_cost_savings tracks estimated API cost avoided

### Requirement: Cache Invalidation
The system SHALL support manual cache invalidation for testing and debugging purposes.

#### Scenario: Clear all cached results
- **WHEN** administrator calls POST /api/v1/eval/cache/clear
- **THEN** all cached evaluation results are removed from Redis

#### Scenario: Clear cache for specific evaluator
- **WHEN** administrator calls DELETE /api/v1/eval/cache/{evaluatorName}
- **THEN** only results for specified evaluator are invalidated

#### Scenario: Clear cache for specific dataset
- **WHEN** dataset is updated with new examples
- **THEN** all cached results for that dataset are invalidated

### Requirement: Redis Configuration
The system SHALL support standard Spring Data Redis configuration for connection and serialization.

#### Scenario: Redis connection configured
- **WHEN** application.yml specifies spring.data.redis.host and spring.data.redis.port
- **THEN** caching service connects to specified Redis instance

#### Scenario: Redis unavailable fallback
- **WHEN** Redis connection fails or is unavailable
- **THEN** evaluations continue without caching and log warning

#### Scenario: Cache serialization configured
- **WHEN** complex evaluation results are cached
- **THEN** JSON serialization is used for compatibility

### Requirement: TTL Configuration
The system SHALL support configurable Time-To-Live (TTL) for cached results based on evaluation stability.

#### Scenario: Default TTL applied
- **WHEN** no TTL specified in configuration
- **THEN** cached results expire after 24 hours (default)

#### Scenario: Custom TTL per evaluator
- **WHEN** application.yml sets dokimos.cache.ttl.faithfulness=48h
- **THEN** faithfulness results are cached for 48 hours

#### Scenario: Permanent caching for golden datasets
- **WHEN** application.yml sets dokimos.cache.ttl.golden-set=0
- **THEN** results for golden-set dataset never expire

### Requirement: Documentation
The system SHALL provide documentation on caching setup, configuration, and troubleshooting.

#### Scenario: Caching setup guide available
- **WHEN** developer reads docs/caching/SETUP.md
- **THEN** guide covers Redis installation, Spring configuration, and verification

#### Scenario: Performance tuning guide available
- **WHEN** developer reads docs/caching/PERFORMANCE.md
- **THEN** guide explains TTL selection, selective caching, and monitoring

#### Scenario: Lab exercise provided
- **WHEN** student completes Lab 6.5: Evaluation Result Caching
- **THEN** student can configure Redis, measure cache hit rates, and optimize costs
