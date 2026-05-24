package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CitationQualityEvaluator.
 * Tests citation detection and scoring logic.
 */
class CitationQualityEvaluatorTest {

    @Test
    void shouldPassWhenResponseHasMinimumCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithCitations = "According to the documentation [1], this feature is supported. " +
                "The API reference [2] confirms this behavior.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput(responseWithCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).contains("Found 2 citations");
        assertThat(result.reason()).contains("Meets threshold");
    }

    @Test
    void shouldPassWhenResponseHasMoreThanMinimumCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithManyCitations = "The feature [1] is supported [2] and documented [3] in multiple places.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("Tell me about the feature")
                .actualOutput(responseWithManyCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).contains("Found 3 citations");
    }

    @Test
    void shouldFailWhenResponseHasNoCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithoutCitations = "This feature is supported according to the documentation.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput(responseWithoutCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("Found 0 citations");
        assertThat(result.reason()).contains("Below threshold");
    }

    @Test
    void shouldFailWhenResponseHasFewerThanMinimumCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(3, 1.0);
        String responseWithFewCitations = "The documentation [1] states this clearly [2].";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput(responseWithFewCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("Found 2 citations");
        assertThat(result.reason()).contains("minimum required: 3");
    }

    @Test
    void shouldHandleBlankResponse() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput("")
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("Response is missing or empty");
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput("   ")
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("Response is missing or empty");
    }

    @Test
    void shouldDetectMultipleDigitCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithLargeCitations = "Sources [10] and [42] confirm this.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What do the sources say?")
                .actualOutput(responseWithLargeCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).contains("Found 2 citations");
    }

    @Test
    void shouldNotDetectMalformedCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithMalformedCitations = "According to source (1) and reference {2}.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What do the sources say?")
                .actualOutput(responseWithMalformedCitations)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("Found 0 citations");
    }

    @Test
    void shouldCountDuplicateCitationsSeparately() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);
        String responseWithDuplicates = "Feature [1] is mentioned [1] multiple times.";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("Tell me about the feature")
                .actualOutput(responseWithDuplicates)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).contains("Found 2 citations");
    }

    @Test
    void shouldRespectCustomMinimumCitations() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(1, 1.0);
        String responseWithOneCitation = "According to the documentation [1].";
        EvalTestCase testCase = EvalTestCase.builder()
                .input("What does the documentation say?")
                .actualOutput(responseWithOneCitation)
                .build();

        // When
        EvalResult result = evaluator.evaluate(testCase);

        // Then
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).contains("Found 1 citations");
        assertThat(result.reason()).contains("minimum required: 1");
    }

    @Test
    void shouldHaveCorrectEvaluatorName() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 1.0);

        // Then
        assertThat(evaluator.name()).isEqualTo("citation-quality");
    }

    @Test
    void shouldHaveCorrectThreshold() {
        // Given
        CitationQualityEvaluator evaluator = new CitationQualityEvaluator(2, 0.8);

        // Then
        assertThat(evaluator.threshold()).isEqualTo(0.8);
    }
}
