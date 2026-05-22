package com.techcorp.assistant.rag;

import java.util.List;

/**
 * REST response for {@code POST /api/v1/rag/query}.
 *
 * <p>{@code answer} is the natural-language answer the LLM produced.
 *
 * <p>{@code sources} mirrors {@link RAGService.RagAnswer#sources()}: one entry per
 * {@code [Source N]} block injected into the prompt, in the post-fusion ranked order.
 * Each entry exposes the chunk text, the document title from metadata, the cross-variant
 * RRF score, and the query variant that first surfaced the chunk. A UI can map a
 * {@code [Source 3]} citation in {@link #answer()} back to {@code sources[2]} (zero-indexed)
 * to render attribution.
 *
 * <p>{@code transformedQueries} is the list of strings actually run through retrieval:
 * the original question, followed by the multi-query alternatives, followed by a
 * {@code "[HyDE] ..."} preview of the hypothetical document (truncated) when HyDE fired.
 * Useful for showing learners (or auditors) what query expansion actually produced.
 *
 * <p>{@code elapsedMs} is the wall-clock time the pipeline spent. Cheap diagnostic that
 * keeps observability available without enabling debug logging.
 *
 * <p>Older clients reading only {@link #answer()} keep working unchanged because Jackson
 * tolerates extra fields by default.
 */
public record RAGResponse(
        String answer,
        List<RAGService.Source> sources,
        List<String> transformedQueries,
        long elapsedMs) {

    /** Convenience constructor for the legacy answer-only shape (used by error paths). */
    public RAGResponse(String answer) {
        this(answer, List.of(), List.of(), 0L);
    }
}
