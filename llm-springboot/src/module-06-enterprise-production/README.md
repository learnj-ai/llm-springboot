# Module 06: Enterprise Best Practices and Production

> ⚠️ **This file is partially superseded.** The authoritative tutorial lives at
> [`docs/tutorials/module-06-enterprise-production/`](../../docs/tutorials/module-06-enterprise-production/).
>
> Specifically, the **Spring AI Configuration**, **Dependencies**, and
> **Troubleshooting** sections below describe an older wiring that used Spring AI
> 1.0.0-M5 alongside LangChain4J. That stack does not work on Spring Boot 4 /
> Spring Framework 7 (autoconfig references classes deleted in Spring Boot 4),
> so Module 06 was migrated to a single LangChain4J stack. Dokimos's `JudgeLM`
> is now adapted from `dev.langchain4j.model.chat.ChatModel` directly — see
> `src/main/java/com/techcorp/assistant/module06/dokimos/DokimosEvaluationConfig.java`.
> Sections that pre-date that change are **kept for the audit trail** but
> shouldn't be followed verbatim. The published tutorial in `docs/tutorials/` is
> up to date.

Production-ready patterns for LLM applications including evaluation, observability, and deployment.

## Overview

This module demonstrates enterprise patterns for deploying LLM applications to production:

- **Evaluation Framework (Dokimos)**: Comprehensive assessment of RAG system quality
- **Distributed Tracing (OpenTelemetry)**: Request flow tracking across services
- **Caching (Redis)**: Response caching for performance and cost optimization
- **Metrics (Prometheus)**: Application health and performance monitoring
- **Deployment (OpenShift/K8s)**: Container orchestration and scaling

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.9+
- OpenAI API key
- Redis (optional, for caching)

### Environment Setup

```bash
# Set OpenAI API key
export OPENAI_API_KEY=sk-your-key-here

# Optional: Start Redis
docker run -d -p 6379:6379 redis:latest
```

### Run the Application

```bash
cd src/module-06-enterprise-production
mvn spring-boot:run
```

### Run Evaluation

```bash
# Via REST API
curl -X POST http://localhost:8080/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{"datasetName": "eval-golden-set"}'

# Via JUnit tests
mvn test -Dtest=DokimosEvaluationTest
```

## Features

### 1. Dokimos Evaluation Framework

Production-grade evaluation framework for RAG systems with 5 evaluators:

**Built-in Evaluators:**
- **FaithfulnessEvaluator**: Verify responses are grounded in source documents (LLM-as-judge)
- **HallucinationEvaluator**: Detect unsupported or false claims (LLM-as-judge)
- **ContextualRelevanceEvaluator**: Assess relevance of retrieved context (LLM-as-judge)
- **ExactMatchEvaluator**: String matching for factual accuracy (rule-based)

**Custom Evaluators:**
- **ResponseLengthEvaluator**: Validate response length within bounds (rule-based)

**Key Capabilities:**
- Experiment orchestration with dataset management
- Automatic result aggregation (averages, min/max, pass rates)
- Multiple export formats (JSON, HTML, Markdown, CSV)
- JUnit integration for CI/CD pipelines
- Extensible via `BaseEvaluator`

**Documentation:**
- [Workshop Guide](./docs/MODULE-06-WORKSHOP.md)
- [Lab 6.1: Load and Run Dataset](./docs/labs/LAB-6.1-LOAD-DATASET.md)
- [Lab 6.2: Create Custom Evaluator](./docs/labs/LAB-6.2-CUSTOM-EVALUATOR.md)
- [Lab 6.3: JUnit Integration](./docs/labs/LAB-6.3-JUNIT-INTEGRATION.md)
- [Dokimos vs Custom Comparison](./docs/DOKIMOS-COMPARISON.md)
- [Troubleshooting](./docs/TROUBLESHOOTING.md)

### 2. Distributed Tracing

OpenTelemetry instrumentation for request flow tracking:

- Automatic span creation for RAG operations
- Custom `@Traced` annotation for method-level tracing
- Jaeger integration for visualization
- Span attributes for LLM token usage and latency

### 3. Caching

Redis-based caching for LLM responses:

- Response caching with configurable TTL
- Embedding cache for repeated queries
- Cache key generation based on query + parameters
- Significant cost savings on repeated queries

### 4. Metrics

Prometheus metrics for observability:

- Request count, latency, errors
- LLM token usage tracking
- Cache hit/miss rates
- Custom business metrics

### 5. Deployment

Container and Kubernetes patterns:

- Multi-stage Dockerfile for optimization
- OpenShift deployment manifests
- Health check endpoints
- Resource limits and scaling configuration

## Project Structure

```
module-06-enterprise-production/
├── src/
│   ├── main/
│   │   ├── java/com/techcorp/assistant/module06/
│   │   │   ├── dokimos/                    # Dokimos integration
│   │   │   │   ├── DokimosEvaluationConfig.java
│   │   │   │   ├── DokimosEvaluationService.java
│   │   │   │   ├── DatasetLoader.java
│   │   │   │   ├── RAGEvaluationTask.java
│   │   │   │   ├── EvaluatorConfig.java
│   │   │   │   ├── ResponseLengthEvaluator.java
│   │   │   │   └── TaskConfig.java
│   │   │   ├── controller/                 # REST endpoints
│   │   │   │   └── EvaluationController.java
│   │   │   ├── service/                    # Business logic
│   │   │   │   ├── SimpleRAGService.java
│   │   │   │   └── ExperimentResultDTO.java
│   │   │   ├── tracing/                    # OpenTelemetry
│   │   │   ├── cache/                      # Redis caching
│   │   │   └── metrics/                    # Prometheus
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data/
│   │           └── eval-golden-set.json    # Evaluation dataset
│   └── test/
│       └── java/com/techcorp/assistant/module06/
│           ├── dokimos/                    # Evaluation tests
│           │   ├── DokimosEvaluationTest.java
│           │   ├── DatasetLoaderTest.java
│           │   ├── RAGEvaluationTaskTest.java
│           │   ├── ResponseLengthEvaluatorTest.java
│           │   └── DokimosIntegrationTest.java
│           └── controller/
│               └── EvaluationControllerTest.java
├── docs/                                   # Documentation
│   ├── MODULE-06-WORKSHOP.md
│   ├── DOKIMOS-COMPARISON.md
│   ├── TROUBLESHOOTING.md
│   └── labs/
│       ├── LAB-6.1-LOAD-DATASET.md
│       ├── LAB-6.2-CUSTOM-EVALUATOR.md
│       └── LAB-6.3-JUNIT-INTEGRATION.md
└── pom.xml
```

## Configuration

### Evaluation Configuration

```yaml
# Dokimos judge model settings
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

# Dataset location
evaluation:
  dataset:
    path: src/main/resources/data/eval-golden-set.json
```

### Spring AI Configuration

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.0
```

## API Endpoints

### Evaluation API

**Run Evaluation:**
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
    "passRate": 0.833,
    "averageScores": {
      "faithfulness": 0.85,
      "hallucination": 0.92
    }
  }
}
```

## Testing

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=DatasetLoaderTest

# Run evaluation tests (requires OPENAI_API_KEY)
export OPENAI_API_KEY=sk-your-key
mvn test -Dtest=DokimosEvaluationTest
```

### Integration Tests

```bash
# Run integration tests
mvn verify

# Skip evaluation tests (no API key)
mvn test -Dgroups='!evaluation'
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run Evaluation Tests
  env:
    OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
  run: mvn test -Dtest=DokimosEvaluationTest
```

### Generate Reports

```bash
# Maven Surefire generates XML reports
mvn clean test

# Reports location
ls target/surefire-reports/
```

## Dependencies

### Key Dependencies

- **Dokimos**: `0.14.2` (Evaluation framework)
- **Spring AI**: `1.0.0-M5` (Judge LLM integration)
- **Langchain4J**: `1.11.0` (RAG service)
- **OpenTelemetry**: `1.45.0` (Distributed tracing)
- **Spring Boot**: `4.0.5`

### Dependency Management

```xml
<dependencies>
    <!-- Dokimos Evaluation -->
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
        <groupId>dev.dokimos</groupId>
        <artifactId>dokimos-junit</artifactId>
        <version>0.14.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Spring AI (for JudgeLM) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M5</version>
    </dependency>

    <!-- Langchain4J (for RAG) -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>1.11.0</version>
    </dependency>
</dependencies>
```

**Note:** Spring AI and Langchain4J coexist—Spring AI is used only for Dokimos judge LLM.

## Key Concepts

### Dataset Format

Dokimos uses structured JSON datasets:

```json
{
  "name": "Dataset Name",
  "description": "Purpose of dataset",
  "examples": [
    {
      "inputs": {"input": "Query text"},
      "expectedOutputs": {"output": "Expected answer"},
      "metadata": {"id": "001", "category": "test"}
    }
  ]
}
```

### Task Implementation

Tasks execute the system under test:

```java
public class RAGEvaluationTask implements Task {
    @Override
    public Map<String, Object> run(Example example) {
        // Execute RAG query
        RAGResponse response = ragService.query(example.input());

        // Return outputs for evaluators
        return Map.of(
            "output", response.response(),
            "context", String.join("\n", response.sources())
        );
    }
}
```

### Experiment Orchestration

Experiments combine datasets, tasks, and evaluators:

```java
Experiment experiment = Experiment.builder()
    .name("RAG System Evaluation")
    .dataset(dataset)
    .task(ragEvaluationTask)
    .evaluators(List.of(
        faithfulnessEvaluator,
        hallucinationEvaluator,
        contextualRelevanceEvaluator
    ))
    .build();

ExperimentResult result = experiment.run();
```

## Troubleshooting

Common issues and solutions:

- **Dataset not found**: Verify path in `application.yml`
- **API key errors**: Set `OPENAI_API_KEY` environment variable
- **Dependency conflicts**: See [Troubleshooting Guide](./docs/TROUBLESHOOTING.md)
- **Slow evaluation**: Filter to fast evaluators or reduce dataset size

Full troubleshooting guide: [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md)

## Resources

### Dokimos
- [Documentation](https://dokimos.dev)
- [GitHub](https://github.com/dokimos-dev/dokimos)
- [Examples](https://dokimos.dev/examples)
- [Tutorial: Spring AI Agent Evaluation](https://dokimos.dev/tutorials/spring-ai-agent-evaluation)

### Spring AI
- [Documentation](https://docs.spring.io/spring-ai/reference/)
- [Evaluation Guide](https://docs.spring.io/spring-ai/reference/api/evaluating.html)
- [GitHub](https://github.com/spring-projects/spring-ai)

### OpenTelemetry
- [Java Documentation](https://opentelemetry.io/docs/languages/java/)
- [Best Practices](https://opentelemetry.io/docs/concepts/instrumentation/manual/)

## License

This workshop module is part of the LLM Spring Boot Workshop.

## Support

For issues or questions:
- Module documentation: See `docs/` directory
- Dokimos issues: https://github.com/dokimos-dev/dokimos/issues
- Workshop repository: https://github.com/learnj-ai/learnj-workshops
