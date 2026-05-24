# Dokimos vs Custom Evaluation Implementation

## Overview

This document compares the Dokimos framework integration with the previous custom evaluation implementation, highlighting the benefits, trade-offs, and migration considerations.

## Architecture Comparison

### Custom Implementation (Before)

```
┌─────────────────────────────────────────────────────┐
│              EvaluationService                       │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ loadEvalSet()                                │   │
│  │  - Manual JSON parsing                       │   │
│  │  - Custom data structures                    │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ calculateAccuracy()                          │   │
│  │  - Embedding cosine similarity               │   │
│  │  - Manual normalization                      │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ calculateRelevance()                         │   │
│  │  - Custom similarity scoring                 │   │
│  │  - Ad-hoc aggregation                        │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ calculateFaithfulness()                      │   │
│  │  - Hand-written LLM prompts                  │   │
│  │  - Manual score parsing                      │   │
│  │  - Error-prone extraction                    │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │ runEvaluation()                              │   │
│  │  - Sequential execution                      │   │
│  │  - Manual aggregation                        │   │
│  │  - No experiment tracking                    │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Dokimos Implementation (After)

```
┌─────────────────────────────────────────────────────────┐
│               DokimosEvaluationService                   │
│                                                           │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────┐ │
│  │ DatasetLoader  │──│ RAGEvaluation  │──│ Evaluators │ │
│  │ (Framework)    │  │ Task (Custom)  │  │ (5 types)  │ │
│  └────────────────┘  └────────────────┘  └───────────┘ │
│          │                   │                   │       │
│          ▼                   ▼                   ▼       │
│  ┌──────────────────────────────────────────────────┐  │
│  │          Experiment Orchestration                 │  │
│  │  - Built-in dataset management                    │  │
│  │  - Automatic result aggregation                   │  │
│  │  - Pass/fail threshold tracking                   │  │
│  │  - Min/max score extraction                       │  │
│  │  - Export formats (JSON, HTML, MD, CSV)          │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Code Comparison

### Dataset Loading

**Custom (Before):**
```java
private List<EvalCase> loadEvalSet() throws IOException {
    File file = new File(datasetPath);
    JsonNode root = objectMapper.readTree(file);
    JsonNode testCases = root.get("test_cases");

    List<EvalCase> cases = new ArrayList<>();
    for (JsonNode testCase : testCases) {
        cases.add(new EvalCase(
            testCase.get("id").asText(),
            testCase.get("query").asText(),
            testCase.get("expected_answer").asText(),
            testCase.get("category").asText(),
            testCase.get("difficulty").asText()
        ));
    }
    return cases;
}
```
**Lines of Code:** ~20 lines + error handling

**Dokimos (After):**
```java
@Service
public class DatasetLoader {
    public Dataset loadDataset() throws DatasetLoadException {
        Path path = Paths.get(datasetPath);
        return Dataset.fromJson(path);  // Framework handles parsing
    }
}
```
**Lines of Code:** ~3 lines (framework handles validation)

**Benefits:**
- Built-in JSON parsing and validation
- Automatic error handling for malformed data
- No manual field extraction
- Type-safe Example objects

---

### Accuracy Evaluation

**Custom (Before):**
```java
double calculateAccuracy(String actual, String expected) {
    Embedding actualEmbedding = embeddingModel.embed(actual).content();
    Embedding expectedEmbedding = embeddingModel.embed(expected).content();
    return cosineSimilarity(actualEmbedding, expectedEmbedding);
}

private double cosineSimilarity(Embedding a, Embedding b) {
    float[] vectorA = a.vector();
    float[] vectorB = b.vector();

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```
**Lines of Code:** ~25 lines + edge case handling

**Dokimos (After):**
```java
@Bean
public ExactMatchEvaluator exactMatchEvaluator() {
    return ExactMatchEvaluator.builder()
            .threshold(exactMatchThreshold)
            .build();
}
```
**Lines of Code:** ~4 lines

**Benefits:**
- Production-tested implementation
- Built-in edge case handling
- Configurable thresholds
- Consistent scoring logic

---

### Faithfulness Evaluation

**Custom (Before):**
```java
double calculateFaithfulness(String response, List<String> sources) {
    if (sources == null || sources.isEmpty()) {
        return 0.0;
    }

    String sourcesText = String.join("\n\n", sources);
    String prompt = """
        Evaluate if the AI response is faithful to the source documents.

        Source Documents:
        %s

        AI Response:
        %s

        Score the faithfulness on a scale of 0.0 to 1.0, where:
        - 1.0 = completely faithful, all claims are supported by sources
        - 0.5 = partially faithful, some claims supported
        - 0.0 = not faithful, contains unsupported claims

        Respond with only a number between 0.0 and 1.0.
        """.formatted(sourcesText, response);

    String result = chatModel.chat(prompt);

    try {
        String cleaned = result.trim().replaceAll("[^0-9.]", "");
        return Double.parseDouble(cleaned);
    } catch (NumberFormatException e) {
        log.warn("Failed to parse faithfulness score: {}", result);
        return 0.5;
    }
}
```
**Lines of Code:** ~40 lines
**Issues:**
- Ad-hoc prompt design
- Fragile score parsing
- No standardization across evaluations
- Manual error handling

**Dokimos (After):**
```java
@Bean
public FaithfulnessEvaluator faithfulnessEvaluator(JudgeLM judgeLM) {
    return FaithfulnessEvaluator.builder()
            .judge(judgeLM)
            .threshold(faithfulnessThreshold)
            .contextKey("context")
            .build();
}
```
**Lines of Code:** ~5 lines

**Benefits:**
- Research-backed prompts
- Robust score extraction
- Standardized LLM-as-judge pattern
- Threshold configuration
- Automatic reason extraction

---

### Experiment Execution

**Custom (Before):**
```java
public EvaluationReport runEvaluation() throws IOException {
    List<EvalCase> testCases = loadEvalSet();
    List<EvalResult> results = new ArrayList<>();

    for (EvalCase testCase : testCases) {
        SimpleRAGService.RAGResponse response = ragService.query(testCase.query());

        double accuracy = calculateAccuracy(response.response(), testCase.expectedAnswer());
        double relevance = calculateRelevance(response.response(), response.sourceDocuments());
        double faithfulness = calculateFaithfulness(response.response(), response.sourceDocuments());

        EvalResult result = new EvalResult(
            testCase.id(), testCase.query(), testCase.expectedAnswer(),
            response.response(), accuracy, relevance, faithfulness
        );
        results.add(result);
    }

    MetricAverages averages = calculateAverages(results);
    return new EvaluationReport(results, averages);
}

private MetricAverages calculateAverages(List<EvalResult> results) {
    if (results.isEmpty()) {
        return new MetricAverages(0.0, 0.0, 0.0);
    }

    double totalAccuracy = 0.0;
    double totalRelevance = 0.0;
    double totalFaithfulness = 0.0;

    for (EvalResult result : results) {
        totalAccuracy += result.accuracy();
        totalRelevance += result.relevance();
        totalFaithfulness += result.faithfulness();
    }

    int count = results.size();
    return new MetricAverages(
        totalAccuracy / count,
        totalRelevance / count,
        totalFaithfulness / count
    );
}
```
**Lines of Code:** ~50+ lines
**Issues:**
- Manual iteration and aggregation
- No pass/fail tracking
- No min/max extraction
- Limited export options
- No experiment metadata

**Dokimos (After):**
```java
public ExperimentResult runExperiment(List<String> evaluatorFilter) {
    Dataset dataset = datasetLoader.loadDataset();
    List<Evaluator> evaluators = buildEvaluatorList(evaluatorFilter);

    Experiment experiment = Experiment.builder()
            .name("RAG System Evaluation")
            .description("Comprehensive evaluation of RAG system performance")
            .dataset(dataset)
            .task(ragEvaluationTask)
            .evaluators(evaluators)
            .metadata(buildMetadata(evaluatorFilter))
            .build();

    return experiment.run();  // Framework handles execution & aggregation
}
```
**Lines of Code:** ~12 lines

**Benefits:**
- Automatic aggregation (averages, min/max, pass rate)
- Built-in pass/fail threshold tracking
- Experiment metadata tracking
- Multiple export formats
- Per-example result details

---

## Feature Comparison

| Feature | Custom | Dokimos | Benefit |
|---------|--------|---------|---------|
| **Dataset Parsing** | Manual JSON parsing | Built-in `Dataset.fromJson()` | Reduced code, validation included |
| **Evaluators** | 3 custom metrics | 5 evaluators (3 built-in + 2 custom) | Production-tested, extensible |
| **LLM-as-Judge** | Ad-hoc prompts | Standardized `JudgeLM` pattern | Research-backed, robust |
| **Aggregation** | Manual averaging | Auto aggregation (avg, min/max, pass rate) | Comprehensive metrics |
| **Thresholds** | Hardcoded | Configurable via `application.yml` | Environment-specific tuning |
| **Experiment Tracking** | None | Built-in metadata & versioning | Reproducibility, audit trail |
| **Export Formats** | JSON only (manual) | JSON, HTML, Markdown, CSV | Multiple stakeholder formats |
| **JUnit Integration** | None | `@DatasetSource` annotations | CI/CD ready |
| **Extensibility** | Modify service class | Implement `BaseEvaluator` | Clean extension point |
| **Error Handling** | Manual try-catch | Framework-level handling | Consistent error reporting |

## Lines of Code Reduction

| Component | Custom (LOC) | Dokimos (LOC) | Reduction |
|-----------|--------------|---------------|-----------|
| Dataset Loading | ~20 | ~3 | **85%** |
| Accuracy Evaluation | ~25 | ~4 | **84%** |
| Faithfulness Evaluation | ~40 | ~5 | **87%** |
| Experiment Execution | ~50 | ~12 | **76%** |
| **Total** | **~135 LOC** | **~24 LOC** | **82% reduction** |

**Note:** This excludes configuration beans and Spring setup, focusing on core logic.

## Trade-offs

### What You Gain

1. **Reduced Complexity**
   - 82% less code to write and maintain
   - Framework handles edge cases
   - Production-tested evaluators

2. **Standardization**
   - Industry-standard evaluation patterns
   - Consistent LLM-as-judge prompts
   - Reproducible experiments

3. **Extensibility**
   - Clean extension points via `BaseEvaluator`
   - Easy to add domain-specific evaluators
   - Reusable across projects

4. **Observability**
   - Built-in experiment metadata
   - Pass/fail threshold tracking
   - Multiple export formats for stakeholders

5. **CI/CD Integration**
   - JUnit annotations (`@DatasetSource`)
   - Automatic XML report generation
   - Threshold assertions for regression testing

### What You Trade

1. **Dependency Addition**
   - Adds `dokimos-core`, `dokimos-spring-ai`, `dokimos-junit`
   - Introduces Spring AI (for judge LLM)
   - ~2MB additional JAR size

2. **Framework Learning Curve**
   - Need to understand Dokimos concepts (Dataset, Task, Evaluator, Experiment)
   - Documentation available but requires initial investment
   - Different mental model than custom implementation

3. **Spring AI Requirement**
   - Custom code used Langchain4J `ChatModel` directly
   - Dokimos requires Spring AI `ChatModel` for `JudgeLM`
   - Both can coexist (shown in this module)

4. **Less Granular Control**
   - LLM-as-judge prompts are built-in (but customizable)
   - Scoring logic is framework-defined
   - Can still extend for full customization

## When to Use Custom vs Dokimos

### Use Dokimos When:
- ✅ You need production-ready evaluation quickly
- ✅ You want industry-standard patterns
- ✅ You need multiple evaluator types
- ✅ You require CI/CD integration
- ✅ You want comprehensive result tracking
- ✅ You plan to extend with custom evaluators

### Use Custom Implementation When:
- ❌ You have highly specialized, non-standard metrics
- ❌ You need complete control over every aspect
- ❌ You cannot add new dependencies
- ❌ Framework overhead is unacceptable
- ❌ You have unique LLM-as-judge requirements

## Migration Path

If migrating from custom to Dokimos:

### Step 1: Add Dependencies
```xml
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-core</artifactId>
    <version>0.14.2</version>
</dependency>
```

### Step 2: Convert Dataset Format
```bash
# Old format (custom)
{
  "test_cases": [
    {"query": "...", "expected_answer": "..."}
  ]
}

# New format (Dokimos)
{
  "examples": [
    {"inputs": {"input": "..."}, "expectedOutputs": {"output": "..."}}
  ]
}
```

### Step 3: Create Task Implementation
```java
public class RAGEvaluationTask implements Task {
    public Map<String, Object> run(Example example) {
        // Execute RAG query, return outputs
    }
}
```

### Step 4: Configure Evaluators
```java
@Bean
public FaithfulnessEvaluator faithfulnessEvaluator(JudgeLM judgeLM) {
    return FaithfulnessEvaluator.builder().judge(judgeLM).build();
}
```

### Step 5: Replace Service Logic
```java
// Old: evaluationService.runEvaluation()
// New: dokimosEvaluationService.runExperiment()
```

### Step 6: Update Tests
```java
// Add JUnit assertions
@Test
void testPassRate() {
    ExperimentResult result = service.runExperiment();
    assertTrue(result.passRate() >= 0.7);
}
```

## Conclusion

**Dokimos provides an 82% reduction in code** while adding production-ready features like experiment tracking, multiple evaluators, and CI/CD integration. The trade-off is an additional dependency and a learning curve, but for most LLM application evaluations, Dokimos accelerates development and improves maintainability.

**Recommendation:** Use Dokimos for production LLM applications. Reserve custom implementations for highly specialized scenarios where framework overhead is prohibitive.

## Resources

- [Dokimos Documentation](https://dokimos.dev)
- [Dokimos GitHub](https://github.com/dokimos-dev/dokimos)
- [Dokimos Examples](https://dokimos.dev/examples)
- [Spring AI Evaluation Guide](https://docs.spring.io/spring-ai/reference/api/evaluating.html)
