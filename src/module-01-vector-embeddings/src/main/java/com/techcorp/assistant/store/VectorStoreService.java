package com.techcorp.assistant.store;

import com.techcorp.assistant.chunking.ChunkingStrategy;
import com.techcorp.assistant.chunking.DocumentChunker;
import com.techcorp.assistant.embeddings.EmbeddingService;
import com.techcorp.assistant.similarity.SearchMetric;
import com.techcorp.assistant.similarity.SimilarityCalculator;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private static final int DEFAULT_RECURSIVE_CHUNK_SIZE = 300;
    private static final int DEFAULT_RECURSIVE_OVERLAP = 30;

    private final EmbeddingService embeddingService;
    private final SimilarityCalculator similarityCalculator;
    private final DocumentChunker documentChunker;
    private final TechCorpDocumentLoader documentLoader;
    private final Map<ChunkingStrategy, List<IndexedSegment>> indexes =
            new EnumMap<>(ChunkingStrategy.class);

    public VectorStoreService(
            EmbeddingService embeddingService,
            SimilarityCalculator similarityCalculator,
            DocumentChunker documentChunker,
            TechCorpDocumentLoader documentLoader) {
        this.embeddingService = embeddingService;
        this.similarityCalculator = similarityCalculator;
        this.documentChunker = documentChunker;
        this.documentLoader = documentLoader;
    }

    @PostConstruct
    protected void initialize() {
        log.info("Loading documents and building vector indexes...");
        long start = System.currentTimeMillis();

        List<Document> documents = documentLoader.loadDocuments();
        log.info("Loaded {} documents", documents.size());

        for (ChunkingStrategy strategy : ChunkingStrategy.values()) {
            List<IndexedSegment> index = buildIndex(documents, strategy);
            indexes.put(strategy, index);
            log.info("Indexed {} segments using {} strategy", index.size(), strategy);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Vector index initialization completed in {}ms", elapsed);
    }

    public List<SearchMatch> search(String query, int maxResults, SearchMetric metric, ChunkingStrategy strategy) {
        Embedding queryEmbedding = embeddingService.generateEmbedding(query);

        return indexes.getOrDefault(strategy, List.of()).stream()
                .map(indexedSegment -> toSearchMatch(indexedSegment, queryEmbedding, metric))
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(maxResults)
                .toList();
    }

    public int embeddingDimension() {
        return embeddingService.dimension();
    }

    public int indexedSegmentCount(ChunkingStrategy strategy) {
        return indexes.getOrDefault(strategy, List.of()).size();
    }

    private List<IndexedSegment> buildIndex(List<Document> documents, ChunkingStrategy strategy) {
        return documents.stream()
                .flatMap(document -> chunkDocument(document, strategy).stream())
                .map(segment -> new IndexedSegment(segment, embeddingService.generateEmbedding(segment.text())))
                .toList();
    }

    private List<TextSegment> chunkDocument(Document document, ChunkingStrategy strategy) {
        List<TextSegment> segments = switch (strategy) {
            case RECURSIVE -> documentChunker.recursiveChunk(document, DEFAULT_RECURSIVE_CHUNK_SIZE, DEFAULT_RECURSIVE_OVERLAP);
            case PARAGRAPH -> documentChunker.paragraphChunk(document);
        };

        return annotateSegments(document, segments, strategy);
    }

    private List<TextSegment> annotateSegments(
            Document document,
            List<TextSegment> segments,
            ChunkingStrategy strategy) {
        return IntStream.range(0, segments.size())
                .mapToObj(index -> {
                    TextSegment segment = segments.get(index);
                    Metadata metadata = document.metadata().copy()
                            .put("chunkingStrategy", strategy.name())
                            .put("chunkIndex", index);
                    return TextSegment.from(segment.text(), metadata);
                })
                .toList();
    }

    private SearchMatch toSearchMatch(IndexedSegment indexedSegment, Embedding queryEmbedding, SearchMetric metric) {
        double score = similarityCalculator.score(
                queryEmbedding.vector(),
                indexedSegment.embedding().vector(),
                metric);

        return new SearchMatch(
                indexedSegment.segment().text(),
                score,
                indexedSegment.segment().metadata());
    }
}
