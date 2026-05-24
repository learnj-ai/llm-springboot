package com.techcorp.assistant.module06.controller;

import com.techcorp.assistant.module06.dokimos.DatasetLoader;
import com.techcorp.assistant.module06.dokimos.DokimosEvaluationService;
import dev.dokimos.core.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for evaluation operations.
 * Provides endpoints to run experiments and retrieve results.
 */
@RestController
@RequestMapping("/api/v1/eval")
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);

    private final DokimosEvaluationService evaluationService;

    public EvaluationController(DokimosEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Runs an evaluation experiment with optional evaluator filtering.
     *
     * POST /api/v1/eval/run
     *
     * Request body:
     * {
     *   "datasetName": "eval-golden-set",
     *   "evaluators": ["faithfulness", "hallucination"]  // optional
     * }
     *
     * @param request the evaluation request
     * @return experiment results wrapped in response DTO
     */
    @PostMapping("/run")
    public ResponseEntity<EvalResponse> runEvaluation(@RequestBody EvalRequest request) {
        try {
            log.info("Running evaluation experiment with request: {}", request);

            // Validate request
            if (request.datasetName() == null || request.datasetName().isBlank()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(EvalResponse.error("Dataset name is required"));
            }

            // Validate evaluator names if specified
            if (request.evaluators() != null && !request.evaluators().isEmpty()) {
                validateEvaluatorNames(request.evaluators());
            }

            // Run experiment with optional filter
            ExperimentResult result = evaluationService.runExperiment(request.evaluators());

            log.info("Evaluation experiment completed successfully");
            return ResponseEntity.ok(EvalResponse.success(result));

        } catch (DatasetLoader.DatasetLoadException e) {
            log.error("Dataset not found or invalid: {}", request.datasetName(), e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(EvalResponse.error("Dataset not found: " + request.datasetName()));

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(EvalResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error running evaluation experiment", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EvalResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Validates that evaluator names are recognized.
     * Throws IllegalArgumentException if any name is invalid.
     */
    private void validateEvaluatorNames(java.util.List<String> evaluatorNames) {
        java.util.Set<String> validNames = java.util.Set.of(
                "faithfulness",
                "hallucination",
                "contextual-relevance",
                "exact-match",
                "response-length"
        );

        for (String name : evaluatorNames) {
            if (!validNames.contains(name)) {
                throw new IllegalArgumentException(
                        "Invalid evaluator name: " + name + ". Valid names: " + validNames
                );
            }
        }
    }
}
