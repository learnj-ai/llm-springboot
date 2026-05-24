package com.techcorp.assistant.module04.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing session-based conversation memories.
 *
 * Each session has its own ChatMemory backed by Redis for persistence.
 */
@Service
public class ConversationMemoryService {
    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    private final RedisChatMemoryStore memoryStore;
    private final Map<String, ChatMemory> activeMemories = new ConcurrentHashMap<>();

    @Value("${agent.memory.max-messages:20}")
    private int maxMessages;

    public ConversationMemoryService(RedisChatMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    /**
     * Gets or creates a chat memory for the given session ID.
     *
     * @param sessionId Unique session identifier
     * @return ChatMemory instance for this session
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return activeMemories.computeIfAbsent(sessionId, id -> {
            log.info("Creating new memory for session: {}", id);
            return MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(maxMessages)
                    .chatMemoryStore(memoryStore)
                    .build();
        });
    }

    /**
     * Adds a user message to the session memory.
     *
     * @param sessionId Session identifier
     * @param message User's message text
     */
    public void addMessage(String sessionId, String message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(UserMessage.from(message));
        log.debug("Added user message to session {}: {}", sessionId, message);
    }

    /**
     * Adds an AI response to the session memory.
     *
     * @param sessionId Session identifier
     * @param response AI's response text
     */
    public void addAiMessage(String sessionId, String response) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(AiMessage.from(response));
        log.debug("Added AI message to session {}", sessionId);
    }

    /**
     * Gets conversation history for a session.
     *
     * @param sessionId Session identifier
     * @return List of chat messages
     */
    public List<ChatMessage> getHistory(String sessionId) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        return memory.messages();
    }

    /**
     * Clears all messages for a session.
     *
     * @param sessionId Session identifier
     */
    public void clearMemory(String sessionId) {
        ChatMemory memory = activeMemories.get(sessionId);
        if (memory != null) {
            memory.clear();
            activeMemories.remove(sessionId);
            log.info("Cleared memory for session: {}", sessionId);
        }
    }

    /**
     * Gets the number of messages in a session's memory.
     *
     * @param sessionId Session identifier
     * @return Message count
     */
    public int getMessageCount(String sessionId) {
        ChatMemory memory = activeMemories.get(sessionId);
        return memory != null ? memory.messages().size() : 0;
    }
}
