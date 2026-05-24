package com.techcorp.assistant.module03.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * MCP (Model Context Protocol) Server Configuration.
 *
 * This configuration registers tools with OpenAI's chat model,
 * making them discoverable and executable by the LLM during conversations.
 */
@Configuration
public class MCPServerConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.model.name:gpt-4o-mini}")
    private String modelName;

    @Value("${OPENAI_API_BASE}")
    private String apiBase;

    /**
     * Creates a chat model bean for use throughout the application.
     *
     * Tools will be registered with this model via the ToolOrchestrator service.
     *
     * @return Configured ChatModel (OpenAI implementation)
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName)
                .baseUrl(apiBase)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
