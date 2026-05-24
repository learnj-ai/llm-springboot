package com.techcorp.assistant.module06.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RAGTracingAspect {

    private static final Logger log = LoggerFactory.getLogger(RAGTracingAspect.class);
    private final Tracer tracer;

    public RAGTracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String className = signature.getDeclaringType().getSimpleName();

        // Create span name from annotation value or method name
        String spanName = traced.value().isEmpty() ?
                className + "." + methodName :
                traced.value();

        Span span = tracer.spanBuilder(spanName)
                .setAttribute("component", className)
                .setAttribute("method", methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.debug("Starting traced method: {}", spanName);

            Object result = joinPoint.proceed();

            span.setStatus(StatusCode.OK);
            return result;

        } catch (Throwable throwable) {
            log.error("Error in traced method: {}", spanName, throwable);

            span.setStatus(StatusCode.ERROR, throwable.getMessage());
            span.recordException(throwable);

            throw throwable;

        } finally {
            span.end();
        }
    }
}
