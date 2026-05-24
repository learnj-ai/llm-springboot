package com.techcorp.assistant.module04.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * External API tool for fetching weather information.
 * Demonstrates integration with third-party REST APIs.
 *
 * Note: For workshop purposes, this uses a simplified mock implementation.
 * In production, you would integrate with OpenWeatherMap or similar API.
 */
@Component
public class WeatherTool {
    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);
    private final RestTemplate restTemplate;

    @Value("${weather.api.key:demo}")
    private String apiKey;

    public WeatherTool() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Retrieves current weather for a specified city.
     *
     * @param city The city name to get weather for
     * @return Current weather description and temperature
     */
    @Tool("Retrieves current weather information for a specified city including temperature and conditions")
    public String getCurrentWeather(@P("The city name to get weather for") String city) {
        log.debug("Tool invoked: getCurrentWeather({})", city);

        try {
            // For workshop purposes, using a simplified mock response
            // In production, replace with actual OpenWeatherMap API call:
            // String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, apiKey);
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // Mock weather data for common cities
            String weatherInfo = switch (city.toLowerCase().trim()) {
                case "boston" -> """
                    Current Weather in Boston:
                    - Temperature: 18°C (64°F)
                    - Conditions: Partly cloudy
                    - Humidity: 65%
                    - Wind: 12 km/h NE
                    """;
                case "new york", "nyc" -> """
                    Current Weather in New York:
                    - Temperature: 22°C (72°F)
                    - Conditions: Sunny
                    - Humidity: 55%
                    - Wind: 8 km/h SW
                    """;
                case "san francisco" -> """
                    Current Weather in San Francisco:
                    - Temperature: 16°C (61°F)
                    - Conditions: Foggy
                    - Humidity: 80%
                    - Wind: 15 km/h W
                    """;
                case "seattle" -> """
                    Current Weather in Seattle:
                    - Temperature: 14°C (57°F)
                    - Conditions: Light rain
                    - Humidity: 85%
                    - Wind: 10 km/h S
                    """;
                case "chicago" -> """
                    Current Weather in Chicago:
                    - Temperature: 20°C (68°F)
                    - Conditions: Clear
                    - Humidity: 60%
                    - Wind: 18 km/h NW
                    """;
                default -> String.format("""
                    Current Weather in %s:
                    - Temperature: 19°C (66°F)
                    - Conditions: Partly cloudy
                    - Humidity: 62%%
                    - Wind: 10 km/h W
                    (Note: Using sample data for workshop. In production, integrate with OpenWeatherMap API.)
                    """, city);
            };

            return weatherInfo;

        } catch (Exception e) {
            log.error("Error fetching weather for city: {}", city, e);
            return "Unable to retrieve weather information at this time. The weather service may be temporarily unavailable.";
        }
    }
}
