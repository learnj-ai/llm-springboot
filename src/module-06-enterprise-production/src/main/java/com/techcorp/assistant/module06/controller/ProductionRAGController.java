package com.techcorp.assistant.module06.controller;

import com.techcorp.assistant.module06.cache.CachingService;
import com.techcorp.assistant.module06.metrics.MetricsCollector;
import com.techcorp.assistant.module06.service.SimpleRAGService;
import com.techcorp.assistant.module06.tracing.Traced;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/production")
public class ProductionRAGController {

    private static final Logger log = LoggerFactory.getLogger(ProductionRAGController.class);

    private final SimpleRAGService ragService;
    private final CachingService cachingService;
    private final MetricsCollector metricsCollector;

    @Value("${semantic-cache.enabled:true}")
    private boolean semanticCacheEnabled;

    public ProductionRAGController(
            SimpleRAGService ragService,
            CachingService cachingService,
            MetricsCollector metricsCollector) {
        this.ragService = ragService;
        this.cachingService = cachingService;
        this.metricsCollector = metricsCollector;
    }

    @PostMapping("/query")
    @Traced("rag.query")
    public ResponseEntity<RAGResponse> query(@RequestBody QueryRequest request) {
        log.info("Received production query: {}", request.query());

        // Record metrics
        metricsCollector.recordQuery();
        Timer.Sample sample = metricsCollector.startTimer();

        try {
            // Check semantic cache
            if (semanticCacheEnabled) {
                String cachedResponse = cachingService.semanticCacheGet(request.query());
                if (cachedResponse != null) {
                    metricsCollector.recordResponseTime(sample);
                    return ResponseEntity.ok(new RAGResponse(cachedResponse, true));
                }
            }

            // Execute RAG query
            SimpleRAGService.RAGResponse response = ragService.query(request.query());

            // Cache the response
            if (semanticCacheEnabled) {
                cachingService.semanticCachePut(request.query(), response.response());
            }

            // Record token usage (estimated)
            metricsCollector.recordTokens(estimateTokens(request.query(), response.response()));

            // Record response time
            metricsCollector.recordResponseTime(sample);

            return ResponseEntity.ok(new RAGResponse(response.response(), false));

        } catch (Exception e) {
            log.error("Error processing query", e);
            metricsCollector.recordResponseTime(sample);
            return ResponseEntity.internalServerError()
                    .body(new RAGResponse("An error occurred processing your request.", false));
        }
    }

    private int estimateTokens(String query, String response) {
        int words = (query + response).split("\\s+").length;
        return (int) Math.ceil(words / 0.75);
    }

    public record QueryRequest(String query) {}

    public record RAGResponse(String response, boolean fromCache) {}
}
