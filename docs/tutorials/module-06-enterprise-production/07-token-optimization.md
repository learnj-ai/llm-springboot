# Token Optimization: Reducing Costs and Latency

LLM API costs scale linearly with token count. A careless RAG system that sends 4,000-token contexts for every query will burn through budgets fast. **Token optimization** is the art of delivering the right amount of context—enough to answer accurately, but not so much that you waste tokens and time. This chapter shows you how to optimize context selection and prompt compression.

## Why Token Optimization Matters

Consider a typical RAG query:

**Unoptimized**:
- System prompt: 200 tokens
- Retrieved context: 3,500 tokens (10 documents, no filtering)
- User query: 20 tokens
- **Total input**: 3,720 tokens @ $0.03/1K = $0.11 per query
- Response: 150 tokens @ $0.06/1K = $0.009
- **Cost per query**: $0.12
- **At 10,000 queries/day**: $1,200/day = $36,000/month

**Optimized**:
- System prompt: 150 tokens (compressed)
- Retrieved context: 800 tokens (relevance-filtered, top 3 documents)
- User query: 20 tokens
- **Total input**: 970 tokens @ $0.03/1K = $0.03 per query
- Response: 150 tokens @ $0.06/1K = $0.009
- **Cost per query**: $0.04
- **At 10,000 queries/day**: $400/day = $12,000/month

**Savings**: $24,000/month (67% reduction)

## Optimization Strategies

### 1. Context Selection by Relevance

Rank retrieved documents by relevance score and only include top-k:

```java
public List<Document> selectByRelevance(List<Document> documents,
                                         List<Double> scores,
                                         int maxDocuments) {
    // Pair documents with scores
    List<ScoredDocument> scored = IntStream.range(0, documents.size())
        .mapToObj(i -> new ScoredDocument(documents.get(i), scores.get(i)))
        .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
        .limit(maxDocuments)
        .toList();

    return scored.stream()
        .map(ScoredDocument::document)
        .toList();
}
```

### 2. Token Budget Management

The `TokenOptimizer` service manages context within a token budget:

```java
@Service
public class TokenOptimizer {

    private static final double WORDS_PER_TOKEN = 0.75;

    @Value("${token-optimizer.max-tokens:4000}")
    private int maxTokens;

    @Value("${token-optimizer.compression-enabled:true}")
    private boolean compressionEnabled;

    public List<TokenizedSegment> optimizeContext(List<String> segments,
                                                   List<Double> relevanceScores) {
        // Create tokenized segments
        List<TokenizedSegment> tokenized = segments.stream()
                .map(this::tokenize)
                .collect(Collectors.toList());

        // Add relevance scores
        for (int i = 0; i < tokenized.size(); i++) {
            TokenizedSegment segment = tokenized.get(i);
            tokenized.set(i, new TokenizedSegment(
                    segment.content(),
                    segment.tokenCount(),
                    relevanceScores.get(i)
            ));
        }

        // Select segments within budget
        List<TokenizedSegment> selected = selectByBudget(tokenized, maxTokens);

        log.info("Optimized context: {} segments selected, {} tokens used of {} budget",
                selected.size(), totalTokens(selected), maxTokens);

        return selected;
    }

    private List<TokenizedSegment> selectByBudget(List<TokenizedSegment> segments,
                                                   int budget) {
        // Sort by relevance score descending
        List<TokenizedSegment> sorted = segments.stream()
                .sorted(Comparator.comparingDouble(TokenizedSegment::relevanceScore).reversed())
                .collect(Collectors.toList());

        // Select segments until budget is exhausted
        List<TokenizedSegment> selected = new ArrayList<>();
        int usedTokens = 0;

        for (TokenizedSegment segment : sorted) {
            if (usedTokens + segment.tokenCount() <= budget) {
                selected.add(segment);
                usedTokens += segment.tokenCount();
            }
        }

        return selected;
    }

    private int estimateTokens(String text) {
        // Simple word-based estimation
        int words = text.split("\\s+").length;
        return (int) Math.ceil(words / WORDS_PER_TOKEN);
    }

    public record TokenizedSegment(
            String content,
            int tokenCount,
            double relevanceScore
    ) {}
}
```

**Algorithm**:
1. Tokenize each context segment
2. Sort by relevance score (highest first)
3. Add segments to budget until exhausted
4. Return selected segments

**Result**: Only the most relevant context within the token budget.

### 3. Prompt Compression

Remove redundancy and filler words:

```java
public String compressPrompt(String prompt) {
    if (!compressionEnabled) {
        return prompt;
    }

    // Remove redundant whitespace
    String compressed = prompt.replaceAll("\\s+", " ").trim();

    // Remove filler words
    compressed = compressed.replaceAll(
        "\\b(basically|actually|literally|just|really|very)\\b", "");

    // Normalize multiple spaces again after filler removal
    compressed = compressed.replaceAll("\\s+", " ").trim();

    int originalTokens = estimateTokens(prompt);
    int compressedTokens = estimateTokens(compressed);

    log.debug("Compressed prompt: {} -> {} tokens ({} reduction)",
            originalTokens, compressedTokens, originalTokens - compressedTokens);

    return compressed;
}
```

**Techniques**:
- Remove excessive whitespace
- Eliminate filler words
- Use concise phrasing
- Avoid redundant instructions

### 4. Dynamic Context Adjustment

Adjust context size based on query complexity:

```java
public int determineTokenBudget(String query) {
    // Simple queries need less context
    if (query.split("\\s+").length < 5) {
        return 1000;
    }

    // Complex queries need more context
    if (query.contains("compare") || query.contains("explain")) {
        return 3000;
    }

    // Default
    return 2000;
}
```

## Integration Example

Using the token optimizer in the RAG pipeline:

```java
@Service
public class SimpleRAGService {

    private final TokenOptimizer tokenOptimizer;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;

    public RAGResponse query(String query) {
        // 1. Retrieve documents
        List<Document> documents = vectorStore.search(query, 10);
        List<Double> scores = vectorStore.getScores();

        // 2. Extract content and scores
        List<String> contents = documents.stream()
            .map(Document::content)
            .toList();

        // 3. Optimize within budget
        List<TokenizedSegment> optimized =
            tokenOptimizer.optimizeContext(contents, scores);

        // 4. Build context from selected segments
        String context = optimized.stream()
            .map(TokenizedSegment::content)
            .collect(Collectors.joining("\n\n"));

        // 5. Compress prompt
        String prompt = buildPrompt(query, context);
        String compressedPrompt = tokenOptimizer.compressPrompt(prompt);

        // 6. Call LLM with optimized input
        String response = chatModel.generate(compressedPrompt);

        return RAGResponse.success(response);
    }

    private String buildPrompt(String query, String context) {
        return String.format("""
            Use the following context to answer the question.

            Context:
            %s

            Question: %s

            Answer:
            """, context, query);
    }
}
```

## Monitoring Token Usage

Track optimization effectiveness:

```java
@Service
public class TokenOptimizer {

    private final Counter tokensFiltered;
    private final Counter tokensUsed;

    public TokenOptimizer(MeterRegistry meterRegistry) {
        this.tokensFiltered = Counter.builder("token_optimizer.filtered")
            .description("Tokens filtered by optimizer")
            .register(meterRegistry);

        this.tokensUsed = Counter.builder("token_optimizer.used")
            .description("Tokens used after optimization")
            .register(meterRegistry);
    }

    public List<TokenizedSegment> optimizeContext(List<String> segments,
                                                   List<Double> scores) {
        int totalTokens = segments.stream()
            .mapToInt(this::estimateTokens)
            .sum();

        List<TokenizedSegment> selected = selectByBudget(tokenized, maxTokens);

        int usedTokens = totalTokens(selected);
        int filteredTokens = totalTokens - usedTokens;

        tokensFiltered.increment(filteredTokens);
        tokensUsed.increment(usedTokens);

        return selected;
    }
}
```

View metrics to see optimization impact:

```bash
curl http://localhost:8086/actuator/prometheus | grep token_optimizer

# token_optimizer_filtered_total 234567.0
# token_optimizer_used_total 89234.0
# Reduction: 234567 - 89234 = 145333 tokens (62% reduction)
```

## Key Takeaways

- **Token costs dominate LLM expenses** - Optimization can save 50-70% of costs
- **Relevance-based selection** ensures the best context within budget
- **Token budgets** prevent runaway costs
- **Prompt compression** removes waste without losing meaning
- **Dynamic budgets** adjust to query complexity
- **Monitoring is essential** to measure optimization effectiveness

## Practice Exercise

Implement a more sophisticated token estimator using a tokenizer library.

### Task: Use TikToken for Accurate Token Counting

The current estimator uses word count, which is inaccurate. GPT models use BPE tokenization.

1. **Add TikToken dependency**:

```xml
<dependency>
    <groupId>com.knuddels</groupId>
    <artifactId>jtokkit</artifactId>
    <version>0.6.1</version>
</dependency>
```

2. **Create accurate token counter**:

```java
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

@Service
public class AccurateTokenOptimizer {

    private final Encoding encoding;

    public AccurateTokenOptimizer() {
        this.encoding = Encodings.newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);
    }

    private int estimateTokens(String text) {
        return encoding.encode(text).size();
    }
}
```

3. **Compare accuracy**:

```java
String text = "The quick brown fox jumps over the lazy dog";

// Old estimator: 9 words / 0.75 = 12 tokens
int oldEstimate = (int) Math.ceil(text.split("\\s+").length / 0.75);

// New estimator: 10 tokens (actual)
int newEstimate = encoding.encode(text).size();

log.info("Old: {} tokens, New: {} tokens, Actual: 10 tokens", oldEstimate, newEstimate);
```

**Expected Outcome**: More accurate token counting leads to better budget management and cost predictions.

---

## Navigation

👈 **[Previous: Metrics and Monitoring: Observability in Production](06-metrics-monitoring.md)**

👉 **[Next: Kubernetes Deployment: Scaling to Production](08-kubernetes-deployment.md)**
