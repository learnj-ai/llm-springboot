package com.techcorp.assistant.store;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class TechCorpDocumentLoader {

    public List<Document> loadDocuments() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/*.md");

            if (resources.length == 0) {
                throw new IllegalStateException(
                        "No documents found at classpath:data/*.md — check that resource files are on the classpath");
            }

            return Arrays.stream(resources)
                    .map(this::toDocument)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load TechCorp documents", exception);
        }
    }

    private Document toDocument(Resource resource) {
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            Metadata metadata = Metadata.metadata("source", resource.getFilename());
            return Document.from(text, metadata);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read %s".formatted(resource.getFilename()), exception);
        }
    }
}
