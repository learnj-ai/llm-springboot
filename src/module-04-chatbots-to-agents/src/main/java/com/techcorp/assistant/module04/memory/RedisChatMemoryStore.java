package com.techcorp.assistant.module04.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis-backed implementation of {@link ChatMemoryStore}.
 *
 * <p>Messages are serialized to JSON via LangChain4J's {@code ChatMessageSerializer} /
 * {@code ChatMessageDeserializer}, which handles the polymorphic ChatMessage hierarchy
 * (User/Ai/System/ToolExecutionResult) correctly. Each conversation lives under a
 * single Redis key with a TTL — no in-memory backing map, so multiple app instances
 * sharing the same Redis see the same conversation state.
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);
    private static final String MEMORY_KEY_PREFIX = "chat:memory:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static String redisKey(Object memoryId) {
        return MEMORY_KEY_PREFIX + memoryId.toString();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(redisKey(memoryId));
        if (json == null || json.isEmpty()) {
            log.debug("No messages found for memory ID: {}", memoryId);
            return List.of();
        }
        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
        log.debug("Retrieved {} messages for memory ID: {}", messages.size(), memoryId);
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = redisKey(memoryId);
        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        String json = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(key, json, Duration.ofHours(TTL_HOURS));
        log.debug("Stored {} messages for memory ID: {} with TTL of {} hours",
                messages.size(), memoryId, TTL_HOURS);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(redisKey(memoryId));
        log.debug("Deleted messages for memory ID: {}", memoryId);
    }
}
