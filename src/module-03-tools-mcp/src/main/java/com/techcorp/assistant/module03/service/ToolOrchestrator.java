package com.techcorp.assistant.module03.service;

import com.techcorp.assistant.module03.tool.CustomerDataTool;
import com.techcorp.assistant.module03.tool.WeatherTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates tool execution during AI conversations.
 *
 * This service uses Langchain4J's AiServices to automatically handle:
 * 1. Detecting when tools should be invoked based on user queries
 * 2. Executing the appropriate tools
 * 3. Feeding tool results back to the LLM
 * 4. Generating the final natural language response
 */
@Service
public class ToolOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(ToolOrchestrator.class);

    private final Assistant assistant;

    /**
     * Internal interface for the AI assistant with tool support.
     * AiServices automatically handles tool execution flow.
     */
    interface Assistant {
        String chat(String userMessage);
    }

    public ToolOrchestrator(
            ChatModel chatModel,
            CustomerDataTool customerDataTool,
            WeatherTool weatherTool) {

        // Build AI service with automatic tool execution
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(customerDataTool, weatherTool)
                .build();

        log.info("ToolOrchestrator initialized with CustomerDataTool and WeatherTool");
    }

    /**
     * Processes a user message with automatic tool execution.
     *
     * The flow is:
     * 1. User message is sent to the LLM
     * 2. LLM decides if tools are needed and requests execution
     * 3. Tools are executed and results returned to LLM
     * 4. LLM generates final response incorporating tool results
     * 5. Final response is returned to user
     *
     * @param userMessage The user's query
     * @return AI-generated response (potentially augmented with tool data)
     */
    public String processRequest(String userMessage) {
        log.debug("Processing request: {}", userMessage);

        try {
            // AiServices handles the entire tool orchestration flow automatically
            String response = assistant.chat(userMessage);
            log.debug("Generated response: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error processing request", e);
            return "I apologize, but I encountered an error processing your request. Please try again.";
        }
    }
}
