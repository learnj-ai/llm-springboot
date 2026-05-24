package com.techcorp.assistant.search;

import com.techcorp.assistant.store.SearchMatch;
import com.techcorp.assistant.store.VectorStoreService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class VectorSearchController {

    private final VectorStoreService vectorStoreService;

    public VectorSearchController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/vector")
    public ResponseEntity<SearchResponse> vectorSearch(@Valid @RequestBody SearchRequest request) {
        List<SearchResult> results = vectorStoreService
                .search(request.query(), request.maxResults(), request.metric(), request.chunkingStrategy())
                .stream()
                .map(VectorSearchController::toSearchResult)
                .toList();

        SearchResponse response = new SearchResponse(
                vectorStoreService.embeddingDimension(),
                request.metric(),
                request.chunkingStrategy(),
                vectorStoreService.indexedSegmentCount(request.chunkingStrategy()),
                results);

        return ResponseEntity.ok(response);
    }

    private static SearchResult toSearchResult(SearchMatch match) {
        return new SearchResult(match.content(), match.score(), match.metadata().toMap());
    }
}
