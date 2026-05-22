package com.techcorp.assistant.rag;

import dev.langchain4j.data.segment.TextSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the RAG pipeline and search comparison.
 * <p>
 * POST /api/v1/rag/query — full RAG pipeline (retrieve + generate)
 * POST /api/v1/rag/compare — compare vector-only, keyword-only, and hybrid search results
 */
@RestController
@RequestMapping("/api/v1/rag")
public class RAGController {

    private final RAGService ragService;
    private final HybridSearchService hybridSearchService;

    public RAGController(RAGService ragService, HybridSearchService hybridSearchService) {
        this.ragService = ragService;
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/query")
    public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
        // Use the rich pipeline result so the response can carry citations and a
        // pipeline trace alongside the answer. The thin `query(...) -> String` API
        // is still available for callers that only need the answer text.
        RAGService.RagAnswer result = ragService.queryWithSources(
                request.question(), request.useQueryExpansion());
        return ResponseEntity.ok(new RAGResponse(
                result.answer(),
                result.sources(),
                result.transformedQueries(),
                result.elapsedMs()));
    }

    @PostMapping("/compare")
    public ResponseEntity<SearchComparisonResponse> compareSearchMethods(
            @Valid @RequestBody CompareRequest request) {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<List<String>>allSuccessfulOrThrow())) {
            var vectorTask = scope.fork(() -> hybridSearchService.vectorOnlySearch(request.query(), request.topK())
                    .stream()
                    .map(TextSegment::text)
                    .toList());

            var keywordTask = scope.fork(() -> hybridSearchService.keywordOnlySearch(request.query(), request.topK())
                    .stream()
                    .map(TextSegment::text)
                    .toList());

            var hybridTask = scope.fork(() -> hybridSearchService.hybridSearch(request.query(), request.topK())
                    .stream()
                    .map(TextSegment::text)
                    .toList());

            scope.join();

            return ResponseEntity.ok(new SearchComparisonResponse(
                    request.query(),
                    vectorTask.get(),
                    keywordTask.get(),
                    hybridTask.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Search comparison interrupted", e);
        }
    }

    record CompareRequest(
            @NotBlank String query,
            @Min(1) @Max(20) int topK) {

        CompareRequest {
            topK = (topK == 0) ? 5 : topK;
        }
    }
}
