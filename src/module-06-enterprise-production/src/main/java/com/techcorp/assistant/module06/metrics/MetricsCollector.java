package com.techcorp.assistant.module06.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsCollector {

    private final Counter queryCounter;
    private final Timer responseTimer;
    private final AtomicLong tokensUsed;

    public MetricsCollector(MeterRegistry meterRegistry) {
        // Counter for total queries
        this.queryCounter = Counter.builder("rag.queries.total")
                .description("Total number of RAG queries processed")
                .tag("type", "rag")
                .register(meterRegistry);

        // Timer for response time
        this.responseTimer = Timer.builder("rag.response.time")
                .description("RAG query response time (Prometheus exports as `_seconds`)")
                .tag("type", "rag")
                .register(meterRegistry);

        // Gauge for token usage
        this.tokensUsed = new AtomicLong(0);
        Gauge.builder("rag.tokens.used", tokensUsed, AtomicLong::get)
                .description("Total tokens used by RAG queries")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    public void recordQuery() {
        queryCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordResponseTime(Timer.Sample sample) {
        sample.stop(responseTimer);
    }

    public void recordTokens(long tokens) {
        tokensUsed.addAndGet(tokens);
    }
}
