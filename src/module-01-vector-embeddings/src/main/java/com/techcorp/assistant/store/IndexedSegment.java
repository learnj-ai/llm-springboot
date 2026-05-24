package com.techcorp.assistant.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public record IndexedSegment(TextSegment segment, Embedding embedding) {
}
