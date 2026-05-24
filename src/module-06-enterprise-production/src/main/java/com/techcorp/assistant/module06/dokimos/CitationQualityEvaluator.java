package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.BaseEvaluator;
import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import dev.dokimos.core.EvalTestCaseParam;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom evaluator that checks for proper citations in RAG responses.
 * Verifies responses include minimum number of citations in [1], [2] format.
 *
 * This is a rule-based evaluator that uses pattern matching to detect
 * citation markers in the format [n] where n is a number.
 */
public class CitationQualityEvaluator extends BaseEvaluator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");
    private final int minCitations;

    /**
     * Creates a citation quality evaluator.
     *
     * @param minCitations Minimum number of citations required for a passing score
     * @param threshold Threshold for passing (typically 1.0 for binary pass/fail)
     */
    public CitationQualityEvaluator(int minCitations, double threshold) {
        super("citation-quality", threshold, List.of(EvalTestCaseParam.ACTUAL_OUTPUT));
        this.minCitations = minCitations;
    }

    @Override
    protected EvalResult runEvaluation(EvalTestCase testCase) {
        String response = testCase.actualOutput();

        // Handle null or empty responses
        if (response == null || response.isBlank()) {
            return EvalResult.of(
                name(),
                0.0,
                threshold(),
                "Response is missing or empty"
            );
        }

        // Count citations using pattern matching
        Matcher matcher = CITATION_PATTERN.matcher(response);
        int citationCount = 0;
        while (matcher.find()) {
            citationCount++;
        }

        // Binary scoring: 1.0 if meets minimum, 0.0 otherwise
        double score = citationCount >= minCitations ? 1.0 : 0.0;

        String reason = String.format(
            "Found %d citations (minimum required: %d)%s",
            citationCount,
            minCitations,
            citationCount >= minCitations ? " - Meets threshold" : " - Below threshold"
        );

        return EvalResult.of(name(), score, threshold(), reason);
    }
}
