package com.techcorp.assistant.module05.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class PromptInjectionGuard {

    private static final Logger log = LoggerFactory.getLogger(PromptInjectionGuard.class);

    // Patterns for common injection attempts.
    //
    // The "ignore .* instructions" pattern accepts one or more qualifiers between
    // "ignore" and "instructions/prompts" — the canonical attacker phrasing is
    // "ignore all previous instructions" (two qualifiers), and the original
    // single-qualifier pattern (`ignore (previous|all|prior) (instructions|prompts)`)
    // did not match that.
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(?:(?:previous|all|prior|the|your|those|these|every)\\s+)+(?:instructions?|prompts?|rules?|directives?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system:\\s*override", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[INST\\].*?\\[/INST\\]", Pattern.DOTALL),
            Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(everything|all|previous|prior|the\\s+above)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(?:(?:previous|all|prior|the|your)\\s+)+(?:instructions?|prompts?|rules?|directives?)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\|im_start\\|>|<\\|im_end\\|>", Pattern.CASE_INSENSITIVE)
    );

    @Value("${security.prompt-injection.max-special-char-ratio:0.30}")
    private double maxSpecialCharRatio;

    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return new ValidationResult(false, "Empty input");
        }

        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                String reason = "Potential prompt injection detected: " + pattern.pattern();
                log.warn("Rejected input - {}", reason);
                return new ValidationResult(false, reason);
            }
        }

        // Check special character ratio
        long specialCharCount = input.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        double ratio = (double) specialCharCount / input.length();

        if (ratio > maxSpecialCharRatio) {
            String reason = String.format("Excessive special characters detected: %.2f%% (threshold: %.2f%%)",
                    ratio * 100, maxSpecialCharRatio * 100);
            log.warn("Rejected input - {}", reason);
            return new ValidationResult(false, reason);
        }

        return new ValidationResult(true, null);
    }

    public String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove HTML/XML tags
        String sanitized = input.replaceAll("<[^>]*>", "");

        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }

    public record ValidationResult(boolean approved, String reason) {
        public boolean isRejected() {
            return !approved;
        }
    }
}
