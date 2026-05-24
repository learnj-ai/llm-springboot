package com.techcorp.assistant.module03.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WeatherTool.
 */
class WeatherToolTest {

    private WeatherTool weatherTool;

    @BeforeEach
    void setUp() {
        weatherTool = new WeatherTool();
    }

    @Test
    void testGetCurrentWeather_Boston() {
        // When
        String result = weatherTool.getCurrentWeather("Boston");

        // Then
        assertThat(result).contains("Boston");
        assertThat(result).contains("Temperature");
        assertThat(result).contains("Conditions");
    }

    @Test
    void testGetCurrentWeather_CaseInsensitive() {
        // When
        String result = weatherTool.getCurrentWeather("SEATTLE");

        // Then
        assertThat(result).contains("Seattle");
        assertThat(result).contains("rain");
    }

    @Test
    void testGetCurrentWeather_UnknownCity() {
        // When
        String result = weatherTool.getCurrentWeather("UnknownCity123");

        // Then
        assertThat(result).contains("UnknownCity123");
        assertThat(result).contains("Temperature");
        assertThat(result).contains("sample data");
    }

    @Test
    void testGetCurrentWeather_HandlesErrors() {
        // When - even with empty/null, should handle gracefully
        String result = weatherTool.getCurrentWeather("");

        // Then
        assertThat(result).isNotEmpty();
    }
}
