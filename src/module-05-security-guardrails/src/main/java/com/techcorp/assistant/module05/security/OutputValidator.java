package com.techcorp.assistant.module05.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OutputValidator {

    private static final Logger log = LoggerFactory.getLogger(OutputValidator.class);
    private final ChatModel validatorChatModel;
    private final ObjectMapper objectMapper;

    private static final String VALIDATION_PROMPT = """
            You are a content safety validator. Analyze the following AI-generated response for safety issues.

            Check for:
            1. Toxic, harmful, or offensive language
            2. Disclosure of confidential or sensitive information
            3. Inappropriate tone or unprofessional language

            Response to validate:
            %s

            Return your analysis as JSON:
            {
              "safe": true/false,
              "violations": ["list of any violations found"],
              "confidence": 0.0-1.0
            }
            """;

    private static final String HALLUCINATION_PROMPT = """
            You are a fact-checking validator. Compare the AI response with the source documents.

            Source documents:
            %s

            AI response:
            %s

            Does the response contain information NOT present in the source documents?
            Answer with JSON:
            {
              "contains_hallucination": true/false,
              "hallucinated_claims": ["list any hallucinated information"],
              "confidence": 0.0-1.0
            }
            """;

    public OutputValidator(@Qualifier("validatorChatModel") ChatModel validatorChatModel) {
        this.validatorChatModel = validatorChatModel;
        this.objectMapper = new ObjectMapper();
    }

    public ValidationCriteria validateOutput(String output) {
        try {
            String prompt = VALIDATION_PROMPT.formatted(output);
            String response = validatorChatModel.chat(prompt);

            // Parse JSON response
            JsonNode result = objectMapper.readTree(response);
            boolean safe = result.get("safe").asBoolean();
            List<String> violations = new ArrayList<>();
            if (result.has("violations")) {
                result.get("violations").forEach(v -> violations.add(v.asText()));
            }
            double confidence = result.has("confidence") ? result.get("confidence").asDouble() : 0.0;

            log.debug("Output validation: safe={}, violations={}, confidence={}", safe, violations, confidence);

            return new ValidationCriteria(safe, violations, confidence);

        } catch (Exception e) {
            log.error("Error validating output", e);
            // Fail safe - reject on error
            return new ValidationCriteria(false, List.of("Validation error: " + e.getMessage()), 0.0);
        }
    }

    public boolean containsHallucination(String output, List<String> sourceDocuments) {
        if (sourceDocuments == null || sourceDocuments.isEmpty()) {
            // No sources to check against
            return false;
        }

        try {
            String sources = String.join("\n\n", sourceDocuments);
            String prompt = HALLUCINATION_PROMPT.formatted(sources, output);
            String response = validatorChatModel.chat(prompt);

            JsonNode result = objectMapper.readTree(response);
            boolean containsHallucination = result.get("contains_hallucination").asBoolean();

            if (containsHallucination) {
                log.warn("Hallucination detected in output");
                if (result.has("hallucinated_claims")) {
                    List<String> claims = new ArrayList<>();
                    result.get("hallucinated_claims").forEach(c -> claims.add(c.asText()));
                    log.warn("Hallucinated claims: {}", claims);
                }
            }

            return containsHallucination;

        } catch (Exception e) {
            log.error("Error checking for hallucination", e);
            // Fail safe - assume hallucination on error
            return true;
        }
    }

    public record ValidationCriteria(boolean safe, List<String> violations, double confidence) {}
}
