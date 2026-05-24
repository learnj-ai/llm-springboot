package com.techcorp.assistant.module04.agent;

import com.techcorp.assistant.module04.tool.CustomerDataTool;
import com.techcorp.assistant.module04.tool.WeatherTool;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct (Reason + Act) Agent Implementation.
 *
 * This agent uses an iterative loop of:
 * 1. THOUGHT: Reasoning about what to do next
 * 2. ACTION: Executing a tool or action
 * 3. OBSERVATION: Receiving the result
 * 4. Repeat until reaching a FINAL ANSWER
 */
@Service
public class ReActAgent {
    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    private static final String REACT_PROMPT = """
            You are a helpful AI assistant that uses tools to answer questions.

            You have access to the following tools:
            - getCustomerInfo(customerId): Get customer details by ID
            - searchTickets(status): Search support tickets (open/pending/closed)
            - getCurrentWeather(city): Get current weather for a city

            For each step, use this exact format:

            THOUGHT: [your reasoning about what to do]
            ACTION: [tool_name(parameters)]

            After receiving an observation, either:
            - Continue with another THOUGHT/ACTION if you need more information
            - Or provide the FINAL ANSWER when you have enough information

            FINAL ANSWER: [your complete answer to the user]

            Question: {question}

            {history}
            """;

    private final ChatModel chatModel;
    private final CustomerDataTool customerDataTool;
    private final WeatherTool weatherTool;

    @Value("${agent.react.max-iterations:5}")
    private int maxIterations;

    public ReActAgent(
            ChatModel chatModel,
            CustomerDataTool customerDataTool,
            WeatherTool weatherTool) {
        this.chatModel = chatModel;
        this.customerDataTool = customerDataTool;
        this.weatherTool = weatherTool;
    }

    /**
     * Solves a problem using the ReAct pattern.
     *
     * @param question The user's question
     * @return The final answer after iterative reasoning
     */
    public String solve(String question) {
        return solve(question, maxIterations);
    }

    /**
     * Solves a problem using the ReAct pattern with custom iteration limit.
     *
     * @param question The user's question
     * @param maxIter Maximum number of thought-action-observation cycles
     * @return The final answer
     */
    public String solve(String question, int maxIter) {
        log.info("Starting ReAct agent to solve: {}", question);

        StringBuilder history = new StringBuilder();
        String currentPrompt = REACT_PROMPT
                .replace("{question}", question)
                .replace("{history}", "");

        for (int iteration = 1; iteration <= maxIter; iteration++) {
            log.debug("Iteration {}/{}", iteration, maxIter);

            // Generate thought and action
            String response = chatModel.chat(currentPrompt);
            log.debug("LLM Response:\n{}", response);

            // Check if we have a final answer
            if (response.contains("FINAL ANSWER:")) {
                String finalAnswer = extractFinalAnswer(response);
                log.info("Reached final answer after {} iterations", iteration);
                return finalAnswer;
            }

            // Extract thought and action
            String thought = extractThought(response);
            String action = extractAction(response);

            if (action == null || action.isBlank()) {
                log.warn("No valid action found in iteration {}", iteration);
                continue;
            }

            log.debug("THOUGHT: {}", thought);
            log.debug("ACTION: {}", action);

            // Execute the action
            String observation = executeAction(action);
            log.debug("OBSERVATION: {}", observation);

            // Update history
            history.append("THOUGHT: ").append(thought).append("\n");
            history.append("ACTION: ").append(action).append("\n");
            history.append("OBSERVATION: ").append(observation).append("\n\n");

            // Update prompt with history
            currentPrompt = REACT_PROMPT
                    .replace("{question}", question)
                    .replace("{history}", history.toString());
        }

        // Reached max iterations without final answer
        log.warn("Reached max iterations ({}) without final answer", maxIter);
        return "I've analyzed the question but need more iterations to provide a complete answer. " +
               "Here's what I found:\n\n" + history.toString();
    }

    /**
     * Extracts the thought from the LLM response.
     */
    private String extractThought(String response) {
        Pattern pattern = Pattern.compile("THOUGHT:\\s*(.+?)(?=ACTION:|FINAL ANSWER:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * Extracts the action from the LLM response.
     * Expected format: ACTION: tool_name(parameters)
     */
    private String extractAction(String response) {
        Pattern pattern = Pattern.compile("ACTION:\\s*(.+?)(?=OBSERVATION:|THOUGHT:|FINAL ANSWER:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts the final answer from the LLM response.
     */
    private String extractFinalAnswer(String response) {
        Pattern pattern = Pattern.compile("FINAL ANSWER:\\s*(.+)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return response;
    }

    /**
     * Executes a tool action and returns the observation.
     *
     * @param action The action string (e.g., "getCustomerInfo(12345)")
     * @return The tool execution result
     */
    private String executeAction(String action) {
        try {
            // Parse action format: tool_name(parameters)
            Pattern pattern = Pattern.compile("(\\w+)\\(([^)]*)\\)");
            Matcher matcher = pattern.matcher(action.trim());

            if (!matcher.find()) {
                return "Error: Invalid action format. Use tool_name(parameters)";
            }

            String toolName = matcher.group(1);
            String parameters = matcher.group(2).replaceAll("['\"]", "").trim();

            log.debug("Executing tool: {} with parameters: {}", toolName, parameters);

            return switch (toolName) {
                case "getCustomerInfo" -> customerDataTool.getCustomerInfo(parameters);
                case "searchTickets" -> customerDataTool.searchTickets(parameters);
                case "getCurrentWeather" -> weatherTool.getCurrentWeather(parameters);
                default -> "Error: Unknown tool: " + toolName;
            };

        } catch (Exception e) {
            log.error("Error executing action: {}", action, e);
            return "Error executing action: " + e.getMessage();
        }
    }
}
