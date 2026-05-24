package com.techcorp.assistant.search;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.similarity.SearchMetric;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank String query,
        @Min(1) @Max(20) int maxResults,
        SearchMetric metric,
        ChunkingStrategy chunkingStrategy) {

    public SearchRequest {
        maxResults = (maxResults == 0) ? 5 : maxResults;
        metric = (metric == null) ? SearchMetric.COSINE : metric;
        chunkingStrategy = (chunkingStrategy == null) ? ChunkingStrategy.RECURSIVE : chunkingStrategy;
    }
}
