# Module 06 Troubleshooting Guide

> ⚠️ **Partially superseded.** Sections of this guide describe Spring AI 1.0.0-M5
> wiring (e.g. "Spring AI and LangChain4J conflict") that no longer applies —
> Module 06 was migrated off Spring AI when Spring Boot 4 dropped the autoconfig
> classes that Spring AI 1.0.0-M5 statically depended on. Dokimos's `JudgeLM` is
> now adapted directly from LangChain4J's `ChatModel` in
> `DokimosEvaluationConfig`. Troubleshooting entries that mention `spring-ai`,
> `dokimos-spring-ai`, or `OpenAiAutoConfiguration` are kept for historical
> context but do not apply to the current build.

## Evaluation Framework Issues

### Issue: Dataset File Not Found

**Error:**
```
DatasetLoadException: Dataset file not found at path: src/main/resources/data/eval-golden-set.json
```

**Cause:** Path is incorrect or file doesn't exist.

**Solution:**

1. Verify file exists:
```bash
ls -la src/main/resources/data/eval-golden-set.json
```

2. Check `application.yml` path is relative to working directory:
```yaml
evaluation:
  dataset:
    path: src/main/resources/data/eval-golden-set.json
```

3. If running from different directory, use absolute path:
```yaml
evaluation:
  dataset:
    path: ${user.dir}/src/main/resources/data/eval-golden-set.json
```

4. For tests, use classpath:
```yaml
evaluation:
  dataset:
    path: classpath:data/eval-golden-set.json
```

---

### Issue: Malformed Dataset JSON

**Error:**
```
DatasetLoadException: Malformed dataset JSON in file: eval-golden-set.json
```

**Cause:** JSON structure doesn't match Dokimos Dataset format.

**Solution:**

1. Validate JSON syntax:
```bash
python -m json.tool src/main/resources/data/eval-golden-set.json
```

2. Ensure correct Dokimos format:
```json
{
  "name": "Dataset Name",
  "examples": [
    {
      "inputs": {"input": "Query text"},
      "expectedOutputs": {"output": "Expected answer"},
      "metadata": {"id": "001"}
    }
  ]
}
```

**Common mistakes:**
- Using `test_cases` instead of `examples`
- Using `query` instead of `inputs.input`
- Using `expected_answer` instead of `expectedOutputs.output`

3. Check for trailing commas (invalid in JSON):
```json
{
  "examples": [
    {...},  // <-- No comma after last item
  ]
}
```

---

### Issue: OpenAI API Key Not Configured

**Error:**
```
401 Unauthorized: Incorrect API key provided
```

**Cause:** `OPENAI_API_KEY` environment variable not set.

**Solution:**

1. Set environment variable:
```bash
export OPENAI_API_KEY=sk-your-api-key-here
```

2. Verify it's set:
```bash
echo $OPENAI_API_KEY
```

3. For Spring Boot, ensure `application.yml` references it:
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

4. For CI/CD, add as secret:
- GitHub Actions: Repository Settings > Secrets > `OPENAI_API_KEY`
- GitLab CI: Project Settings > CI/CD > Variables
- Jenkins: Credentials > Add > Secret text

---

### Issue: Evaluation Times Out

**Error:**
```
java.net.SocketTimeoutException: Read timed out
```

**Cause:** LLM API calls taking too long (common with GPT-4).

**Solution:**

1. **Quick fix:** Filter to fast evaluators during development:
```java
// Only run rule-based evaluators (no LLM calls)
ExperimentResult result = evaluationService.runExperiment(
    List.of("exact-match", "response-length")
);
```

2. **Increase timeout** in `application.yml`:
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          timeout: 120  # seconds
```

3. **Use faster model** for judge:
```yaml
dokimos:
  judge:
    model: gpt-4o-mini  # Faster/cheaper than gpt-4o; acceptable for many judges
```

4. **Run in parallel** (if supported):
```yaml
# Maven Surefire
<configuration>
  <parallel>methods</parallel>
  <threadCount>4</threadCount>
</configuration>
```

---

### Issue: Rate Limiting

**Error:**
```
429 Too Many Requests: Rate limit exceeded
```

**Cause:** Exceeding OpenAI API rate limits.

**Solution:**

1. **Add retry logic** with backoff:
```java
@Bean
public ChatModel dokimosJudgeChatModel(ChatModel chatModel) {
    return new RetryingChatModel(chatModel, 3, 2000); // 3 retries, 2s delay
}
```

2. **Reduce dataset size** during development:
```json
{
  "examples": [
    // Use only 5-10 examples for rapid iteration
  ]
}
```

3. **Cache responses** (custom implementation):
```java
@Cacheable("judge-responses")
public JudgeLMResponse judge(String prompt) {
    // Cached by Spring
}
```

4. **Use tier limits wisely:**
- Free tier: 3 requests/minute
- Pay-as-you-go: rate limits vary by model and tier — see [OpenAI rate limits](https://platform.openai.com/docs/guides/rate-limits)
- Upgrade if running large evaluations

---

## Dependency Conflicts

### Issue: Spring AI and Langchain4J Conflict

**Error:**
```
NoSuchMethodError: org.springframework.ai.chat.ChatModel.call(...)
```

**Cause:** Version incompatibility between Spring AI and Langchain4J.

**Solution:**

1. **Verify compatible versions** in `pom.xml`:
```xml
<properties>
    <spring-ai.version>1.0.0-M5</spring-ai.version>
    <langchain4j.version>1.11.0</langchain4j.version>
    <dokimos.version>0.14.2</dokimos.version>
</properties>
```

2. **Exclude transitive dependencies** if conflicts persist:
```xml
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-spring-ai</artifactId>
    <version>0.14.2</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.ai</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Then explicitly add Spring AI version:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>
```

3. **Check for duplicate JARs:**
```bash
mvn dependency:tree | grep "spring-ai\|langchain4j"
```

4. **Use separate ChatModel beans:**
```java
@Bean
@Primary
public ChatModel langchain4jChatModel() {
    // For RAG service (Langchain4J)
}

@Bean
public ChatModel dokimosJudgeChatModel() {
    // For Dokimos (Spring AI)
}
```

---

### Issue: Jackson Version Conflict

**Error:**
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: ...
```

**Cause:** Multiple Jackson versions on classpath.

**Solution:**

1. **Enforce Spring Boot's Jackson version:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>${jackson.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

2. **Check for conflicts:**
```bash
mvn dependency:tree -Dverbose | grep jackson
```

3. **Exclude conflicting versions:**
```xml
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-core</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

### Issue: SLF4J Multiple Bindings

**Error:**
```
SLF4J: Class path contains multiple SLF4J bindings.
```

**Cause:** Multiple logging implementations on classpath.

**Solution:**

1. **Use Logback** (Spring Boot default):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>
```

2. **Exclude other SLF4J bindings:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## Evaluation Accuracy Issues

### Issue: Low Faithfulness Scores

**Symptom:** Faithfulness evaluator consistently scores < 0.5

**Possible Causes:**

1. **RAG context not included in task outputs**
```java
// WRONG: Missing context
outputs.put("output", response.response());

// CORRECT: Include context
outputs.put("output", response.response());
outputs.put("context", String.join("\n\n", response.sourceDocuments()));
```

2. **Wrong context key configured**
```java
@Bean
public FaithfulnessEvaluator faithfulnessEvaluator(JudgeLM judgeLM) {
    return FaithfulnessEvaluator.builder()
        .judge(judgeLM)
        .contextKey("context")  // Must match task output key
        .build();
}
```

3. **Insufficient context retrieved**
- Check RAG service is returning enough source documents
- Increase `topK` or `maxResults` in retrieval

---

### Issue: Exact Match Always Failing

**Symptom:** ExactMatchEvaluator scores 0.0 for all examples

**Possible Causes:**

1. **Expected output not set in dataset:**
```json
{
  "inputs": {"input": "Query"},
  "expectedOutputs": {"output": "Expected answer"},  // Must be present
}
```

2. **Case sensitivity:**
```java
// Dokimos ExactMatchEvaluator is case-sensitive by default
// Create custom evaluator for case-insensitive matching
```

3. **Whitespace differences:**
```java
// Trim and normalize whitespace
String actual = testCase.actualOutput().trim().replaceAll("\\s+", " ");
String expected = testCase.expectedOutput().trim().replaceAll("\\s+", " ");
```

---

### Issue: Response Length Evaluator Not Running

**Symptom:** Custom evaluator not included in results

**Possible Causes:**

1. **Bean not registered:**
```java
@Configuration
public class EvaluatorConfig {
    @Bean
    public ResponseLengthEvaluator responseLengthEvaluator() {
        return new ResponseLengthEvaluator(50, 500, 1.0);
    }
}
```

2. **Not added to service:**
```java
@Service
public class DokimosEvaluationService {
    private final ResponseLengthEvaluator responseLengthEvaluator;

    private List<Evaluator> buildEvaluatorList(List<String> filter) {
        // Add to list
        if (shouldInclude("response-length", filter)) {
            evaluators.add(responseLengthEvaluator);
        }
    }
}
```

3. **Filtered out in request:**
```json
{
  "evaluators": ["faithfulness"]  // Excludes response-length
}
```

---

## JUnit Test Issues

### Issue: Tests Skipped

**Symptom:** Tests show "SKIPPED" in Maven output

**Possible Causes:**

1. **Assumption failed (API key missing):**
```java
@Test
void test() {
    assumeTrue(System.getenv("OPENAI_API_KEY") != null);
    // Test logic
}
```

Solution: Set `OPENAI_API_KEY` environment variable

2. **Test class not discovered:**
```bash
# Ensure test class name matches pattern
*Test.java, Test*.java, *Tests.java, *TestCase.java
```

3. **Maven Surefire configuration:**
```xml
<configuration>
    <skipTests>false</skipTests>  <!-- Ensure not set to true -->
</configuration>
```

---

### Issue: Flaky Test Scores

**Symptom:** Same test passes/fails inconsistently

**Cause:** LLM non-determinism even with `temperature=0.0`

**Solution:**

1. **Use score ranges:**
```java
// Instead of exact match
assertTrue(score >= 0.70 && score <= 0.80, "Score in expected range");
```

2. **Run multiple times and average:**
```java
@RepeatedTest(3)
void testWithRepetition() {
    // Runs 3 times, majority must pass
}
```

3. **Mock LLM for unit tests:**
```java
@MockBean
private JudgeLM mockJudge;

@Test
void testWithMock() {
    when(mockJudge.judge(anyString()))
        .thenReturn(new JudgeLMResponse(0.8, "Mocked"));
    // Deterministic behavior
}
```

---

## Performance Issues

### Issue: Slow Evaluation Execution

**Symptom:** Experiment takes > 5 minutes

**Solutions:**

1. **Profile which evaluators are slow:**
```java
long start = System.currentTimeMillis();
ExperimentResult result = evaluationService.runExperiment();
long duration = System.currentTimeMillis() - start;
System.out.println("Execution time: " + duration + "ms");
```

2. **Reduce dataset size for development:**
```json
{
  "examples": [
    // Only include 5-10 examples during iteration
  ]
}
```

3. **Use fast evaluators first:**
```java
// Quick smoke test
evaluationService.runExperiment(List.of("exact-match", "response-length"));

// Full evaluation in CI/CD only
```

4. **Enable parallel execution:**
```java
// Spring AI parallel calls (if supported)
@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.initialize();
    return executor;
}
```

---

## Production Deployment Issues

### Issue: Out of Memory in Production

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause:** Large datasets or embedding models consuming memory

**Solution:**

1. **Increase heap size:**
```bash
java -Xmx4G -jar module-06.jar
```

2. **Process dataset in batches:**
```java
// Split dataset into smaller chunks
List<Example> batch = dataset.examples().subList(0, 10);
Dataset batchDataset = Dataset.builder()
    .examples(batch)
    .build();
```

3. **Stream processing:**
```java
// Process examples one at a time
for (Example example : dataset.examples()) {
    // Process individually
}
```

---

## Getting Help

If you encounter issues not covered here:

1. **Check Dokimos documentation:** https://dokimos.dev
2. **Search Dokimos GitHub issues:** https://github.com/dokimos-dev/dokimos/issues
3. **Check Spring AI issues:** https://github.com/spring-projects/spring-ai/issues
4. **Enable debug logging:**
```yaml
logging:
  level:
    dev.dokimos: DEBUG
    org.springframework.ai: DEBUG
    com.techcorp.assistant.module06: DEBUG
```

5. **Share logs and configuration** when asking for help
