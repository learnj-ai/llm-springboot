package com.techcorp.assistant.rag;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * Re-ranks a list of candidate segments by relevance to the query.
 * In production, this would use a cross-encoder model (e.g., ms-marco-MiniLM-L-6-v2)
 * that jointly encodes query + document for more accurate relevance scoring.
 */
public interface ReRanker {

    List<TextSegment> rerank(String query, List<TextSegment> candidates, int topK);
}
