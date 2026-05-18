package com.techcorp.assistant.module06.cache;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Semantic + exact-match response cache.
 *
 * <p><b>Scale note:</b> the semantic-cache lookup is O(n) over every key in the Redis hash because we
 * recompute cosine similarity against each entry per query. That's fine for the workshop's small caches
 * (≤ a few hundred entries), but at production scale you want an approximate-nearest-neighbour index
 * (HNSW via Redis Stack's vector search, or a dedicated vector DB) so lookup stays sub-linear.
 *
 * <p><b>Embedding-cache bound:</b> the in-process {@code embeddingCache} is a synchronized
 * access-order LRU capped at {@link #DEFAULT_EMBEDDING_CACHE_MAX_ENTRIES} entries (override via
 * {@code semantic-cache.embedding-cache-max-entries}). Once the cap is reached the
 * least-recently-used embedding is evicted on the next put, which prevents the previously
 * unbounded {@code ConcurrentHashMap} from growing without limit under high-cardinality query
 * traffic.
 */
@Service
public class CachingService {

    private static final Logger log = LoggerFactory.getLogger(CachingService.class);
    private static final String SEMANTIC_CACHE_PREFIX = "semantic:";
    private static final int DEFAULT_EMBEDDING_CACHE_MAX_ENTRIES = 1000;

    private final RedisTemplate<String, String> redisTemplate;
    private final EmbeddingModel embeddingModel;
    // Bounded access-order LRU, wrapped in synchronizedMap because multiple request threads
    // hit it concurrently. We don't use ConcurrentHashMap here because it has no built-in
    // eviction; a synchronized LinkedHashMap with `accessOrder=true` is the simplest stdlib
    // path to LRU semantics without pulling in Caffeine/Guava just for one cache.
    private final Map<String, Embedding> embeddingCache;

    @Value("${semantic-cache.similarity-threshold:0.95}")
    private double similarityThreshold;

    @Value("${semantic-cache.ttl-seconds:3600}")
    private long ttlSeconds;

    /**
     * Single constructor so Spring's component scan can autowire it without ambiguity.
     * `maxEntries` is @Value-injected with a default; tests that need to pin a specific
     * value should override the property via {@code @TestPropertySource} rather than
     * adding a separate constructor.
     */
    public CachingService(RedisTemplate<String, String> redisTemplate,
                          EmbeddingModel embeddingModel,
                          @Value("${semantic-cache.embedding-cache-max-entries:1000}") int maxEntries) {
        this.redisTemplate = redisTemplate;
        this.embeddingModel = embeddingModel;
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("embedding-cache-max-entries must be > 0");
        }
        this.embeddingCache = Collections.synchronizedMap(
                new LinkedHashMap<String, Embedding>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Embedding> eldest) {
                        return size() > maxEntries;
                    }
                });
    }

    // `unless = "#result == null"` prevents the cache miss (null return) from being cached
    // as a sentinel — otherwise every subsequent call for the same query would short-circuit
    // here and the upstream service would never get a chance to compute the real answer.
    @Cacheable(value = "exactQueryCache", key = "#query", unless = "#result == null")
    public String exactCacheGet(String query) {
        log.debug("Exact cache miss for: {}", query);
        return null;
    }

    public String semanticCacheGet(String query) {
        // Fast path: exact-match lookup under the per-query key. If we've seen this
        // exact string before and it hasn't expired, skip the similarity scan
        // entirely. (Spring's @Cacheable on exactCacheGet covers the in-process
        // cache; this hits even when the cache abstraction layer has been bypassed.)
        String exact = redisTemplate.opsForValue().get(SEMANTIC_CACHE_PREFIX + "query:" + query);
        if (exact != null) {
            log.debug("Semantic cache exact hit for: {}", query);
            return exact;
        }

        // Slow path: embedding-based similarity scan over the index hash.
        Embedding queryEmbedding = getOrComputeEmbedding(query);
        Map<Object, Object> cache = redisTemplate.opsForHash().entries(SEMANTIC_CACHE_PREFIX + "queries");
        for (Map.Entry<Object, Object> entry : cache.entrySet()) {
            String cachedQuery = (String) entry.getKey();
            // Skip stale index entries whose per-key value has already expired —
            // they're orphans waiting for the index TTL backstop to clean them up.
            String perKey = redisTemplate.opsForValue().get(SEMANTIC_CACHE_PREFIX + "query:" + cachedQuery);
            if (perKey == null) {
                continue;
            }
            Embedding cachedEmbedding = getOrComputeEmbedding(cachedQuery);
            double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);
            if (similarity >= similarityThreshold) {
                log.info("Semantic cache hit - similarity: {}", similarity);
                return perKey;
            }
        }

        log.debug("Semantic cache miss for: {}", query);
        return null;
    }

    public void semanticCachePut(String query, String response) {
        // Per-query Redis key — owns the entry's lifetime. After ttlSeconds it expires
        // and `semanticCacheGet` will treat any leftover index entry as orphaned.
        String key = SEMANTIC_CACHE_PREFIX + "query:" + query;
        redisTemplate.opsForValue().set(key, response, Duration.ofSeconds(ttlSeconds));

        // Index hash holds the query strings for the similarity scan. Its TTL is a
        // coarse cleanup backstop — the per-key check in `semanticCacheGet` filters
        // out stale entries even if the index hasn't been swept yet.
        redisTemplate.opsForHash().put(SEMANTIC_CACHE_PREFIX + "queries", query, response);
        redisTemplate.expire(SEMANTIC_CACHE_PREFIX + "queries", Duration.ofSeconds(ttlSeconds * 2));

        getOrComputeEmbedding(query);

        log.debug("Cached response for query: {}", query);
    }

    private Embedding getOrComputeEmbedding(String text) {
        return embeddingCache.computeIfAbsent(text, key -> {
            log.debug("Computing embedding for: {}", key);
            return embeddingModel.embed(key).content();
        });
    }

    private double cosineSimilarity(Embedding a, Embedding b) {
        float[] vectorA = a.vector();
        float[] vectorB = b.vector();

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        // Guard against zero-norm vectors. An all-zero embedding (degenerate input, e.g. empty
        // query) would otherwise produce 0/0 = NaN, which silently propagates and is treated as
        // "below threshold" by the >= check, but masks the real bug. Surface it as 0 similarity
        // and log so callers can investigate.
        double normProduct = Math.sqrt(normA) * Math.sqrt(normB);
        if (normProduct == 0.0) {
            log.warn("cosineSimilarity called with zero-norm vector — returning 0");
            return 0.0;
        }

        return dotProduct / normProduct;
    }
}
