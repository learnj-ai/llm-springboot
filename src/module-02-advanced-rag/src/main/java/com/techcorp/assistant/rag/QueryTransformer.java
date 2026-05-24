package com.techcorp.assistant.rag;

import dev.langchain4j.model.chat.ChatModel;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Transforms user queries to improve retrieval quality.
 * Two techniques:
 * <ul>
 *   <li><b>Multi-Query:</b> generates alternative phrasings to capture different aspects
 *       of the information need, increasing recall.</li>
 *   <li><b>HyDE (Hypothetical Document Embeddings):</b> generates a hypothetical answer
 *       document, then uses its embedding for retrieval — often matches real documents
 *       better than the short query alone.</li>
 * </ul>
 */
@Service
public class QueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(QueryTransformer.class);
    private static final int ALTERNATIVE_QUERY_COUNT = 3;

    private final ChatModel llm;

    public QueryTransformer(ChatModel llm) {
        this.llm = llm;
    }

    public List<String> multiQuery(String originalQuery) {
        try {
            String response = llm.chat(buildMultiQueryPrompt(originalQuery));
            List<String> alternatives = parseAlternativeQueries(response, originalQuery);
            log.debug("╠══ Multi-query generated {} alternatives for: {}", alternatives.size(), originalQuery);
            return alternatives;
        } catch (RuntimeException e) {
            log.warn("╠══ Multi-query generation failed for query: {}", originalQuery, e);
            return List.of();
        }
    }

    public String generateHypotheticalDocument(String query) {
        try {
            String hypothetical = llm.chat(buildHydePrompt(query)).trim();
            if (hypothetical.isBlank()) {
                log.debug("╠══ HyDE returned blank output for query: {}", query);
                return query;
            }
            log.debug("╠══ HyDE generated hypothetical document for: {}", query);
            return hypothetical;
        } catch (RuntimeException e) {
            log.warn("╠══ HyDE generation failed for query: {}", query, e);
            return query;
        }
    }

    private String buildMultiQueryPrompt(String originalQuery) {
        return """
                You are an AI assistant helping to improve search results.
                Given the user query, generate 3 alternative phrasings that capture
                different aspects or perspectives of the same information need.

                Original query: %s

                Return only the 3 alternative queries, one per line. Do not number them.
                """.formatted(originalQuery);
    }

    private String buildHydePrompt(String query) {
        return """
                Given the following question, write a detailed paragraph that would
                contain the answer. This is a hypothetical document - write it as if
                it were an excerpt from an internal knowledge base article.

                Question: %s

                Hypothetical Document:
                """.formatted(query);
    }

    private List<String> parseAlternativeQueries(String response, String originalQuery) {
        Set<String> alternatives = new LinkedHashSet<>();

        Arrays.stream(response.split("\\R"))
                .map(this::normalizeAlternativeQuery)
                .filter(candidate -> !candidate.isBlank())
                .filter(candidate -> !candidate.equalsIgnoreCase(originalQuery.trim()))
                .forEach(candidate -> {
                    if (alternatives.size() >= ALTERNATIVE_QUERY_COUNT || containsIgnoreCase(alternatives, candidate)) {
                        return;
                    }
                    alternatives.add(candidate);
                });

        return List.copyOf(alternatives);
    }

    private String normalizeAlternativeQuery(String candidate) {
        return candidate
                .replaceFirst("^[-*•\\d.\\)\\s]+", "")
                .replaceAll("^\"|\"$", "")
                .trim();
    }

    private boolean containsIgnoreCase(Set<String> values, String candidate) {
        return values.stream().anyMatch(existing -> existing.equalsIgnoreCase(candidate));
    }
}
