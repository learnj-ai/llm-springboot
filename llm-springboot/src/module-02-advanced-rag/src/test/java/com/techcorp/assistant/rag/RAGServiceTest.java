package com.techcorp.assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RAGServiceTest {

    @Test
    void queryUsesHydeDocumentForAdditionalVectorRetrieval() {
        RecordingHybridSearchService searchService = new RecordingHybridSearchService(
                List.of(TextSegment.from("Reset passwords via the identity portal.")),
                List.of(TextSegment.from("The identity portal supports self-service password reset."))
        );
        StubQueryTransformer queryTransformer = new StubQueryTransformer(
                List.of("Password reset steps"),
                "Employees can reset passwords from the identity portal."
        );
        RAGService ragService = new RAGService(searchService, new StubChatModel("Use the identity portal."), queryTransformer);

        String answer = ragService.queryWithSources("How do I reset my password?", true).answer();

        assertThat(answer).isEqualTo("Use the identity portal.");
        assertThat(searchService.hybridQueries).containsExactly(
                "How do I reset my password?",
                "Password reset steps");
        assertThat(searchService.vectorQueries)
                .containsExactly("Employees can reset passwords from the identity portal.");
    }

    @Test
    void querySkipsHydeVectorRetrievalWhenFallbackMatchesOriginalQuery() {
        RecordingHybridSearchService searchService = new RecordingHybridSearchService(
                List.of(TextSegment.from("Reset passwords via the identity portal.")),
                List.of()
        );
        StubQueryTransformer queryTransformer = new StubQueryTransformer(
                List.of(),
                "How do I reset my password?"
        );
        RAGService ragService = new RAGService(searchService, new StubChatModel("Use the identity portal."), queryTransformer);

        String answer = ragService.queryWithSources("How do I reset my password?", true).answer();

        assertThat(answer).isEqualTo("Use the identity portal.");
        assertThat(searchService.hybridQueries).containsExactly("How do I reset my password?");
        assertThat(searchService.vectorQueries).isEmpty();
    }

    @Test
    void rrfBeatsRawScoreAcrossShards() {
        // Different segments. RRF should rank A above C even though C carries a huge
        // raw score in the HyDE shard, because A appears in both shards (original-hybrid
        // and HyDE-vector) and earns two RRF contributions. C earns only one.
        //
        // RRF math (RRF_K = 60, WEIGHT_ORIGINAL = 1.25, WEIGHT_HYDE = 0.75):
        //   A: 1.25/(60+1) + 0.75/(60+2) ≈ 0.03259   (original rank 1 + HyDE rank 2)
        //   B: 1.25/(60+2)               ≈ 0.02016   (original rank 2)
        //   C: 0.75/(60+1)               ≈ 0.01230   (HyDE rank 1 only — raw score 100 is ignored)
        // Expected ranking: A, B, C.
        TextSegment a = TextSegment.from("Alpha chunk about identity portal.");
        TextSegment b = TextSegment.from("Beta chunk about password rules.");
        TextSegment c = TextSegment.from("Charlie chunk about logging in.");

        HybridSearchService searchService = new HybridSearchService(null, null, null) {
            @Override
            public List<ScoredSegment> hybridSearchScored(String query, int topK) {
                return List.of(
                        new ScoredSegment(a, 0.60, query),
                        new ScoredSegment(b, 0.59, query));
            }

            @Override
            public List<ScoredSegment> vectorOnlySearchScored(String query, int topK) {
                // Deliberately huge raw scores. Rank-based RRF must ignore them.
                return List.of(
                        new ScoredSegment(c, 100.0, query),
                        new ScoredSegment(a, 99.0, query));
            }
        };

        StubQueryTransformer queryTransformer = new StubQueryTransformer(
                List.of(),
                "Sign in with single sign-on and verify with a hardware token.");

        RAGService ragService = new RAGService(
                searchService,
                new StubChatModel("answer text"),
                queryTransformer);

        RAGService.RagAnswer result = ragService.queryWithSources("How do I log in?", true);

        List<RAGService.Source> sources = result.sources();
        assertThat(sources).extracting(RAGService.Source::text)
                .containsExactly(a.text(), b.text(), c.text());

        // Provenance must combine the two shards A surfaced in (the spec called this out
        // explicitly: don't drop the cross-shard signal to first-write-wins).
        String aProvenance = sources.get(0).sourceQuery();
        assertThat(aProvenance).contains("hybrid:");
        assertThat(aProvenance).contains("vector(HyDE):");

        // A's fused score must beat C's despite C's 100.0 raw score on the HyDE shard.
        assertThat(sources.get(0).score()).isGreaterThan(sources.get(2).score());
    }

    private static final class RecordingHybridSearchService extends HybridSearchService {

        private final List<TextSegment> hybridResults;
        private final List<TextSegment> vectorResults;
        private final List<String> hybridQueries = new ArrayList<>();
        private final List<String> vectorQueries = new ArrayList<>();

        private RecordingHybridSearchService(List<TextSegment> hybridResults, List<TextSegment> vectorResults) {
            super(null, null, null);
            this.hybridResults = hybridResults;
            this.vectorResults = vectorResults;
        }

        // Both API shapes are exercised by callers: the legacy unscored path (for
        // backward compat) and the scored path (used by RAGService since the
        // cross-variant fusion refactor). Record the query under both so the
        // assertions don't care which API the production code happens to be using.
        @Override
        public List<TextSegment> hybridSearch(String query, int topK) {
            hybridQueries.add(query);
            return hybridResults;
        }

        @Override
        public List<ScoredSegment> hybridSearchScored(String query, int topK) {
            hybridQueries.add(query);
            return asScored(hybridResults, query);
        }

        @Override
        public List<TextSegment> vectorOnlySearch(String query, int topK) {
            vectorQueries.add(query);
            return vectorResults;
        }

        @Override
        public List<ScoredSegment> vectorOnlySearchScored(String query, int topK) {
            vectorQueries.add(query);
            return asScored(vectorResults, query);
        }

        private static List<ScoredSegment> asScored(List<TextSegment> hits, String query) {
            List<ScoredSegment> out = new ArrayList<>(hits.size());
            for (int i = 0; i < hits.size(); i++) {
                out.add(new ScoredSegment(hits.get(i), 1.0 / (60 + i + 1), query));
            }
            return out;
        }
    }

    private static final class StubQueryTransformer extends QueryTransformer {

        private final List<String> alternatives;
        private final String hypotheticalDocument;

        private StubQueryTransformer(List<String> alternatives, String hypotheticalDocument) {
            super(new StubChatModel(""));
            this.alternatives = alternatives;
            this.hypotheticalDocument = hypotheticalDocument;
        }

        @Override
        public List<String> multiQuery(String originalQuery) {
            return alternatives;
        }

        @Override
        public String generateHypotheticalDocument(String query) {
            return hypotheticalDocument;
        }
    }

    private static final class StubChatModel implements ChatModel {

        private final String response;

        private StubChatModel(String response) {
            this.response = response;
        }

        @Override
        public String chat(String userMessage) {
            return response;
        }
    }
}
