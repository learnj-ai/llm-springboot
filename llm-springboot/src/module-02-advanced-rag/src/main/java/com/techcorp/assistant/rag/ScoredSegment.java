package com.techcorp.assistant.rag;

import dev.langchain4j.data.segment.TextSegment;

/**
 * A {@link TextSegment} paired with its retrieval score.
 *
 * <p>{@link HybridSearchService} computes Reciprocal Rank Fusion (RRF) scores when merging
 * vector and keyword results, but the legacy {@code hybridSearch(...) -> List<TextSegment>}
 * API dropped those scores at the boundary. Downstream consumers like {@link RAGService}
 * then had no way to do cross-query rank fusion, so the final "top-K" was just
 * insertion-order-first-N, regardless of how strongly any one chunk was ranked.
 *
 * <p>This record threads the score through so callers can fuse rankings from multiple
 * query variants (multi-query expansion + HyDE) into a single ranked list with explicit
 * score arithmetic instead of relying on insertion order.
 *
 * <p>The {@code sourceQuery} field is the query string that produced this hit. It's not
 * used for scoring but is useful for diagnostics and for weighting fusion across query
 * variants (e.g. preferring hits from the user's original phrasing over hits from an
 * LLM-generated alternative).
 */
public record ScoredSegment(TextSegment segment, double score, String sourceQuery) {
}
