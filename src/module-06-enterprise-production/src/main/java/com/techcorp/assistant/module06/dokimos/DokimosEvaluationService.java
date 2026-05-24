package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.*;
import dev.dokimos.core.evaluators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for running Dokimos evaluation experiments.
 * Orchestrates dataset loading, task execution, and evaluator runs.
 */
@Service
public class DokimosEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(DokimosEvaluationService.class);

    private final DatasetLoader datasetLoader;
    private final Task ragEvaluationTask;
    private final FaithfulnessEvaluator faithfulnessEvaluator;
    private final HallucinationEvaluator hallucinationEvaluator;
    private final ContextualRelevanceEvaluator contextualRelevanceEvaluator;
    private final ExactMatchEvaluator exactMatchEvaluator;
    private final ResponseLengthEvaluator responseLengthEvaluator;

    public DokimosEvaluationService(
            DatasetLoader datasetLoader,
            Task ragEvaluationTask,
            FaithfulnessEvaluator faithfulnessEvaluator,
            HallucinationEvaluator hallucinationEvaluator,
            ContextualRelevanceEvaluator contextualRelevanceEvaluator,
            ExactMatchEvaluator exactMatchEvaluator,
            ResponseLengthEvaluator responseLengthEvaluator) {
        this.datasetLoader = datasetLoader;
        this.ragEvaluationTask = ragEvaluationTask;
        this.faithfulnessEvaluator = faithfulnessEvaluator;
        this.hallucinationEvaluator = hallucinationEvaluator;
        this.contextualRelevanceEvaluator = contextualRelevanceEvaluator;
        this.exactMatchEvaluator = exactMatchEvaluator;
        this.responseLengthEvaluator = responseLengthEvaluator;
    }

    /**
     * Runs a complete evaluation experiment with all configured evaluators.
     *
     * @return the experiment results with aggregated metrics
     * @throws DatasetLoader.DatasetLoadException if dataset loading fails
     */
    public ExperimentResult runExperiment() throws DatasetLoader.DatasetLoadException {
        return runExperiment(null);
    }

    /**
     * Runs an evaluation experiment with optionally filtered evaluators.
     *
     * @param evaluatorFilter list of evaluator names to include, or null for all
     * @return the experiment results with aggregated metrics
     * @throws DatasetLoader.DatasetLoadException if dataset loading fails
     */
    public ExperimentResult runExperiment(List<String> evaluatorFilter)
            throws DatasetLoader.DatasetLoadException {

        log.info("Starting Dokimos evaluation experiment");

        // Load dataset
        Dataset dataset = datasetLoader.loadDataset();

        // Build evaluator list with optional filtering
        List<Evaluator> evaluators = buildEvaluatorList(evaluatorFilter);

        log.info("Running experiment with {} evaluators on {} examples",
                evaluators.size(), dataset.examples().size());

        // Build and run experiment
        Experiment experiment = Experiment.builder()
                .name("RAG System Evaluation")
                .description("Comprehensive evaluation of RAG system performance")
                .dataset(dataset)
                .task(ragEvaluationTask)
                .evaluators(evaluators)
                .metadata(buildMetadata(evaluatorFilter))
                .build();

        // Execute experiment
        ExperimentResult result = experiment.run();

        // Log summary
        logExperimentSummary(result);

        return result;
    }

    /**
     * Extracts aggregated metrics from experiment results.
     *
     * @param result the experiment result
     * @return map of evaluator name to average score
     */
    public Map<String, Double> extractAverages(ExperimentResult result) {
        Map<String, Double> averages = new HashMap<>();

        for (Evaluator evaluator : getAllEvaluators()) {
            if (evaluator != null) {
                double avgScore = result.averageScore(evaluator.name());
                averages.put(evaluator.name(), avgScore);
            }
        }

        return averages;
    }

    /**
     * Extracts min and max scores for each evaluator.
     *
     * @param result the experiment result
     * @return map of evaluator name to [min, max] array
     */
    public Map<String, double[]> extractMinMax(ExperimentResult result) {
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

        return minMax;
    }

    private List<Evaluator> buildEvaluatorList(List<String> filter) {
        List<Evaluator> evaluators = new ArrayList<>();

        if (shouldInclude("faithfulness", filter) && faithfulnessEvaluator != null) {
            evaluators.add(faithfulnessEvaluator);
        }
        if (shouldInclude("hallucination", filter) && hallucinationEvaluator != null) {
            evaluators.add(hallucinationEvaluator);
        }
        if (shouldInclude("contextual-relevance", filter) && contextualRelevanceEvaluator != null) {
            evaluators.add(contextualRelevanceEvaluator);
        }
        if (shouldInclude("exact-match", filter) && exactMatchEvaluator != null) {
            evaluators.add(exactMatchEvaluator);
        }
        if (shouldInclude("response-length", filter) && responseLengthEvaluator != null) {
            evaluators.add(responseLengthEvaluator);
        }

        return evaluators;
    }

    private boolean shouldInclude(String evaluatorName, List<String> filter) {
        return filter == null || filter.isEmpty() || filter.contains(evaluatorName);
    }

    private List<Evaluator> getAllEvaluators() {
        return List.of(
                faithfulnessEvaluator,
                hallucinationEvaluator,
                contextualRelevanceEvaluator,
                exactMatchEvaluator,
                responseLengthEvaluator
        );
    }

    private Map<String, Object> buildMetadata(List<String> evaluatorFilter) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("filter_applied", evaluatorFilter != null && !evaluatorFilter.isEmpty());
        if (evaluatorFilter != null) {
            metadata.put("filtered_evaluators", evaluatorFilter);
        }
        return metadata;
    }

    /**
     * Exports experiment results to JSON string.
     *
     * @param result the experiment result to export
     * @return JSON string representation
     */
    public String exportToJson(ExperimentResult result) {
        return result.toJson();
    }

    /**
     * Exports experiment results to JSON file.
     *
     * @param result the experiment result to export
     * @param outputPath path to write JSON file
     */
    public void exportToJsonFile(ExperimentResult result, String outputPath) {
        try {
            result.exportJson(java.nio.file.Paths.get(outputPath));
            log.info("Experiment results exported to: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to export results to file: {}", outputPath, e);
            throw new RuntimeException("Failed to export experiment results", e);
        }
    }

    /**
     * Formats experiment results for console output (development mode).
     *
     * @param result the experiment result
     * @return formatted string for console display
     */
    public String formatForConsole(ExperimentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("EXPERIMENT RESULTS\n");
        sb.append("========================================\n");
        sb.append(String.format("Name: %s\n", result.name()));
        sb.append(String.format("Description: %s\n", result.description()));
        sb.append(String.format("Total Examples: %d\n", result.totalCount()));
        sb.append(String.format("Pass Count: %.0f\n", result.passCount()));
        sb.append(String.format("Fail Count: %.0f\n", result.failCount()));
        sb.append(String.format("Pass Rate: %.2f%%\n", result.passRate() * 100));
        sb.append("\n--- Evaluator Averages ---\n");

        Map<String, Double> averages = extractAverages(result);
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            sb.append(String.format("  %s: %.3f\n", entry.getKey(), entry.getValue()));
        }

        sb.append("\n--- Score Ranges (Min/Max) ---\n");
        Map<String, double[]> minMax = extractMinMax(result);
        for (Map.Entry<String, double[]> entry : minMax.entrySet()) {
            double[] range = entry.getValue();
            sb.append(String.format("  %s: [%.3f, %.3f]\n", entry.getKey(), range[0], range[1]));
        }

        sb.append("========================================\n");
        return sb.toString();
    }

    private void logExperimentSummary(ExperimentResult result) {
        log.info("Experiment completed: {}", result.name());
        log.info("Total examples: {}", result.totalCount());
        log.info("Pass rate: {}", result.passRate());

        Map<String, Double> averages = extractAverages(result);
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            log.info("Average {}: {}", entry.getKey(), entry.getValue());
        }
    }
}
