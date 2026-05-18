package com.techcorp.assistant.module06.cache;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CachingService cachingService;

    @BeforeEach
    void setUp() {
        // Lenient stubbing because individual tests exercise different paths:
        // miss tests hit only opsForValue; hit tests also hit opsForHash.
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Cheap deterministic embedding so getOrComputeEmbedding doesn't NPE
        // when the slow path or semanticCachePut needs one.
        lenient().when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(new Embedding(new float[]{1.0f, 0.0f, 0.0f})));

        cachingService = new CachingService(redisTemplate, embeddingModel, 1000);
        ReflectionTestUtils.setField(cachingService, "similarityThreshold", 0.95);
        ReflectionTestUtils.setField(cachingService, "ttlSeconds", 3600L);
    }

    @Test
    void testSemanticCacheMiss() {
        // No exact match on the per-query key, no entries in the index hash.
        when(valueOperations.get(anyString())).thenReturn(null);
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());

        String result = cachingService.semanticCacheGet("test query");

        assertNull(result);
    }

    @Test
    void testSemanticCacheHit() {
        // The per-query exact-match key short-circuits the similarity scan;
        // when present, `semanticCacheGet` returns the value without consulting
        // the index hash. This exercises the fast path added in the
        // semantic-cache fix.
        when(valueOperations.get("semantic:query:test query")).thenReturn("cached response");

        String result = cachingService.semanticCacheGet("test query");

        assertEquals("cached response", result);
    }

    @Test
    void testSemanticCachePut() {
        cachingService.semanticCachePut("query", "response");

        // Per-key write (owns the entry's TTL clock).
        verify(valueOperations).set(eq("semantic:query:query"), eq("response"),
                eq(Duration.ofSeconds(3600L)));
        // Index hash write (so `semanticCacheGet` can iterate candidates).
        verify(hashOperations).put(anyString(), eq("query"), eq("response"));
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(7200L)));
    }
}
