package com.techcorp.assistant.rag;

import com.techcorp.assistant.embeddings.EmbeddingService;
import com.techcorp.assistant.similarity.SimilarityCalculator;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Re-ranks candidates using cosine similarity between query and candidate embeddings.
 * This is a bi-encoder approach — query and document are encoded independently.
 * <p>
 * A true cross-encoder (like ms-marco-MiniLM-L-6-v2) would encode the query-document
 * pair jointly, producing more accurate relevance scores at the cost of being slower.
 * The bi-encoder re-ranker here demonstrates the concept while keeping Module 02
 * free of external model downloads beyond the embedding model.
 */
@Component
public class EmbeddingBasedReRanker implements ReRanker {

    private final EmbeddingService embeddingService;
    private final SimilarityCalculator similarityCalculator;

    public EmbeddingBasedReRanker(EmbeddingService embeddingService, SimilarityCalculator similarityCalculator) {
        this.embeddingService = embeddingService;
        this.similarityCalculator = similarityCalculator;
    }

    @Override
    public List<TextSegment> rerank(String query, List<TextSegment> candidates, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Embedding queryEmbedding = embeddingService.generateEmbedding(query);

        record ScoredCandidate(TextSegment segment, double score) {}

        return candidates.stream()
                .map(candidate -> {
                    Embedding candidateEmbedding = embeddingService.generateEmbedding(candidate.text());
                    double score = similarityCalculator.cosineSimilarity(
                            queryEmbedding.vector(), candidateEmbedding.vector());
                    return new ScoredCandidate(candidate, score);
                })
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .limit(topK)
                .map(ScoredCandidate::segment)
                .toList();
    }
}
