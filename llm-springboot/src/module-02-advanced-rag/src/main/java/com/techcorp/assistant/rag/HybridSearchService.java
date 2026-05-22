package com.techcorp.assistant.rag;

import com.techcorp.assistant.store.VectorStoreService;
import dev.langchain4j.data.segment.TextSegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Combines vector search (semantic) with keyword search (lexical) using
 * Reciprocal Rank Fusion (RRF) to merge ranked lists from different retrieval methods.
 * <p>
 * Why hybrid? Vector search captures meaning ("how do I connect remotely" produces VPN docs)
 * but misses exact terms. Keyword search finds exact matches ("SEV1" produces incident docs)
 * but misses paraphrases. Hybrid search gets the best of both.
 *
 * <p><b>Two return shapes.</b> {@link #hybridSearchScored(String, int)} returns
 * {@link ScoredSegment} so callers can do their own cross-query rank fusion;
 * {@link #hybridSearch(String, int)} returns plain {@code TextSegment} for callers that
 * only need the ordered list. The scored API is the primary one; the unscored API is a
 * thin convenience wrapper.
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int RRF_RANK_CONSTANT = 60;

    private final VectorStoreService vectorStore;
    private final KeywordSearchService keywordSearch;
    private final ReRanker reRanker;

    public HybridSearchService(
            VectorStoreService vectorStore,
            KeywordSearchService keywordSearch,
            ReRanker reRanker) {
        this.vectorStore = vectorStore;
        this.keywordSearch = keywordSearch;
        this.reRanker = reRanker;
    }

    /**
     * Hybrid search that returns plain {@link TextSegment}s in ranked order.
     * Backward-compatible API for callers that don't need scores.
     */
    public List<TextSegment> hybridSearch(String query, int topK) {
        return hybridSearchScored(query, topK).stream()
                .map(ScoredSegment::segment)
                .toList();
    }

    /**
     * Hybrid search that returns scored hits. Use this when you plan to fuse results
     * across multiple queries (e.g. {@code RAGService} fusing multi-query + HyDE outputs).
     */
    public List<ScoredSegment> hybridSearchScored(String query, int topK) {
        int retrievalSize = topK * 2;

        // Stage 1: retrieval from both sources.
        List<TextSegment> vectorResults = vectorStore.searchSegments(query, retrievalSize);
        List<TextSegment> keywordResults = keywordSearch.search(query, retrievalSize);

        log.debug("Vector search returned {} results, keyword search returned {} results",
                vectorResults.size(), keywordResults.size());

        // Stage 2: merge via RRF, keeping scores.
        List<ScoredSegment> fused = reciprocalRankFusionScored(
                vectorResults, keywordResults, retrievalSize, query);

        log.debug("RRF merged to {} results", fused.size());

        // Stage 3: re-rank (operates on TextSegments). We preserve the fused score for
        // each segment that survives re-ranking by looking it up in a text->score map.
        List<TextSegment> rerankedTexts = reRanker.rerank(
                query, fused.stream().map(ScoredSegment::segment).toList(), topK);

        Map<String, Double> scoreByText = new HashMap<>();
        for (ScoredSegment s : fused) {
            scoreByText.put(s.segment().text(), s.score());
        }

        List<ScoredSegment> result = new ArrayList<>(rerankedTexts.size());
        for (TextSegment seg : rerankedTexts) {
            double score = scoreByText.getOrDefault(seg.text(), 0.0);
            result.add(new ScoredSegment(seg, score, query));
        }
        return result;
    }

    public List<TextSegment> vectorOnlySearch(String query, int topK) {
        return vectorStore.searchSegments(query, topK);
    }

    /**
     * Vector-only search returning scores. Used by RAGService for HyDE retrieval so the
     * HyDE hits can be fused with the multi-query hits using a single RRF arithmetic.
     * The score is derived from rank position via the RRF formula applied to a single source.
     */
    public List<ScoredSegment> vectorOnlySearchScored(String query, int topK) {
        List<TextSegment> hits = vectorStore.searchSegments(query, topK);
        List<ScoredSegment> scored = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            double rrfScore = 1.0 / (RRF_RANK_CONSTANT + i + 1);
            scored.add(new ScoredSegment(hits.get(i), rrfScore, query));
        }
        return scored;
    }

    public List<TextSegment> keywordOnlySearch(String query, int topK) {
        return keywordSearch.search(query, topK);
    }

    /**
     * Pure-text RRF, kept for callers/tests that operate on {@code List<TextSegment>}.
     * The scored variant {@link #reciprocalRankFusionScored} is what production code paths
     * should use; this method delegates to it and strips the scores.
     */
    List<TextSegment> reciprocalRankFusion(
            List<TextSegment> list1, List<TextSegment> list2, int maxResults) {
        return reciprocalRankFusionScored(list1, list2, maxResults, null).stream()
                .map(ScoredSegment::segment)
                .toList();
    }

    List<ScoredSegment> reciprocalRankFusionScored(
            List<TextSegment> list1,
            List<TextSegment> list2,
            int maxResults,
            String sourceQuery) {

        Map<String, Double> scores = new HashMap<>();
        Map<String, TextSegment> segmentsByText = new HashMap<>();

        for (int i = 0; i < list1.size(); i++) {
            TextSegment segment = list1.get(i);
            String text = segment.text();
            scores.merge(text, 1.0 / (RRF_RANK_CONSTANT + i + 1), Double::sum);
            segmentsByText.putIfAbsent(text, segment);
        }

        for (int i = 0; i < list2.size(); i++) {
            TextSegment segment = list2.get(i);
            String text = segment.text();
            scores.merge(text, 1.0 / (RRF_RANK_CONSTANT + i + 1), Double::sum);
            segmentsByText.putIfAbsent(text, segment);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> {
                    TextSegment seg = segmentsByText.get(e.getKey());
                    return seg == null ? null : new ScoredSegment(seg, e.getValue(), sourceQuery);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
