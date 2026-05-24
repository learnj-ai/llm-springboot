package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.*;
import dev.dokimos.junit.DatasetSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit integration tests for Dokimos evaluation framework.
 * Demonstrates how to run evaluations as JUnit tests with threshold assertions.
 *
 * <p>These tests run real LLM-judge evaluations against OpenAI's chat API
 * (faithfulness, hallucination, contextual relevance). They require both an
 * {@code OPENAI_API_KEY} and a project that has access to the configured
 * judge model. Gated on the env var so CI without an API key skips cleanly.
 */
@SpringBootTest
@DisplayName("Dokimos Evaluation Tests")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class DokimosEvaluationTest {

    @Autowired
    private DokimosEvaluationService evaluationService;

    @Autowired
    private DatasetLoader datasetLoader;

    /**
     * Test: Run complete experiment and assert minimum pass rate.
     */
    @Test
    @DisplayName("Complete experiment should meet minimum pass rate threshold")
    void testCompleteExperiment() throws DatasetLoader.DatasetLoadException {
        // Run full experiment
        ExperimentResult result = evaluationService.runExperiment();

        // Assert minimum pass rate (70%)
        assertNotNull(result, "Experiment result should not be null");
        assertTrue(result.passRate() >= 0.7,
                String.format("Pass rate %.2f%% should be >= 70%%", result.passRate() * 100));

        // Log summary
        System.out.println(evaluationService.formatForConsole(result));
    }

    /**
     * Test: Faithfulness evaluator should meet minimum threshold.
     */
    @Test
    @DisplayName("Faithfulness scores should meet minimum threshold")
    void testFaithfulnessThreshold() throws DatasetLoader.DatasetLoadException {
        // Run experiment with only faithfulness evaluator
        ExperimentResult result = evaluationService.runExperiment(List.of("faithfulness"));

        // Assert minimum average score (0.7)
        double avgScore = result.averageScore("faithfulness");
        assertTrue(avgScore >= 0.7,
                String.format("Average faithfulness score %.3f should be >= 0.7", avgScore));
    }

    /**
     * Test: Hallucination evaluator should meet minimum threshold.
     */
    @Test
    @DisplayName("Hallucination scores should meet minimum threshold")
    void testHallucinationThreshold() throws DatasetLoader.DatasetLoadException {
        // Run experiment with only hallucination evaluator
        ExperimentResult result = evaluationService.runExperiment(List.of("hallucination"));

        // Assert minimum average score (0.8)
        double avgScore = result.averageScore("hallucination");
        assertTrue(avgScore >= 0.8,
                String.format("Average hallucination score %.3f should be >= 0.8", avgScore));
    }

    /**
     * Test: Contextual relevance evaluator should meet minimum threshold.
     */
    @Test
    @DisplayName("Contextual relevance scores should meet minimum threshold")
    void testContextualRelevanceThreshold() throws DatasetLoader.DatasetLoadException {
        // Run experiment with only contextual-relevance evaluator
        ExperimentResult result = evaluationService.runExperiment(List.of("contextual-relevance"));

        // Assert minimum average score (0.7)
        double avgScore = result.averageScore("contextual-relevance");
        assertTrue(avgScore >= 0.7,
                String.format("Average contextual relevance score %.3f should be >= 0.7", avgScore));
    }

    /**
     * Test: Exact match evaluator should detect correct answers.
     */
    @Test
    @DisplayName("Exact match evaluator should identify correct answers")
    void testExactMatchEvaluator() throws DatasetLoader.DatasetLoadException {
        // Run experiment with only exact-match evaluator
        ExperimentResult result = evaluationService.runExperiment(List.of("exact-match"));

        // Verify results exist
        assertNotNull(result);
        assertTrue(result.totalCount() > 0, "Should have processed examples");
    }

    /**
     * Test: Response length evaluator should detect valid response lengths.
     */
    @Test
    @DisplayName("Response length evaluator should validate response sizes")
    void testResponseLengthEvaluator() throws DatasetLoader.DatasetLoadException {
        // Run experiment with only response-length evaluator
        ExperimentResult result = evaluationService.runExperiment(List.of("response-length"));

        // Verify results exist
        assertNotNull(result);
        assertTrue(result.totalCount() > 0, "Should have processed examples");
    }

    /**
     * Test: All evaluators should run successfully.
     */
    @Test
    @DisplayName("All evaluators should run without errors")
    void testAllEvaluators() throws DatasetLoader.DatasetLoadException {
        // Run experiment with all evaluators
        ExperimentResult result = evaluationService.runExperiment();

        // Verify all evaluators ran
        assertNotNull(result);
        assertEquals(5, result.evaluatorNames().size(),
                "Should have 5 evaluators: " + result.evaluatorNames());

        // Verify each evaluator has results
        for (String evaluatorName : result.evaluatorNames()) {
            double avgScore = result.averageScore(evaluatorName);
            assertTrue(avgScore >= 0.0 && avgScore <= 1.0,
                    String.format("Score for %s (%.3f) should be between 0.0 and 1.0", evaluatorName, avgScore));
        }
    }

    /**
     * Test: Result aggregation should extract correct averages.
     */
    @Test
    @DisplayName("Result aggregation should extract correct averages")
    void testResultAggregation() throws DatasetLoader.DatasetLoadException {
        ExperimentResult result = evaluationService.runExperiment();

        Map<String, Double> averages = evaluationService.extractAverages(result);

        // Verify averages extracted for all evaluators
        assertNotNull(averages);
        assertFalse(averages.isEmpty(), "Averages map should not be empty");

        // Verify all scores are valid
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            double score = entry.getValue();
            assertTrue(Double.isFinite(score), "Score should be finite: " + entry.getKey());
            assertTrue(score >= 0.0 && score <= 1.0,
                    String.format("Score for %s (%.3f) should be between 0.0 and 1.0", entry.getKey(), score));
        }
    }

    /**
     * Test: Min/max extraction should identify score ranges.
     */
    @Test
    @DisplayName("Min/max extraction should identify score ranges")
    void testMinMaxExtraction() throws DatasetLoader.DatasetLoadException {
        ExperimentResult result = evaluationService.runExperiment();

        Map<String, double[]> minMax = evaluationService.extractMinMax(result);

        // Verify min/max extracted for all evaluators
        assertNotNull(minMax);
        assertFalse(minMax.isEmpty(), "Min/max map should not be empty");

        // Verify all ranges are valid
        for (Map.Entry<String, double[]> entry : minMax.entrySet()) {
            double[] range = entry.getValue();
            assertNotNull(range);
            assertEquals(2, range.length, "Range should have min and max");
            assertTrue(range[0] <= range[1],
                    String.format("Min %.3f should be <= max %.3f for %s", range[0], range[1], entry.getKey()));
        }
    }

    /**
     * Parameterized test: Run evaluation for each example in dataset.
     */
    @ParameterizedTest
    @DatasetSource(json = "classpath:data/eval-golden-set.json")
    @DisplayName("Each example should be processable")
    void testEachExample(Example example) {
        // Verify example is valid
        assertNotNull(example, "Example should not be null");
        assertNotNull(example.input(), "Example input should not be null");
        assertFalse(example.input().isBlank(), "Example input should not be blank");
    }
}
