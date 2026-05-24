package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.JudgeLM;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dokimos judge-model wiring.
 *
 * <p>Originally this class wired the judge through {@code SpringAiSupport.asJudge(ChatModel)}
 * with Spring AI's {@code OpenAiChatModel}. Spring AI 1.0.0-M5's autoconfig pulls in
 * Spring Framework 6 internals that were removed/renamed in Spring Framework 7
 * (e.g. {@code HttpHeaders.addAll(MultiValueMap)} signature changed, and
 * {@code org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration}
 * was deleted) — so on Spring Boot 4 the context refresh blows up. There is no released
 * Spring AI version compatible with Spring Boot 4 / Spring Framework 7 at this time.
 *
 * <p>{@link JudgeLM} is a single-method functional interface
 * ({@code String generate(String)}), so we can adapt directly from LangChain4J's
 * {@link ChatModel} and skip Spring AI entirely. The rest of Module 06 (including
 * the primary chat path in {@link com.techcorp.assistant.module06.config.LLMConfig})
 * already uses LangChain4J, so this also makes the LLM-client stack consistent.
 */
@Configuration
public class DokimosEvaluationConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${dokimos.judge.model:gpt-4o}")
    private String judgeModelName;

    @Value("${dokimos.judge.temperature:0.0}")
    private Double judgeTemperature;

    @Value("${dokimos.judge.timeout-seconds:60}")
    private long timeoutSeconds;

    /**
     * LangChain4J ChatModel sized for the judge role (low temperature, longer timeout).
     */
    @Bean("dokimosJudgeChatModel")
    public ChatModel dokimosJudgeChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(judgeModelName)
                .temperature(judgeTemperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Dokimos {@link JudgeLM} backed by the LangChain4J judge model.
     */
    @Bean
    public JudgeLM judgeLM(ChatModel dokimosJudgeChatModel) {
        return dokimosJudgeChatModel::chat;
    }
}
