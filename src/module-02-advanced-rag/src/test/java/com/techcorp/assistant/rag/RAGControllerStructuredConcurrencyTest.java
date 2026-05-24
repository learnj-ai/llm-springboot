package com.techcorp.assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class RAGControllerStructuredConcurrencyTest {

    @Test
    void compareSearchMethodsReturnsResultsFromAllSearchStrategies() {
        RAGController controller = new RAGController(
                new StubRagService(),
                new StubHybridSearchService());

        ResponseEntity<SearchComparisonResponse> response = controller.compareSearchMethods(
                new RAGController.CompareRequest("password reset", 3));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().query()).isEqualTo("password reset");
        assertThat(response.getBody().vectorResults()).containsExactly("vector result");
        assertThat(response.getBody().keywordResults()).containsExactly("keyword result");
        assertThat(response.getBody().hybridResults()).containsExactly("hybrid result");
    }

    private static final class StubRagService extends RAGService {

        private StubRagService() {
            super(null, null, null);
        }
    }

    private static final class StubHybridSearchService extends HybridSearchService {

        private StubHybridSearchService() {
            super(null, null, null);
        }

        @Override
        public List<TextSegment> vectorOnlySearch(String query, int topK) {
            return List.of(TextSegment.from("vector result"));
        }

        @Override
        public List<TextSegment> keywordOnlySearch(String query, int topK) {
            return List.of(TextSegment.from("keyword result"));
        }

        @Override
        public List<TextSegment> hybridSearch(String query, int topK) {
            return List.of(TextSegment.from("hybrid result"));
        }
    }
}
