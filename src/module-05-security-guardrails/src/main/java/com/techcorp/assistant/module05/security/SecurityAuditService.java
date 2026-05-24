package com.techcorp.assistant.module05.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${security.audit.redis-key:security-events}")
    private String redisKey;

    @Value("${security.audit.max-events:1000}")
    private int maxEvents;

    public SecurityAuditService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Register the JSR-310 module so `Instant` (used for `AuditEvent.timestamp`) serializes
        // as an ISO-8601 string instead of throwing `InvalidDefinitionException` — without this,
        // every audit write fails silently into the catch below and Redis never sees the events.
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void logSecurityEvent(SecurityEvent event) {
        AuditEvent auditEvent = new AuditEvent(
                UUID.randomUUID().toString(),
                event.type(),
                event.severity(),
                event.userId(),
                Instant.now(),
                event.details()
        );

        // Log to application logs
        String logMessage = String.format("Security Event: type=%s, severity=%s, userId=%s, details=%s",
                auditEvent.type(), auditEvent.severity(), auditEvent.userId(), auditEvent.details());

        switch (event.severity()) {
            case CRITICAL, HIGH -> log.error(logMessage);
            case MEDIUM -> log.warn(logMessage);
            case LOW -> log.info(logMessage);
        }

        // Store in Redis
        try {
            String eventJson = objectMapper.writeValueAsString(auditEvent);
            redisTemplate.opsForList().leftPush(redisKey, eventJson);

            // Trim to max events
            redisTemplate.opsForList().trim(redisKey, 0, maxEvents - 1);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event to Redis", e);
        }
    }

    public List<AuditEvent> getRecentEvents(int count) {
        List<String> eventJsons = redisTemplate.opsForList().range(redisKey, 0, count - 1);

        if (eventJsons == null) {
            return List.of();
        }

        return eventJsons.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, AuditEvent.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize audit event", e);
                        return null;
                    }
                })
                .filter(event -> event != null)
                .collect(Collectors.toList());
    }

    public record SecurityEvent(
            String type,
            Severity severity,
            String userId,
            String details
    ) {}

    public record AuditEvent(
            String id,
            String type,
            Severity severity,
            String userId,
            Instant timestamp,
            String details
    ) {}

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
