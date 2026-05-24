package com.techcorp.assistant.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
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

    public float[] getVector(String text) {
        return generateEmbedding(text).vector();
    }

    public int dimension() {
        return embeddingModel.dimension();
    }
}
