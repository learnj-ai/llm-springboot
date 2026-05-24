package com.techcorp.assistant.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.similarity.SearchMetric;
import com.techcorp.assistant.store.SearchMatch;
import com.techcorp.assistant.store.VectorStoreService;
import dev.langchain4j.data.document.Metadata;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VectorSearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VectorStoreService stubStore = new VectorStoreService(null, null, null, null) {
            @Override
            protected void initialize() {
                // Skip document loading in tests
            }

            @Override
            public List<SearchMatch> search(String query, int maxResults, SearchMetric metric, ChunkingStrategy strategy) {
                return List.of(
                        new SearchMatch(
                                "Employees can reset their TechCorp password from the identity portal.",
                                0.92,
                                Metadata.metadata("source", "password-reset.md")),
                        new SearchMatch(
                                "Choose Forgot Password, confirm your employee number.",
                                0.85,
                                Metadata.metadata("source", "password-reset.md"))
                );
            }

            @Override
            public int embeddingDimension() {
                return 384;
            }

            @Override
            public int indexedSegmentCount(ChunkingStrategy strategy) {
                return 10;
            }
        };

        VectorSearchController controller = new VectorSearchController(stubStore);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnRelevantVectorMatches() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "query": "How do I reset my password?",
                                  "maxResults": 2,
                                  "metric": "COSINE",
                                  "chunkingStrategy": "RECURSIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingDimension").value(384))
                .andExpect(jsonPath("$.metric").value("COSINE"))
                .andExpect(jsonPath("$.results[0].content").value(org.hamcrest.Matchers.containsString("reset")));
    }

    @Test
    void shouldReturnBadRequestForBlankQuery() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "query": "  "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidEnum() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "query": "test",
                                  "metric": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldApplyDefaultsWhenOptionalFieldsOmitted() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "query": "vpn access",
                                  "maxResults": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("COSINE"))
                .andExpect(jsonPath("$.chunkingStrategy").value("RECURSIVE"))
                .andExpect(jsonPath("$.results").isArray());
    }
}
