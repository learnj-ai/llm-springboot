package com.techcorp.assistant.module06.dokimos;

import com.techcorp.assistant.module06.service.SimpleRAGService;
import dev.dokimos.core.Example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RAGEvaluationTask.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RAGEvaluationTask Tests")
class RAGEvaluationTaskTest {

    @Mock
    private SimpleRAGService ragService;

    private RAGEvaluationTask task;

    @BeforeEach
    void setUp() {
        task = new RAGEvaluationTask(ragService);
    }

    @Test
    @DisplayName("Should execute RAG query and return outputs")
    void testRunWithValidExample() {
        // Create test example
        Example example = Example.of("What is Java?", "Java is a programming language");

        // Mock RAG service response
        SimpleRAGService.RAGResponse mockResponse = new SimpleRAGService.RAGResponse(
                "Java is a programming language",
                List.of("Java is a high-level programming language", "Java was developed by Sun Microsystems")
        );
        when(ragService.query(anyString())).thenReturn(mockResponse);

        // Execute task
        Map<String, Object> outputs = task.run(example);

        // Verify outputs
        assertNotNull(outputs);
        assertEquals("Java is a programming language", outputs.get("output"));
        assertNotNull(outputs.get("context"));
        assertEquals(2, outputs.get("source_count"));

        // Verify RAG service was called
        verify(ragService, times(1)).query("What is Java?");
    }

    @Test
    @DisplayName("Should handle empty query gracefully")
    void testRunWithEmptyQuery() {
        // Create example with empty query
        Example example = Example.of("", "Expected output");

        // Execute task
        Map<String, Object> outputs = task.run(example);

        // Verify error handling
        assertNotNull(outputs);
        assertEquals("", outputs.get("output"));
        assertEquals("", outputs.get("context"));
        assertTrue(outputs.containsKey("error"));
        assertEquals("Empty or null query", outputs.get("error"));

        // Verify RAG service was not called
        verify(ragService, never()).query(anyString());
    }

    @Test
    @DisplayName("Should handle RAG service exceptions")
    void testRunWithServiceException() {
        // Create test example
        Example example = Example.of("Test query", "Expected answer");

        // Mock RAG service to throw exception
        when(ragService.query(anyString())).thenThrow(new RuntimeException("Service error"));

        // Execute task
        Map<String, Object> outputs = task.run(example);

        // Verify error handling
        assertNotNull(outputs);
        assertEquals("", outputs.get("output"));
        assertEquals("", outputs.get("context"));
        assertTrue(outputs.containsKey("error"));
        assertEquals("Service error", outputs.get("error"));
        assertEquals("RuntimeException", outputs.get("error_type"));
    }

    @Test
    @DisplayName("Should format context from multiple source documents")
    void testContextFormatting() {
        // Create test example
        Example example = Example.of("Test query", "Expected answer");

        // Mock RAG service with multiple sources
        SimpleRAGService.RAGResponse mockResponse = new SimpleRAGService.RAGResponse(
                "Response",
                List.of("Source 1", "Source 2", "Source 3")
        );
        when(ragService.query(anyString())).thenReturn(mockResponse);

        // Execute task
        Map<String, Object> outputs = task.run(example);

        // Verify context formatting
        String context = (String) outputs.get("context");
        assertTrue(context.contains("Source 1"));
        assertTrue(context.contains("Source 2"));
        assertTrue(context.contains("Source 3"));
        assertEquals(3, outputs.get("source_count"));
    }
}
