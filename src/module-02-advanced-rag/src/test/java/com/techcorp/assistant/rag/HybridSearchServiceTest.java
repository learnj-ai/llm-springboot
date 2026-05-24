package com.techcorp.assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;

class HybridSearchServiceTest {

    @Test
    void rrfMergesResultsFromBothLists() {
        HybridSearchService service = new HybridSearchService(null, null, null);

        List<TextSegment> list1 = List.of(
                TextSegment.from("doc A"),
                TextSegment.from("doc B"),
                TextSegment.from("doc C")
        );

        List<TextSegment> list2 = List.of(
                TextSegment.from("doc B"),
                TextSegment.from("doc D"),
                TextSegment.from("doc A")
        );

        List<TextSegment> merged = service.reciprocalRankFusion(list1, list2, 10);

        assertThat(merged).isNotEmpty();
        // doc A and doc B appear in both lists so should have highest RRF scores
        List<String> topTexts = merged.stream().map(TextSegment::text).limit(2).toList();
        assertThat(topTexts).containsExactlyInAnyOrder("doc A", "doc B");
    }

    @Test
    void rrfDeduplicatesSegments() {
        HybridSearchService service = new HybridSearchService(null, null, null);

        List<TextSegment> list1 = List.of(TextSegment.from("same doc"));
        List<TextSegment> list2 = List.of(TextSegment.from("same doc"));

        List<TextSegment> merged = service.reciprocalRankFusion(list1, list2, 10);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).text()).isEqualTo("same doc");
    }

    @Test
    void rrfRespectsMaxResults() {
        HybridSearchService service = new HybridSearchService(null, null, null);

        List<TextSegment> list1 = List.of(
                TextSegment.from("a"), TextSegment.from("b"), TextSegment.from("c"));
        List<TextSegment> list2 = List.of(
                TextSegment.from("d"), TextSegment.from("e"), TextSegment.from("f"));

        List<TextSegment> merged = service.reciprocalRankFusion(list1, list2, 3);

        assertThat(merged).hasSize(3);
    }

    @Test
    void rrfDocInBothListsRanksHigherThanDocInOneList() {
        HybridSearchService service = new HybridSearchService(null, null, null);

        List<TextSegment> list1 = List.of(
                TextSegment.from("only-in-list1"),
                TextSegment.from("in-both")
        );

        List<TextSegment> list2 = List.of(
                TextSegment.from("only-in-list2"),
                TextSegment.from("in-both")
        );

        List<TextSegment> merged = service.reciprocalRankFusion(list1, list2, 10);

        // "in-both" gets RRF score from both lists, so it should rank first
        assertThat(merged.get(0).text()).isEqualTo("in-both");
    }
}
