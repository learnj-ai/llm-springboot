package com.techcorp.assistant.module04.agent;

import com.techcorp.assistant.module04.tool.CustomerDataTool;
import com.techcorp.assistant.module04.tool.WeatherTool;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReActAgent.
 */
class ReActAgentTest {

    @Test
    void testExtractFinalAnswer() {
        // Given
        ChatModel mockModel = mock(ChatModel.class);
        CustomerDataTool mockCustomerTool = mock(CustomerDataTool.class);
        WeatherTool mockWeatherTool = mock(WeatherTool.class);

        when(mockModel.chat(anyString()))
                .thenReturn("FINAL ANSWER: The weather is sunny.");

        ReActAgent agent = new ReActAgent(mockModel, mockCustomerTool, mockWeatherTool);

        // When
        String result = agent.solve("What's the weather?", 1);

        // Then
        assertThat(result).contains("sunny");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testReActWithRealLLM() {
        // Integration test - requires real API key
        // Would create real agent and test multi-iteration flow
        assertThat(true).isTrue(); // Placeholder
    }
}
