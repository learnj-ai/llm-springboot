# Module 06: Enterprise Best Practices and Production

> ⚠️ **Partially superseded** — see the canonical tutorial at
> [`docs/tutorials/module-06-enterprise-production/`](../../../docs/tutorials/module-06-enterprise-production/).
> Sections referencing `spring-ai-openai-spring-boot-starter` or
> `dokimos-spring-ai` describe the pre-Spring-Boot-4 wiring; Module 06 now uses
> a single LangChain4J stack for both the RAG path and the Dokimos judge.

## Overview

This module demonstrates production-ready patterns for LLM applications:
- **Evaluation Framework**: Dokimos integration for comprehensive RAG system assessment
- **Distributed Tracing**: OpenTelemetry instrumentation for request tracking
- **Caching**: Redis integration for response caching and token optimization
- **Metrics**: Prometheus metrics for observability
- **Deployment**: OpenShift/Kubernetes deployment patterns

## Learning Objectives

By the end of this module, you will:
1. Integrate Dokimos evaluation framework for LLM application assessment
2. Create custom evaluators extending Dokimos BaseEvaluator
3. Orchestrate experiments with datasets, tasks, and evaluators
4. Implement JUnit-based evaluation tests for CI/CD pipelines
5. Export and analyze evaluation results
6. Instrument applications with distributed tracing
7. Implement caching strategies for LLM responses

## Prerequisites

- Completed Module 02 (Advanced RAG)
- Understanding of RAG architecture
- Familiarity with JUnit testing
- OpenAI API key configured in environment

## Architecture

### Evaluation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Experiment Orchestration                  │
│                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Dataset    │───▶│     Task     │───▶│  Evaluators  │  │
│  │  (Examples)  │    │ (RAG Query)  │    │ (5 metrics)  │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                    │                    │          │
│         │                    │                    │          │
│         ▼                    ▼                    ▼          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            ExperimentResult (Aggregated)             │  │
│  │  - Pass Rate  - Averages  - Min/Max  - Per-Item     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Components

1. **DatasetLoader**: Loads evaluation datasets in Dokimos format
2. **RAGEvaluationTask**: Executes RAG queries and returns outputs for evaluation
3. **Evaluators** (5 types):
   - FaithfulnessEvaluator (LLM-as-judge)
   - HallucinationEvaluator (LLM-as-judge)
   - ContextualRelevanceEvaluator (LLM-as-judge)
   - ExactMatchEvaluator (rule-based)
   - ResponseLengthEvaluator (custom, rule-based)
4. **DokimosEvaluationService**: Orchestrates experiment execution
5. **EvaluationController**: REST API for running evaluations

## Configuration

### Dependencies (`pom.xml`)

```xml
<!-- Dokimos Evaluation Framework -->
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-core</artifactId>
    <version>0.14.2</version>
</dependency>
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-spring-ai</artifactId>
    <version>0.14.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-junit</artifactId>
    <version>0.14.2</version>
    <scope>test</scope>
</dependency>
```

### Application Configuration (`application.yml`)

```yaml
# Dokimos Evaluation Configuration
dokimos:
  judge:
    model: ${DOKIMOS_JUDGE_MODEL:gpt-4o}
    temperature: 0.0
  evaluators:
    faithfulness:
      threshold: 0.7
    hallucination:
      threshold: 0.8
    contextual-relevance:
      threshold: 0.7
    exact-match:
      threshold: 1.0
    response-length:
      min-chars: 50
      max-chars: 500
      threshold: 1.0

# Dataset Configuration
evaluation:
  dataset:
    path: ${EVAL_DATASET_PATH:src/main/resources/data/eval-golden-set.json}

# Spring AI Configuration (for Judge LLM)
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.0
```

## Lab Exercises

### Lab 6.1: Load and Run Evaluation Dataset

**Objective**: Load a Dokimos dataset and execute a complete evaluation experiment.

See: [Lab 6.1 - Load and Run Dataset](./labs/LAB-6.1-LOAD-DATASET.md)

### Lab 6.2: Create Custom Evaluator

**Objective**: Build a custom evaluator extending Dokimos BaseEvaluator.

See: [Lab 6.2 - Create Custom Evaluator](./labs/LAB-6.2-CUSTOM-EVALUATOR.md)

### Lab 6.3: Integrate Evaluation into JUnit Tests

**Objective**: Add evaluation assertions to JUnit tests for CI/CD integration.

See: [Lab 6.3 - JUnit Integration](./labs/LAB-6.3-JUNIT-INTEGRATION.md)

## API Reference

### REST Endpoints

#### Run Evaluation Experiment

```http
POST /api/v1/eval/run
Content-Type: application/json

{
  "datasetName": "eval-golden-set",
  "evaluators": ["faithfulness", "hallucination"]  // optional
}
```

**Response:**
```json
{
  "status": "success",
  "timestamp": "2026-05-04T15:00:00Z",
  "result": {
    "name": "RAG System Evaluation",
    "totalCount": 12,
    "passCount": 10,
    "failCount": 2,
    "passRate": 0.833,
    "averageScores": {
      "faithfulness": 0.85,
      "hallucination": 0.92
    },
    "scoreRanges": {
      "faithfulness": {"min": 0.6, "max": 1.0},
      "hallucination": {"min": 0.8, "max": 1.0}
    },
    "itemResults": [...]
  }
}
```

**Error Codes:**
- `400 Bad Request`: Invalid request (missing dataset name, invalid evaluator names)
- `404 Not Found`: Dataset file not found
- `500 Internal Server Error`: Evaluation execution failed

## Dataset Format

Evaluation datasets use Dokimos `Dataset` format:

```json
{
  "name": "RAG Evaluation Golden Set",
  "description": "Comprehensive evaluation dataset for RAG system",
  "examples": [
    {
      "inputs": {
        "input": "What security features does the product offer?"
      },
      "expectedOutputs": {
        "output": "The product offers enterprise-grade security features..."
      },
      "metadata": {
        "id": "tc001",
        "category": "product_features",
        "difficulty": "easy"
      }
    }
  ]
}
```

**Field Descriptions:**
- `name`: Dataset identifier
- `description`: Purpose of the dataset
- `examples`: Array of test cases
  - `inputs.input`: The query/question
  - `expectedOutputs.output`: Expected answer (for accuracy evaluators)
  - `metadata`: Tags for filtering and analysis

## Evaluator Reference

### Built-in Evaluators

#### FaithfulnessEvaluator
- **Type**: LLM-as-judge
- **Purpose**: Verify response claims are supported by source documents
- **Threshold**: 0.7 (default)
- **Config Key**: `dokimos.evaluators.faithfulness.threshold`

#### HallucinationEvaluator
- **Type**: LLM-as-judge
- **Purpose**: Detect unsupported or false claims
- **Threshold**: 0.8 (default)
- **Config Key**: `dokimos.evaluators.hallucination.threshold`

#### ContextualRelevanceEvaluator
- **Type**: LLM-as-judge
- **Purpose**: Assess relevance of retrieved context to query
- **Threshold**: 0.7 (default)
- **Config Key**: `dokimos.evaluators.contextual-relevance.threshold`

#### ExactMatchEvaluator
- **Type**: Rule-based
- **Purpose**: String matching for factual accuracy
- **Threshold**: 1.0 (exact match required)
- **Config Key**: `dokimos.evaluators.exact-match.threshold`

### Custom Evaluators

#### ResponseLengthEvaluator
- **Type**: Rule-based (custom)
- **Purpose**: Validate response length within acceptable bounds
- **Configuration**:
  - `min-chars`: 50 (default)
  - `max-chars`: 500 (default)
  - `threshold`: 1.0

## Best Practices

### 1. Dataset Design
- Include diverse examples covering edge cases
- Tag examples with metadata (category, difficulty, source)
- Maintain separate datasets for development and production evaluation
- Version control datasets alongside code

### 2. Evaluator Selection
- Use LLM-as-judge (faithfulness, hallucination) for semantic quality
- Use rule-based evaluators (exact match, length) for quantifiable metrics
- Combine multiple evaluators for comprehensive assessment
- Set thresholds based on use case criticality

### 3. Experiment Orchestration
- Run evaluations in CI/CD pipelines
- Track results over time to detect regressions
- Filter evaluators for quick feedback loops
- Export results for stakeholder reporting

### 4. Performance Optimization
- Cache LLM-as-judge responses to reduce costs
- Run expensive evaluators (faithfulness) selectively
- Use rule-based evaluators for rapid iteration
- Parallelize evaluation when possible

## Troubleshooting

See: [Troubleshooting Guide](./TROUBLESHOOTING.md)

## Comparison: Dokimos vs Custom Implementation

See: [Dokimos vs Custom Evaluation](./DOKIMOS-COMPARISON.md)

## Additional Resources

- [Dokimos Documentation](https://dokimos.dev)
- [Dokimos GitHub](https://github.com/dokimos-dev/dokimos)
- [Spring AI Evaluation Guide](https://docs.spring.io/spring-ai/reference/api/evaluating.html)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)

## Next Steps

After completing this module:
1. Deploy to OpenShift/Kubernetes
2. Configure production monitoring with Prometheus/Grafana
3. Implement circuit breakers and rate limiting
4. Set up automated evaluation in CI/CD
5. Create custom evaluators for domain-specific metrics
