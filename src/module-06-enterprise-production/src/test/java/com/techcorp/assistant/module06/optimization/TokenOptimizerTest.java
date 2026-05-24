package com.techcorp.assistant.module06.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenOptimizerTest {

    private TokenOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new TokenOptimizer();
        ReflectionTestUtils.setField(optimizer, "maxTokens", 100);
        ReflectionTestUtils.setField(optimizer, "compressionEnabled", true);
    }

    @Test
    void testOptimizeContextWithinBudget() {
        List<String> segments = List.of(
                "Short segment",
                "Another short segment",
                "Third segment"
        );
        List<Double> scores = List.of(0.9, 0.8, 0.7);

        List<TokenOptimizer.TokenizedSegment> result = optimizer.optimizeContext(segments, scores);

        assertFalse(result.isEmpty());
        assertTrue(result.size() <= segments.size());
    }

    @Test
    void testOptimizeContextSelectsByRelevance() {
        List<String> segments = List.of(
                "Low relevance segment",
                "High relevance segment",
                "Medium relevance segment"
        );
        List<Double> scores = List.of(0.3, 0.9, 0.6);

        List<TokenOptimizer.TokenizedSegment> result = optimizer.optimizeContext(segments, scores);

        // Should select high relevance first
        assertTrue(result.stream()
                .anyMatch(s -> s.relevanceScore() == 0.9));
    }

    @Test
    void testCompressPrompt() {
        String prompt = "This   is    basically   a   very   redundant   prompt   with   filler   words";
        String compressed = optimizer.compressPrompt(prompt);

        assertFalse(compressed.contains("basically"));
        assertFalse(compressed.contains("very"));
        assertFalse(compressed.contains("  ")); // No double spaces
    }

    @Test
    void testCompressPromptDisabled() {
        ReflectionTestUtils.setField(optimizer, "compressionEnabled", false);

        String prompt = "This is basically a test";
        String result = optimizer.compressPrompt(prompt);

        assertEquals(prompt, result);
    }
}
