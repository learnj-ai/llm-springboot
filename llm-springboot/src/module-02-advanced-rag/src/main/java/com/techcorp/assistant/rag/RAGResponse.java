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
 * RRF score, and the combined retriever/query provenance for every shard that surfaced
 * the chunk. A UI can map a {@code [Source 3]} citation in {@link #answer()} back to
 * {@code sources[2]} (zero-indexed) to render attribution.
 *
 * <p>{@code transformedQueries} is the list of strings actually run through retrieval:
 * the original question, followed by the multi-query alternatives, followed by a
 * {@code "[HyDE] ..."} preview of the hypothetical document (truncated) when HyDE fired.
 * Useful for showing learners (or auditors) what query expansion actually produced.
 *
 * <p>{@code elapsedMs} is the wall-clock time the pipeline spent. Cheap diagnostic that
 * keeps observability available without enabling debug logging.
 *
 * <p>{@code status} surfaces the {@link RAGService.RagStatus} so a client can distinguish a
 * grounded answer from a degraded response without parsing the {@code answer} string:
 * {@code ANSWERED}, {@code INSUFFICIENT_CONTEXT}, {@code RETRIEVAL_FAILED},
 * {@code GENERATION_FAILED}, or {@code CANCELLED}. {@code ANSWERED} is the only status
 * for which {@code sources} is guaranteed populated and the {@code answer} reflects the
 * model's output.
 *
 * <p>Older clients reading only {@link #answer()} keep working unchanged because Jackson
 * tolerates extra fields by default.
 */
public record RAGResponse(
        String answer,
        List<RAGService.Source> sources,
        List<String> transformedQueries,
        long elapsedMs,
        RAGService.RagStatus status) {

    /** Convenience constructor for legacy answer-only callers. */
    public RAGResponse(String answer) {
        this(answer, List.of(), List.of(), 0L, RAGService.RagStatus.INSUFFICIENT_CONTEXT);
    }
}
