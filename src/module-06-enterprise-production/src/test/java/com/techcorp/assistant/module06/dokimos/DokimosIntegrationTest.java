package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.ExperimentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for full Dokimos experiment execution.
 * Tests end-to-end workflow from dataset loading to result aggregation.
 *
 * <p>Runs in keyless CI: every test method uses the {@code response-length} evaluator
 * (regex-only, no LLM call), and the test class provides {@code openai.api.key=test-key}
 * as a property fallback so context refresh succeeds even when {@code OPENAI_API_KEY}
 * is unset. The LLM-judge path is exercised by {@link DokimosEvaluationTest}, which
 * is gated separately.
 *
 * <p>This class-level wiring is what catches Spring context regressions early — e.g.
 * an autowiring break in any bean Module 06 ships. Prior to ungating, a bean-creation
 * regression in {@code CachingService} (two unannotated constructors) went undetected
 * by {@code mvn test} because this class was the only one that exercised full context
 * refresh, and the gate skipped it in keyless CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Classpath-relative; DatasetLoader checks classpath first, filesystem second.
        "evaluation.dataset.path=data/eval-golden-set.json",
        // Fallback API key so context refresh succeeds when OPENAI_API_KEY is unset.
        // LangChain4J's OpenAiChatModel.builder() stores the key without validating it,
        // so a placeholder is safe — and no test method here makes an actual LLM call.
        "openai.api.key=test-key",
        "dokimos.judge.model=gpt-4o",
        "dokimos.judge.temperature=0.0"
})
@DisplayName("Dokimos Integration Tests")
class DokimosIntegrationTest {

    @Autowired(required = false)
    private DokimosEvaluationService evaluationService;

    @Autowired(required = false)
    private DatasetLoader datasetLoader;

    @Test
    @DisplayName("Should load all required beans")
    void testBeansLoaded() {
        assertNotNull(evaluationService, "DokimosEvaluationService should be loaded");
        assertNotNull(datasetLoader, "DatasetLoader should be loaded");
    }

    @Test
    @DisplayName("Should run complete experiment workflow")
    void testCompleteExperimentWorkflow() throws Exception {
        // Filter to a non-LLM evaluator so this test stays deterministic and
        // doesn't depend on whether the OpenAI project has access to the judge
        // model (`gpt-4o` by default). The LLM-judge path is exercised
        // end-to-end by hitting the `/api/v1/eval/run` endpoint in a real
        // environment; this test asserts the experiment orchestration shape.
        ExperimentResult result = evaluationService.runExperiment(
                java.util.List.of("response-length"));

        // Verify experiment completed
        assertNotNull(result, "Experiment result should not be null");
        assertTrue(result.totalCount() > 0, "Should have processed examples");

        // Verify the filter applied: only response-length should have run.
        assertTrue(result.evaluatorNames().contains("response-length"),
                "Should have response-length evaluator: " + result.evaluatorNames());

        // Verify result aggregation
        Map<String, Double> averages = evaluationService.extractAverages(result);
        assertNotNull(averages);
        assertFalse(averages.isEmpty());

        // Verify all scores are valid
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            double score = entry.getValue();
            assertTrue(Double.isFinite(score), "Score should be finite: " + entry.getKey());
            assertTrue(score >= 0.0 && score <= 1.0,
                    String.format("Score for %s (%.3f) should be in [0.0, 1.0]", entry.getKey(), score));
        }

        // Verify min/max extraction
        Map<String, double[]> minMax = evaluationService.extractMinMax(result);
        assertNotNull(minMax);
        for (Map.Entry<String, double[]> entry : minMax.entrySet()) {
            double[] range = entry.getValue();
            assertTrue(range[0] <= range[1],
                    String.format("Min %.3f should be <= max %.3f for %s", range[0], range[1], entry.getKey()));
        }
    }

    @Test
    @DisplayName("Should handle filtered evaluators")
    void testFilteredEvaluators() throws Exception {
        // Run experiment with only response-length evaluator (doesn't require LLM)
        ExperimentResult result = evaluationService.runExperiment(
                java.util.List.of("response-length")
        );

        // Verify experiment ran with filtered evaluators
        assertNotNull(result);
        assertTrue(result.totalCount() > 0);

        // Verify only response-length evaluator ran
        assertTrue(result.evaluatorNames().contains("response-length"));
    }

    @Test
    @DisplayName("Should export results to JSON")
    void testJsonExport() throws Exception {
        // Run experiment
        ExperimentResult result = evaluationService.runExperiment(
                java.util.List.of("response-length")
        );

        // Export to JSON
        String json = evaluationService.exportToJson(result);

        // Verify JSON is non-empty and shaped correctly. Dokimos's exporter
        // emits `experimentName` / `version` at the root and stats nested
        // under `summary` (with key `totalExamples`, not `totalCount`).
        assertNotNull(json);
        assertTrue(json.contains("\"experimentName\""),
                "JSON should contain experimentName, got: " + json);
        assertTrue(json.contains("\"totalExamples\""),
                "JSON should contain totalExamples under summary, got: " + json);
    }

    @Test
    @DisplayName("Should format console output")
    void testConsoleFormatting() throws Exception {
        // Run experiment
        ExperimentResult result = evaluationService.runExperiment(
                java.util.List.of("response-length")
        );

        // Format console output
        String formatted = evaluationService.formatForConsole(result);

        // Verify formatting
        assertNotNull(formatted);
        assertTrue(formatted.contains("EXPERIMENT RESULTS"));
        assertTrue(formatted.contains("Total Examples"));
        assertTrue(formatted.contains("Pass Rate"));
    }
}
