package com.techcorp.assistant.search;

import java.util.Map;

public record SearchResult(String content, double score, Map<String, Object> metadata) {
}
