package com.techcorp.assistant.module06.service;

import dev.dokimos.core.EvalResult;
import dev.dokimos.core.ExperimentResult;
import dev.dokimos.core.ItemResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for ExperimentResult suitable for REST API responses.
 * Provides structured JSON representation with per-example details.
 */
public record ExperimentResultDTO(
        String name,
        String description,
        Map<String, Object> metadata,
        int totalCount,
        double passCount,
        double failCount,
        double passRate,
        Map<String, Double> averageScores,
        Map<String, ScoreRange> scoreRanges,
        List<ItemResultDTO> itemResults
) {

    /**
     * Score range with min and max values.
     */
    public record ScoreRange(double min, double max) {}

    /**
     * Per-example result details.
     */
    public record ItemResultDTO(
            String input,
            String expectedOutput,
            Map<String, Object> actualOutputs,
            boolean success,
            List<EvalResultDTO> evaluations
    ) {}

    /**
     * Individual evaluator result.
     */
    public record EvalResultDTO(
            String name,
            double score,
            boolean passed,
            String reason
    ) {}

    /**
     * Creates DTO from Dokimos ExperimentResult.
     *
     * @param result the experiment result to convert
     * @return DTO representation suitable for REST API
     */
    public static ExperimentResultDTO from(ExperimentResult result) {
        // Calculate average scores
        Map<String, Double> averageScores = new HashMap<>();
        for (String evaluatorName : result.evaluatorNames()) {
            averageScores.put(evaluatorName, result.averageScore(evaluatorName));
        }

        // Calculate score ranges
        Map<String, ScoreRange> scoreRanges = calculateScoreRanges(result);

        // Convert item results
        List<ItemResultDTO> itemResults = result.itemResults().stream()
                .map(ExperimentResultDTO::toItemResultDTO)
                .collect(Collectors.toList());

        return new ExperimentResultDTO(
                result.name(),
                result.description(),
                result.metadata(),
                result.totalCount(),
                result.passCount(),
                result.failCount(),
                result.passRate(),
                averageScores,
                scoreRanges,
                itemResults
        );
    }

    private static Map<String, ScoreRange> calculateScoreRanges(ExperimentResult result) {
        Map<String, ScoreRange> ranges = new HashMap<>();
        Map<String, double[]> minMax = new HashMap<>();

        for (ItemResult item : result.itemResults()) {
            for (EvalResult eval : item.evalResults()) {
                String name = eval.name();
                double score = eval.score();

                minMax.putIfAbsent(name, new double[]{Double.MAX_VALUE, Double.MIN_VALUE});
                double[] range = minMax.get(name);
                range[0] = Math.min(range[0], score);
                range[1] = Math.max(range[1], score);
            }
        }

        for (Map.Entry<String, double[]> entry : minMax.entrySet()) {
            double[] range = entry.getValue();
            ranges.put(entry.getKey(), new ScoreRange(range[0], range[1]));
        }

        return ranges;
    }

    private static ItemResultDTO toItemResultDTO(ItemResult item) {
        String input = item.example().input();
        String expectedOutput = item.example().expectedOutput();
        Map<String, Object> actualOutputs = item.actualOutputs();
        boolean success = item.success();

        List<EvalResultDTO> evaluations = item.evalResults().stream()
                .map(eval -> new EvalResultDTO(
                        eval.name(),
                        eval.score(),
                        eval.success(),
                        eval.reason()
                ))
                .collect(Collectors.toList());

        return new ItemResultDTO(input, expectedOutput, actualOutputs, success, evaluations);
    }
}
