package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.EvalResult;
import dev.dokimos.core.EvalTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResponseLengthEvaluator.
 *
 * <p>The evaluator scores fractionally: a response inside {@code [minChars, maxChars]}
 * gets {@code 1 - |length - midpoint| / range} where midpoint is
 * {@code (min + max) / 2} and range is {@code max - min}. So:
 *
 * <ul>
 *   <li>Length exactly at midpoint → score 1.0</li>
 *   <li>Length at either boundary → score 0.5</li>
 *   <li>Length outside {@code [min, max]} → score 0.0 with a "too short" / "too long" reason</li>
 * </ul>
 *
 * <p>{@code success()} compares the score against the evaluator's threshold, so a
 * within-bounds response can still report {@code success=false} when the threshold
 * is tighter than the boundary score.
 */
@DisplayName("ResponseLengthEvaluator Tests")
class ResponseLengthEvaluatorTest {

    private static final int MIN = 50;
    private static final int MAX = 500;
    private static final double THRESHOLD = 0.8;
    private static final int IDEAL = (MIN + MAX) / 2;        // 275
    private static final int RANGE = MAX - MIN;              // 450

    private ResponseLengthEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ResponseLengthEvaluator(MIN, MAX, THRESHOLD);
    }

    /** Score for an in-range length, per the evaluator's algorithm. */
    private static double expectedScore(int length) {
        double deviation = Math.abs(length - IDEAL);
        return Math.max(0.0, 1.0 - (deviation / RANGE));
    }

    @Test
    @DisplayName("Should score 1.0 for response exactly at midpoint length")
    void testIdealLength() {
        String response = "x".repeat(IDEAL);
        EvalResult result = evaluator.runEvaluation(testCase(response));

        assertEquals("response-length", result.name());
        assertEquals(1.0, result.score(), 1e-9);
        assertTrue(result.success(), "1.0 is above the 0.8 threshold");
    }

    @Test
    @DisplayName("Should score fractionally for response within bounds, away from midpoint")
    void testResponseWithinBounds() {
        String response = "This is a valid response with appropriate length that falls within the configured bounds.";
        int length = response.length();
        assertTrue(length >= MIN && length <= MAX, "precondition: length must be in-range");

        EvalResult result = evaluator.runEvaluation(testCase(response));

        assertEquals("response-length", result.name());
        assertEquals(expectedScore(length), result.score(), 1e-9);
        // Score for length 91 is ~0.591 < threshold 0.8, so success=false.
        assertEquals(result.score() >= THRESHOLD, result.success());
    }

    @Test
    @DisplayName("Should fail for response shorter than the minimum")
    void testResponseTooShort() {
        EvalResult result = evaluator.runEvaluation(testCase("Too short"));

        assertEquals(0.0, result.score(), 1e-9);
        assertFalse(result.success());
        assertTrue(result.reason().toLowerCase().contains("too short"),
                "reason should mention 'too short', got: " + result.reason());
    }

    @Test
    @DisplayName("Should fail for response longer than the maximum")
    void testResponseTooLong() {
        EvalResult result = evaluator.runEvaluation(testCase("x".repeat(600)));

        assertEquals(0.0, result.score(), 1e-9);
        assertFalse(result.success());
        assertTrue(result.reason().toLowerCase().contains("too long"),
                "reason should mention 'too long', got: " + result.reason());
    }

    @Test
    @DisplayName("Should fail with 'empty' reason when output field is missing")
    void testMissingOutput() {
        EvalResult result = evaluator.runEvaluation(
                EvalTestCase.builder().actualOutputs(Map.of()).build());

        assertEquals(0.0, result.score(), 1e-9);
        assertFalse(result.success());
        // The evaluator routes both null and empty-string through the same branch,
        // returning "Response is empty" — the test now anchors on that wording.
        assertTrue(result.reason().toLowerCase().contains("empty"),
                "reason should mention 'empty', got: " + result.reason());
    }

    @Test
    @DisplayName("Should fail for non-string output")
    void testNonStringOutput() {
        EvalResult result = evaluator.runEvaluation(testCase(12345));

        assertEquals(0.0, result.score(), 1e-9);
        assertFalse(result.success());
    }

    @Test
    @DisplayName("Should score 0.5 at the minimum-length boundary (in-range but max deviation)")
    void testMinimumBoundary() {
        EvalResult result = evaluator.runEvaluation(testCase("x".repeat(MIN)));

        // Boundary length 50: deviation from midpoint 275 is 225, range is 450,
        // so score = 1 - 225/450 = 0.5. Within bounds, but below the 0.8
        // threshold, so success=false.
        assertEquals(0.5, result.score(), 1e-9);
        assertFalse(result.success(), "0.5 is below the 0.8 threshold");
    }

    @Test
    @DisplayName("Should score 0.5 at the maximum-length boundary (in-range but max deviation)")
    void testMaximumBoundary() {
        EvalResult result = evaluator.runEvaluation(testCase("x".repeat(MAX)));

        assertEquals(0.5, result.score(), 1e-9);
        assertFalse(result.success(), "0.5 is below the 0.8 threshold");
    }

    private static EvalTestCase testCase(Object output) {
        return EvalTestCase.builder().actualOutputs(Map.of("output", output)).build();
    }
}
