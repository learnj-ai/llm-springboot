package com.techcorp.assistant.search;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.similarity.SearchMetric;
import java.util.List;

public record SearchResponse(
        int embeddingDimension,
        SearchMetric metric,
        ChunkingStrategy chunkingStrategy,
        int indexedSegmentCount,
        List<SearchResult> results) {
}
