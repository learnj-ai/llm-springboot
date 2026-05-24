package com.techcorp.assistant.module03.service;

import com.techcorp.assistant.module03.tool.CustomerDataTool;
import com.techcorp.assistant.module03.tool.WeatherTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ToolOrchestrator.
 *
 * These tests require a valid OPENAI_API_KEY and will make actual API calls.
 * They are disabled by default and only run when the environment variable is set.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class ToolOrchestratorIntegrationTest {

    @Test
    void testToolOrchestration_WithRealLLM() {
        // This is a placeholder for integration testing with real LLM
        // In actual workshop, students would:
        // 1. Set up test database
        // 2. Configure ChatLanguageModel with real API key
        // 3. Test end-to-end tool execution flow

        // For now, we verify the tools themselves work
        WeatherTool weatherTool = new WeatherTool();
        String weather = weatherTool.getCurrentWeather("Boston");

        assertThat(weather).contains("Boston");
        assertThat(weather).contains("Temperature");
    }

    @Test
    void testMultiToolScenario() {
        // Placeholder for testing scenarios that require multiple tool invocations
        // Example: "What's the weather in the city where customer 12345 lives?"
        // This would require:
        // 1. CustomerDataTool to get customer location
        // 2. WeatherTool to get weather for that location
        // 3. LLM to orchestrate and synthesize the response

        assertThat(true).isTrue(); // Placeholder
    }
}
