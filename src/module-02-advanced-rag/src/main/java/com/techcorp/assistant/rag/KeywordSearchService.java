package com.techcorp.assistant.rag;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.store.IndexedSegment;
import com.techcorp.assistant.store.VectorStoreService;
import dev.langchain4j.data.segment.TextSegment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * BM25-inspired keyword search using TF-IDF scoring.
 * Students compare this with vector search to understand why hybrid search matters:
 * keyword search excels at exact term matching, while vector search captures semantic meaning.
 */
@Service
public class KeywordSearchService {

    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;

    private final Supplier<List<IndexedSegment>> segmentSupplier;

    @Autowired
    public KeywordSearchService(VectorStoreService vectorStoreService) {
        this.segmentSupplier = () -> vectorStoreService.getAllSegments(ChunkingStrategy.RECURSIVE);
    }

    KeywordSearchService(List<IndexedSegment> fixedSegments) {
        this.segmentSupplier = () -> fixedSegments;
    }

    public List<TextSegment> search(String query, int maxResults) {
        List<IndexedSegment> allSegments = segmentSupplier.get();
        if (allSegments.isEmpty()) {
            return List.of();
        }

        Set<String> queryTerms = uniqueTokens(query);
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        // Tokenize each segment exactly once per call. We need both:
        //   - the full token list (for BM25 length normalization — total tokens, not unique)
        //   - the unique-token set (for fast doc-frequency contains-checks)
        // Doing this up-front turns the inner loops from O(segments × terms × tokenize)
        // into O(segments × terms).
        List<SegmentTokens> tokenized = new ArrayList<>(allSegments.size());
        for (IndexedSegment indexed : allSegments) {
            tokenized.add(SegmentTokens.of(indexed));
        }

        double avgDocLength = tokenized.stream()
                .mapToInt(s -> s.allTokens.size())
                .average()
                .orElse(1.0);

        int totalDocs = tokenized.size();

        Map<String, Integer> documentFrequencies = new HashMap<>();
        for (String term : queryTerms) {
            int df = 0;
            for (SegmentTokens s : tokenized) {
                if (s.uniqueTokens.contains(term)) {
                    df++;
                }
            }
            documentFrequencies.put(term, df);
        }

        record ScoredSegment(TextSegment segment, double score) {}

        List<ScoredSegment> scored = new ArrayList<>(tokenized.size());
        for (SegmentTokens s : tokenized) {
            double score = computeBM25Score(s, queryTerms, documentFrequencies, totalDocs, avgDocLength);
            if (score > 0) {
                scored.add(new ScoredSegment(s.indexed.segment(), score));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSegment::score).reversed())
                .limit(maxResults)
                .map(ScoredSegment::segment)
                .toList();
    }

    private double computeBM25Score(
            SegmentTokens segment,
            Set<String> queryTerms,
            Map<String, Integer> documentFrequencies,
            int totalDocs,
            double avgDocLength) {

        int docLength = segment.allTokens.size();  // total tokens, not unique
        Map<String, Long> termFrequencies = segment.termFrequencies;

        double score = 0.0;
        for (String term : queryTerms) {
            int df = documentFrequencies.getOrDefault(term, 0);
            if (df == 0) {
                continue;
            }

            // IDF component: log((N - df + 0.5) / (df + 0.5) + 1)
            double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);

            // TF component with BM25 saturation
            long tf = termFrequencies.getOrDefault(term, 0L);
            double tfNormalized = (tf * (BM25_K1 + 1.0))
                    / (tf + BM25_K1 * (1.0 - BM25_B + BM25_B * docLength / avgDocLength));

            score += idf * tfNormalized;
        }

        return score;
    }

    private static Set<String> uniqueTokens(String text) {
        Set<String> set = new HashSet<>();
        for (String token : text.toLowerCase().split("\\W+")) {
            if (!token.isEmpty() && token.length() > 1) {
                set.add(token);
            }
        }
        return set;
    }

    private static List<String> tokensIn(String text) {
        List<String> list = new ArrayList<>();
        for (String token : text.toLowerCase().split("\\W+")) {
            if (!token.isEmpty() && token.length() > 1) {
                list.add(token);
            }
        }
        return list;
    }

    /**
     * Per-segment tokenization computed once at search-call time.
     * Holds both the full token list (for BM25 length normalization) and
     * the unique-token set (for doc-frequency lookups).
     */
    private record SegmentTokens(
            IndexedSegment indexed,
            List<String> allTokens,
            Set<String> uniqueTokens,
            Map<String, Long> termFrequencies) {

        static SegmentTokens of(IndexedSegment indexed) {
            List<String> tokens = tokensIn(indexed.segment().text());
            Set<String> unique = new HashSet<>(tokens);
            Map<String, Long> tf = new HashMap<>();
            for (String t : tokens) {
                tf.merge(t, 1L, Long::sum);
            }
            return new SegmentTokens(indexed, tokens, unique, tf);
        }
    }
}
