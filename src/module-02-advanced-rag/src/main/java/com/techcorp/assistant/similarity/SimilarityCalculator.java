package com.techcorp.assistant.similarity;

import org.springframework.stereotype.Component;

@Component
public class SimilarityCalculator {

    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        validateDimensions(vectorA, vectorB);

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / Math.sqrt(normA * normB);
    }

    public double euclideanDistance(float[] vectorA, float[] vectorB) {
        validateDimensions(vectorA, vectorB);

        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double difference = vectorA[i] - vectorB[i];
            sum += difference * difference;
        }
        return Math.sqrt(sum);
    }

    public double dotProduct(float[] vectorA, float[] vectorB) {
        validateDimensions(vectorA, vectorB);

        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            sum += vectorA[i] * vectorB[i];
        }
        return sum;
    }

    public double score(float[] vectorA, float[] vectorB, SearchMetric metric) {
        return switch (metric) {
            case COSINE -> cosineSimilarity(vectorA, vectorB);
            case EUCLIDEAN -> -euclideanDistance(vectorA, vectorB);
            case DOT_PRODUCT -> dotProduct(vectorA, vectorB);
        };
    }

    private void validateDimensions(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same dimensions");
        }
    }
}
