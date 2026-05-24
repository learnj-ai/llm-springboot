package com.techcorp.assistant.module06.controller;

import com.techcorp.assistant.module06.dokimos.DatasetLoader;
import com.techcorp.assistant.module06.dokimos.DokimosEvaluationService;
import dev.dokimos.core.ExperimentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EvaluationController REST API.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationController REST API Tests")
class EvaluationControllerTest {

    @Mock
    private DokimosEvaluationService evaluationService;

    private EvaluationController controller;

    @BeforeEach
    void setUp() {
        controller = new EvaluationController(evaluationService);
    }

    @Test
    @DisplayName("Should return success response for valid request")
    void testRunEvaluationSuccess() throws Exception {
        // Create mock experiment result
        ExperimentResult mockResult = createMockExperimentResult();
        when(evaluationService.runExperiment(nullable(java.util.List.class))).thenReturn(mockResult);

        // Create request
        EvalRequest request = new EvalRequest("eval-golden-set", null);

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().status());

        // Verify service was called
        verify(evaluationService, times(1)).runExperiment(null);
    }

    @Test
    @DisplayName("Should filter evaluators when requested")
    void testRunEvaluationWithFilter() throws Exception {
        // Create mock experiment result
        ExperimentResult mockResult = createMockExperimentResult();
        when(evaluationService.runExperiment(nullable(java.util.List.class))).thenReturn(mockResult);

        // Create request with filter
        List<String> filter = List.of("faithfulness", "hallucination");
        EvalRequest request = new EvalRequest("eval-golden-set", filter);

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(200, response.getStatusCode().value());

        // Verify service was called with filter
        verify(evaluationService, times(1)).runExperiment(filter);
    }

    @Test
    @DisplayName("Should return 404 when dataset not found")
    void testDatasetNotFound() throws Exception {
        // Mock dataset load exception
        when(evaluationService.runExperiment(nullable(java.util.List.class)))
                .thenThrow(new DatasetLoader.DatasetLoadException("Dataset not found"));

        // Create request
        EvalRequest request = new EvalRequest("nonexistent-dataset", null);

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(404, response.getStatusCode().value());
        assertEquals("error", response.getBody().status());
    }

    @Test
    @DisplayName("Should return 400 for invalid evaluator name")
    void testInvalidEvaluatorName() throws Exception {
        // Create request with invalid evaluator
        EvalRequest request = new EvalRequest("eval-golden-set", List.of("invalid-evaluator"));

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(400, response.getStatusCode().value());
        assertEquals("error", response.getBody().status());

        // Verify service was not called
        verify(evaluationService, never()).runExperiment(anyList());
    }

    @Test
    @DisplayName("Should return 400 when dataset name is missing")
    void testMissingDatasetName() throws Exception {
        // Create request with null dataset name
        EvalRequest request = new EvalRequest(null, null);

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(400, response.getStatusCode().value());
        assertEquals("error", response.getBody().status());

        // Verify service was not called
        verify(evaluationService, never()).runExperiment(anyList());
    }

    @Test
    @DisplayName("Should return 500 for internal errors")
    void testInternalError() throws Exception {
        // Mock internal error
        when(evaluationService.runExperiment(nullable(java.util.List.class)))
                .thenThrow(new RuntimeException("Internal error"));

        // Create request
        EvalRequest request = new EvalRequest("eval-golden-set", null);

        // Execute request
        ResponseEntity<EvalResponse> response = controller.runEvaluation(request);

        // Verify response
        assertEquals(500, response.getStatusCode().value());
        assertEquals("error", response.getBody().status());
    }

    /**
     * Creates a mock ExperimentResult for testing.
     */
    private ExperimentResult createMockExperimentResult() {
        return new ExperimentResult(
                "Test Experiment",
                "Test description",
                Map.of("test", "metadata"),
                List.of()  // Empty run results
        );
    }
}
