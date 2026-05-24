package com.techcorp.assistant.module04.memory;

import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ConversationMemoryService.
 */
class ConversationMemoryServiceTest {

    private ConversationMemoryService memoryService;
    private RedisChatMemoryStore mockStore;

    @BeforeEach
    void setUp() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        mockStore = new RedisChatMemoryStore(mockRedis);
        memoryService = new ConversationMemoryService(mockStore);
        // `maxMessages` is @Value-injected at runtime. Outside a Spring context
        // the field stays at its Java default (0), which LangChain4J rejects
        // with "maxMessages must be greater than zero". Set it explicitly.
        ReflectionTestUtils.setField(memoryService, "maxMessages", 20);
    }

    @Test
    void testGetOrCreateMemory_CreatesNew() {
        // When
        ChatMemory memory = memoryService.getOrCreateMemory("session-1");

        // Then
        assertThat(memory).isNotNull();
    }

    @Test
    void testGetOrCreateMemory_ReusesExisting() {
        // Given
        ChatMemory first = memoryService.getOrCreateMemory("session-1");

        // When
        ChatMemory second = memoryService.getOrCreateMemory("session-1");

        // Then
        assertThat(first).isSameAs(second);
    }

    @Test
    void testClearMemory() {
        // Given
        memoryService.getOrCreateMemory("session-1");

        // When
        memoryService.clearMemory("session-1");

        // Then
        int count = memoryService.getMessageCount("session-1");
        assertThat(count).isEqualTo(0);
    }
}
