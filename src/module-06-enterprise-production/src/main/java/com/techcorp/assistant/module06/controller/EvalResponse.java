package com.techcorp.assistant.module06.controller;

import com.techcorp.assistant.module06.service.ExperimentResultDTO;
import dev.dokimos.core.ExperimentResult;

import java.time.Instant;

/**
 * Response DTO for evaluation API.
 * Wraps experiment results with metadata for REST API responses.
 */
public record EvalResponse(
        String status,
        Instant timestamp,
        ExperimentResultDTO result
) {
    /**
     * Creates a successful response from ExperimentResult.
     */
    public static EvalResponse success(ExperimentResult experimentResult) {
        return new EvalResponse(
                "success",
                Instant.now(),
                ExperimentResultDTO.from(experimentResult)
        );
    }

    /**
     * Creates an error response.
     */
    public static EvalResponse error(String message) {
        return new EvalResponse(
                "error",
                Instant.now(),
                null
        );
    }
}
