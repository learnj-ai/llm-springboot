package com.techcorp.assistant.rag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RAGControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Stub RAGService that returns a fixed answer with one source so the
        // controller's new rich-response wiring is exercised end-to-end.
        RAGService ragService = new RAGService(null, null, null) {
            @Override
            public RAGService.RagAnswer queryWithSources(String userQuestion, boolean useQueryExpansion) {
                return new RAGService.RagAnswer(
                        "You can reset your password from the identity portal.",
                        List.of(new RAGService.Source(
                                1,
                                "password-reset.md",
                                "Employees can reset their TechCorp password from the identity portal.",
                                0.15,
                                userQuestion)),
                        List.of(userQuestion),
                        42L);
            }
        };

        // Stub HybridSearchService (not used by /query endpoint tests)
        HybridSearchService hybridSearchService = new HybridSearchService(null, null, null);

        RAGController controller = new RAGController(ragService, hybridSearchService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void queryReturnsAnswerAndSources() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {"question": "How do I reset my password?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You can reset your password from the identity portal."))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].number").value(1))
                .andExpect(jsonPath("$.sources[0].title").value("password-reset.md"))
                .andExpect(jsonPath("$.sources[0].score").value(0.15))
                .andExpect(jsonPath("$.transformedQueries[0]").value("How do I reset my password?"))
                .andExpect(jsonPath("$.elapsedMs").value(42));
    }

    @Test
    void queryRejectsMissingQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryRejectsMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }
}
