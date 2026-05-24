package com.techcorp.assistant.store;

import dev.langchain4j.data.document.Metadata;

public record SearchMatch(String content, double score, Metadata metadata) {
}
