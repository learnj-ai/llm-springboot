# Lab 6.1: Load and Run Evaluation Dataset

## Objective

Learn how to load a Dokimos dataset and execute a complete evaluation experiment against your RAG system.

## Duration

30 minutes

## Prerequisites

- Completed Module 02 (SimpleRAGService is functional)
- OpenAI API key configured: `export OPENAI_API_KEY=your-key`
- Module 06 dependencies installed

## Overview

In this lab, you will:
1. Understand Dokimos dataset format
2. Load a pre-configured golden dataset
3. Execute an evaluation experiment
4. Analyze experiment results

## Step 1: Understand the Dataset Format

Dokimos datasets use a structured JSON format:

```json
{
  "name": "RAG Evaluation Golden Set",
  "description": "Test cases for RAG system evaluation",
  "examples": [
    {
      "inputs": {
        "input": "What security features does the product offer?"
      },
      "expectedOutputs": {
        "output": "The product offers enterprise-grade security..."
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

**Key Fields:**
- `inputs.input`: The query to send to your RAG system
- `expectedOutputs.output`: Expected answer (used by accuracy evaluators)
- `metadata`: Tags for filtering and analysis

## Step 2: Review the Golden Dataset

Open the pre-configured dataset:

```bash
cat src/main/resources/data/eval-golden-set.json
```

**Exercise**: Review the dataset and answer:
- How many test cases are included?
- What categories are covered?
- Which examples are marked as "easy" vs "hard"?

## Step 3: Configure Dataset Path

Verify your `application.yml` contains:

```yaml
evaluation:
  dataset:
    path: src/main/resources/data/eval-golden-set.json
```

**Note**: The path is relative to the module's working directory.

## Step 4: Run Evaluation via REST API

Start the application:

```bash
mvn spring-boot:run
```

Run evaluation using curl:

```bash
curl -X POST http://localhost:8080/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{
    "datasetName": "eval-golden-set"
  }'
```

**Expected Output:**
```json
{
  "status": "success",
  "timestamp": "2026-05-04T...",
  "result": {
    "name": "RAG System Evaluation",
    "totalCount": 12,
    "passCount": 10,
    "failCount": 2,
    "passRate": 0.833,
    "averageScores": {
      "faithfulness": 0.85,
      "hallucination": 0.92,
      "contextual-relevance": 0.78,
      "exact-match": 0.67,
      "response-length": 1.0
    }
  }
}
```

## Step 5: Run Evaluation Programmatically

Create a test class to run evaluation from code:

```java
@SpringBootTest
class EvaluationExperimentTest {

    @Autowired
    private DokimosEvaluationService evaluationService;

    @Test
    void runFullEvaluation() throws Exception {
        // Run experiment with all evaluators
        ExperimentResult result = evaluationService.runExperiment();

        // Print summary
        System.out.println(evaluationService.formatForConsole(result));

        // Verify pass rate meets threshold
        assertTrue(result.passRate() >= 0.7,
                "Pass rate should be >= 70%");
    }
}
```

Run the test:

```bash
mvn test -Dtest=EvaluationExperimentTest
```

## Step 6: Filter Evaluators

Run evaluation with only specific evaluators:

```bash
curl -X POST http://localhost:8080/api/v1/eval/run \
  -H "Content-Type: application/json" \
  -d '{
    "datasetName": "eval-golden-set",
    "evaluators": ["faithfulness", "hallucination"]
  }'
```

**Use Case**: Quick feedback loops during development—run only fast evaluators.

Programmatic filtering:

```java
// Run only rule-based evaluators (fast)
ExperimentResult result = evaluationService.runExperiment(
    List.of("exact-match", "response-length")
);
```

## Step 7: Analyze Results

### View Aggregated Metrics

```java
ExperimentResult result = evaluationService.runExperiment();

// Extract average scores
Map<String, Double> averages = evaluationService.extractAverages(result);
for (Map.Entry<String, Double> entry : averages.entrySet()) {
    System.out.printf("%s: %.3f%n", entry.getKey(), entry.getValue());
}

// Extract min/max ranges
Map<String, double[]> minMax = evaluationService.extractMinMax(result);
for (Map.Entry<String, double[]> entry : minMax.entrySet()) {
    double[] range = entry.getValue();
    System.out.printf("%s: [%.3f, %.3f]%n", entry.getKey(), range[0], range[1]);
}
```

### View Per-Example Results

```java
// Iterate through individual test case results
for (ItemResult item : result.itemResults()) {
    System.out.println("Query: " + item.example().input());
    System.out.println("Success: " + item.success());

    for (EvalResult eval : item.evalResults()) {
        System.out.printf("  %s: %.3f (%s)%n",
                eval.name(),
                eval.score(),
                eval.success() ? "PASS" : "FAIL");
    }
}
```

## Step 8: Export Results

### Export to JSON

```java
// Export to JSON string
String json = evaluationService.exportToJson(result);
System.out.println(json);

// Export to JSON file
evaluationService.exportToJsonFile(result, "results/experiment-001.json");
```

### Export Using Dokimos Built-in Formats

```java
// Dokimos supports multiple export formats
result.exportJson(Path.of("results/eval.json"));
result.exportHtml(Path.of("results/eval.html"));
result.exportMarkdown(Path.of("results/eval.md"));
result.exportCsv(Path.of("results/eval.csv"));
```

## Exercises

### Exercise 1: Create a Custom Dataset

Create a new dataset `data/custom-eval.json` with 5 test cases for your domain:

```json
{
  "name": "Custom Evaluation Set",
  "description": "Domain-specific test cases",
  "examples": [
    {
      "inputs": {"input": "Your domain-specific query"},
      "expectedOutputs": {"output": "Expected answer"},
      "metadata": {"id": "custom-001", "category": "custom"}
    }
  ]
}
```

Update configuration and run evaluation.

### Exercise 2: Analyze Failure Cases

Run full evaluation and identify failed examples:

```java
for (ItemResult item : result.itemResults()) {
    if (!item.success()) {
        System.out.println("FAILED: " + item.example().input());
        // Print which evaluators failed
        for (EvalResult eval : item.evalResults()) {
            if (!eval.success()) {
                System.out.println("  " + eval.name() + ": " + eval.reason());
            }
        }
    }
}
```

**Task**: Fix RAG system issues causing failures and re-run.

### Exercise 3: Comparative Analysis

Run evaluation before and after a RAG system change:

```java
// Before change
ExperimentResult before = evaluationService.runExperiment();
double beforePassRate = before.passRate();

// Make a change to RAG system (e.g., adjust chunk size)

// After change
ExperimentResult after = evaluationService.runExperiment();
double afterPassRate = after.passRate();

System.out.printf("Pass rate change: %.1f%% → %.1f%%%n",
        beforePassRate * 100,
        afterPassRate * 100);
```

## Key Takeaways

1. **Datasets** are versioned collections of test cases in structured format
2. **Experiments** run datasets through tasks and evaluators systematically
3. **Results** provide aggregated metrics and per-example details
4. **Filtering** enables fast iteration during development
5. **Export** supports multiple formats for reporting and analysis

## Common Issues

### Issue: Dataset file not found

**Error**: `DatasetLoadException: Dataset file not found`

**Solution**: Verify path is relative to working directory:
```yaml
evaluation:
  dataset:
    path: src/main/resources/data/eval-golden-set.json
```

### Issue: OpenAI API key not configured

**Error**: `401 Unauthorized` from OpenAI

**Solution**: Set environment variable:
```bash
export OPENAI_API_KEY=sk-...
```

### Issue: Evaluation times out

**Error**: Experiment takes too long

**Solution**: Filter to fast evaluators during development:
```java
// Only run rule-based evaluators
evaluationService.runExperiment(List.of("exact-match", "response-length"));
```

## Next Steps

- **Lab 6.2**: Create a custom evaluator
- **Lab 6.3**: Integrate evaluation into JUnit tests
- Explore Dokimos documentation: https://dokimos.dev
