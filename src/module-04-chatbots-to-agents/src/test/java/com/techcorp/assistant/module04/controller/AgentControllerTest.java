package com.techcorp.assistant.module04.controller;

import com.techcorp.assistant.module04.agent.ReActAgent;
import com.techcorp.assistant.module04.memory.ConversationMemoryService;
import com.techcorp.assistant.module04.orchestrator.MultiAgentOrchestrator;
import com.techcorp.assistant.module04.service.TaskDecomposer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for AgentController.
 *
 * <p>Spring Boot 4 removed {@code @WebMvcTest} and {@code @MockBean}; this test now
 * uses {@link MockMvcBuilders#standaloneSetup} against a directly-instantiated
 * controller wired with Mockito mocks. No Spring context is loaded.
 */
class AgentControllerTest {

    private ReActAgent reActAgent;
    private MultiAgentOrchestrator multiAgentOrchestrator;
    private TaskDecomposer taskDecomposer;
    private ConversationMemoryService memoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reActAgent = mock(ReActAgent.class);
        multiAgentOrchestrator = mock(MultiAgentOrchestrator.class);
        taskDecomposer = mock(TaskDecomposer.class);
        memoryService = mock(ConversationMemoryService.class);
        // History defaults: getHistory returns an empty list so the controller's
        // renderHistory path produces an empty prefix and the agent sees only the
        // current user message.
        when(memoryService.getHistory(anyString())).thenReturn(List.of());

        AgentController controller = new AgentController(
                reActAgent, multiAgentOrchestrator, taskDecomposer, memoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/agent/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Module 04: Agents - OK"));
    }

    @Test
    void testExecuteEndpoint_ReActMode() throws Exception {
        when(reActAgent.solve(anyString())).thenReturn("Agent response");

        mockMvc.perform(post("/api/v1/agent/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "message": "Test question",
                                    "mode": "react"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Agent response"))
                .andExpect(jsonPath("$.mode").value("react"));
    }

    @Test
    void testClearSession() throws Exception {
        mockMvc.perform(delete("/api/v1/agent/session/test-session"))
                .andExpect(status().isOk())
                .andExpect(content().string("Session cleared"));
    }
}
