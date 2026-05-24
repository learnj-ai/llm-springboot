package com.techcorp.assistant.module03.controller;

import com.techcorp.assistant.module03.service.ToolOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for AssistantController.
 *
 * <p>Spring Boot 4 removed {@code @WebMvcTest} and {@code @MockBean}. We instead build
 * a standalone {@link MockMvc} around a controller instance wired with Mockito mocks —
 * no Spring context is loaded, which is much faster and avoids the workshop's full-context
 * boot requirements (PostgreSQL, OpenAI key, etc.) just to assert HTTP wiring.
 */
class AssistantControllerTest {

    private ToolOrchestrator toolOrchestrator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        toolOrchestrator = mock(ToolOrchestrator.class);
        AssistantController controller = new AssistantController(toolOrchestrator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/assistant/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Module 03: Tools & MCP - OK"));
    }

    @Test
    void testChatEndpoint_Success() throws Exception {
        when(toolOrchestrator.processRequest(anyString()))
                .thenReturn("Customer Alice Johnson has email alice.johnson@example.com");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "message": "What is customer 12345's email?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Customer Alice Johnson has email alice.johnson@example.com"));
    }

    @Test
    void testChatEndpoint_OrchestratorFailure() throws Exception {
        // The controller catches Exception and returns 500 with a friendly response.
        // (The previous version of this test asserted 4xx for an empty body — that
        // assertion only held under the now-removed `@WebMvcTest` Bean Validation
        // path, which never matched the controller's actual behaviour. The new
        // assertion exercises what the controller actually does on a downstream
        // failure.)
        when(toolOrchestrator.processRequest(anyString()))
                .thenThrow(new RuntimeException("simulated tool failure"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "message": "anything"
                                }
                                """))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.response").value("An error occurred processing your request"));
    }
}
