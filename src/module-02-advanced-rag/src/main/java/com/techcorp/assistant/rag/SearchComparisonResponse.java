package com.techcorp.assistant.rag;

import java.util.List;

public record SearchComparisonResponse(
        String query,
        List<String> vectorResults,
        List<String> keywordResults,
        List<String> hybridResults) {
}
