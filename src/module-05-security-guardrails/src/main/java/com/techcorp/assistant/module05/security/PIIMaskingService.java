package com.techcorp.assistant.module05.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PIIMaskingService {

    private static final Logger log = LoggerFactory.getLogger(PIIMaskingService.class);

    // PII patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+?1[-.]?)?\\(?([0-9]{3})\\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\\b"
    );
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b([0-9]{3})-([0-9]{2})-([0-9]{4})\\b"
    );
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b([0-9]{4})[\\s-]?([0-9]{4})[\\s-]?([0-9]{4})[\\s-]?([0-9]{4})\\b"
    );

    public String maskPII(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;

        // Mask emails
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("[EMAIL_REDACTED]");

        // Mask phones
        masked = PHONE_PATTERN.matcher(masked).replaceAll("[PHONE_REDACTED]");

        // Mask SSNs
        masked = SSN_PATTERN.matcher(masked).replaceAll("[SSN_REDACTED]");

        // Mask credit cards
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll("[CARD_REDACTED]");

        if (!masked.equals(text)) {
            log.debug("PII masked in text");
        }

        return masked;
    }

    public PIIDetectionResult detectPII(String text) {
        if (text == null || text.isBlank()) {
            return new PIIDetectionResult(false, List.of());
        }

        List<PIIMatch> matches = new ArrayList<>();

        // Detect emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            matches.add(new PIIMatch(
                    "EMAIL",
                    emailMatcher.group(),
                    emailMatcher.start(),
                    emailMatcher.end()
            ));
        }

        // Detect phones
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            matches.add(new PIIMatch(
                    "PHONE",
                    phoneMatcher.group(),
                    phoneMatcher.start(),
                    phoneMatcher.end()
            ));
        }

        // Detect SSNs
        Matcher ssnMatcher = SSN_PATTERN.matcher(text);
        while (ssnMatcher.find()) {
            matches.add(new PIIMatch(
                    "SSN",
                    ssnMatcher.group(),
                    ssnMatcher.start(),
                    ssnMatcher.end()
            ));
        }

        // Detect credit cards
        Matcher cardMatcher = CREDIT_CARD_PATTERN.matcher(text);
        while (cardMatcher.find()) {
            matches.add(new PIIMatch(
                    "CREDIT_CARD",
                    cardMatcher.group(),
                    cardMatcher.start(),
                    cardMatcher.end()
            ));
        }

        boolean containsPII = !matches.isEmpty();
        if (containsPII) {
            log.info("Detected {} PII instances in text", matches.size());
        }

        return new PIIDetectionResult(containsPII, matches);
    }

    public record PIIMatch(String type, String value, int start, int end) {}

    public record PIIDetectionResult(boolean containsPII, List<PIIMatch> matches) {}
}
