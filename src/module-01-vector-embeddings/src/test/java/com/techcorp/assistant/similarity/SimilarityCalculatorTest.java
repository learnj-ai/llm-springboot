package com.techcorp.assistant.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void shouldReturnOneForIdenticalVectors() {
        double score = calculator.cosineSimilarity(
                new float[]{1.0f, 0.0f},
                new float[]{1.0f, 0.0f});

        assertThat(score).isEqualTo(1.0d);
    }

    @Test
    void shouldReturnZeroForOrthogonalVectors() {
        double score = calculator.cosineSimilarity(
                new float[]{1.0f, 0.0f},
                new float[]{0.0f, 1.0f});

        assertThat(score).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void shouldReturnNegativeOneForOppositeVectors() {
        double score = calculator.cosineSimilarity(
                new float[]{1.0f, 0.0f},
                new float[]{-1.0f, 0.0f});

        assertThat(score).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void shouldReturnZeroForZeroVector() {
        double score = calculator.cosineSimilarity(
                new float[]{0.0f, 0.0f},
                new float[]{1.0f, 1.0f});

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void shouldComputeEuclideanDistance() {
        double distance = calculator.euclideanDistance(
                new float[]{0.0f, 0.0f},
                new float[]{3.0f, 4.0f});

        assertThat(distance).isEqualTo(5.0d);
    }

    @Test
    void shouldComputeDotProduct() {
        double result = calculator.dotProduct(
                new float[]{2.0f, 3.0f},
                new float[]{4.0f, 5.0f});

        assertThat(result).isEqualTo(23.0); // 2*4 + 3*5
    }

    @Test
    void shouldRejectVectorsWithDifferentDimensions() {
        assertThatThrownBy(() -> calculator.dotProduct(
                new float[]{1.0f},
                new float[]{1.0f, 2.0f}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same dimensions");
    }

    @Test
    void scoreShouldReturnCosineSimilarityForCosineMetric() {
        double score = calculator.score(
                new float[]{1.0f, 0.0f},
                new float[]{1.0f, 0.0f},
                SearchMetric.COSINE);

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void scoreShouldReturnNegatedEuclideanForEuclideanMetric() {
        double score = calculator.score(
                new float[]{0.0f, 0.0f},
                new float[]{3.0f, 4.0f},
                SearchMetric.EUCLIDEAN);

        assertThat(score).isEqualTo(-5.0);
    }

    @Test
    void scoreShouldReturnDotProductForDotProductMetric() {
        double score = calculator.score(
                new float[]{2.0f, 3.0f},
                new float[]{4.0f, 5.0f},
                SearchMetric.DOT_PRODUCT);

        assertThat(score).isEqualTo(23.0);
    }
}
