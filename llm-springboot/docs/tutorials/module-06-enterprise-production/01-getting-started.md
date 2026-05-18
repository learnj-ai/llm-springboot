# Getting Started

This guide will walk you through setting up and running the **Enterprise Production Best Practices** module on your local machine. You'll configure the environment, run the RAG system, execute evaluations, and verify that all production features are working correctly.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 25 or higher** - Check with `java -version`
- **Maven 3.9+** - Check with `mvn -version`
- **Git** - For cloning the repository
- **Docker** (optional) - For running Redis locally
- **OpenAI API key** - Required for LLM calls and evaluation
- **curl or Postman** - For testing REST API endpoints
- **Dokimos** — bundled in this repo at `libs/dev/dokimos/` and wired in via a `file:`-backed Maven repo in the parent pom. No separate clone or install step needed; `mvn install` from the workshop root resolves it transparently. See [02-dokimos-evaluation.md](02-dokimos-evaluation.md) for details.

## Clone and Setup

1. **Clone the repository** (or navigate to your existing copy):

```bash
git clone <repository-url>
cd llm-springboot-workshop/src/module-06-enterprise-production
```

2. **Verify project structure**:

```bash
ls -la
# You should see: pom.xml, src/, data/, k8s/, Dockerfile
```

## Environment Configuration

This module requires configuration for API keys and optional services.

### Required: OpenAI API Key

The system uses OpenAI for:
- RAG generation (GPT-4 by default)
- Dokimos judge evaluators (LLM-as-judge pattern)

**Set your API key**:

```bash
export OPENAI_API_KEY=sk-your-key-here
```

To persist this, add it to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
echo 'export OPENAI_API_KEY=sk-your-key-here' >> ~/.bashrc
source ~/.bashrc
```

### Optional: Redis for Caching

The caching features require Redis. If Redis isn't available, the application will still run but caching will be disabled.

**Start Redis with Docker**:

```bash
docker run -d \
  --name redis-module06 \
  -p 6379:6379 \
  redis:latest
```

**Verify Redis is running**:

```bash
docker ps | grep redis
# Should show redis container running on port 6379
```

If you don't want to use Redis, you can skip this step—the application gracefully handles Redis unavailability.

## Build System

This module uses **Maven** with comprehensive dependencies:

### Key Dependencies

**Dokimos Evaluation Framework** (vendored under `libs/`):
- `dokimos-core` - Core evaluation abstractions + built-in evaluators (faithfulness, hallucination, contextual-relevance, exact-match)
- `dokimos-junit` - JUnit integration for CI/CD

> The `dokimos-spring-ai` bridge from earlier drafts has been removed. Module 06 now wires Dokimos's `JudgeLM` directly to LangChain4J's `ChatModel` in `DokimosEvaluationConfig` (`dokimosJudgeChatModel::chat`), so the module has a **single** LLM client stack rather than running Spring AI and LangChain4J side-by-side. See chapter 02 for the wiring.

**LangChain4J 1.11.0** (RAG path **and** Dokimos judge):
- `langchain4j` - Core abstractions, `ChatModel` interface, message types
- `langchain4j-open-ai` - OpenAI integration for both the primary chat model and the judge model

**Observability**:
- `opentelemetry-api` & `opentelemetry-sdk` - Distributed tracing
- `micrometer-registry-prometheus` - Metrics collection
- `spring-boot-starter-actuator` - Health checks and monitoring

**Caching**:
- `spring-boot-starter-data-redis` - Redis integration
- `spring-cache-abstraction` - Caching annotations

### Building the Project

Build the project and download all dependencies:

```bash
mvn clean install
```

This command:
- Cleans previous builds
- Compiles source code (main and test)
- Downloads all dependencies from Maven Central
- Runs unit tests (evaluation tests are skipped without API key)
- Packages the application as a JAR file

**Expected output**:
```
BUILD SUCCESS
Total time: 45 s
```

The JAR file will be in `target/module-06-enterprise-production-1.0.0-SNAPSHOT.jar`

## Configuration Files

The application uses Spring Boot's YAML configuration in `src/main/resources/application.yml`.

### Key Configuration Sections

**LangChain4J + Judge-Model Configuration**:
```yaml
openai:
  api:
    key: ${OPENAI_API_KEY}
  model:
    name: ${OPENAI_MODEL_NAME:gpt-4o-mini}
    embedding: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
```

The single `openai.*` block drives both the primary chat model (`LLMConfig.chatModel()` for the RAG path) and the Dokimos judge model (`DokimosEvaluationConfig.dokimosJudgeChatModel()`, which then becomes the `JudgeLM` bean via a method-reference adapter).

**Dokimos Configuration** (for evaluation):
```yaml
dokimos:
  judge:
    model: ${DOKIMOS_JUDGE_MODEL:gpt-4o}  # judge uses the stronger model
    temperature: 0.0
    timeout-seconds: 60
  evaluators:
    faithfulness:
      enabled: true
      threshold: 0.7
    hallucination:
      enabled: true
      threshold: 0.8
    contextual-relevance:
      enabled: true
      threshold: 0.7
    response-length:
      enabled: true
      min-chars: 50
      max-chars: 1000
```

**Redis Configuration** (for caching):
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms

semantic-cache:
  enabled: true
  similarity-threshold: 0.95
  ttl-seconds: 3600
```

**Prometheus Configuration** (for metrics):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Customizing Models

You can override the defaults — for example, drop the chat model to a cheaper tier or upgrade the judge:

```bash
export OPENAI_MODEL_NAME=gpt-4o          # upgrade chat model (default: gpt-4o-mini)
export DOKIMOS_JUDGE_MODEL=gpt-4o-mini   # downgrade judge if cost matters more than rigor
```

## Sample Evaluation Dataset

The module includes a golden dataset at `data/eval-golden-set.json`:

```json
{
  "name": "RAG Evaluation Golden Set",
  "description": "Test cases for evaluating RAG system performance",
  "examples": [
    {
      "input": "What security features does the product offer?",
      "expected": "The product offers enterprise-grade security features...",
      "metadata": {
        "id": "tc001",
        "category": "product_features",
        "difficulty": "easy"
      }
    }
  ]
}
```

This dataset contains 10 test cases covering product features, support, and pricing questions.

## Running Locally

Follow these steps to run the application:

### Option A: Using Maven

```bash
mvn spring-boot:run
```

### Option B: Using the JAR

```bash
java -jar target/module-06-enterprise-production-1.0.0-SNAPSHOT.jar
```

### Watch the Startup Logs

You should see:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

Started Module06Application in 2.847 seconds
Server running on http://localhost:8086
```

The application runs on **http://localhost:8086** (not 8080—configured to avoid conflicts).

## Verification

Once the application is running, verify all features are working:

### 1. Check Application Health

Open your browser to: http://localhost:8086/actuator/health

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "redis": {"status": "UP"}  // or DOWN if Redis not running
  }
}
```

### 2. Check Prometheus Metrics

Visit: http://localhost:8086/actuator/prometheus

You should see metrics like:
```
# HELP rag_queries_total Total number of RAG queries processed
# TYPE rag_queries_total counter
rag_queries_total{type="rag"} 0.0
```

### 3. Test RAG Query

Use curl to query the RAG system:

```bash
curl -X POST http://localhost:8086/api/v1/production/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What security features does the product offer?"
  }'
```

**Expected Response**:
```json
{
  "response": "The product offers enterprise-grade security features including encryption at rest and in transit.",
  "sources": ["product-docs.md"],
  "metadata": {
    "cached": false,
    "tokensUsed": 145,
    "latencyMs": 832
  }
}
```

### 4. Run Evaluation

Execute a comprehensive evaluation:

```bash
curl -X POST http://localhost:8086/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{
    "datasetName": "eval-golden-set"
  }'
```

**Expected Response** (abbreviated):
```json
{
  "status": "success",
  "timestamp": "2026-05-08T10:00:00Z",
  "result": {
    "name": "RAG System Evaluation",
    "totalCount": 10,
    "passCount": 8,
    "failCount": 2,
    "passRate": 0.80,
    "averageScores": {
      "faithfulness": 0.85,
      "hallucination": 0.92,
      "contextual-relevance": 0.78,
      "exact-match": 0.60,
      "response-length": 1.00
    }
  }
}
```

This confirms the evaluation framework is working!

## Troubleshooting

### Issue: "OPENAI_API_KEY not found"

**Solution**: Ensure the environment variable is set:

```bash
echo $OPENAI_API_KEY
# Should print: sk-...
```

If empty, export it:
```bash
export OPENAI_API_KEY=sk-your-key-here
```

### Issue: "Port 8086 already in use"

**Solution**: Stop other applications using port 8086, or change the port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8087
```

### Issue: "Redis connection refused"

**Symptom**: Health check shows `"redis": {"status": "DOWN"}`

**Solution**: This is non-fatal—the app works without Redis (caching disabled). To enable caching, start Redis:

```bash
docker run -d -p 6379:6379 redis:latest
```

Then restart the application.

### Issue: "Evaluation tests fail with 401 Unauthorized"

**Solution**: Verify your OpenAI API key is valid and has sufficient credits:

```bash
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

Should return a list of available models.

### Issue: "Java version mismatch"

**Solution**: Verify Java 25+ is active:

```bash
java -version
# Should show: openjdk version "25.x.x" or higher
```

Update `JAVA_HOME` if needed:
```bash
export JAVA_HOME=/path/to/java25
```

## Practice Exercise

Verify your setup by running a custom evaluation:

### Task: Evaluate with Specific Evaluators

1. **Run evaluation with only fast evaluators** (skip LLM-based ones):

```bash
curl -X POST http://localhost:8086/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{
    "datasetName": "eval-golden-set",
    "evaluators": ["exact-match", "response-length"]
  }'
```

2. **Compare results** - The evaluation should complete much faster (no LLM judge calls).

3. **Check the metrics** - Visit http://localhost:8086/actuator/prometheus and look for:
   - `rag_queries_total` should have incremented by 10 (one per dataset example)
   - `rag_response_time_*` should show latency statistics

4. **Verify caching** - Run the same query twice:

```bash
# First request
curl -X POST http://localhost:8086/api/v1/production/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the pricing for the basic plan?"}'

# Second request (should be cached if Redis is running)
curl -X POST http://localhost:8086/api/v1/production/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the pricing for the basic plan?"}'
```

Compare the `latencyMs` and `cached` fields in the responses.

**Expected Outcome**:
- First request: `"cached": false`, latency ~800ms
- Second request: `"cached": true`, latency ~50ms

**Hints**:
- If caching doesn't work, check Redis connection in health endpoint
- The cache TTL is 1 hour (3600 seconds) by default
- Semantic caching means slight variations in wording may still hit the cache

---

## What's Next?

Now that your environment is configured and working, you're ready to dive into the evaluation framework. In the next chapter, you'll learn how Dokimos measures RAG system quality using multiple evaluators—from LLM-as-judge patterns to rule-based checks.

---

## Navigation

👈 **[Previous: Introduction](README.md)**

👉 **[Next: Dokimos Evaluation Framework: Measuring RAG Quality](02-dokimos-evaluation.md)**
