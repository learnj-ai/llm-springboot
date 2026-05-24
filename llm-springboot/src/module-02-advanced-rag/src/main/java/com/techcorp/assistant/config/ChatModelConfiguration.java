package com.techcorp.assistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatModelConfiguration {

    @Bean
    ChatModel chatModel(
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.model-name:gpt-4o-mini}") String modelName,
            @Value("${langchain4j.open-ai.api-base:}") String apiBase) {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);

        if (apiBase != null && !apiBase.isBlank()) {
            builder.baseUrl(apiBase);
        }

        return builder.build();
    }
}
