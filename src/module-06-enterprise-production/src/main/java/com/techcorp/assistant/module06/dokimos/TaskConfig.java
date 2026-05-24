package com.techcorp.assistant.module06.dokimos;

import com.techcorp.assistant.module06.service.SimpleRAGService;
import dev.dokimos.core.Task;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Dokimos Task implementation.
 * Defines the task that will be executed for each dataset example.
 */
@Configuration
public class TaskConfig {

    /**
     * RAG evaluation task that executes queries and returns outputs for evaluation.
     * Bridges the SimpleRAGService with Dokimos evaluation framework.
     */
    @Bean
    public Task ragEvaluationTask(SimpleRAGService ragService) {
        return new RAGEvaluationTask(ragService);
    }
}
