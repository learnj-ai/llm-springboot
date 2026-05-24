package com.techcorp.assistant.module05.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PIIMaskingServiceTest {

    private PIIMaskingService service;

    @BeforeEach
    void setUp() {
        service = new PIIMaskingService();
    }

    @Test
    void testMaskEmailAddress() {
        String text = "Contact me at john.doe@example.com for more info";
        String masked = service.maskPII(text);

        assertFalse(masked.contains("john.doe@example.com"));
        assertTrue(masked.contains("[EMAIL_REDACTED]"));
    }

    @Test
    void testMaskPhoneNumber() {
        String text = "Call me at 555-123-4567";
        String masked = service.maskPII(text);

        assertFalse(masked.contains("555-123-4567"));
        assertTrue(masked.contains("[PHONE_REDACTED]"));
    }

    @Test
    void testMaskSSN() {
        String text = "My SSN is 123-45-6789";
        String masked = service.maskPII(text);

        assertFalse(masked.contains("123-45-6789"));
        assertTrue(masked.contains("[SSN_REDACTED]"));
    }

    @Test
    void testMaskCreditCard() {
        String text = "Card number: 1234-5678-9012-3456";
        String masked = service.maskPII(text);

        assertFalse(masked.contains("1234-5678-9012-3456"));
        assertTrue(masked.contains("[CARD_REDACTED]"));
    }

    @Test
    void testDetectMultiplePIITypes() {
        String text = "Email: user@test.com, Phone: 555-123-4567";
        PIIMaskingService.PIIDetectionResult result = service.detectPII(text);

        assertTrue(result.containsPII());
        assertEquals(2, result.matches().size());

        long emailCount = result.matches().stream()
                .filter(m -> m.type().equals("EMAIL"))
                .count();
        long phoneCount = result.matches().stream()
                .filter(m -> m.type().equals("PHONE"))
                .count();

        assertEquals(1, emailCount);
        assertEquals(1, phoneCount);
    }

    @Test
    void testNoPIIDetected() {
        String text = "This is a clean message with no PII";
        PIIMaskingService.PIIDetectionResult result = service.detectPII(text);

        assertFalse(result.containsPII());
        assertTrue(result.matches().isEmpty());
    }

    @Test
    void testPIIMatchIncludesPosition() {
        String text = "Contact: john@example.com";
        PIIMaskingService.PIIDetectionResult result = service.detectPII(text);

        assertTrue(result.containsPII());
        assertEquals(1, result.matches().size());

        PIIMaskingService.PIIMatch match = result.matches().get(0);
        assertEquals("EMAIL", match.type());
        assertEquals("john@example.com", match.value());
        assertTrue(match.start() >= 0);
        assertTrue(match.end() > match.start());
    }
}
