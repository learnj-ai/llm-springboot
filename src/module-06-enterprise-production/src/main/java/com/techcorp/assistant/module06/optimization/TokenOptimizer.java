package com.techcorp.assistant.module06.optimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TokenOptimizer {

    private static final Logger log = LoggerFactory.getLogger(TokenOptimizer.class);

    // Simple word-based token estimation (GPT tokens are ~0.75 words)
    private static final double WORDS_PER_TOKEN = 0.75;

    @Value("${token-optimizer.max-tokens:4000}")
    private int maxTokens;

    @Value("${token-optimizer.compression-enabled:true}")
    private boolean compressionEnabled;

    public List<TokenizedSegment> optimizeContext(List<String> segments, List<Double> relevanceScores) {
        if (segments.size() != relevanceScores.size()) {
            throw new IllegalArgumentException("Segments and scores must have same size");
        }

        // Create tokenized segments
        List<TokenizedSegment> tokenized = segments.stream()
                .map(this::tokenize)
                .collect(Collectors.toList());

        // Add relevance scores
        for (int i = 0; i < tokenized.size(); i++) {
            TokenizedSegment segment = tokenized.get(i);
            tokenized.set(i, new TokenizedSegment(
                    segment.content(),
                    segment.tokenCount(),
                    relevanceScores.get(i)
            ));
        }

        // Select segments within budget
        List<TokenizedSegment> selected = selectByBudget(tokenized, maxTokens);

        log.info("Optimized context: {} segments selected, {} tokens used of {} budget",
                selected.size(), totalTokens(selected), maxTokens);

        return selected;
    }

    public String compressPrompt(String prompt) {
        if (!compressionEnabled) {
            return prompt;
        }

        // Remove redundant whitespace
        String compressed = prompt.replaceAll("\\s+", " ").trim();

        // Remove filler words
        compressed = compressed.replaceAll("\\b(basically|actually|literally|just|really|very)\\b", "");

        // Normalize multiple spaces again after filler removal
        compressed = compressed.replaceAll("\\s+", " ").trim();

        int originalTokens = estimateTokens(prompt);
        int compressedTokens = estimateTokens(compressed);

        log.debug("Compressed prompt: {} -> {} tokens ({} reduction)",
                originalTokens, compressedTokens, originalTokens - compressedTokens);

        return compressed;
    }

    private TokenizedSegment tokenize(String content) {
        int tokens = estimateTokens(content);
        return new TokenizedSegment(content, tokens, 0.0);
    }

    private int estimateTokens(String text) {
        // Simple word-based estimation
        int words = text.split("\\s+").length;
        return (int) Math.ceil(words / WORDS_PER_TOKEN);
    }

    private List<TokenizedSegment> selectByBudget(List<TokenizedSegment> segments, int budget) {
        // Sort by relevance score descending
        List<TokenizedSegment> sorted = segments.stream()
                .sorted(Comparator.comparingDouble(TokenizedSegment::relevanceScore).reversed())
                .collect(Collectors.toList());

        // Select segments until budget is exhausted
        List<TokenizedSegment> selected = new java.util.ArrayList<>();
        int usedTokens = 0;

        for (TokenizedSegment segment : sorted) {
            if (usedTokens + segment.tokenCount() <= budget) {
                selected.add(segment);
                usedTokens += segment.tokenCount();
            }
        }

        return selected;
    }

    private int totalTokens(List<TokenizedSegment> segments) {
        return segments.stream()
                .mapToInt(TokenizedSegment::tokenCount)
                .sum();
    }

    public record TokenizedSegment(
            String content,
            int tokenCount,
            double relevanceScore
    ) {}
}
