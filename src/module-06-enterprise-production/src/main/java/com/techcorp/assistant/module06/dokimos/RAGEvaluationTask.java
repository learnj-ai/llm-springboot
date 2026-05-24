package com.techcorp.assistant.module06.dokimos;

import com.techcorp.assistant.module06.service.SimpleRAGService;
import dev.dokimos.core.Example;
import dev.dokimos.core.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Dokimos Task implementation that executes RAG queries for evaluation.
 * Bridges the SimpleRAGService with Dokimos evaluation framework.
 */
public class RAGEvaluationTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(RAGEvaluationTask.class);

    private final SimpleRAGService ragService;

    public RAGEvaluationTask(SimpleRAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * Executes the RAG query from the example and returns outputs for evaluation.
     *
     * @param example the dataset example containing the query input
     * @return map containing the response output and context for evaluators
     */
    @Override
    public Map<String, Object> run(Example example) {
        String query = example.input();

        if (query == null || query.isBlank()) {
            log.warn("Empty query in example, returning empty output");
            return Map.of(
                    "output", "",
                    "context", "",
                    "error", "Empty or null query"
            );
        }

        try {
            log.debug("Executing RAG query: {}", query);

            // Execute RAG service query
            SimpleRAGService.RAGResponse response = ragService.query(query);

            // Build context from source documents for evaluators
            String context = String.join("\n\n", response.sourceDocuments());

            // Return outputs for evaluation
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("output", response.response());
            outputs.put("context", context);
            outputs.put("source_count", response.sourceDocuments().size());

            log.debug("RAG query completed successfully");
            return outputs;

        } catch (Exception e) {
            log.error("Error executing RAG query: {}", query, e);

            // Return error information for evaluation tracking
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("output", "");
            outputs.put("context", "");
            outputs.put("error", e.getMessage());
            outputs.put("error_type", e.getClass().getSimpleName());

            return outputs;
        }
    }
}
