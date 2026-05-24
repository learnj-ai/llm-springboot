# Structured Concurrency: Modern Java Parallelism

When you need to run multiple searches in parallel—vector, keyword, and hybrid—how do you ensure they all complete successfully, cancel if one fails, and clean up resources properly? Traditional Java concurrency with `ExecutorService` and `Future` is powerful but error-prone. **Structured concurrency**, previewed since Java 21 and refined as JEP 505 in **Java 25**, provides a safer, more maintainable approach. This chapter uses the Java 25 preview API (`StructuredTaskScope.open(...)`), which requires `--enable-preview` at both compile and runtime (the workshop pom enables this automatically). It explores how `RAGController` uses `StructuredTaskScope` to run parallel searches with guaranteed cleanup and cancellation semantics.

## The Problem with Traditional Concurrency

Let's see what's wrong with the traditional approach:

### Using ExecutorService (The Old Way)

```java
ExecutorService executor = Executors.newFixedThreadPool(3);
try {
    Future<List<String>> vectorFuture = executor.submit(() -> vectorSearch(query));
    Future<List<String>> keywordFuture = executor.submit(() -> keywordSearch(query));
    Future<List<String>> hybridFuture = executor.submit(() -> hybridSearch(query));

    List<String> vectorResults = vectorFuture.get();
    List<String> keywordResults = keywordFuture.get();
    List<String> hybridResults = hybridFuture.get();

    return new SearchComparisonResponse(query, vectorResults, keywordResults, hybridResults);
} finally {
    executor.shutdown();
}
```

**Problems:**
1. **Resource leaks**: If you forget `executor.shutdown()`, threads leak
2. **No cancellation**: If one task fails, the others keep running (wasting resources)
3. **Error handling complexity**: Need try-catch around each `future.get()`
4. **No automatic cleanup**: Must manually manage executor lifecycle

### What We Want Instead

- **Automatic cancellation**: If one task fails, cancel all others immediately
- **Guaranteed cleanup**: Resources (threads) are released when the scope exits
- **Simple error handling**: If any task fails, the entire operation fails
- **Parent-child relationship**: Tasks belong to a scope, can't outlive it

**This is exactly what structured concurrency provides.**

## What is Structured Concurrency?

**Structured concurrency** treats concurrent tasks like code blocks—they have a clear entry point, exit point, and lifecycle. Just as a `try` block can't "leak" execution (it always exits), a structured concurrency scope can't leak tasks.

### Key Principles

1. **Task scopes have lifetimes**: Tasks can't outlive their parent scope
2. **Explicit joining**: Parent waits for all children to complete
3. **Automatic cancellation**: If the parent is interrupted, all children are cancelled
4. **Resource safety**: Threads are automatically cleaned up when the scope exits

### Comparison: Traditional vs. Structured Concurrency

| Aspect | Traditional (ExecutorService) | Structured Concurrency |
|--------|-------------------------------|------------------------|
| **Lifecycle** | Manual (shutdown required) | Automatic (try-with-resources) |
| **Cancellation** | Manual (must call cancel on each Future) | Automatic (scope cancels all tasks) |
| **Error handling** | Try-catch per Future.get() | Single try-catch for scope |
| **Resource leaks** | Possible if shutdown forgotten | Impossible (try-with-resources) |
| **Code clarity** | Verbose, error-prone | Concise, safe by default |

## StructuredTaskScope API

Java's `StructuredTaskScope` provides the structured concurrency abstraction.

### Basic Structure

```java
try (var scope = StructuredTaskScope.open(joiner)) {
    // Fork tasks
    var task1 = scope.fork(() -> doWork1());
    var task2 = scope.fork(() -> doWork2());

    // Join (wait for all tasks)
    scope.join();

    // Access results
    Result result1 = task1.get();
    Result result2 = task2.get();
} // Scope automatically closes, cancels any running tasks
```

**Components:**
- **`StructuredTaskScope.open(joiner)`**: Creates a scope with a joining policy
- **`scope.fork(callable)`**: Starts a task, returns a handle
- **`scope.join()`**: Waits for tasks to complete based on the joining policy
- **`task.get()`**: Retrieves the result (or throws if the task failed)
- **Try-with-resources**: Ensures scope is closed even if an exception occurs

### Joiner Policies

Joiners determine **when the scope completes**:

| Joiner | Behavior | Use Case |
|--------|----------|----------|
| `allSuccessfulOrThrow()` | Wait for all tasks; if any fails, throw exception | All tasks must succeed (our use case) |
| `anySuccessfulResultOrThrow()` | Return as soon as one succeeds; cancel others | First successful result wins |
| `allUntilFirst(predicate)` | Wait until first task matching predicate | Find first match, cancel rest |

**For search comparison**, we use `allSuccessfulOrThrow()` because we need all three result sets.

## Code Deep Dive: Search Comparison

Let's examine how `RAGController` uses structured concurrency for parallel search:

```java
@PostMapping("/compare")
public ResponseEntity<SearchComparisonResponse> compareSearchMethods(
        @Valid @RequestBody CompareRequest request) {
    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<List<String>>allSuccessfulOrThrow())) {
        var vectorTask = scope.fork(() -> hybridSearchService.vectorOnlySearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        var keywordTask = scope.fork(() -> hybridSearchService.keywordOnlySearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        var hybridTask = scope.fork(() -> hybridSearchService.hybridSearch(request.query(), request.topK())
                .stream()
                .map(TextSegment::text)
                .toList());

        scope.join();

        return ResponseEntity.ok(new SearchComparisonResponse(
                request.query(),
                vectorTask.get(),
                keywordTask.get(),
                hybridTask.get()));
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Search comparison interrupted", e);
    }
}
```

### Step-by-Step Breakdown

**Step 1: Open the scope**
```java
try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<List<String>>allSuccessfulOrThrow())) {
```

- **Try-with-resources**: Ensures scope is closed (and tasks cancelled) if exception occurs
- **`Joiner.<List<String>>allSuccessfulOrThrow()`**: Type parameter specifies task return type
- **`allSuccessfulOrThrow()`**: All three searches must succeed; if any fails, throw exception

**Step 2: Fork tasks**
```java
var vectorTask = scope.fork(() -> hybridSearchService.vectorOnlySearch(request.query(), request.topK())
        .stream()
        .map(TextSegment::text)
        .toList());
```

- **`scope.fork(callable)`**: Launches task asynchronously
- **Lambda**: Defines the work to do
- **Returns `Subtask<List<String>>`**: Handle to access result later

**Step 3: Join (wait for all tasks)**
```java
scope.join();
```

- **Blocks** until all tasks complete or one fails
- **Throws `InterruptedException`** if the scope is interrupted
- **After join**: Safe to call `task.get()` without blocking

**Step 4: Retrieve results**
```java
return ResponseEntity.ok(new SearchComparisonResponse(
        request.query(),
        vectorTask.get(),
        keywordTask.get(),
        hybridTask.get()));
```

- **`task.get()`**: Returns the result (doesn't block—already joined)
- **Throws** if the task failed
- **Construct response**: Combine all three result sets

**Step 5: Error handling**
```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("Search comparison interrupted", e);
}
```

- **`InterruptedException`**: Scope was interrupted (e.g., server shutdown)
- **`Thread.currentThread().interrupt()`**: Restore interrupt status
- **Throw wrapped exception**: Propagate to Spring's error handler

## Structured Concurrency Guarantees

### Guarantee 1: No Task Outlives Its Scope

```java
try (var scope = ...) {
    var task = scope.fork(() -> longRunningTask());
    // ... do other work ...
} // <- Scope closes here
// Task CANNOT still be running after this point
```

If the scope exits (even via exception), all tasks are **automatically cancelled**.

### Guarantee 2: Automatic Cancellation

```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
    var task1 = scope.fork(() -> searchDatabase());
    var task2 = scope.fork(() -> searchIndex());  // This fails
    var task3 = scope.fork(() -> searchCache());

    scope.join();  // task2 throws exception
    // -> task1 and task3 are AUTOMATICALLY CANCELLED
}
```

If `task2` fails, `task1` and `task3` stop immediately—no wasted work.

### Guarantee 3: Structured Exception Handling

```java
try (var scope = ...) {
    var task1 = scope.fork(() -> workThatMightFail());
    var task2 = scope.fork(() -> moreWork());

    scope.join();  // If task1 throws, this propagates the exception

    // This code only runs if ALL tasks succeeded
    return new Result(task1.get(), task2.get());
} catch (InterruptedException e) {
    // Handle interruption
}
```

No need to wrap each `task.get()` in try-catch—the scope handles failures uniformly.

## When to Use Structured Concurrency

| Scenario | Use Structured Concurrency | Use Traditional Concurrency |
|----------|----------------------------|----------------------------|
| **Short-lived parallel operations** | ✅ Perfect fit | ❌ Overkill (ExecutorService overhead) |
| **All results needed** | ✅ `allSuccessfulOrThrow()` | ⚠️ Manual Future.get() each |
| **First result wins** | ✅ `anySuccessfulResultOrThrow()` | ⚠️ Complex cancellation logic |
| **Long-lived background tasks** | ❌ Scope shouldn't outlive request | ✅ ExecutorService with lifecycle |
| **Fine-grained control** | ❌ Limited configurability | ✅ Thread pools, queues, etc. |

**For RAG search comparison:** Structured concurrency is ideal—we need all three results, operations are short-lived, and automatic cancellation prevents wasted work.

## Performance Implications

### Latency Improvement

**Sequential execution:**
```
Vector search:   200ms
Keyword search:  150ms
Hybrid search:   250ms
---------------------
Total:          600ms
```

**Parallel execution (structured concurrency):**
```
All three in parallel
---------------------
Total:          250ms (max of the three)
```

**2.4× speedup** by parallelizing independent searches.

### Resource Usage

- **Threads**: Uses virtual threads (Java 21+, standard in Java 25)
- **Memory**: Minimal overhead per task
- **CPU**: Efficient—only as many threads as needed

## Practice Exercises

### Exercise 1: Measure Parallel Speedup

Add timing to compare sequential vs. parallel:

```java
// Sequential
long start = System.currentTimeMillis();
List<String> vectorResults = hybridSearchService.vectorOnlySearch(query, topK)
        .stream().map(TextSegment::text).toList();
List<String> keywordResults = hybridSearchService.keywordOnlySearch(query, topK)
        .stream().map(TextSegment::text).toList();
List<String> hybridResults = hybridSearchService.hybridSearch(query, topK)
        .stream().map(TextSegment::text).toList();
long sequential = System.currentTimeMillis() - start;

// Parallel (structured concurrency)
start = System.currentTimeMillis();
// ... scope.fork() all three ...
long parallel = System.currentTimeMillis() - start;

log.info("Sequential: {}ms, Parallel: {}ms, Speedup: {}x",
    sequential, parallel, (double) sequential / parallel);
```

**Questions to explore:**
- What speedup do you observe?
- Does it vary based on query complexity?

### Exercise 2: Simulate Task Failure

Modify one search to fail and observe cancellation:

```java
var vectorTask = scope.fork(() -> {
    Thread.sleep(1000);
    throw new RuntimeException("Vector search failed!");
});

var keywordTask = scope.fork(() -> {
    Thread.sleep(3000);  // Long-running task
    return keywordSearch(query);
});

scope.join();  // Throws exception from vectorTask
// keywordTask is CANCELLED (doesn't wait 3 seconds)
```

**Questions to explore:**
- Does `keywordTask` complete or get cancelled?
- What exception is thrown from `scope.join()`?

### Exercise 3: Implement Timeout

Bound total execution time. The JEP 505 preview API only exposes a single-arg `open(Joiner)` (which is what `RAGController` uses) — a `Duration`-bearing overload is not in the API the rest of this module teaches. Use a deadline pattern instead, e.g. a scheduled cancellation or `Future.get(timeout)` style at the caller:

```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
    Subtask<List<Hit>> vector  = scope.fork(this::vectorSearch);
    Subtask<List<Hit>> keyword = scope.fork(this::keywordSearch);
    scope.joinUntil(Instant.now().plus(Duration.ofSeconds(5)));  // joinUntil enforces the deadline
    return merge(vector.get(), keyword.get());
}
```

**Questions to explore:**
- What happens if a search takes longer than 5 seconds? (`joinUntil` throws `TimeoutException`; in-flight forks are cancelled.)
- How do you handle timeout exceptions gracefully and surface a partial result?

### Exercise 4: Implement "First Successful Result" Pattern

Use `anySuccessfulResultOrThrow()` to return the fastest search result:

```java
try (var scope = StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())) {
    var vectorTask = scope.fork(() -> vectorSearch(query));
    var keywordTask = scope.fork(() -> keywordSearch(query));
    var hybridTask = scope.fork(() -> hybridSearch(query));

    scope.join();

    // Returns the first successful result, cancels the others
    return scope.result();
}
```

**Questions to explore:**
- Which search typically completes first?
- How much latency do you save compared to waiting for all three?

## Virtual Threads and Structured Concurrency

Since Java 21, **virtual threads** (Project Loom) are part of standard Java. Java 25's structured concurrency works seamlessly with them:

```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(),
                                          Thread.ofVirtual().factory())) {
    // Tasks run on virtual threads
    var task1 = scope.fork(() -> work1());
    var task2 = scope.fork(() -> work2());

    scope.join();
}
```

**Benefits:**
- **Millions of virtual threads**: No thread pool limits
- **Cheap**: Virtual threads are ~1KB of memory
- **Blocking-friendly**: Can block without wasting OS threads

**For RAG systems:** Virtual threads enable thousands of concurrent queries without tuning thread pools.

## Key Takeaways

- **Structured concurrency** treats concurrent tasks like code blocks—clear lifetime, automatic cleanup
- **`StructuredTaskScope`** provides safe, maintainable parallel execution
- **`allSuccessfulOrThrow()`** joiner ensures all tasks succeed or the entire operation fails
- **Automatic cancellation** prevents wasted work if one task fails
- **Try-with-resources** guarantees scope cleanup even on exceptions
- **Virtual threads** (standard since Java 21) make structured concurrency scale to millions of tasks
- **Use for short-lived parallel operations** where all results are needed

---

## Navigation

⬅️ **[Previous: RAG Controller: Building the API](07-rag-controller.md)**
➡️ **[Next: Conclusion](conclusion.md)**
