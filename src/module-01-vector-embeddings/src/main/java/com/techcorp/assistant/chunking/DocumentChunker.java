package com.techcorp.assistant.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DocumentChunker {

    // Simple sentence splitter — does not handle abbreviations (Dr.), decimals (3.14), or ellipsis (...)
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?\\s*");
    private static final int DEFAULT_MAX_PARAGRAPH_CHUNK_LENGTH = 500;

    public List<TextSegment> recursiveChunk(Document document, int chunkSize, int overlap) {
        DocumentSplitter splitter = DocumentSplitters.recursive(chunkSize, overlap);
        return splitter.split(document);
    }

    public List<TextSegment> paragraphChunk(Document document) {
        String[] paragraphs = document.text().split("\\R\\R+");
        List<TextSegment> segments = new ArrayList<>();

        for (String paragraph : paragraphs) {
            String normalizedParagraph = paragraph.trim();
            if (normalizedParagraph.isEmpty()) {
                continue;
            }

            if (normalizedParagraph.length() > DEFAULT_MAX_PARAGRAPH_CHUNK_LENGTH) {
                segments.addAll(splitBySemanticBoundaries(normalizedParagraph));
            } else {
                segments.add(TextSegment.from(normalizedParagraph));
            }
        }

        return segments;
    }

    private List<TextSegment> splitBySemanticBoundaries(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        StringBuilder currentChunk = new StringBuilder();

        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (sentence.isEmpty()) {
                continue;
            }

            if (!currentChunk.isEmpty()
                    && currentChunk.length() + sentence.length() + 1 > DEFAULT_MAX_PARAGRAPH_CHUNK_LENGTH) {
                segments.add(TextSegment.from(currentChunk.toString().trim()));
                currentChunk.setLength(0);
            }

            if (!currentChunk.isEmpty()) {
                currentChunk.append(' ');
            }
            currentChunk.append(sentence);
        }

        if (!currentChunk.isEmpty()) {
            segments.add(TextSegment.from(currentChunk.toString().trim()));
        }

        if (segments.isEmpty()) {
            segments.add(TextSegment.from(text));
        }

        return segments;
    }
}
