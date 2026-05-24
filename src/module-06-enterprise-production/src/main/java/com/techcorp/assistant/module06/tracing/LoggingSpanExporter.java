package com.techcorp.assistant.module06.tracing;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Simple span exporter that logs spans to application logs.
 * In production, replace with OTLP exporter or Jaeger exporter.
 */
public class LoggingSpanExporter implements SpanExporter {

    private static final Logger log = LoggerFactory.getLogger(LoggingSpanExporter.class);

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        for (SpanData span : spans) {
            log.info("TRACE: {} [{}] duration={}ms status={}",
                    span.getName(),
                    span.getTraceId(),
                    (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1_000_000,
                    span.getStatus().getStatusCode()
            );
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
