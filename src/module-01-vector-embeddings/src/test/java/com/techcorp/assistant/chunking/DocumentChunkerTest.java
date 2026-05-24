package com.techcorp.assistant.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void recursiveChunkShouldSplitLargeDocument() {
        String text = "A".repeat(600);
        Document document = Document.from(text);

        List<TextSegment> segments = chunker.recursiveChunk(document, 300, 30);

        assertThat(segments).hasSizeGreaterThan(1);
        segments.forEach(segment ->
                assertThat(segment.text().length()).isLessThanOrEqualTo(300));
    }

    @Test
    void recursiveChunkShouldReturnSingleSegmentForSmallDocument() {
        Document document = Document.from("Short text.");

        List<TextSegment> segments = chunker.recursiveChunk(document, 300, 30);

        assertThat(segments).hasSize(1);
        assertThat(segments.getFirst().text()).isEqualTo("Short text.");
    }

    @Test
    void paragraphChunkShouldSplitOnBlankLines() {
        Document document = Document.from("First paragraph.\n\nSecond paragraph.");

        List<TextSegment> segments = chunker.paragraphChunk(document);

        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).text()).isEqualTo("First paragraph.");
        assertThat(segments.get(1).text()).isEqualTo("Second paragraph.");
    }

    @Test
    void paragraphChunkShouldSplitLargeParagraphBySentence() {
        String longParagraph = "This is sentence one. ".repeat(30); // ~660 chars
        Document document = Document.from(longParagraph);

        List<TextSegment> segments = chunker.paragraphChunk(document);

        assertThat(segments).hasSizeGreaterThan(1);
        segments.forEach(segment ->
                assertThat(segment.text().length()).isLessThanOrEqualTo(500));
    }

    @Test
    void paragraphChunkShouldSkipEmptyParagraphs() {
        Document document = Document.from("Content.\n\n\n\n\nMore content.");

        List<TextSegment> segments = chunker.paragraphChunk(document);

        assertThat(segments).hasSize(2);
    }
}
