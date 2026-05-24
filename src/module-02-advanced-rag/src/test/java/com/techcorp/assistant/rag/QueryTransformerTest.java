package com.techcorp.assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryTransformerTest {

    @Test
    void multiQueryNormalizesAndDeduplicatesModelOutput() {
        StubChatModel llm = new StubChatModel("""
                1. How can I reset my password?
                - Password reset steps for employees
                How do I reset my password?
                "Forgot password process in TechCorp"
                Password reset steps for employees
                """);
        QueryTransformer queryTransformer = new QueryTransformer(llm);

        List<String> alternatives = queryTransformer.multiQuery("How do I reset my password?");

        assertThat(alternatives).containsExactly(
                "How can I reset my password?",
                "Password reset steps for employees",
                "Forgot password process in TechCorp");
    }

    @Test
    void multiQueryReturnsEmptyListWhenModelCallFails() {
        QueryTransformer queryTransformer = new QueryTransformer(new StubChatModel(new RuntimeException("LLM unavailable")));

        List<String> alternatives = queryTransformer.multiQuery("How do I reset my password?");

        assertThat(alternatives).isEmpty();
    }

    @Test
    void generateHypotheticalDocumentFallsBackToOriginalQueryWhenModelReturnsBlank() {
        QueryTransformer queryTransformer = new QueryTransformer(new StubChatModel("   "));

        String hypothetical = queryTransformer.generateHypotheticalDocument("How do I reset my password?");

        assertThat(hypothetical).isEqualTo("How do I reset my password?");
    }

    private static final class StubChatModel implements ChatModel {

        private final String response;
        private final RuntimeException failure;

        private StubChatModel(String response) {
            this.response = response;
            this.failure = null;
        }

        private StubChatModel(RuntimeException failure) {
            this.response = null;
            this.failure = failure;
        }

        @Override
        public String chat(String userMessage) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
