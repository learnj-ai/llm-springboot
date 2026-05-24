package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.JudgeLM;
import dev.dokimos.core.evaluators.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Dokimos built-in evaluators.
 * Creates evaluator beans with configurable thresholds.
 */
@Configuration
public class EvaluatorConfig {

    @Value("${dokimos.evaluators.faithfulness.enabled:true}")
    private boolean faithfulnessEnabled;

    @Value("${dokimos.evaluators.faithfulness.threshold:0.7}")
    private double faithfulnessThreshold;

    @Value("${dokimos.evaluators.hallucination.enabled:true}")
    private boolean hallucinationEnabled;

    @Value("${dokimos.evaluators.hallucination.threshold:0.8}")
    private double hallucinationThreshold;

    @Value("${dokimos.evaluators.contextual-relevance.enabled:true}")
    private boolean contextualRelevanceEnabled;

    @Value("${dokimos.evaluators.contextual-relevance.threshold:0.7}")
    private double contextualRelevanceThreshold;

    @Value("${dokimos.evaluators.exact-match.enabled:true}")
    private boolean exactMatchEnabled;

    @Value("${dokimos.evaluators.response-length.enabled:true}")
    private boolean responseLengthEnabled;

    @Value("${dokimos.evaluators.response-length.min-chars:50}")
    private int responseLengthMinChars;

    @Value("${dokimos.evaluators.response-length.max-chars:1000}")
    private int responseLengthMaxChars;

    @Value("${dokimos.evaluators.citation-quality.enabled:true}")
    private boolean citationQualityEnabled;

    @Value("${dokimos.evaluators.citation-quality.min-citations:2}")
    private int citationQualityMinCitations;

    @Value("${dokimos.evaluators.citation-quality.threshold:1.0}")
    private double citationQualityThreshold;

    /**
     * Faithfulness evaluator - checks if response is faithful to source context.
     * Uses LLM-as-judge to verify all claims are supported by sources.
     */
    @Bean
    public FaithfulnessEvaluator faithfulnessEvaluator(JudgeLM judgeLM) {
        if (!faithfulnessEnabled) {
            return null;
        }

        return FaithfulnessEvaluator.builder()
                .judge(judgeLM)
                .threshold(faithfulnessThreshold)
                .contextKey("context")
                .build();
    }

    /**
     * Hallucination evaluator - detects fabricated information in responses.
     * Lower scores indicate better performance (less hallucination).
     */
    @Bean
    public HallucinationEvaluator hallucinationEvaluator(JudgeLM judgeLM) {
        if (!hallucinationEnabled) {
            return null;
        }

        return HallucinationEvaluator.builder()
                .judge(judgeLM)
                .threshold(hallucinationThreshold)
                .build();
    }

    /**
     * Contextual relevance evaluator - assesses quality of retrieved context.
     * Scores how relevant the retrieved documents are to the query.
     */
    @Bean
    public ContextualRelevanceEvaluator contextualRelevanceEvaluator(JudgeLM judgeLM) {
        if (!contextualRelevanceEnabled) {
            return null;
        }

        return ContextualRelevanceEvaluator.builder()
                .judge(judgeLM)
                .threshold(contextualRelevanceThreshold)
                .build();
    }

    /**
     * Exact match evaluator - binary pass/fail for exact string matching.
     * Useful for testing specific expected outputs.
     */
    @Bean
    public ExactMatchEvaluator exactMatchEvaluator() {
        if (!exactMatchEnabled) {
            return null;
        }

        return ExactMatchEvaluator.builder()
                .build();
    }

    /**
     * Custom response length evaluator - checks min/max character thresholds.
     * Demonstrates custom evaluator implementation extending BaseEvaluator.
     */
    @Bean
    public ResponseLengthEvaluator responseLengthEvaluator() {
        if (!responseLengthEnabled) {
            return null;
        }

        return ResponseLengthEvaluator.builder()
                .minChars(responseLengthMinChars)
                .maxChars(responseLengthMaxChars)
                .build();
    }

    /**
     * Citation quality evaluator - checks for proper citations in RAG responses.
     * Verifies responses include minimum number of citations in [1], [2] format.
     */
    @Bean
    public CitationQualityEvaluator citationQualityEvaluator() {
        if (!citationQualityEnabled) {
            return null;
        }

        return new CitationQualityEvaluator(
                citationQualityMinCitations,
                citationQualityThreshold
        );
    }
}
