package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.BaseEvaluator;
import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import dev.dokimos.core.EvalTestCaseParam;

import java.util.List;

/**
 * Custom evaluator that checks if response length is within acceptable bounds.
 * Demonstrates extending Dokimos BaseEvaluator for domain-specific criteria.
 */
public class ResponseLengthEvaluator extends BaseEvaluator {

    private final int minChars;
    private final int maxChars;

    public ResponseLengthEvaluator(int minChars, int maxChars, double threshold) {
        super("response-length", threshold, List.of(EvalTestCaseParam.ACTUAL_OUTPUT));
        this.minChars = minChars;
        this.maxChars = maxChars;
    }

    @Override
    protected EvalResult runEvaluation(EvalTestCase testCase) {
        String response = testCase.actualOutput();

        if (response == null || response.isEmpty()) {
            return EvalResult.failure(name(), 0.0, "Response is empty");
        }

        int length = response.length();

        if (length < minChars) {
            String reason = String.format("Response too short: %d chars (min: %d)", length, minChars);
            return EvalResult.failure(name(), 0.0, reason);
        }

        if (length > maxChars) {
            String reason = String.format("Response too long: %d chars (max: %d)", length, maxChars);
            return EvalResult.failure(name(), 0.0, reason);
        }

        // Calculate score based on how well it fits the ideal range
        int idealLength = (minChars + maxChars) / 2;
        int range = maxChars - minChars;
        double deviation = Math.abs(length - idealLength);
        double score = Math.max(0.0, 1.0 - (deviation / range));

        String reason = String.format("Response length: %d chars (range: %d-%d)", length, minChars, maxChars);
        return EvalResult.of(name(), score, threshold(), reason);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int minChars = 50;
        private int maxChars = 1000;
        private double threshold = 0.7;

        public Builder minChars(int minChars) {
            this.minChars = minChars;
            return this;
        }

        public Builder maxChars(int maxChars) {
            this.maxChars = maxChars;
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public ResponseLengthEvaluator build() {
            if (minChars <= 0) {
                throw new IllegalArgumentException("minChars must be positive");
            }
            if (maxChars <= minChars) {
                throw new IllegalArgumentException("maxChars must be greater than minChars");
            }
            return new ResponseLengthEvaluator(minChars, maxChars, threshold);
        }
    }
}
