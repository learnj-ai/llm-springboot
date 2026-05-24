package com.techcorp.assistant.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Embedding generateEmbedding(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        return response.content();
    }

    /**
     * Batch embeddings — one model call instead of N. The re-ranking and
     * caching paths embed many candidate strings per query; sending them
     * together is significantly cheaper than looping over {@link #generateEmbedding}.
     */
    public List<Embedding> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        Response<List<Embedding>> response = embeddingModel.embedAll(segments);
        return response.content();
    }

    public float[] getVector(String text) {
        return generateEmbedding(text).vector();
    }

    public int dimension() {
        return embeddingModel.dimension();
    }
}
