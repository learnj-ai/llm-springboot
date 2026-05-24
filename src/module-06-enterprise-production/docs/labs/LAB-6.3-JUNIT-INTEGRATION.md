# Lab 6.3: Integrate Evaluation into JUnit Tests

## Objective

Learn how to integrate Dokimos evaluations into JUnit tests for CI/CD pipeline integration and automated regression testing.

## Duration

40 minutes

## Prerequisites

- Completed Lab 6.1 (Load and Run Dataset)
- Completed Lab 6.2 (Create Custom Evaluator)
- Understanding of JUnit 5 testing
- Familiarity with CI/CD concepts

## Overview

In this lab, you will:
1. Create JUnit tests that run evaluations
2. Add assertions for minimum score thresholds
3. Use `@DatasetSource` for parameterized testing
4. Configure Maven Surefire for XML reports
5. Integrate evaluation into CI/CD pipelines

## Part 1: Basic Evaluation Test

### Create Test Class

```java
package com.techcorp.assistant.module06.evaluation;

import com.techcorp.assistant.module06.dokimos.DokimosEvaluationService;
import dev.dokimos.core.ExperimentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("RAG System Evaluation Tests")
class RAGEvaluationTest {

    @Autowired
    private DokimosEvaluationService evaluationService;

    @Test
    @DisplayName("RAG system should meet minimum pass rate of 70%")
    void testMinimumPassRate() throws Exception {
        // Run full evaluation
        ExperimentResult result = evaluationService.runExperiment();

        // Assert minimum pass rate
        assertTrue(result.passRate() >= 0.7,
                String.format("Pass rate %.2f%% is below 70%% threshold",
                        result.passRate() * 100));

        // Log summary for debugging
        System.out.println(evaluationService.formatForConsole(result));
    }
}
```

### Run the Test

```bash
mvn test -Dtest=RAGEvaluationTest
```

**Expected Output:**
```
[INFO] Running RAGEvaluationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## Part 2: Threshold Assertions for Specific Evaluators

### Test Individual Evaluator Scores

```java
@Test
@DisplayName("Faithfulness score should be >= 0.7")
void testFaithfulnessThreshold() throws Exception {
    ExperimentResult result = evaluationService.runExperiment(
        List.of("faithfulness")
    );

    double avgScore = result.averageScore("faithfulness");

    assertTrue(avgScore >= 0.7,
            String.format("Faithfulness score %.3f is below 0.7 threshold", avgScore));
}

@Test
@DisplayName("Hallucination score should be >= 0.8")
void testHallucinationThreshold() throws Exception {
    ExperimentResult result = evaluationService.runExperiment(
        List.of("hallucination")
    );

    double avgScore = result.averageScore("hallucination");

    assertTrue(avgScore >= 0.8,
            String.format("Hallucination score %.3f is below 0.8 threshold", avgScore));
}

@Test
@DisplayName("All evaluators should produce valid scores")
void testAllEvaluatorsValid() throws Exception {
    ExperimentResult result = evaluationService.runExperiment();

    // Verify each evaluator has valid scores (0.0 to 1.0)
    for (String evaluatorName : result.evaluatorNames()) {
        double avgScore = result.averageScore(evaluatorName);

        assertTrue(avgScore >= 0.0 && avgScore <= 1.0,
                String.format("Score for %s (%.3f) is outside [0.0, 1.0] range",
                        evaluatorName, avgScore));
    }
}
```

## Part 3: Parameterized Testing with @DatasetSource

### Test Each Example Individually

```java
import dev.dokimos.core.Example;
import dev.dokimos.junit.DatasetSource;
import org.junit.jupiter.params.ParameterizedTest;

@ParameterizedTest
@DatasetSource(json = "classpath:data/eval-golden-set.json")
@DisplayName("Each example should be processable without errors")
void testEachExample(Example example) {
    // Verify example structure
    assertNotNull(example.input(), "Example input should not be null");
    assertFalse(example.input().isBlank(), "Example input should not be blank");

    // Additional validation
    assertNotNull(example.expectedOutput(), "Expected output should not be null");
    assertNotNull(example.metadata(), "Metadata should not be null");
}
```

**How it works:**
- `@DatasetSource` loads the dataset
- JUnit runs test once for each example
- Failures show which specific example failed

### Advanced Parameterized Test

```java
@ParameterizedTest
@DatasetSource(json = "classpath:data/eval-golden-set.json")
@DisplayName("Each example should receive non-empty RAG response")
void testEachExampleReceivesResponse(Example example) {
    // Execute RAG query
    RAGEvaluationTask task = applicationContext.getBean(RAGEvaluationTask.class);
    Map<String, Object> outputs = task.run(example);

    // Verify output
    assertNotNull(outputs.get("output"), "Output should not be null");
    assertFalse(((String) outputs.get("output")).isBlank(),
            "Output should not be empty for query: " + example.input());
}
```

## Part 4: Configure Maven Surefire for CI/CD

### Update pom.xml

```xml
<build>
    <plugins>
        <!-- Maven Surefire Plugin for JUnit XML reports -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <!-- Generate XML reports for CI/CD -->
                <useFile>true</useFile>
                <printSummary>true</printSummary>
                <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>

                <!-- Parallel execution for faster tests -->
                <parallel>methods</parallel>
                <threadCount>4</threadCount>

                <!-- Fail build on evaluation failures -->
                <testFailureIgnore>false</testFailureIgnore>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Generate XML Reports

```bash
mvn clean test
```

Reports are generated in: `target/surefire-reports/`

**XML Report Format:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="RAGEvaluationTest" tests="3" failures="0" errors="0" skipped="0" time="15.234">
  <testcase name="testMinimumPassRate" classname="...RAGEvaluationTest" time="15.123"/>
  <testcase name="testFaithfulnessThreshold" classname="...RAGEvaluationTest" time="0.056"/>
  <testcase name="testHallucinationThreshold" classname="...RAGEvaluationTest" time="0.055"/>
</testsuite>
```

## Part 5: Conditional Test Execution

### Skip Tests Without API Key

```java
@SpringBootTest
class RAGEvaluationTest {

    @Autowired(required = false)
    private DokimosEvaluationService evaluationService;

    @Test
    void testMinimumPassRate() throws Exception {
        // Skip if service not available (no API key)
        if (evaluationService == null) {
            System.out.println("Skipping test - OPENAI_API_KEY not configured");
            return;
        }

        ExperimentResult result = evaluationService.runExperiment();
        assertTrue(result.passRate() >= 0.7);
    }
}
```

### Using JUnit Assumptions

```java
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Test
void testMinimumPassRate() throws Exception {
    // Skip test if API key not configured
    assumeTrue(System.getenv("OPENAI_API_KEY") != null,
            "OPENAI_API_KEY not configured - skipping evaluation test");

    ExperimentResult result = evaluationService.runExperiment();
    assertTrue(result.passRate() >= 0.7);
}
```

## Part 6: CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/evaluation.yml`:

```yaml
name: RAG Evaluation Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  evaluate:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 25
      uses: actions/setup-java@v3
      with:
        java-version: '25'
        distribution: 'temurin'

    - name: Run Evaluation Tests
      env:
        OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
      run: |
        cd src/module-06-enterprise-production
        mvn clean test -Dtest=RAGEvaluationTest

    - name: Upload Test Reports
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: src/module-06-enterprise-production/target/surefire-reports/

    - name: Publish Test Results
      if: always()
      uses: EnricoMi/publish-unit-test-result-action@v2
      with:
        files: src/module-06-enterprise-production/target/surefire-reports/*.xml
```

### GitLab CI Pipeline

Create `.gitlab-ci.yml`:

```yaml
stages:
  - test

evaluation-tests:
  stage: test
  image: maven:3.9-eclipse-temurin-25
  script:
    - cd src/module-06-enterprise-production
    - mvn clean test -Dtest=RAGEvaluationTest
  artifacts:
    when: always
    reports:
      junit: src/module-06-enterprise-production/target/surefire-reports/*.xml
  variables:
    OPENAI_API_KEY: $OPENAI_API_KEY
```

### Jenkins Pipeline

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any

    environment {
        OPENAI_API_KEY = credentials('openai-api-key')
    }

    stages {
        stage('Evaluation Tests') {
            steps {
                dir('src/module-06-enterprise-production') {
                    sh 'mvn clean test -Dtest=RAGEvaluationTest'
                }
            }
        }
    }

    post {
        always {
            junit 'src/module-06-enterprise-production/target/surefire-reports/*.xml'
        }
    }
}
```

## Part 7: Regression Testing

### Track Scores Over Time

```java
@Test
@DisplayName("Faithfulness should not regress below baseline")
void testFaithfulnessNoRegression() throws Exception {
    double BASELINE_SCORE = 0.75;  // From previous release

    ExperimentResult result = evaluationService.runExperiment(
        List.of("faithfulness")
    );

    double currentScore = result.averageScore("faithfulness");

    assertTrue(currentScore >= BASELINE_SCORE,
            String.format("Faithfulness regressed from %.3f to %.3f",
                    BASELINE_SCORE, currentScore));
}
```

### Store Baseline in Properties

```properties
# baseline-scores.properties
faithfulness.baseline=0.75
hallucination.baseline=0.80
contextual-relevance.baseline=0.70
```

Load and compare:

```java
@TestPropertySource("classpath:baseline-scores.properties")
class RegressionTest {

    @Value("${faithfulness.baseline}")
    private double faithfulnessBaseline;

    @Test
    void testNoRegression() throws Exception {
        ExperimentResult result = evaluationService.runExperiment();
        double current = result.averageScore("faithfulness");

        assertTrue(current >= faithfulnessBaseline,
                "Faithfulness regressed below baseline");
    }
}
```

## Exercises

### Exercise 1: Create Smoke Test

Create a fast smoke test for CI that only runs rule-based evaluators:

```java
@Test
@DisplayName("Smoke test: Rule-based evaluators")
void smoketeTest() throws Exception {
    // Only run fast evaluators
    ExperimentResult result = evaluationService.runExperiment(
        List.of("exact-match", "response-length")
    );

    // Quick sanity check
    assertTrue(result.passRate() >= 0.5, "Smoke test failed");
}
```

### Exercise 2: Create Test Suite

Organize tests into suites for different purposes:

```java
@Tag("evaluation")
@Tag("integration")
class FullEvaluationTest {
    // Comprehensive tests with all evaluators
}

@Tag("evaluation")
@Tag("smoke")
class SmokeEvaluationTest {
    // Fast tests for pre-commit hooks
}
```

Run specific suite:
```bash
mvn test -Dgroups="smoke"
```

### Exercise 3: Add Custom Assertion Helpers

Create reusable assertion methods:

```java
class EvaluationAssertions {

    static void assertPassRateAbove(ExperimentResult result, double threshold) {
        assertTrue(result.passRate() >= threshold,
                String.format("Pass rate %.2f%% below %.0f%% threshold",
                        result.passRate() * 100, threshold * 100));
    }

    static void assertEvaluatorScoreAbove(ExperimentResult result,
                                          String evaluator, double threshold) {
        double score = result.averageScore(evaluator);
        assertTrue(score >= threshold,
                String.format("%s score %.3f below %.3f threshold",
                        evaluator, score, threshold));
    }
}
```

## Best Practices

1. **Fast Feedback**: Run quick evaluators in pre-commit hooks, comprehensive evaluation in CI
2. **Clear Messages**: Provide actionable failure messages with score details
3. **Baseline Tracking**: Store and compare against baseline scores
4. **Conditional Execution**: Skip tests gracefully when API keys unavailable
5. **Parallel Execution**: Enable parallel test execution for faster builds
6. **Report Artifacts**: Upload test reports for debugging failures
7. **Tag Organization**: Use tags to group tests by speed and purpose

## Common Issues

### Issue: Tests timeout in CI

**Solution**: Increase Maven Surefire timeout or filter to fast evaluators:
```xml
<configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
</configuration>
```

### Issue: API rate limits

**Solution**: Cache LLM responses or use mocks for frequent test runs:
```java
@MockBean
private JudgeLM mockJudge;

@Test
void testWithMock() {
    when(mockJudge.judge(anyString()))
        .thenReturn(new JudgeLMResponse(0.8, "Mocked response"));
    // Test logic
}
```

### Issue: Flaky test scores

**Solution**: Use score ranges instead of exact values:
```java
// Instead of assertEquals(0.75, score)
assertTrue(score >= 0.70 && score <= 0.80, "Score in expected range");
```

## Key Takeaways

1. **JUnit integration** enables automated evaluation in CI/CD pipelines
2. **Threshold assertions** catch regressions in evaluation metrics
3. **@DatasetSource** supports parameterized testing per example
4. **Maven Surefire** generates XML reports for CI/CD tools
5. **Conditional execution** handles missing API keys gracefully
6. **Test organization** (tags, suites) enables flexible test execution

## Next Steps

- Set up evaluation tests in your CI/CD pipeline
- Create baseline scores for regression tracking
- Explore Dokimos JUnit documentation: https://dokimos.dev/testing/junit
- Consider automated evaluation on every pull request
