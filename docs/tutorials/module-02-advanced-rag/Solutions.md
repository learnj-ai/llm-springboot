# Module 02: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 02: Advanced RAG Techniques.

---

## Chapter 2: Query Transformation - Solutions

### Exercise 1: Implement multi-query generation

**Solution:**

```java
@Service
public class QueryTransformationService {
    
    private final ChatLanguageModel chatModel;
    
    public List<String> generateMultipleQueries(String originalQuery, int count) {
        String prompt = String.format("""
            Given the user query: "%s"
            
            Generate %d alternative phrasings of this query that maintain the same intent.
            Each alternative should use different words and sentence structure.
            
            Return only the alternative queries, one per line.
            """, originalQuery, count);
        
        String response = chatModel.generate(prompt);
        
        return Arrays.stream(response.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(count)
            .toList();
    }
}
```

### Exercise 2: Test query transformation

**Solution:**

```java
@Test
void shouldGenerateMultipleQueries() {
    List<String> queries = queryTransformationService
        .generateMultipleQueries("How do I reset my password?", 3);
    
    assertThat(queries).hasSize(3);
    assertThat(queries).allMatch(q -> q.length() > 5);
    assertThat(queries).doesNotContain("How do I reset my password?");
}

@Test
void shouldMaintainQueryIntent() {
    List<String> queries = queryTransformationService
        .generateMultipleQueries("password reset", 2);
    
    assertThat(queries).allMatch(q -> 
        q.toLowerCase().contains("password") || 
        q.toLowerCase().contains("credential")
    );
}
```

### Exercise 3 (Bonus): Implement query decomposition

**Solution:**

```java
public List<String> decompose Query(String complexQuery) {
    String prompt = String.format("""
        Break down this complex query into simpler sub-queries:
        "%s"
        
        Each sub-query should be independently answerable.
        Return one sub-query per line.
        """, complexQuery);
    
    String response = chatModel.generate(prompt);
    
    return Arrays.stream(response.split("\n"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
}

@Test
void shouldDecomposeComplexQuery() {
    String complex = "What are the authentication methods and how do I configure rate limiting?";
    List<String> subQueries = queryTransformationService.decomposeQuery(complex);
    
    assertThat(subQueries).hasSizeGreaterThanOrEqualTo(2);
    assertThat(subQueries).anyMatch(q -> q.toLowerCase().contains("authentication"));
    assertThat(subQueries).anyMatch(q -> q.toLowerCase().contains("rate limiting"));
}
```

### Exercise 4 (Challenge): Query expansion with synonyms

**Solution:**

```java
public List<String> expandWithSynonyms(String query) {
    String prompt = String.format("""
        Expand this query with synonyms and related terms:
        "%s"
        
        Generate 3 expanded versions that include:
        - Technical synonyms
        - Related concepts
        - Alternative terminology
        
        Return one expanded query per line.
        """, query);
    
    String response = chatModel.generate(prompt);
    
    return Arrays.stream(response.split("\n"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
}
```

---

## Chapter 3: Keyword Search Service - Solutions

### Exercise 1: Implement TF-IDF scoring

**Solution:**

```java
public class TfIdfCalculator {
    
    public double calculateTF(String term, List<String> document) {
        long termCount = document.stream()
            .filter(word -> word.equalsIgnoreCase(term))
            .count();
        
        return (double) termCount / document.size();
    }
    
    public double calculateIDF(String term, List<List<String>> corpus) {
        long docsWithTerm = corpus.stream()
            .filter(doc -> doc.stream()
                .anyMatch(word -> word.equalsIgnoreCase(term)))
            .count();
        
        return Math.log((double) corpus.size() / (1 + docsWithTerm));
    }
    
    public double calculateTfIdf(String term, List<String> document, 
                                 List<List<String>> corpus) {
        return calculateTF(term, document) * calculateIDF(term, corpus);
    }
}
```

### Exercise 2: Test BM25 implementation

**Solution:**

```java
@Test
void bm25ShouldRankRelevantDocsHigher() {
    KeywordSearchService service = new KeywordSearchService();
    
    // Index documents
    service.indexDocument("doc1", "Java Spring Boot tutorial");
    service.indexDocument("doc2", "Python machine learning");
    service.indexDocument("doc3", "Java Spring framework guide");
    
    List<SearchResult> results = service.search("Java Spring", 10);
    
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).documentId()).isIn("doc1", "doc3");
}

@Test
void bm25ShouldHandleTermFrequency() {
    KeywordSearchService service = new KeywordSearchService();
    
    service.indexDocument("doc1", "Java Java Java Spring");
    service.indexDocument("doc2", "Java Spring Boot");
    
    List<SearchResult> results = service.search("Java", 10);
    
    // doc1 should rank higher due to higher term frequency
    assertThat(results.get(0).documentId()).isEqualTo("doc1");
}
```

### Exercise 3 (Bonus): Implement phrase search

**Solution:**

```java
public List<SearchResult> phraseSearch(String phrase, int limit) {
    String[] terms = phrase.toLowerCase().split("\\s+");
    
    return documents.entrySet().stream()
        .filter(entry -> containsPhrase(entry.getValue(), terms))
        .map(entry -> new SearchResult(
            entry.getKey(),
            calculatePhraseScore(entry.getValue(), terms)
        ))
        .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
        .limit(limit)
        .toList();
}

private boolean containsPhrase(String document, String[] terms) {
    String[] words = document.toLowerCase().split("\\s+");
    
    for (int i = 0; i <= words.length - terms.length; i++) {
        boolean match = true;
        for (int j = 0; j < terms.length; j++) {
            if (!words[i + j].equals(terms[j])) {
                match = false;
                break;
            }
        }
        if (match) return true;
    }
    return false;
}
```

### Exercise 4 (Challenge): N-gram indexing

**Solution:**

```java
public class NgramIndexer {
    
    private final Map<String, Set<String>> ngramIndex = new HashMap<>();
    
    public void indexDocument(String docId, String text, int n) {
        List<String> ngrams = generateNgrams(text, n);
        
        for (String ngram : ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
        }
    }
    
    private List<String> generateNgrams(String text, int n) {
        String[] words = text.toLowerCase().split("\\s+");
        List<String> ngrams = new ArrayList<>();
        
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder ngram = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) ngram.append(" ");
                ngram.append(words[i + j]);
            }
            ngrams.add(ngram.toString());
        }
        
        return ngrams;
    }
    
    public Set<String> searchNgram(String ngram) {
        return ngramIndex.getOrDefault(ngram.toLowerCase(), Set.of());
    }
}
```

---

## Chapter 4: Hybrid Search Service - Solutions

### Exercise 1: Implement weighted hybrid search

**Solution:**

```java
@Service
public class HybridSearchService {
    
    private final VectorSearchService vectorSearch;
    private final KeywordSearchService keywordSearch;
    
    public List<SearchResult> hybridSearch(String query, 
                                          double vectorWeight, 
                                          double keywordWeight, 
                                          int limit) {
        var vectorResults = vectorSearch.search(query, limit * 2);
        var keywordResults = keywordSearch.search(query, limit * 2);
        
        Map<String, Double> combinedScores = new HashMap<>();
        
        // Combine vector scores
        for (var result : vectorResults) {
            combinedScores.merge(result.documentId(), 
                result.score() * vectorWeight, 
                Double::sum);
        }
        
        // Combine keyword scores
        for (var result : keywordResults) {
            combinedScores.merge(result.documentId(), 
                result.score() * keywordWeight, 
                Double::sum);
        }
        
        return combinedScores.entrySet().stream()
            .map(e -> new SearchResult(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
            .limit(limit)
            .toList();
    }
}
```

### Exercise 2: Test hybrid search combinations

**Solution:**

```java
@Test
void vectorOnlySearchShouldMatchVectorResults() {
    var hybrid = hybridService.hybridSearch("test query", 1.0, 0.0, 5);
    var vector = vectorService.search("test query", 5);
    
    assertThat(hybrid).hasSameSizeAs(vector);
}

@Test
void keywordOnlySearchShouldMatchKeywordResults() {
    var hybrid = hybridService.hybridSearch("test query", 0.0, 1.0, 5);
    var keyword = keywordService.search("test query", 5);
    
    assertThat(hybrid).hasSameSizeAs(keyword);
}

@Test
void balancedSearchShouldCombineResults() {
    var hybrid = hybridService.hybridSearch("test query", 0.5, 0.5, 5);
    
    assertThat(hybrid).isNotEmpty();
    assertThat(hybrid.size()).isLessThanOrEqualTo(5);
}
```

### Exercise 3 (Bonus): Reciprocal Rank Fusion

**Solution:**

```java
public List<SearchResult> reciprocalRankFusion(String query, int limit, int k) {
    var vectorResults = vectorSearch.search(query, limit * 2);
    var keywordResults = keywordSearch.search(query, limit * 2);
    
    Map<String, Double> rrfScores = new HashMap<>();
    
    // Calculate RRF for vector results
    for (int i = 0; i < vectorResults.size(); i++) {
        String docId = vectorResults.get(i).documentId();
        double score = 1.0 / (k + i + 1);
        rrfScores.merge(docId, score, Double::sum);
    }
    
    // Calculate RRF for keyword results
    for (int i = 0; i < keywordResults.size(); i++) {
        String docId = keywordResults.get(i).documentId();
        double score = 1.0 / (k + i + 1);
        rrfScores.merge(docId, score, Double::sum);
    }
    
    return rrfScores.entrySet().stream()
        .map(e -> new SearchResult(e.getKey(), e.getValue()))
        .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
        .limit(limit)
        .toList();
}
```

### Exercise 4 (Challenge): Adaptive weighting

**Solution:**

```java
public List<SearchResult> adaptiveHybridSearch(String query, int limit) {
    double vectorWeight = calculateVectorWeight(query);
    double keywordWeight = 1.0 - vectorWeight;
    
    return hybridSearch(query, vectorWeight, keywordWeight, limit);
}

private double calculateVectorWeight(String query) {
    // Longer queries → favor vector search (semantic understanding)
    // Shorter queries → favor keyword search (exact matching)
    int wordCount = query.split("\\s+").length;
    
    if (wordCount <= 2) {
        return 0.3; // 30% vector, 70% keyword
    } else if (wordCount <= 5) {
        return 0.5; // Balanced
    } else {
        return 0.7; // 70% vector, 30% keyword
    }
}
```

---

## Chapter 5: Re-Ranking - Solutions

### Exercise 1: Implement cross-encoder re-ranking

**Solution:**

```java
@Service
public class ReRankingService {
    
    private final ChatLanguageModel chatModel;
    
    public List<SearchResult> rerank(String query, List<SearchResult> results, int limit) {
        List<ScoredResult> reranked = results.stream()
            .map(result -> {
                double relevanceScore = calculateRelevance(query, result.text());
                return new ScoredResult(result, relevanceScore);
            })
            .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
            .limit(limit)
            .map(ScoredResult::result)
            .toList();
        
        return reranked;
    }
    
    private double calculateRelevance(String query, String document) {
        String prompt = String.format("""
            Query: %s
            Document: %s
            
            Rate the relevance of this document to the query on a scale of 0.0 to 1.0.
            Return only the numeric score.
            """, query, document);
        
        String response = chatModel.generate(prompt).trim();
        
        try {
            return Double.parseDouble(response);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    record ScoredResult(SearchResult result, double score) {}
}
```

### Exercise 2: Test re-ranking improves results

**Solution:**

```java
@Test
void rerankingShouldImproveTopResults() {
    List<SearchResult> original = List.of(
        new SearchResult("doc1", 0.8, "Irrelevant content"),
        new SearchResult("doc2", 0.75, "Very relevant to query"),
        new SearchResult("doc3", 0.7, "Somewhat relevant")
    );
    
    List<SearchResult> reranked = reRankingService.rerank("specific query", original, 3);
    
    // Top result after re-ranking should be more relevant
    assertThat(reranked.get(0).documentId()).isEqualTo("doc2");
}
```

### Exercise 3 (Bonus): Diversity-aware re-ranking

**Solution:**

```java
public List<SearchResult> diversityRerank(String query, 
                                         List<SearchResult> results, 
                                         int limit,
                                         double diversityWeight) {
    List<SearchResult> selected = new ArrayList<>();
    List<SearchResult> candidates = new ArrayList<>(results);
    
    // Select first result (highest score)
    if (!candidates.isEmpty()) {
        selected.add(candidates.remove(0));
    }
    
    // Select remaining results with diversity penalty
    while (selected.size() < limit && !candidates.isEmpty()) {
        SearchResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        
        for (int i = 0; i < candidates.size(); i++) {
            SearchResult candidate = candidates.get(i);
            double diversityPenalty = calculateDiversityPenalty(candidate, selected);
            double adjustedScore = candidate.score() - (diversityWeight * diversityPenalty);
            
            if (adjustedScore > bestScore) {
                bestScore = adjustedScore;
                best = candidate;
                bestIndex = i;
            }
        }
        
        if (best != null) {
            selected.add(best);
            candidates.remove(bestIndex);
        }
    }
    
    return selected;
}

private double calculateDiversityPenalty(SearchResult candidate, 
                                        List<SearchResult> selected) {
    return selected.stream()
        .mapToDouble(s -> calculateSimilarity(candidate.text(), s.text()))
        .average()
        .orElse(0.0);
}
```

### Exercise 4 (Challenge): Learning-to-rank

**Solution:**

```java
public class LearnToRankService {
    
    private final Map<String, Double> featureWeights = new HashMap<>();
    
    public List<SearchResult> rankWithFeatures(String query, List<SearchResult> results) {
        return results.stream()
            .map(result -> {
                Map<String, Double> features = extractFeatures(query, result);
                double score = calculateWeightedScore(features);
                return new ScoredResult(result, score);
            })
            .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
            .map(ScoredResult::result)
            .toList();
    }
    
    private Map<String, Double> extractFeatures(String query, SearchResult result) {
        Map<String, Double> features = new HashMap<>();
        
        features.put("bm25_score", result.score());
        features.put("query_length", (double) query.split("\\s+").length);
        features.put("doc_length", (double) result.text().split("\\s+").length);
        features.put("exact_match", query.toLowerCase().contains(result.text().toLowerCase()) ? 1.0 : 0.0);
        features.put("term_overlap", calculateTermOverlap(query, result.text()));
        
        return features;
    }
    
    private double calculateWeightedScore(Map<String, Double> features) {
        return features.entrySet().stream()
            .mapToDouble(e -> e.getValue() * featureWeights.getOrDefault(e.getKey(), 1.0))
            .sum();
    }
    
    private double calculateTermOverlap(String query, String text) {
        Set<String> queryTerms = Set.of(query.toLowerCase().split("\\s+"));
        Set<String> docTerms = Set.of(text.toLowerCase().split("\\s+"));
        
        long overlap = queryTerms.stream()
            .filter(docTerms::contains)
            .count();
        
        return (double) overlap / queryTerms.size();
    }
}
```

---

## Chapter 6: RAG Service - Solutions

### Exercise 1: Build complete RAG pipeline

**Solution:**

```java
@Service
public class RagService {
    
    private final HybridSearchService searchService;
    private final ReRankingService reRankingService;
    private final ChatLanguageModel chatModel;
    
    public String answer(String question, int retrievalLimit) {
        // 1. Retrieve relevant documents
        var searchResults = searchService.hybridSearch(question, 0.6, 0.4, retrievalLimit);
        
        // 2. Re-rank results
        var reranked = reRankingService.rerank(question, searchResults, 5);
        
        // 3. Build context from top results
        String context = reranked.stream()
            .map(SearchResult::text)
            .collect(Collectors.joining("\n\n"));
        
        // 4. Generate answer
        String prompt = String.format("""
            Answer the question based on the context below.
            
            Context:
            %s
            
            Question: %s
            
            Answer:
            """, context, question);
        
        return chatModel.generate(prompt);
    }
}
```

### Exercise 2: Test RAG pipeline

**Solution:**

```java
@Test
void ragShouldAnswerFromContext() {
    String question = "How do I reset my password?";
    String answer = ragService.answer(question, 10);
    
    assertThat(answer).isNotBlank();
    assertThat(answer.toLowerCase()).contains("password");
}

@Test
void ragShouldUseRetrievedContext() {
    // Mock the search service to return specific documents
    when(searchService.hybridSearch(anyString(), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(
            new SearchResult("doc1", 0.9, "To reset your password, click the forgot password link")
        ));
    
    String answer = ragService.answer("password reset", 10);
    
    assertThat(answer).contains("forgot password");
}
```

### Exercise 3 (Bonus): Multi-step reasoning

**Solution:**

```java
public String answerWithReasoning(String question, int retrievalLimit) {
    // Step 1: Decompose complex question
    List<String> subQuestions = decomposeQuestion(question);
    
    // Step 2: Answer each sub-question
    List<String> subAnswers = subQuestions.stream()
        .map(q -> answer(q, retrievalLimit))
        .toList();
    
    // Step 3: Synthesize final answer
    String prompt = String.format("""
        Original question: %s
        
        Sub-questions and answers:
        %s
        
        Provide a comprehensive answer to the original question by synthesizing the sub-answers.
        """, question, formatSubAnswers(subQuestions, subAnswers));
    
    return chatModel.generate(prompt);
}

private List<String> decomposeQuestion(String question) {
    String prompt = String.format("""
        Break down this complex question into simpler sub-questions:
        %s
        
        Return one sub-question per line.
        """, question);
    
    return Arrays.stream(chatModel.generate(prompt).split("\n"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
}
```

### Exercise 4 (Challenge): Citation generation

**Solution:**

```java
public record RagAnswer(String answer, List<Citation> citations) {}

public record Citation(String text, String source, double relevanceScore) {}

public RagAnswer answerWithCitations(String question, int retrievalLimit) {
    var searchResults = searchService.hybridSearch(question, 0.6, 0.4, retrievalLimit);
    var reranked = reRankingService.rerank(question, searchResults, 5);
    
    String context = buildContextWithMarkers(reranked);
    
    String prompt = String.format("""
        Answer the question based on the numbered context passages below.
        Include [1], [2], etc. to cite which passage supports each claim.
        
        %s
        
        Question: %s
        
        Answer (with citations):
        """, context, question);
    
    String answer = chatModel.generate(prompt);
    
    List<Citation> citations = reranked.stream()
        .map(r -> new Citation(r.text(), r.documentId(), r.score()))
        .toList();
    
    return new RagAnswer(answer, citations);
}

private String buildContextWithMarkers(List<SearchResult> results) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < results.size(); i++) {
        sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).text()));
    }
    return sb.toString();
}
```

---

## Chapter 7: RAG Controller - Solutions

### Exercise 1: Add error handling

**Solution:**

```java
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {
    
    private final RagService ragService;
    
    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestBody @Valid RagRequest request) {
        try {
            String answer = ragService.answer(request.question(), request.limit());
            return ResponseEntity.ok(new RagResponse(answer, "Success", null));
        } catch (Exception e) {
            logger.error("Error processing RAG request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RagResponse(null, "Error", e.getMessage()));
        }
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RagResponse> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(new RagResponse(null, "Validation Error", e.getMessage()));
    }
}
```

### Exercise 2: Test error scenarios

**Solution:**

```java
@Test
void shouldHandleEmptyQuestion() throws Exception {
    mockMvc.perform(post("/api/v1/rag/ask")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "question": "",
                "limit": 5
            }
            """))
        .andExpect(status().isBadRequest());
}

@Test
void shouldHandleServiceException() throws Exception {
    when(ragService.answer(anyString(), anyInt()))
        .thenThrow(new RuntimeException("Service error"));
    
    mockMvc.perform(post("/api/v1/rag/ask")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "question": "test",
                "limit": 5
            }
            """))
        .andExpect(status().isInternalServerError());
}
```

### Exercise 3 (Bonus): Streaming responses

**Solution:**

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamAnswer(@RequestParam String question,
                                 @RequestParam(defaultValue = "10") int limit) {
    return Flux.create(sink -> {
        try {
            String answer = ragService.answer(question, limit);
            
            // Stream answer word by word
            String[] words = answer.split("\\s+");
            for (String word : words) {
                sink.next(word + " ");
                Thread.sleep(100); // Simulate streaming delay
            }
            
            sink.complete();
        } catch (Exception e) {
            sink.error(e);
        }
    });
}
```

### Exercise 4 (Challenge): Request metrics

**Solution:**

```java
@Component
public class RagMetricsInterceptor implements HandlerInterceptor {
    
    private final MeterRegistry meterRegistry;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        meterRegistry.timer("rag.request.duration",
            "method", request.getMethod(),
            "status", String.valueOf(response.getStatus()))
            .record(duration, TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("rag.request.total",
            "status", String.valueOf(response.getStatus()))
            .increment();
    }
}
```

---

## Chapter 8: Structured Concurrency - Solutions

### Exercise 1: Parallel retrieval with StructuredTaskScope

**Solution:**

```java
public List<SearchResult> parallelSearch(String query, int limit) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        
        // Launch parallel searches
        var vectorFuture = scope.fork(() -> vectorSearch.search(query, limit));
        var keywordFuture = scope.fork(() -> keywordSearch.search(query, limit));
        
        // Wait for all to complete
        scope.join();
        scope.throwIfFailed();
        
        // Combine results
        var vectorResults = vectorFuture.resultNow();
        var keywordResults = keywordFuture.resultNow();
        
        return mergeResults(vectorResults, keywordResults, limit);
    }
}
```

### Exercise 2: Test parallel execution

**Solution:**

```java
@Test
void parallelSearchShouldBeFaster() {
    long sequentialStart = System.currentTimeMillis();
    var seq1 = vectorSearch.search("query", 10);
    var seq2 = keywordSearch.search("query", 10);
    long sequentialDuration = System.currentTimeMillis() - sequentialStart;
    
    long parallelStart = System.currentTimeMillis();
    var parallelResults = searchService.parallelSearch("query", 10);
    long parallelDuration = System.currentTimeMillis() - parallelStart;
    
    assertThat(parallelDuration).isLessThan(sequentialDuration);
}
```

### Exercise 3 (Bonus): Timeout handling

**Solution:**

```java
public List<SearchResult> searchWithTimeout(String query, 
                                           int limit, 
                                           Duration timeout) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        
        var vectorFuture = scope.fork(() -> vectorSearch.search(query, limit));
        var keywordFuture = scope.fork(() -> keywordSearch.search(query, limit));
        
        scope.joinUntil(Instant.now().plus(timeout));
        scope.throwIfFailed();
        
        return mergeResults(
            vectorFuture.resultNow(), 
            keywordFuture.resultNow(), 
            limit
        );
    } catch (TimeoutException e) {
        logger.warn("Search timed out after {}", timeout);
        throw new SearchTimeoutException("Search exceeded timeout: " + timeout);
    }
}
```

### Exercise 4 (Challenge): Result aggregation

**Solution:**

```java
public class AggregatingScope<T> extends StructuredTaskScope<T> {
    
    private final List<T> results = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    protected void handleComplete(Subtask<? extends T> subtask) {
        if (subtask.state() == Subtask.State.SUCCESS) {
            results.add(subtask.get());
        }
    }
    
    public List<T> getResults() {
        return new ArrayList<>(results);
    }
}

public List<SearchResult> aggregateMultipleSearches(List<String> queries) throws Exception {
    try (var scope = new AggregatingScope<List<SearchResult>>()) {
        
        for (String query : queries) {
            scope.fork(() -> vectorSearch.search(query, 10));
        }
        
        scope.join();
        
        return scope.getResults().stream()
            .flatMap(List::stream)
            .distinct()
            .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
            .toList();
    }
}
```

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Next Module: Tools and Model Context Protocol](../module-03-tools-mcp/README.md)**
