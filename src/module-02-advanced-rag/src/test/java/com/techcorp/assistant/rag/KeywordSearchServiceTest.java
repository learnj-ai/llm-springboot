package com.techcorp.assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.store.IndexedSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordSearchServiceTest {

    private static final float[] DUMMY_VECTOR = new float[]{0.1f, 0.2f, 0.3f};

    @Test
    void searchReturnsSegmentsContainingQueryTerms() {
        KeywordSearchService service = serviceWith(
                indexedSegment("Reset your password from the identity portal"),
                indexedSegment("VPN access requires the SecureConnect client"),
                indexedSegment("Password must contain at least 14 characters")
        );

        List<TextSegment> results = service.search("password reset", 5);

        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("password");
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() {
        KeywordSearchService service = serviceWith(
                indexedSegment("VPN access requires the SecureConnect client")
        );

        List<TextSegment> results = service.search("kubernetes deployment", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void searchRespectsMaxResults() {
        KeywordSearchService service = serviceWith(
                indexedSegment("Password reset guide for employees"),
                indexedSegment("Password policy requires 14 characters"),
                indexedSegment("Forgot your password? Use the identity portal")
        );

        List<TextSegment> results = service.search("password", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    void searchRanksMoreRelevantDocumentsHigher() {
        KeywordSearchService service = serviceWith(
                indexedSegment("The deployment process uses Jenkins for CI/CD"),
                indexedSegment("Deploy to staging first, then production deployment. Deployment rollbacks are automated."),
                indexedSegment("VPN access policy for remote workers")
        );

        List<TextSegment> results = service.search("deployment", 5);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.get(0).text()).containsIgnoringCase("deployment");
    }

    private KeywordSearchService serviceWith(IndexedSegment... segments) {
        List<IndexedSegment> segmentList = List.of(segments);
        return new KeywordSearchService(segmentList);
    }

    private static IndexedSegment indexedSegment(String text) {
        return new IndexedSegment(TextSegment.from(text), new Embedding(DUMMY_VECTOR));
    }
}
