# Lab 6.2: Create Custom Evaluator

## Objective

Learn how to create custom evaluators by extending Dokimos `BaseEvaluator` to implement domain-specific evaluation metrics.

## Duration

45 minutes

## Prerequisites

- Completed Lab 6.1 (Load and Run Dataset)
- Understanding of Java inheritance and interfaces
- Familiarity with evaluation metrics concepts

## Overview

In this lab, you will:
1. Understand the `BaseEvaluator` API
2. Create a custom rule-based evaluator
3. Create a custom LLM-as-judge evaluator
4. Register evaluators as Spring beans
5. Test custom evaluators

## Part 1: Understanding BaseEvaluator

### BaseEvaluator API

```java
public abstract class BaseEvaluator implements Evaluator {
    protected final String name;
    protected final double threshold;
    protected final List<EvalTestCaseParam> requiredParams;

    protected BaseEvaluator(String name, double threshold,
                           List<EvalTestCaseParam> requiredParams) {
        this.name = name;
        this.threshold = threshold;
        this.requiredParams = requiredParams;
    }

    // Abstract method to implement
    protected abstract EvalResult runEvaluation(EvalTestCase testCase);

    // Implemented by framework
    @Override
    public EvalResult evaluate(EvalTestCase testCase) {
        // Validates required params, calls runEvaluation()
    }
}
```

### EvalTestCase Structure

```java
public class EvalTestCase {
    Map<String, Object> inputs;           // Example inputs
    Map<String, Object> actualOutputs;     // Task outputs
    Map<String, Object> expectedOutputs;   // Expected values
    Map<String, Object> metadata;          // Example metadata

    // Convenience accessors
    String input();          // inputs.get("input")
    String actualOutput();   // actualOutputs.get("output")
    String expectedOutput(); // expectedOutputs.get("output")
    String context();        // actualOutputs.get("context")
}
```

### EvalResult Structure

```java
public record EvalResult(
    String name,       // Evaluator name
    double score,      // 0.0 to 1.0
    Double threshold,  // Pass/fail threshold (optional)
    boolean success,   // score >= threshold
    String reason,     // Explanation of score
    Map<String, Object> metadata  // Additional data
) {}
```

## Part 2: Create a Rule-Based Custom Evaluator

### Example: Citation Count Evaluator

Create an evaluator that checks if the response includes proper citations:

```java
package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.BaseEvaluator;
import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import dev.dokimos.core.EvalTestCaseParam;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates whether response includes proper citations to sources.
 * Citations are detected using pattern: [1], [2], etc.
 */
public class CitationCountEvaluator extends BaseEvaluator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");
    private final int minCitations;

    public CitationCountEvaluator(int minCitations, double threshold) {
        super(
            "citation-count",
            threshold,
            List.of(EvalTestCaseParam.ACTUAL_OUTPUT)  // Requires "output" field
        );
        this.minCitations = minCitations;
    }

    @Override
    protected EvalResult runEvaluation(EvalTestCase testCase) {
        String response = testCase.actualOutput();

        if (response == null || response.isBlank()) {
            return EvalResult.of(
                name(),
                0.0,
                threshold(),
                "Response is missing or empty"
            );
        }

        // Count citations
        Matcher matcher = CITATION_PATTERN.matcher(response);
        int citationCount = 0;
        while (matcher.find()) {
            citationCount++;
        }

        // Calculate score
        double score = citationCount >= minCitations ? 1.0 : 0.0;

        // Build reason
        String reason = String.format(
            "Found %d citations (minimum: %d)",
            citationCount,
            minCitations
        );

        return EvalResult.of(name(), score, threshold(), reason);
    }
}
```

### Register as Spring Bean

```java
@Configuration
public class CustomEvaluatorConfig {

    @Bean
    public CitationCountEvaluator citationCountEvaluator(
            @Value("${dokimos.evaluators.citation-count.min-citations:2}") int minCitations,
            @Value("${dokimos.evaluators.citation-count.threshold:1.0}") double threshold) {
        return new CitationCountEvaluator(minCitations, threshold);
    }
}
```

### Add Configuration

```yaml
dokimos:
  evaluators:
    citation-count:
      min-citations: 2
      threshold: 1.0
```

### Update DokimosEvaluationService

Add the custom evaluator to the service:

```java
@Service
public class DokimosEvaluationService {

    private final CitationCountEvaluator citationCountEvaluator;

    public DokimosEvaluationService(
            // ... existing evaluators ...
            CitationCountEvaluator citationCountEvaluator) {
        // ... existing code ...
        this.citationCountEvaluator = citationCountEvaluator;
    }

    private List<Evaluator> buildEvaluatorList(List<String> filter) {
        List<Evaluator> evaluators = new ArrayList<>();

        // ... existing evaluators ...

        if (shouldInclude("citation-count", filter) && citationCountEvaluator != null) {
            evaluators.add(citationCountEvaluator);
        }

        return evaluators;
    }
}
```

## Part 3: Create an LLM-as-Judge Custom Evaluator

### Example: Tone Evaluator

Create an evaluator that assesses response tone (professional, friendly, technical):

```java
package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.BaseEvaluator;
import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import dev.dokimos.core.EvalTestCaseParam;
import dev.dokimos.core.JudgeLM;
import dev.dokimos.core.JudgeLMResponse;

import java.util.List;

/**
 * Evaluates response tone using LLM-as-judge pattern.
 */
public class ToneEvaluator extends BaseEvaluator {

    private final JudgeLM judgeLM;
    private final String targetTone;

    public ToneEvaluator(JudgeLM judgeLM, String targetTone, double threshold) {
        super(
            "tone-evaluation",
            threshold,
            List.of(EvalTestCaseParam.ACTUAL_OUTPUT)
        );
        this.judgeLM = judgeLM;
        this.targetTone = targetTone;
    }

    @Override
    protected EvalResult runEvaluation(EvalTestCase testCase) {
        String response = testCase.actualOutput();

        if (response == null || response.isBlank()) {
            return EvalResult.of(name(), 0.0, threshold(), "Response is empty");
        }

        // Build evaluation prompt
        String prompt = String.format("""
            Evaluate if the following response has a %s tone.

            Response:
            %s

            Rate the tone on a scale of 0.0 to 1.0:
            - 1.0: Perfectly matches %s tone
            - 0.5: Somewhat matches tone
            - 0.0: Does not match tone at all

            Respond with only a number between 0.0 and 1.0.
            """, targetTone, response, targetTone);

        // Call LLM judge
        JudgeLMResponse judgeResponse = judgeLM.judge(prompt);
        double score = judgeResponse.score();
        String reason = judgeResponse.reasoning();

        return EvalResult.of(name(), score, threshold(), reason);
    }
}
```

### Register as Spring Bean

```java
@Configuration
public class CustomEvaluatorConfig {

    @Bean
    public ToneEvaluator toneEvaluator(
            JudgeLM judgeLM,
            @Value("${dokimos.evaluators.tone.target:professional}") String targetTone,
            @Value("${dokimos.evaluators.tone.threshold:0.7}") double threshold) {
        return new ToneEvaluator(judgeLM, targetTone, threshold);
    }
}
```

### Add Configuration

```yaml
dokimos:
  evaluators:
    tone:
      target: professional
      threshold: 0.7
```

## Part 4: Test Custom Evaluators

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class CitationCountEvaluatorTest {

    private CitationCountEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CitationCountEvaluator(2, 1.0);
    }

    @Test
    void shouldPassWithSufficientCitations() {
        EvalTestCase testCase = EvalTestCase.builder()
            .actualOutputs(Map.of("output", "Answer based on sources [1] and [2]."))
            .build();

        EvalResult result = evaluator.runEvaluation(testCase);

        assertEquals(1.0, result.score());
        assertTrue(result.success());
    }

    @Test
    void shouldFailWithInsufficientCitations() {
        EvalTestCase testCase = EvalTestCase.builder()
            .actualOutputs(Map.of("output", "Answer with only one source [1]."))
            .build();

        EvalResult result = evaluator.runEvaluation(testCase);

        assertEquals(0.0, result.score());
        assertFalse(result.success());
    }
}
```

### Integration Test

```java
@SpringBootTest
class CustomEvaluatorIntegrationTest {

    @Autowired
    private DokimosEvaluationService evaluationService;

    @Test
    void shouldRunCustomEvaluators() throws Exception {
        // Run experiment with custom evaluators
        ExperimentResult result = evaluationService.runExperiment(
            List.of("citation-count", "tone-evaluation")
        );

        assertNotNull(result);
        assertTrue(result.totalCount() > 0);

        // Verify custom evaluators ran
        assertTrue(result.evaluatorNames().contains("citation-count"));
        assertTrue(result.evaluatorNames().contains("tone-evaluation"));
    }
}
```

## Exercises

### Exercise 1: Create Source Count Evaluator

Create an evaluator that checks if minimum number of sources were retrieved:

```java
public class SourceCountEvaluator extends BaseEvaluator {
    private final int minSources;

    // Check testCase.actualOutputs().get("source_count")
    // Return 1.0 if >= minSources, else 0.0
}
```

**Hint**: `RAGEvaluationTask` includes `source_count` in outputs.

### Exercise 2: Create Keyword Presence Evaluator

Create an evaluator that verifies response contains required keywords:

```java
public class KeywordPresenceEvaluator extends BaseEvaluator {
    private final List<String> requiredKeywords;

    // Check if all keywords appear in response (case-insensitive)
    // Score = (keywords found) / (total keywords)
}
```

### Exercise 3: Create Comparative Length Evaluator

Create an evaluator that compares response length to expected answer length:

```java
public class ComparativeLengthEvaluator extends BaseEvaluator {
    private final double toleranceRatio;  // e.g., 1.5 = allow 50% longer

    // Compare actualOutput.length() to expectedOutput.length()
    // Pass if within tolerance ratio
}
```

## Advanced Topics

### 1. Metadata in EvalResult

Include additional data for analysis:

```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("citation_count", citationCount);
metadata.put("citation_positions", citationPositions);

return EvalResult.builder()
    .name(name())
    .score(score)
    .threshold(threshold())
    .success(score >= threshold())
    .reason(reason)
    .metadata(metadata)
    .build();
```

### 2. Gradual Scoring

Instead of binary pass/fail, use gradual scoring:

```java
// Score based on how close to ideal
double ideal = 3.0;  // Ideal number of citations
double actual = citationCount;
double score = Math.min(1.0, actual / ideal);
```

### 3. Combining Multiple Criteria

Implement evaluators that check multiple aspects:

```java
@Override
protected EvalResult runEvaluation(EvalTestCase testCase) {
    double citationScore = evaluateCitations(testCase);
    double lengthScore = evaluateLength(testCase);
    double formatScore = evaluateFormat(testCase);

    // Weighted average
    double score = (citationScore * 0.4) +
                   (lengthScore * 0.3) +
                   (formatScore * 0.3);

    return EvalResult.of(name(), score, threshold(), buildReason(...));
}
```

## Best Practices

1. **Clear Naming**: Use descriptive evaluator names (e.g., "citation-count", not "eval1")
2. **Informative Reasons**: Provide actionable feedback in reason strings
3. **Appropriate Thresholds**: Set thresholds based on use case criticality
4. **Required Parameters**: Specify only necessary `EvalTestCaseParam` values
5. **Error Handling**: Return valid `EvalResult` even for edge cases
6. **Unit Tests**: Test evaluators independently before integration
7. **Documentation**: Document what the evaluator measures and why

## Common Pitfalls

### Pitfall 1: Forgetting to Register Bean

**Problem**: Evaluator class exists but doesn't run.

**Solution**: Add `@Bean` method in configuration class.

### Pitfall 2: Incorrect Required Parameters

**Problem**: Evaluator expects `CONTEXT` but task doesn't provide it.

**Solution**: Only require parameters guaranteed by your task:
```java
List.of(EvalTestCaseParam.ACTUAL_OUTPUT)  // Safe for RAGEvaluationTask
```

### Pitfall 3: LLM Judge Without Error Handling

**Problem**: LLM API failures crash evaluation.

**Solution**: Wrap LLM calls in try-catch:
```java
try {
    JudgeLMResponse response = judgeLM.judge(prompt);
    return EvalResult.of(...);
} catch (Exception e) {
    return EvalResult.of(name(), 0.0, threshold(),
        "LLM judge failed: " + e.getMessage());
}
```

## Key Takeaways

1. **Custom evaluators** extend `BaseEvaluator` and implement `runEvaluation()`
2. **Rule-based evaluators** use deterministic logic (patterns, counts, rules)
3. **LLM-as-judge evaluators** use `JudgeLM` for semantic assessment
4. **Registration** as Spring beans enables automatic discovery
5. **Testing** ensures evaluators work correctly before integration

## Next Steps

- **Lab 6.3**: Integrate evaluation into JUnit tests
- Explore Dokimos custom evaluator examples: https://dokimos.dev/evaluators/custom
- Consider domain-specific metrics for your use case
