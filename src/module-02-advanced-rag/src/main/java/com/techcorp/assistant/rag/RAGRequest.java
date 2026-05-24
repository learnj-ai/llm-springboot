package com.techcorp.assistant.rag;

import jakarta.validation.constraints.NotBlank;

public record RAGRequest(
        @NotBlank String question,
        Boolean useQueryExpansion) {

    public RAGRequest {
        useQueryExpansion = (useQueryExpansion == null) ? true : useQueryExpansion;
    }
}
