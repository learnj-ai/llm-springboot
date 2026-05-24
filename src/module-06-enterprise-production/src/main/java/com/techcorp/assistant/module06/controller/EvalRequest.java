package com.techcorp.assistant.module06.controller;

import java.util.List;

/**
 * Request DTO for evaluation API.
 * Specifies dataset name and optional evaluator filter.
 */
public record EvalRequest(
        String datasetName,
        List<String> evaluators
) {
    /**
     * Convenience constructor for running all evaluators.
     */
    public EvalRequest(String datasetName) {
        this(datasetName, null);
    }
}
