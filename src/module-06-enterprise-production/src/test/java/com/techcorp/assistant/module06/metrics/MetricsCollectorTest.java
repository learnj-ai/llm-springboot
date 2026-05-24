package com.techcorp.assistant.module06.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private MetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new MetricsCollector(meterRegistry);
    }

    @Test
    void testRecordQuery() {
        metricsCollector.recordQuery();

        Counter counter = meterRegistry.find("rag.queries.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void testRecordMultipleQueries() {
        metricsCollector.recordQuery();
        metricsCollector.recordQuery();
        metricsCollector.recordQuery();

        Counter counter = meterRegistry.find("rag.queries.total").counter();
        assertEquals(3.0, counter.count());
    }

    @Test
    void testRecordTokens() {
        metricsCollector.recordTokens(100);
        metricsCollector.recordTokens(50);

        Double tokens = meterRegistry.find("rag.tokens.used").gauge().value();
        assertNotNull(tokens);
        assertEquals(150.0, tokens);
    }

    @Test
    void testTimerCreated() {
        Timer.Sample sample = metricsCollector.startTimer();
        assertNotNull(sample);

        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        metricsCollector.recordResponseTime(sample);

        Timer timer = meterRegistry.find("rag.response.time").timer();
        assertNotNull(timer);
        assertTrue(timer.count() > 0);
    }
}
