# Getting Started

This guide will walk you through setting up and running the **Vectors and Embeddings** module on your local machine. You'll learn how to build the project, configure it, and run your first semantic search query.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 25** - Check with `java -version`. The workshop poms build with `--enable-preview` enabled.
- **Maven 3.8+** - Check with `mvn -version`
- **Git** - For cloning the repository
- **A text editor or IDE** - IntelliJ IDEA, VS Code, or Eclipse recommended
- **curl or Postman** - For testing REST API endpoints

## Clone and Setup

1. **Clone the repository** (or navigate to your existing copy):

```bash
git clone <repository-url>
cd llm-springboot-workshop/src/module-01-vector-embeddings
```

2. **Verify project structure**:

```bash
ls -la
# You should see: pom.xml, src/, target/
```

## Build System

This module uses **Maven** as the build tool. The `pom.xml` file defines all dependencies, including:

- **Spring Boot 4.0** - Web framework and REST API support
- **LangChain4J** - AI integration library for embeddings
- **AllMiniLM-L6-v2** - Local embedding model (no API keys needed!)

The key advantage: **everything runs locally**—no external API calls, no API keys, no internet required after initial dependency download.

### Building the Project

Build the project and download all dependencies:

```bash
mvn clean install
```

This command:
- Cleans previous builds (`clean`)
- Compiles source code
- Downloads all dependencies from Maven Central
- Runs tests
- Packages the application as a JAR file

**Expected output**: `BUILD SUCCESS` and a JAR file in `target/module-01-vector-embeddings-1.0.0-SNAPSHOT.jar`

## Configuration

The application uses Spring Boot's configuration system. All settings are in `src/main/resources/application.yml`.

### Configuration Files

**application.yml**:
```yaml
spring:
  application:
    name: module-01-vector-embeddings
```

That's it! The simplicity is intentional—the embedding model is configured via Java code (`EmbeddingConfiguration.java`), and there are **no API keys or environment variables required** because we're using a local embedding model.

### Sample Data

The application loads knowledge base documents from `src/main/resources/data/`:
- `api-rate-limits.md`
- `password-reset.md`
- `vpn-access.md`

These markdown files simulate a company knowledge base. Feel free to add your own `.md` files to this directory!

## Infrastructure Setup

**No external infrastructure needed!** Unlike production vector databases (Pinecone, Weaviate, Qdrant), this module uses an **in-memory vector store** for simplicity and learning purposes.

The embedding model (AllMiniLM-L6-v2) downloads automatically on first run (~80MB) and runs entirely on your CPU—no GPU required.

## Running Locally

Follow these steps to run the application:

1. **Option A: Using Maven**:
```bash
mvn spring-boot:run
```

2. **Option B: Using the JAR**:
```bash
java -jar target/module-01-vector-embeddings-1.0.0-SNAPSHOT.jar
```

3. **Watch the startup logs**:
```
Loading documents and building vector indexes...
Loaded 3 documents
Indexed 4 segments using RECURSIVE strategy
Indexed 7 segments using PARAGRAPH strategy
Vector index initialization completed in ~60ms
Started Module01VectorEmbeddingsApplication in ~6 seconds
```

The exact numbers depend on the contents of `src/main/resources/data/` (3 markdown files in the workshop default) and on your machine — the bulk of the 6-second startup is the AllMiniLM-L6-v2 model loading (~80 MB onnxruntime initialisation), not the indexing itself.

The application runs on **http://localhost:8080** by default.

## Verification

Once the application is running, verify it's working correctly:

### 1. Check Application Health

Open your browser to: http://localhost:8080

You should see a Spring Boot error page (because we haven't defined a root endpoint). This is fine! It means the server is running.

### 2. Test Semantic Search

Use curl to perform a semantic search:

```bash
curl -X POST http://localhost:8080/api/v1/search/vector \
  -H "Content-Type: application/json" \
  -d '{
    "query": "how do I reset my password",
    "maxResults": 3,
    "metric": "COSINE",
    "chunkingStrategy": "RECURSIVE"
  }'
```

**Expected Response** (shape; absolute scores will vary with the AllMiniLM-L6-v2 model and the contents of `src/main/resources/data/`):
```json
{
  "embeddingDimension": 384,
  "metric": "COSINE",
  "chunkingStrategy": "RECURSIVE",
  "indexedSegmentCount": 4,
  "results": [
    {
      "content": "# Password Reset Guide\n\nEmployees can reset their TechCorp password from the identity portal...",
      "score": 0.57,
      "metadata": {
        "chunkingStrategy": "RECURSIVE",
        "chunkIndex": 0,
        "source": "password-reset.md"
      }
    }
  ]
}
```

The top result is the password-reset doc — that's semantic search working: the query "how do I reset my password" matched the chunk semantically, not by exact keywords. With the workshop's 3 default markdown files, the AllMiniLM-L6-v2 model produces cosine scores in roughly the 0.4–0.7 range for near-topical matches; what matters is that the **right** chunk lands first, not that the score is near 1.0.

### 3. Try Different Queries

Experiment with different search queries:

```bash
# Query about API limits
curl -X POST http://localhost:8080/api/v1/search/vector \
  -H "Content-Type: application/json" \
  -d '{"query": "API request limits", "maxResults": 2}'

# Query about network access
curl -X POST http://localhost:8080/api/v1/search/vector \
  -H "Content-Type: application/json" \
  -d '{"query": "connecting to corporate network", "maxResults": 2}'
```

Notice how the search finds relevant content **even when exact keywords don't match**—this is semantic search in action!

## Troubleshooting

### Issue: "Port 8080 already in use"

**Solution**: Stop other applications using port 8080, or change the port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Issue: "BUILD FAILURE - Java version mismatch"

**Solution**: Verify Java 25 is active:

```bash
java -version
# Should show: openjdk version "25" (or "25.x.x")
```

Update `JAVA_HOME` if needed:
```bash
export JAVA_HOME=/path/to/java25
```

### Issue: "Embedding model not found"

**Solution**: There is no runtime model download. The `AllMiniLM-L6-v2` ONNX model and its tokenizer are bundled inside the `langchain4j-embeddings-all-minilm-l6-v2` JAR pulled from Maven Central — once Maven resolves the dependency, the model is already on the classpath. If the model fails to load:

- Make sure your `~/.m2/repository/dev/langchain4j/langchain4j-embeddings-all-minilm-l6-v2/<version>/` directory contains a non-empty JAR (re-run `mvn -U dependency:resolve` if it's missing).
- There is **no** `~/.cache/langchain4j/` directory in play — that path was incorrect in earlier drafts.
- If you switched LangChain4J versions, run `mvn clean` so an old transitive JAR isn't picked up.

### Issue: "No documents found at classpath:data/*.md"

**Solution**: Verify markdown files exist in `src/main/resources/data/`. If you're running from IDE, ensure resources are marked as "Resources Root".

## Practice Exercise

Verify your setup by making a small change:

1. **Add a new knowledge base document**:
   - Create `src/main/resources/data/code-review.md`
   - Add content:
     ```markdown
     # Code Review Process

     All code changes require peer review before merging.
     Submit a pull request and tag at least two reviewers.
     ```

2. **Restart the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Search for code review content**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/search/vector \
     -H "Content-Type: application/json" \
     -d '{"query": "pull request review", "maxResults": 2}'
   ```

4. **Verify the new document appears** in search results with metadata `"source": "code-review.md"`

**Expected Outcome**: Your new document should be automatically loaded, chunked, embedded, and indexed. The search should return relevant segments from `code-review.md` with high similarity scores.

**Hints**:
- Check the startup logs—you should see "Loaded 4 documents" instead of 3
- The `indexedSegmentCount` in the response will increase
- Use `"chunkingStrategy": "PARAGRAPH"` to see how chunking affects the results

---

## Navigation

👈 **[Previous: Introduction](README.md)**

👉 **[Next: Embedding Service: Turning Words into Numbers](02-embedding-service.md)**
