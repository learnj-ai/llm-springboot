package com.techcorp.assistant.module06.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LLMConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${OPENAI_API_BASE}")
    private String apiBase;

    @Value("${openai.model.name}")
    private String modelName;

    @Value("${openai.model.embedding}")
    private String embeddingModelName;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(apiBase)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(apiBase)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}
