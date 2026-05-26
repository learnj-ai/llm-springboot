# Module 01: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 01: Vector Embeddings and Semantic Search.

---

## Chapter 2: Embedding Service - Solutions

### Exercise 1: Add a similarity method to EmbeddingService

**Solution:**

```java
@Service
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    private final SimilarityCalculator similarityCalculator;

    public EmbeddingService(EmbeddingModel embeddingModel, 
                           SimilarityCalculator similarityCalculator) {
        this.embeddingModel = embeddingModel;
        this.similarityCalculator = similarityCalculator;
    }

    public double similarity(String text1, String text2) {
        float[] vector1 = getVector(text1);
        float[] vector2 = getVector(text2);
        return similarityCalculator.cosineSimilarity(vector1, vector2);
    }

    // ... existing methods
}
```

### Exercise 2: Test similar texts

**Solution:**

```java
@SpringBootTest
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void similarTextsShouldHaveHighSimilarity() {
        double score = embeddingService.similarity(
            "reset my password",
            "change my password"
        );
        assertThat(score).isGreaterThan(0.7);
    }

    @Test
    void dissimilarTextsShouldHaveLowSimilarity() {
        double score = embeddingService.similarity(
            "reset my password",
            "API rate limits"
        );
        assertThat(score).isLessThan(0.3);
    }
}
```

### Exercise 3 (Bonus): Create similarity endpoint

**Solution:**

```java
@RestController
@RequestMapping("/api/v1")
public class SimilarityController {

    private final EmbeddingService embeddingService;

    public SimilarityController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/similarity")
    public SimilarityResponse calculateSimilarity(@RequestBody SimilarityRequest request) {
        double score = embeddingService.similarity(request.text1(), request.text2());
        return new SimilarityResponse(request.text1(), request.text2(), score);
    }

    public record SimilarityRequest(String text1, String text2) {}
    public record SimilarityResponse(String text1, String text2, double similarity) {}
}
```

### Exercise 4 (Challenge): Log embedding dimensions

**Solution:**

```java
public void logEmbeddingDimensions(String text) {
    float[] vector = getVector(text);
    logger.info("First 10 dimensions of '{}': {}", 
        text, 
        Arrays.toString(Arrays.copyOf(vector, 10))
    );
}

// Usage:
embeddingService.logEmbeddingDimensions("cat");
embeddingService.logEmbeddingDimensions("dog");
```

**Expected Output:**
```
First 10 dimensions of 'cat': [0.123, -0.456, 0.789, 0.234, -0.567, 0.891, -0.123, 0.456, -0.789, 0.012]
First 10 dimensions of 'dog': [0.145, -0.423, 0.812, 0.267, -0.534, 0.923, -0.098, 0.489, -0.756, 0.034]
```

---

## Chapter 3: Document Chunker - Solutions

### Exercise 1: Add chunk count validation

**Solution:**

```java
@Test
void longDocumentShouldProduceMultipleChunks() {
    String longText = "This is a sentence. ".repeat(100); // 2000 characters
    List<String> chunks = documentChunker.chunkDocument(longText, 500, 100);
    
    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks).allMatch(chunk -> chunk.length() <= 600); // maxSize + overlap
}
```

### Exercise 2: Test overlap behavior

**Solution:**

```java
@Test
void chunksShouldOverlapCorrectly() {
    String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";
    List<String> chunks = documentChunker.chunkDocument(text, 30, 10);
    
    // Verify overlap: end of chunk N should appear in start of chunk N+1
    for (int i = 0; i < chunks.size() - 1; i++) {
        String currentChunk = chunks.get(i);
        String nextChunk = chunks.get(i + 1);
        
        // Last 10 chars of current should match first 10 chars of next (approximately)
        String overlap = currentChunk.substring(Math.max(0, currentChunk.length() - 15));
        assertThat(nextChunk).contains(overlap.trim());
    }
}
```

### Exercise 3 (Bonus): Test paragraph preservation

**Solution:**

```java
@Test
void paragraphsUnder500CharsShouldBePreserved() {
    String text = """
        First paragraph with some content.
        
        Second paragraph with more content.
        
        Third paragraph.
        """;
    
    List<String> chunks = documentChunker.chunkDocument(text, 500, 50);
    
    // Should keep paragraphs intact
    assertThat(chunks.get(0)).contains("First paragraph");
    assertThat(chunks.get(0)).doesNotContain("Second paragraph");
}
```

### Exercise 4 (Challenge): Handle abbreviations

**Solution:**

```java
// Updated regex pattern in DocumentChunker
private static final Pattern SENTENCE_PATTERN = Pattern.compile(
    "(?<![A-Z]\\.)(?<=\\.|\\!|\\?)\\s+(?=[A-Z])"
);

// This uses negative lookbehind to avoid splitting on "Dr." or "Mr."
// (?<![A-Z]\\.) means "not preceded by uppercase letter and period"
```

**Test:**

```java
@Test
void shouldHandleAbbreviationsCorrectly() {
    String text = "Dr. Smith works here. Mr. Jones does too. This is a sentence.";
    List<String> sentences = documentChunker.splitIntoSentences(text);
    
    assertThat(sentences).hasSize(3);
    assertThat(sentences.get(0)).contains("Dr. Smith");
    assertThat(sentences.get(1)).contains("Mr. Jones");
}
```

---

## Chapter 4: Similarity Calculator - Solutions

### Exercise 1: Implement dot product similarity

**Solution:**

```java
public double dotProduct(float[] vectorA, float[] vectorB) {
    validateVectors(vectorA, vectorB);
    
    double sum = 0.0;
    for (int i = 0; i < vectorA.length; i++) {
        sum += vectorA[i] * vectorB[i];
    }
    return sum;
}
```

### Exercise 2: Write tests for edge cases

**Solution:**

```java
@Test
void identicalVectorsShouldHaveSimilarityOne() {
    float[] vector = {1.0f, 2.0f, 3.0f};
    double similarity = calculator.cosineSimilarity(vector, vector);
    assertThat(similarity).isCloseTo(1.0, within(0.0001));
}

@Test
void oppositeVectorsShouldHaveNegativeSimilarity() {
    float[] vectorA = {1.0f, 2.0f, 3.0f};
    float[] vectorB = {-1.0f, -2.0f, -3.0f};
    double similarity = calculator.cosineSimilarity(vectorA, vectorB);
    assertThat(similarity).isCloseTo(-1.0, within(0.0001));
}

@Test
void orthogonalVectorsShouldHaveZeroSimilarity() {
    float[] vectorA = {1.0f, 0.0f};
    float[] vectorB = {0.0f, 1.0f};
    double similarity = calculator.cosineSimilarity(vectorA, vectorB);
    assertThat(similarity).isCloseTo(0.0, within(0.0001));
}
```

### Exercise 3 (Bonus): Implement Manhattan distance

**Solution:**

```java
public double manhattanDistance(float[] vectorA, float[] vectorB) {
    validateVectors(vectorA, vectorB);
    
    double sum = 0.0;
    for (int i = 0; i < vectorA.length; i++) {
        sum += Math.abs(vectorA[i] - vectorB[i]);
    }
    return sum;
}
```

### Exercise 4 (Challenge): Normalize euclidean scores

**Solution:**

```java
public double normalizedEuclideanScore(float[] vectorA, float[] vectorB) {
    double distance = euclideanDistance(vectorA, vectorB);
    return 1.0 / (1.0 + distance);
}

@Test
void normalizedScoreShouldBeInRange() {
    float[] vectorA = {1.0f, 2.0f, 3.0f};
    float[] vectorB = {4.0f, 5.0f, 6.0f};
    
    double score = calculator.normalizedEuclideanScore(vectorA, vectorB);
    
    assertThat(score).isBetween(0.0, 1.0);
}
```

---

## Chapter 5: Vector Store Service - Solutions

### Exercise 1: Add metadata filtering

**Solution:**

```java
public List<SearchResult> searchWithMetadata(String query, 
                                             Map<String, String> requiredMetadata, 
                                             int limit) {
    var embedding = embeddingService.generateEmbedding(query);
    
    return segments.stream()
        .filter(segment -> matchesMetadata(segment, requiredMetadata))
        .map(segment -> new ScoredSegment(
            segment,
            similarityCalculator.score(embedding.vector(), segment.vector())
        ))
        .sorted(Comparator.comparingDouble(ScoredSegment::score).reversed())
        .limit(limit)
        .map(scored -> new SearchResult(scored.segment().text(), scored.score()))
        .toList();
}

private boolean matchesMetadata(IndexedSegment segment, 
                                Map<String, String> requiredMetadata) {
    return requiredMetadata.entrySet().stream()
        .allMatch(entry -> entry.getValue()
            .equals(segment.metadata().get(entry.getKey())));
}
```

### Exercise 2: Test metadata search

**Solution:**

```java
@Test
void shouldFilterByMetadata() {
    Map<String, String> metadata = Map.of("category", "authentication");
    List<SearchResult> results = vectorStoreService.searchWithMetadata(
        "password reset",
        metadata,
        5
    );
    
    assertThat(results).isNotEmpty();
    // Verify all results have the required metadata
}
```

### Exercise 3 (Bonus): Add metrics logging

**Solution:**

```java
@Service
public class VectorStoreService {
    
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong totalSearchTime = new AtomicLong(0);
    
    public List<SearchResult> search(String query, String strategy, int limit) {
        long startTime = System.currentTimeMillis();
        
        var results = performSearch(query, strategy, limit);
        
        long duration = System.currentTimeMillis() - startTime;
        totalSearches.incrementAndGet();
        totalSearchTime.addAndGet(duration);
        
        logger.info("Search completed in {}ms. Avg: {}ms over {} searches",
            duration,
            totalSearchTime.get() / totalSearches.get(),
            totalSearches.get()
        );
        
        return results;
    }
}
```

### Exercise 4 (Challenge): Implement query cache

**Solution:**

```java
@Service
public class VectorStoreService {
    
    private final Map<String, Embedding> queryCache = 
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Embedding> eldest) {
                return size() > 10;
            }
        };
    
    public List<SearchResult> search(String query, String strategy, int limit) {
        Embedding embedding = queryCache.get(query);
        
        if (embedding == null) {
            embedding = embeddingService.generateEmbedding(query);
            queryCache.put(query, embedding);
            logger.debug("Cache miss for query: {}", query);
        } else {
            logger.debug("Cache hit for query: {}", query);
        }
        
        return performSearch(embedding, strategy, limit);
    }
}
```

---

## Chapter 6: Document Loader - Solutions

### Exercise 1: Load documents from a directory

**Solution:**

```java
@PostConstruct
public void loadDocumentsFromDirectory() throws IOException {
    Path docsPath = Paths.get("src/main/resources/data");
    
    try (var stream = Files.list(docsPath)) {
        List<Document> allDocuments = stream
            .filter(path -> path.toString().endsWith(".txt"))
            .map(this::loadDocument)
            .toList();
        
        vectorStoreService.indexDocuments(allDocuments);
        logger.info("Loaded {} documents from {}", allDocuments.size(), docsPath);
    }
}

private Document loadDocument(Path path) {
    try {
        String content = Files.readString(path);
        Map<String, String> metadata = Map.of(
            "filename", path.getFileName().toString(),
            "path", path.toString()
        );
        return new Document(content, metadata);
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}
```

### Exercise 2: Test document count

**Solution:**

```java
@Test
void shouldLoadExpectedNumberOfDocuments() throws IOException {
    DocumentLoader loader = new DocumentLoader(vectorStoreService);
    
    // Assuming we have 5 .txt files in the data directory
    int expectedCount = 5;
    
    loader.loadDocumentsFromDirectory();
    
    // Query vector store to verify
    var results = vectorStoreService.search("test", "cosine", 100);
    assertThat(results.size()).isGreaterThanOrEqualTo(expectedCount);
}
```

### Exercise 3 (Bonus): Support multiple file types

**Solution:**

```java
@PostConstruct
public void loadDocumentsFromDirectory() throws IOException {
    Path docsPath = Paths.get("src/main/resources/data");
    
    try (var stream = Files.list(docsPath)) {
        List<Document> allDocuments = stream
            .filter(this::isSupportedFileType)
            .map(this::loadDocumentWithType)
            .toList();
        
        vectorStoreService.indexDocuments(allDocuments);
    }
}

private boolean isSupportedFileType(Path path) {
    String filename = path.getFileName().toString().toLowerCase();
    return filename.endsWith(".txt") || 
           filename.endsWith(".md") || 
           filename.endsWith(".pdf");
}

private Document loadDocumentWithType(Path path) {
    try {
        String content = loadContent(path);
        Map<String, String> metadata = Map.of(
            "filename", path.getFileName().toString(),
            "path", path.toString(),
            "fileType", getFileExtension(path)
        );
        return new Document(content, metadata);
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}

private String getFileExtension(Path path) {
    String filename = path.getFileName().toString();
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
}
```

### Exercise 4 (Challenge): Recursive directory scanner

**Solution:**

```java
public void loadDocumentsRecursively(Path rootPath) throws IOException {
    try (var stream = Files.walk(rootPath)) {
        List<Document> allDocuments = stream
            .filter(Files::isRegularFile)
            .filter(this::isSupportedFileType)
            .map(path -> loadDocumentWithPath(path, rootPath))
            .toList();
        
        vectorStoreService.indexDocuments(allDocuments);
        logger.info("Recursively loaded {} documents from {}", 
            allDocuments.size(), rootPath);
    }
}

private Document loadDocumentWithPath(Path filePath, Path rootPath) {
    try {
        String content = Files.readString(filePath);
        String relativePath = rootPath.relativize(filePath).toString();
        
        Map<String, String> metadata = Map.of(
            "filename", filePath.getFileName().toString(),
            "path", relativePath,
            "fullPath", filePath.toString(),
            "directory", filePath.getParent().getFileName().toString()
        );
        
        return new Document(content, metadata);
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}
```

---

## Chapter 7: Vector Search Controller - Solutions

### Exercise 1: Add request validation

**Solution:**

```java
@PostMapping("/search")
public ResponseEntity<SearchResponse> search(@RequestBody @Valid SearchRequest request) {
    if (request.query() == null || request.query().isBlank()) {
        return ResponseEntity.badRequest()
            .body(new SearchResponse(List.of(), "Query cannot be empty", 0));
    }
    
    if (request.limit() < 1 || request.limit() > 100) {
        return ResponseEntity.badRequest()
            .body(new SearchResponse(List.of(), "Limit must be between 1 and 100", 0));
    }
    
    return processSearch(request);
}

public record SearchRequest(
    @NotBlank(message = "Query is required") String query,
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    int limit,
    String strategy
) {}
```

### Exercise 2: Test validation

**Solution:**

```java
@Test
void shouldRejectEmptyQuery() throws Exception {
    mockMvc.perform(post("/api/v1/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "query": "",
                "limit": 5,
                "strategy": "cosine"
            }
            """))
        .andExpect(status().isBadRequest());
}

@Test
void shouldRejectInvalidLimit() throws Exception {
    mockMvc.perform(post("/api/v1/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "query": "test",
                "limit": 200,
                "strategy": "cosine"
            }
            """))
        .andExpect(status().isBadRequest());
}
```

### Exercise 3 (Bonus): Batch search endpoint

**Solution:**

```java
@PostMapping("/search/batch")
public ResponseEntity<BatchSearchResponse> batchSearch(
        @RequestBody BatchSearchRequest request) {
    
    List<SearchResponse> responses = request.queries().stream()
        .map(query -> {
            var results = vectorStoreService.search(
                query, 
                request.strategy(), 
                request.limit()
            );
            return new SearchResponse(results, "Success", results.size());
        })
        .toList();
    
    return ResponseEntity.ok(new BatchSearchResponse(responses));
}

public record BatchSearchRequest(
    List<String> queries,
    int limit,
    String strategy
) {}

public record BatchSearchResponse(List<SearchResponse> results) {}
```

### Exercise 4 (Challenge): Request caching

**Solution:**

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("searchResults");
    }
}

@Service
public class VectorSearchService {
    
    @Cacheable(value = "searchResults", 
               key = "#query + '-' + #strategy + '-' + #limit")
    public List<SearchResult> search(String query, String strategy, int limit) {
        logger.info("Cache miss - performing search for: {}", query);
        return vectorStoreService.search(query, strategy, limit);
    }
}
```

---

## Chapter 8: Configuration and Models - Solutions

### Exercise 1: Add validation to records

**Solution:**

```java
public record SearchRequest(
    @NotBlank(message = "Query is required")
    @Size(min = 1, max = 1000, message = "Query must be between 1 and 1000 characters")
    String query,
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    int limit,
    
    @Pattern(regexp = "cosine|euclidean|dot", 
             message = "Strategy must be cosine, euclidean, or dot")
    String strategy
) {}
```

### Exercise 2: Test validation

**Solution:**

```java
@Test
void shouldValidateSearchRequest() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    SearchRequest invalid = new SearchRequest("", -1, "invalid");
    Set<ConstraintViolation<SearchRequest>> violations = validator.validate(invalid);
    
    assertThat(violations).hasSize(3);
    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactlyInAnyOrder("query", "limit", "strategy");
}

@Test
void shouldPassValidRequest() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    SearchRequest valid = new SearchRequest("test query", 10, "cosine");
    Set<ConstraintViolation<SearchRequest>> violations = validator.validate(valid);
    
    assertThat(violations).isEmpty();
}
```

### Exercise 3 (Bonus): Paged search request

**Solution:**

```java
public record PagedSearchRequest(
    @NotBlank String query,
    
    @Min(0) int offset,
    
    @Min(1) @Max(100) int pageSize,
    
    String strategy
) {
    public PagedSearchRequest {
        if (strategy == null || strategy.isBlank()) {
            strategy = "cosine";
        }
    }
}

@PostMapping("/search/paged")
public ResponseEntity<PagedSearchResponse> pagedSearch(
        @RequestBody @Valid PagedSearchRequest request) {
    
    var allResults = vectorStoreService.search(
        request.query(), 
        request.strategy(), 
        request.offset() + request.pageSize()
    );
    
    var pagedResults = allResults.stream()
        .skip(request.offset())
        .limit(request.pageSize())
        .toList();
    
    return ResponseEntity.ok(new PagedSearchResponse(
        pagedResults,
        request.offset(),
        request.pageSize(),
        allResults.size()
    ));
}
```

### Exercise 4 (Challenge): Configuration properties

**Solution:**

```java
@ConfigurationProperties(prefix = "embedding")
public record EmbeddingProperties(
    String modelName,
    int dimension,
    int maxTokens,
    boolean cacheEnabled,
    int cacheSize
) {
    public EmbeddingProperties {
        if (modelName == null || modelName.isBlank()) {
            modelName = "all-MiniLM-L6-v2";
        }
        if (dimension <= 0) {
            dimension = 384;
        }
        if (maxTokens <= 0) {
            maxTokens = 512;
        }
        if (cacheSize <= 0) {
            cacheSize = 10;
        }
    }
}

// application.yml
embedding:
  model-name: all-MiniLM-L6-v2
  dimension: 384
  max-tokens: 512
  cache-enabled: true
  cache-size: 100
```

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Next Module: Advanced RAG Techniques](../module-02-advanced-rag/README.md)**
