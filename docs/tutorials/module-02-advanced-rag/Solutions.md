# Module 02: Solutions to Practice Exercises

This document contains solutions to all practice exercises in Module 02: Advanced RAG Techniques.

## How to Run and Test Solutions

### Prerequisites
- Java 21 or later (required for Virtual Threads and Structured Concurrency)
- Maven 3.9+
- OpenAI API key (or compatible API like Azure OpenAI)
- Docker & Docker Compose (for infrastructure services)

### Project Location
```bash
cd src/module-02-advanced-rag/
```

### Setup API Keys and Infrastructure
```bash
# 1. Set environment variables for OpenAI API
export OPENAI_API_KEY=your-api-key-here
export OPENAI_MODEL_NAME=gpt-4o-mini
# export OPENAI_API_BASE=https://api.openai.com/  # Optional: for custom endpoints

# 2. Start infrastructure services
docker compose up -d

# This starts:
# - ChromaDB (port 8000) - vector database
# - Redis (port 6379) - caching

# Verify services are running
docker ps
```

### Running the Application
```bash
# Build and run
mvn clean spring-boot:run

# The application starts on http://localhost:8082
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=HybridSearchServiceTest

# Run with verbose output
mvn test -X
```

---

## Chapter 2: Query Transformation - Solutions

### Exercise 1: Analyze Multi-Query Variants

**Task:** Submit queries and examine the generated variants in the logs.

**Solution:**

```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "VPN troubleshooting", "useQueryExpansion": true}'
```

**What to look for in the logs:**

The pipeline log will show a section like:
```
╠══ Step 1: Query Transformation (1431ms, parallel) ═════════════════
║ Original: VPN troubleshooting
║ Alt[1]:   How to fix VPN issues
║ Alt[2]:   Solutions for VPN connectivity problems
║ Alt[3]:   Troubleshoot and resolve VPN issues
║ HyDE:     When troubleshooting VPN issues, it's essential to first identify...
```

**Analysis:**

1. **Do they capture different aspects?**
   - Yes. Alt[1] is action-oriented ("fix"), Alt[2] focuses on "connectivity problems", Alt[3] pairs "troubleshoot" with "resolve"
   - Each uses different keywords that might match different documents

2. **Are any variants near-duplicates?**
   - Alt[1] and Alt[3] both use "issues" but otherwise differ
   - Near-duplicates are filtered by `sanitizeAlternatives()` which deduplicates case-insensitively

3. **How to improve the multi-query prompt?**
   
   Modify `QueryTransformer.java`:
   ```java
   private String buildMultiQueryPrompt(String originalQuery) {
       return """
               You are an AI assistant helping to improve search results.
               Given the user query, generate 3 alternative phrasings that capture
               different aspects or perspectives of the same information need.
               
               Guidelines:
               - Use different keywords and synonyms
               - Vary the sentence structure (question, statement, command)
               - Focus on different aspects (technical, procedural, troubleshooting)
               
               Original query: %s
               
               Return only the 3 alternative queries, one per line. Do not number them.
               """.formatted(originalQuery);
   }
   ```

**Key takeaway:** Multi-query expansion works best when variants genuinely capture different vocabulary and perspectives, not just minor rewordings.

---

### Exercise 2: Compare With and Without HyDE

**Task:** Test the same question twice—once with HyDE, once without.

**Solution:**

**Step 1: Test with HyDE (standard behavior):**
```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I report a security incident?", "useQueryExpansion": true}'
```

Save the response, noting the `sources` array and which documents were retrieved.

**Step 2: Temporarily disable HyDE in `RAGService.java`:**

```java
// In RAGService.java, around line 190-195
Subtask<List<ScoredSegment>> hydeTask = null;
boolean fireHyde = false; // CHANGED: was: useQueryExpansion && shouldUseHyde(...)
if (fireHyde) {
    final String hyde = hypotheticalDocument;
    hydeTask = scope.fork(() -> safeVectorOnlySearch(hyde, DEFAULT_TOP_K));
}
```

**Step 3: Rebuild and test again:**
```bash
mvn clean spring-boot:run

curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I report a security incident?", "useQueryExpansion": true}'
```

**Analysis:**

Compare the two responses:

1. **Does HyDE improve retrieved segments?**
   - Check if any sources in the WITH-HyDE response have `sourceQuery` containing `"vector(HyDE)"`
   - These are documents that HyDE found but multi-query didn't

2. **Check the logs:**
   - Look for the line: `║ vector(HyDE): 'When troubleshooting VPN issues...' → 5 results`
   - Compare which chunks came from HyDE vs. multi-query variants

3. **For what types of questions does HyDE help most?**
   - **Short, vague queries** ("VPN troubleshooting") - HyDE expands them into detailed paragraphs
   - **Question-answer vocabulary gap** - HyDE generates answer-like text that matches real documents better
   - **Less helpful for**: Already-detailed questions or exact-term lookups

**Recommendation:** Keep HyDE enabled for general Q&A systems, but consider skipping it for very short queries, already well-phrased questions, or latency-critical applications.

---

### Exercise 3: Prompt Engineering - Generate 5 Variants

**Task:** Modify the multi-query prompt to generate 5 variants instead of 3.

**Solution:**

Modify `QueryTransformer.java`:

```java
public class QueryTransformer {
    
    private static final Logger log = LoggerFactory.getLogger(QueryTransformer.class);
    private static final int ALTERNATIVE_QUERY_COUNT = 5;  // CHANGED: was 3

    // ... existing code ...

    private String buildMultiQueryPrompt(String originalQuery) {
        return """
                You are an AI assistant helping to improve search results.
                Given the user query, generate 5 alternative phrasings that capture
                different aspects or perspectives of the same information need.

                Original query: %s

                Return only the 5 alternative queries, one per line. Do not number them.
                """.formatted(originalQuery);
    }
}
```

**Test it:**
```bash
mvn clean spring-boot:run

curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "password reset process", "useQueryExpansion": true}'
```

**Analysis:**

1. **Do 5 variants provide better recall than 3?**
   - More variants = more keyword diversity = higher chance of finding relevant docs
   - But diminishing returns after 3-4 variants

2. **Are variants 4-5 near-duplicates?**
   - Likely yes. The LLM may struggle to generate truly distinct alternatives beyond 3-4

3. **Latency impact:**
   - Stage 1 should show similar time since multi-query is one LLM call
   - Stage 2 will increase: 5 variants + original + HyDE = 7 searches instead of 5
   - Expect ~20-30% increase in total latency

**Recommendation:** 3 variants is the sweet spot. More than 4 often produces duplicates without improving results.

---

## Chapter 3: Keyword Search Service - Solutions

### Exercise 1: Compare Search Methods

**Task:** Use the `/compare` endpoint to see when keyword search outperforms vector search.

**Solution:**

**Test 1: Exact term query (keyword should win):**
```bash
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{"query": "SEV1 incident", "topK": 5}'
```

**Expected result:**
- `keywordResults` should rank documents containing "SEV1" at the top
- `vectorResults` might miss "SEV1" or rank it lower
- `hybridResults` should combine both

**Test 2: Semantic query (vector should win):**
```bash
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{"query": "emergency situation", "topK": 5}'
```

**Expected result:**
- `vectorResults` should find documents about incidents, critical issues, SEV1
- `keywordResults` might return nothing if no docs contain exact words
- `hybridResults` should favor vector results

**Key insight:** Hybrid search provides robustness—it performs well across diverse query types.

---

### Exercise 2: BM25 Parameter Tuning

**Task:** Modify BM25 parameters and observe the impact.

**Solution:**

Modify `KeywordSearchService.java`:

```java
@Service
public class KeywordSearchService {

    private static final double BM25_K1 = 2.0;  // CHANGED: was 1.2
    private static final double BM25_B = 0.5;   // CHANGED: was 0.75

    // ... rest of the code ...
}
```

**Test:**
```bash
mvn clean spring-boot:run

curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{"query": "password reset", "topK": 5}'
```

**Understanding the parameters:**

**`k1` (term frequency saturation):**
- Lower k1: Term frequency saturates quickly
- Higher k1: Term frequency matters more

**`b` (document length normalization):**
- Lower b: Less penalty for long documents
- Higher b: More penalty for long documents

**Recommendation:** Stick with standard BM25 parameters (`k1=1.2, b=0.75`) unless you have a specific corpus characteristic to optimize for.

---

### Exercise 3: Add Stopword Filtering

**Task:** Implement stopword removal to improve precision.

**Solution:**

Modify `KeywordSearchService.java`:

```java
@Service
public class KeywordSearchService {

    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;
    
    // Add stopwords set
    private static final Set<String> STOPWORDS = Set.of(
        "the", "and", "or", "but", "is", "are", "was", "were",
        "in", "on", "at", "to", "for", "of", "a", "an",
        "how", "do", "does", "i", "my", "your", "this", "that"
    );

    // ... existing code ...

    // Modify the tokenization methods
    private static Set<String> uniqueTokens(String text) {
        Set<String> set = new HashSet<>();
        for (String token : text.toLowerCase().split("\\W+")) {
            if (!token.isEmpty() && token.length() > 1 && !STOPWORDS.contains(token)) {
                set.add(token);
            }
        }
        return set;
    }

    private static List<String> allTokens(String text) {
        List<String> list = new ArrayList<>();
        for (String token : text.toLowerCase().split("\\W+")) {
            if (!token.isEmpty() && token.length() > 1 && !STOPWORDS.contains(token)) {
                list.add(token);
            }
        }
        return list;
    }

    // ... rest of the code ...
}
```

**Test:**
```bash
mvn clean spring-boot:run

curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{"query": "how to reset the password", "topK": 5}'
```

**Analysis:**

1. **Do results improve?**
   - Query reduces to ["reset", "password"] - more focused
   - BM25 scores should be higher

2. **Are there queries that get worse?**
   - Yes: Queries where stopwords are meaningful (e.g., "in case of emergency")

3. **Should you remove from query only, documents only, or both?**
   - **Recommended**: Remove from **both** for consistent vocabulary space

---

## Chapter 4: Hybrid Search Service - Solutions

### Exercise 1: Analyze RRF Behavior

**Task:** Submit a query and examine which documents appear in both vector and keyword results.

**Solution:**

```bash
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -H "Content-Type: application/json" \
  -d '{"query": "password reset VPN", "topK": 5}'
```

**Analysis:**

1. **Which documents appear in all three result sets?**
   - These are the most confident results

2. **Do documents appearing in both lists rank higher?**
   - Yes, by design. RRF sums contributions from both lists

3. **Are there any surprises?**
   - Look for documents that rank low in both but high in hybrid

**Key insight:** RRF rewards consensus.

---

### Exercise 2: Tune the RRF Constant

**Task:** Modify `RRF_RANK_CONSTANT` and observe the impact.

**Solution:**

Modify `HybridSearchService.java`:

```java
@Service
public class HybridSearchService {
    private static final int RRF_RANK_CONSTANT = 10;  // CHANGED: was 60
    // ... rest of the code ...
}
```

**Analysis:**

- Lower k (e.g., 10): Strongly favors top-ranked documents
- Higher k (e.g., 100): More democratic—ranks 1-10 treated more equally
- k=60 (standard): Balanced middle ground

**Recommendation:** Stick with k=60 unless you have specific evaluation metrics.

---

### Exercise 3: Implement Weighted Hybrid Search

**Task:** Extend the service to support weighted fusion.

**Solution:**

Add to `HybridSearchService.java`:

```java
public List<TextSegment> weightedHybridSearch(String query, int topK, double vectorWeight) {
    int retrievalSize = topK * 2;
    
    List<TextSegment> vectorResults = vectorStore.searchSegments(query, retrievalSize);
    List<TextSegment> keywordResults = keywordSearch.search(query, retrievalSize);
    
    double keywordWeight = 1.0 - vectorWeight;
    
    Map<String, Double> scores = new HashMap<>();
    Map<String, TextSegment> segmentsByText = new HashMap<>();
    
    // Vector contribution
    for (int i = 0; i < vectorResults.size(); i++) {
        TextSegment segment = vectorResults.get(i);
        String text = segment.text();
        double contribution = vectorWeight / (RRF_RANK_CONSTANT + i + 1);
        scores.merge(text, contribution, Double::sum);
        segmentsByText.putIfAbsent(text, segment);
    }
    
    // Keyword contribution
    for (int i = 0; i < keywordResults.size(); i++) {
        TextSegment segment = keywordResults.get(i);
        String text = segment.text();
        double contribution = keywordWeight / (RRF_RANK_CONSTANT + i + 1);
        scores.merge(text, contribution, Double::sum);
        segmentsByText.putIfAbsent(text, segment);
    }
    
    List<TextSegment> mergedResults = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(retrievalSize)
            .map(entry -> segmentsByText.get(entry.getKey()))
            .filter(Objects::nonNull)
            .toList();
    
    return reRanker.rerank(query, mergedResults, topK);
}
```

---

### Exercise 4: Implement Parallel Retrieval

**Task:** Implement parallel search using `CompletableFuture`.

**Solution:**

Add to `HybridSearchService.java`:

```java
public List<TextSegment> hybridSearchParallel(String query, int topK) {
    int retrievalSize = topK * 2;

    CompletableFuture<List<TextSegment>> vectorFuture =
        CompletableFuture.supplyAsync(() -> vectorStore.searchSegments(query, retrievalSize));

    CompletableFuture<List<TextSegment>> keywordFuture =
        CompletableFuture.supplyAsync(() -> keywordSearch.search(query, retrievalSize));

    CompletableFuture.allOf(vectorFuture, keywordFuture).join();

    List<TextSegment> vectorResults = vectorFuture.join();
    List<TextSegment> keywordResults = keywordFuture.join();

    List<TextSegment> mergedResults = reciprocalRankFusion(vectorResults, keywordResults, retrievalSize);
    return reRanker.rerank(query, mergedResults, topK);
}
```

For this workshop corpus (~50 docs), sequential is sufficient. For large document stores, parallelize.

---

## Chapter 5: Re-Ranking - Solutions

### Exercise 1: Measure Re-Ranking Impact

**Task:** Compare results with and without re-ranking.

**Solution:**

Add to `HybridSearchService.java`:

```java
public List<TextSegment> hybridSearchNoRerank(String query, int topK) {
    int retrievalSize = topK * 2;

    List<TextSegment> vectorResults = vectorStore.searchSegments(query, retrievalSize);
    List<TextSegment> keywordResults = keywordSearch.search(query, retrievalSize);

    List<TextSegment> mergedResults = reciprocalRankFusion(vectorResults, keywordResults, retrievalSize);
    
    return mergedResults.stream()
            .limit(topK)
            .toList();
}
```

Test:
```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "password reset process", "useQueryExpansion": false}'
```

---

### Exercise 2: Implement LLM-Based Re-Ranking

**Task:** Use the LLM to score relevance.

**Solution:**

Create `LLMReRanker.java`:

```java
@Component("llmReRanker")
public class LLMReRanker implements ReRanker {

    private static final Logger log = LoggerFactory.getLogger(LLMReRanker.class);
    private final ChatModel llm;

    public LLMReRanker(ChatModel llm) {
        this.llm = llm;
    }

    @Override
    public List<TextSegment> rerank(String query, List<TextSegment> candidates, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        record ScoredCandidate(TextSegment segment, double score) {}

        List<ScoredCandidate> scored = candidates.parallelStream()  // Parallel for speed
                .map(candidate -> {
                    double score = scoreRelevance(query, candidate.text());
                    return new ScoredCandidate(candidate, score);
                })
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .limit(topK)
                .toList();

        return scored.stream()
                .map(ScoredCandidate::segment)
                .toList();
    }

    private double scoreRelevance(String query, String document) {
        String prompt = """
                Rate the relevance of the following document to the query on a scale of 0.0 to 10.0.
                
                Query: %s
                Document: %s
                
                Return ONLY a numeric score between 0.0 and 10.0.
                Score:
                """.formatted(query, document.substring(0, Math.min(500, document.length())));
        
        try {
            String response = llm.chat(prompt).trim();
            return Double.parseDouble(response.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            log.warn("LLM scoring failed", e);
            return 0.0;
        }
    }
}
```

**Note:** LLM-based re-ranking is accurate but slow. For production, use a dedicated cross-encoder model.

---

### Exercise 3: Batch Re-Ranking

**Task:** Optimize by batching embedding generation.

**Solution:**

Modify `EmbeddingBasedReRanker.java`:

```java
@Override
public List<TextSegment> rerank(String query, List<TextSegment> candidates, int topK) {
    if (candidates.isEmpty()) {
        return List.of();
    }

    Embedding queryEmbedding = embeddingService.generateEmbedding(query);

    // Batch embed all candidates at once
    List<String> candidateTexts = candidates.stream()
            .map(TextSegment::text)
            .toList();

    List<Embedding> candidateEmbeddings;
    try {
        candidateEmbeddings = embeddingService.generateEmbeddings(candidateTexts);
    } catch (UnsupportedOperationException e) {
        // Fallback to sequential
        candidateEmbeddings = candidateTexts.stream()
                .map(embeddingService::generateEmbedding)
                .toList();
    }

    record ScoredCandidate(TextSegment segment, double score) {}

    return IntStream.range(0, candidates.size())
            .mapToObj(i -> {
                double score = similarityCalculator.cosineSimilarity(
                        queryEmbedding.vector(),
                        candidateEmbeddings.get(i).vector());
                return new ScoredCandidate(candidates.get(i), score);
            })
            .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
            .limit(topK)
            .map(ScoredCandidate::segment)
            .toList();
}
```

Batching provides ~3× speedup for re-ranking.

---

## Chapter 6: RAG Service - Solutions

### Exercise 1: Read the Pipeline Trace

**Task:** Run a query and study the logs.

**Solution:**

```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I report a security incident?", "useQueryExpansion": true}'
```

**Look for:**
- Stage 5 (LLM Generation) takes the longest (~60-80% of total time)
- Stage 2 shows how many candidates (usually 25 = 5 searches × 5 results)
- Stage 3 deduplicates (e.g., 25 → 8 unique segments)
- Check `sources[].sourceQuery` to see if HyDE contributed unique chunks

---

### Exercise 2: Tune the Min Score Filter

**Task:** Raise `MIN_FUSED_SCORE` and observe what gets dropped.

**Solution:**

Modify `RAGService.java`:

```java
private static final double MIN_FUSED_SCORE = 0.016;  // CHANGED: was 0.0
```

Then in `fuseWithRrf`:

```java
return scoreByKey.entrySet().stream()
        .filter(e -> e.getValue() >= MIN_FUSED_SCORE)  // ADDED
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .limit(maxResults)
        // ...
```

At 0.016:
- Keeps rank 1-2 from expanded queries
- Drops rank 3+
- Filters HyDE-only chunks (weight=0.75)
- Keeps multi-shard chunks

**Recommendation:** 0.016-0.020 is optimal for this corpus.

---

### Exercise 3: Use the RagStatus Enum

**Task:** Build a frontend that distinguishes the five status cases.

**Solution:**

React component example:

```typescript
const RagResponseDisplay: React.FC<{ response: RagAnswerResponse }> = ({ response }) => {
  switch (response.status) {
    case 'ANSWERED':
      return <div>Show answer + sources</div>;
    case 'INSUFFICIENT_CONTEXT':
      return <div>⚠️ No relevant info found. Try rephrasing.</div>;
    case 'RETRIEVAL_FAILED':
      return <div>❌ Knowledge base unreachable. <button>Retry</button></div>;
    case 'GENERATION_FAILED':
      return <div>⚠️ Answer unavailable. Here are the sources we found: {sources}</div>;
    case 'CANCELLED':
      return <div>⏱️ Query timed out. <button>Retry</button></div>;
  }
};
```

**Key insight:** Status codes enable graceful degradation.

---

### Exercise 4: Trace a Chunk's Provenance

**Task:** Inspect the `sourceQuery` field for a chunk.

**Solution:**

```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "VPN access", "useQueryExpansion": true}' \
  | jq '.sources[] | {number, title, sourceQuery}'
```

**Example output:**
```json
{
  "number": 1,
  "sourceQuery": "hybrid: VPN access | hybrid: How to access VPN | vector(HyDE): ..."
}
```

This shows the chunk was found by multiple shards—high confidence!

---

## Chapter 7: RAG Controller - Solutions

### Exercise 1: Add Request Logging

**Task:** Log all incoming requests.

**Solution:**

Modify `RAGController.java`:

```java
@PostMapping("/query")
public ResponseEntity<RAGResponse> query(@Valid @RequestBody RAGRequest request) {
    log.info("Received RAG query: question='{}', expansion={}, length={}", 
            request.question(), request.useQueryExpansion(), request.question().length());

    long startTime = System.currentTimeMillis();
    
    RAGService.RagAnswer result = ragService.queryWithSources(
            request.question(), request.useQueryExpansion());
    
    long elapsedTime = System.currentTimeMillis() - startTime;
    
    log.info("RAG query completed: status={}, answerLength={}, sources={}, elapsedMs={}", 
            result.status(), result.answer().length(), result.sources().size(), elapsedTime);

    return ResponseEntity.ok(new RAGResponse(
            result.answer(), result.sources(), result.transformedQueries(),
            result.elapsedMs(), result.status()));
}
```

Analyze logs to find patterns in query expansion usage and status distribution.

---

### Exercise 2: Render Citation Sources

**Task:** Build a client that maps `[Source N]` to the sources array.

**Solution:**

React component:

```typescript
const CitationRenderer: React.FC<{ answer: string; sources: Source[] }> = ({ answer, sources }) => {
  const renderAnswerWithCitations = () => {
    const parts = answer.split(/(\[Source \d+\])/g);
    
    return parts.map((part, index) => {
      const match = part.match(/\[Source (\d+)\]/);
      if (match) {
        const sourceNum = parseInt(match[1]);
        return (
          <sup key={index}>
            <a href={`#source-${sourceNum}`}>[{sourceNum}]</a>
          </sup>
        );
      }
      return <span key={index}>{part}</span>;
    });
  };

  return (
    <div>
      <div className="answer">{renderAnswerWithCitations()}</div>
      <div className="sources">
        {sources.map(s => (
          <div key={s.number} id={`source-${s.number}`}>
            <strong>[{s.number}]</strong> {s.title}
            <p>{s.text}</p>
          </div>
        ))}
      </div>
    </div>
  );
};
```

---

### Exercise 3: Implement Pagination for Compare

**Task:** Allow different pages of results.

**Solution:**

Update `CompareRequest`:

```java
record CompareRequest(
    @NotBlank String query,
    @Min(1) @Max(20) int topK,
    @Min(0) int offset
) {
    CompareRequest {
        topK = (topK == 0) ? 5 : topK;
        offset = Math.max(0, offset);
    }
}
```

Add offset support to search methods:

```java
public List<TextSegment> vectorOnlySearch(String query, int topK, int offset) {
    return vectorStore.searchSegments(query, topK + offset)
            .stream()
            .skip(offset)
            .limit(topK)
            .toList();
}
```

Test:
```bash
# Page 1
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -d '{"query": "VPN", "topK": 5, "offset": 0}'

# Page 2
curl -X POST http://localhost:8082/api/v1/rag/compare \
  -d '{"query": "VPN", "topK": 5, "offset": 5}'
```

---

### Exercise 4: Add Health Check Endpoint

**Task:** Expose a health check.

**Solution:**

Create `HealthCheckController.java`:

```java
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    private final VectorStoreService vectorStore;
    private final ChatModel llm;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        boolean vectorStoreHealthy = checkVectorStore();
        boolean llmHealthy = checkLLM();
        
        health.put("vectorStore", vectorStoreHealthy ? "UP" : "DOWN");
        health.put("llm", llmHealthy ? "UP" : "DOWN");
        health.put("status", (vectorStoreHealthy && llmHealthy) ? "UP" : "DEGRADED");
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        boolean ready = checkVectorStore() && checkLLM();
        return ready ? 
                ResponseEntity.ok(Map.of("status", "READY")) :
                ResponseEntity.status(503).body(Map.of("status", "NOT_READY"));
    }

    private boolean checkVectorStore() {
        try {
            return vectorStore.getAllSegments(ChunkingStrategy.RECURSIVE) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkLLM() {
        return llm != null;
    }
}
```

---

## Chapter 8: Structured Concurrency - Solutions

### Exercise 1: Measure Parallel Speedup

**Task:** Compare sequential vs. parallel timing.

**Solution:**

Add benchmark endpoint:

```java
@GetMapping("/benchmark-concurrency")
public ResponseEntity<Map<String, Object>> benchmarkConcurrency(
        @RequestParam String query, @RequestParam(defaultValue = "5") int topK) {
    
    // Sequential
    long seqStart = System.currentTimeMillis();
    vectorSearch(query, topK);
    keywordSearch(query, topK);
    hybridSearch(query, topK);
    long seqMs = System.currentTimeMillis() - seqStart;

    // Parallel
    long parStart = System.currentTimeMillis();
    try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
        scope.fork(() -> vectorSearch(query, topK));
        scope.fork(() -> keywordSearch(query, topK));
        scope.fork(() -> hybridSearch(query, topK));
        scope.join();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    long parMs = System.currentTimeMillis() - parStart;

    return ResponseEntity.ok(Map.of(
        "sequentialMs", seqMs,
        "parallelMs", parMs,
        "speedup", String.format("%.2fx", (double) seqMs / parMs)
    ));
}
```

Expect **1.5-2.5× speedup** for small corpus.

---

### Exercise 2: Simulate Task Failure

**Task:** Observe automatic cancellation.

**Solution:**

```java
@GetMapping("/test-failure")
public ResponseEntity<Map<String, String>> testFailure() {
    try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
        
        scope.fork(() -> {
            Thread.sleep(500);
            throw new RuntimeException("Failed!");
        });
        
        scope.fork(() -> {
            Thread.sleep(3000);  // Won't complete
            return "Done";
        });
        
        scope.join();
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "status", "ERROR",
            "message": e.getMessage()
        ));
    }
}
```

The 3-second task is **cancelled** when the first task fails at 500ms.

---

### Exercise 3: Implement Timeout

**Task:** Use `joinUntil` for deadline.

**Solution:**

```java
@PostMapping("/compare-with-timeout")
public ResponseEntity<Map<String, Object>> compareWithTimeout(
        @RequestBody CompareRequest request,
        @RequestParam(defaultValue = "5000") long timeoutMs) {
    
    Instant deadline = Instant.now().plusMillis(timeoutMs);
    
    try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
        var v = scope.fork(() -> vectorSearch(request.query(), request.topK()));
        var k = scope.fork(() -> keywordSearch(request.query(), request.topK()));
        var h = scope.fork(() -> hybridSearch(request.query(), request.topK()));

        scope.joinUntil(deadline);

        return ResponseEntity.ok(Map.of(
            "vectorResults", v.get(),
            "keywordResults", k.get(),
            "hybridResults", h.get()
        ));

    } catch (TimeoutException e) {
        return ResponseEntity.status(504).body(Map.of(
            "status", "TIMEOUT",
            "message", "Exceeded " + timeoutMs + "ms"
        ));
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return ResponseEntity.status(500).body(Map.of("status", "INTERRUPTED"));
    }
}
```

---

### Exercise 4: "First Successful Result" Pattern

**Task:** Return fastest search result.

**Solution:**

```java
@PostMapping("/search-fastest")
public ResponseEntity<Map<String, Object>> searchFastest(
        @RequestParam String query, @RequestParam(defaultValue = "5") int topK) {
    
    record Result(String method, List<String> results, long durationMs) {}
    
    try (var scope = StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())) {
        
        scope.fork(() -> {
            long start = System.currentTimeMillis();
            var res = vectorSearch(query, topK);
            return new Result("vector", res, System.currentTimeMillis() - start);
        });
        
        scope.fork(() -> {
            long start = System.currentTimeMillis();
            var res = keywordSearch(query, topK);
            return new Result("keyword", res, System.currentTimeMillis() - start);
        });
        
        scope.fork(() -> {
            long start = System.currentTimeMillis();
            var res = hybridSearch(query, topK);
            return new Result("hybrid", res, System.currentTimeMillis() - start);
        });

        scope.join();
        
        Result winner = scope.result();
        
        return ResponseEntity.ok(Map.of(
            "winner", winner.method,
            "results", winner.results,
            "durationMs", winner.durationMs
        ));

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted", e);
    }
}
```

Typically saves **60-70% latency** compared to waiting for all three.

---

## Navigation

👈 **[Back to Module Overview](README.md)**

👉 **[Next Module: Tools and Model Context Protocol](../module-03-tools-mcp/README.md)**
