# Appendix: Migrating from the In-Memory Store to pgvector

The workshop ships with an **in-memory vector store** because it makes the
chunk → embed → search pipeline visible without any infrastructure to set up.
That's right for learning. For production, you almost certainly want a real
vector store: persistence across restarts, multi-instance scaling, and a query
planner you can monitor.

This appendix walks through the cheapest production swap: keep the existing
chunking + embedding pipeline, replace the in-memory index with **pgvector**.
Postgres is already in the workshop's `docker-compose.yml` for Module 03/04,
and LangChain4J ships a `langchain4j-pgvector` adapter that implements the
same `EmbeddingStore` contract the in-memory store satisfies.

## Why pgvector?

| Property                  | In-memory (workshop)                    | pgvector (production reference)                          |
|---------------------------|-----------------------------------------|----------------------------------------------------------|
| Persistence               | None — gone on restart                  | Durable Postgres rows                                     |
| Indexes                   | Linear scan                             | IVFFlat or HNSW (configurable)                            |
| Scale                     | Bounded by JVM heap                     | Bounded by Postgres disk + index params                   |
| Multi-instance read       | Each pod re-indexes locally             | Shared store, single source of truth                      |
| Operability               | No dashboards, no backups               | Whatever you already do for Postgres                      |
| Cost                      | Free, but you re-embed on every restart | Pay for Postgres; embedding cost amortised over restarts  |

The trade-off you accept: a network round-trip per query. With pgvector
local-network latency this is in the 1–5 ms range, dominated by embedding
generation time. For "I want a real production reference without leaving the
JVM stack you already know", pgvector is the path of least resistance.

## The migration in five steps

### 1. Swap the Postgres image for one with pgvector

The workshop's `docker-compose.yml` uses `postgres:16-alpine`, which **does not
include pgvector** — the extension is a separate C module that must be compiled
and installed. Running `CREATE EXTENSION vector` against the stock image fails
with `could not open extension control file ".../vector.control"`.

The cheapest fix is to swap in the official pgvector image — same Postgres,
plus the extension pre-installed — in `docker-compose.yml`:

```yaml
postgres:
  image: pgvector/pgvector:pg16   # was: postgres:16-alpine
  # ...everything else unchanged: env vars, ports, init scripts, healthcheck
```

Then turn on the extension for whichever database you want to host vectors in:

```sql
-- docker/postgres/init-pgvector.sql (mount under /docker-entrypoint-initdb.d/)
\c workshop_module06
CREATE EXTENSION IF NOT EXISTS vector;
```

Or, if you're extending the workshop's existing `init-multiple-dbs.sh` script,
append:

```bash
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname workshop \
    -c "CREATE DATABASE workshop_module06;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname workshop_module06 \
    -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

A `docker compose down -v && docker compose up -d postgres` recreates the
volume with the extension loaded.

> Alternatives: if you can't pull `pgvector/pgvector` (air-gapped registry,
> policy restrictions), build a small custom image `FROM postgres:16-alpine`
> that `apk add`s `postgresql16-pgvector` (community repo) or compiles pgvector
> from source. The `pgvector/pgvector` image is just that done for you.

### 2. Add the LangChain4J pgvector dependency

In `src/module-06-enterprise-production/pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

(`${langchain4j.version}` is `1.11.0` per the parent pom — the same line of
versions the rest of the workshop pins.)

### 3. Configure connection + dimensions

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workshop_module06
    username: workshop
    password: workshop123
    driver-class-name: org.postgresql.Driver

embedding:
  store:
    type: pgvector              # toggle between "in-memory" and "pgvector"
    pgvector:
      table: knowledge_chunks   # vectors land here
      dimension: 384            # AllMiniLM-L6-v2 produces 384-d vectors
      use-index: true
      index-type: hnsw          # or "ivfflat" for cheaper indexing, slower queries
      create-table: true        # auto-DDL on startup; flip to false once stable
```

### 4. Provide a pgvector-backed `EmbeddingStore` bean

Replace (or sit beside) the in-memory store from Module 01:

```java
@Configuration
@ConditionalOnProperty(name = "embedding.store.type", havingValue = "pgvector")
public class PgVectorStoreConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            DataSource dataSource,                                    // Spring Boot autoconfigured
            @Value("${embedding.store.pgvector.table}")   String table,
            @Value("${embedding.store.pgvector.dimension}") int dimension,
            @Value("${embedding.store.pgvector.use-index}")  boolean useIndex,
            @Value("${embedding.store.pgvector.index-type}") String indexType,
            @Value("${embedding.store.pgvector.create-table}") boolean createTable) {

        // PgVectorEmbeddingStore is the LangChain4J adapter; it implements the
        // same EmbeddingStore<TextSegment> contract as the in-memory store, so
        // VectorStoreService can use it without further changes.
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(table)
                .dimension(dimension)
                .useIndex(useIndex)
                .indexListSize(100)                                   // IVFFlat: number of lists; for HNSW configure m / ef_construction via the relevant builder methods instead — `indexListSize` doesn't map to HNSW's `m` (typical m=16–64)
                .createTable(createTable)
                .dropTableFirst(false)                                // never destructive in prod
                .build();
    }
}
```

`VectorStoreService` from Module 01 talks to `EmbeddingStore<TextSegment>` —
it doesn't care which implementation backs it. The same `.add(...)` /
`.search(...)` calls now hit Postgres.

### 5. Re-index on first run, then never again

The in-memory store rebuilds the index in `@PostConstruct` every startup. With
pgvector, you want that to happen **once**, behind a flag:

```java
@Service
public class IndexBootstrapper {

    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;
    private final TechCorpDocumentLoader loader;
    private final DocumentChunker chunker;

    @Value("${embedding.store.pgvector.reindex-on-startup:false}")
    private boolean reindex;

    @PostConstruct
    void bootstrap() {
        if (!reindex) {
            log.info("Skipping re-index; flip embedding.store.pgvector.reindex-on-startup=true to rebuild.");
            return;
        }
        for (Document doc : loader.loadDocuments()) {
            for (TextSegment seg : chunker.chunk(doc, ChunkingStrategy.RECURSIVE)) {
                Embedding emb = embeddingModel.embed(seg).content();
                store.add(emb, seg);
            }
        }
    }
}
```

Run once with `--embedding.store.pgvector.reindex-on-startup=true`, then leave
it off. The chunks + their embeddings now live in Postgres and survive
restarts.

## What you give up

- **Hot-reload at startup.** With the in-memory store, dropping a new
  `.md` file into `src/main/resources/data/` and restarting the app picks it
  up automatically. With pgvector, you either re-flip the reindex flag or wire
  an admin endpoint that calls `store.add(...)` for new documents.
- **Single-process isolation.** All replicas now read/write the same table.
  This is the point of pgvector, but it does mean that a poison document in
  one replica's seed run affects every replica. Validate your loader.

## What you gain

- **Persistence.** Restart the pod, the index is still there.
- **Real index support.** HNSW under pgvector is sub-millisecond for typical
  workshop-sized corpora and tunable (`m`, `ef_construction`) the same way
  Hnswlib is.
- **Operational visibility.** `EXPLAIN ANALYZE SELECT ... ORDER BY embedding <-> '[…]'` lets you measure what your queries actually do — the in-memory store gives you no such window.
- **Multi-region story.** Put the embedding store behind a Postgres read
  replica and serve search regionally. The in-memory store has no replication
  story at all.

## When to skip pgvector

Pick something else if:

- **You need > ~10M vectors at < 10 ms p99.** That's where dedicated vector
  stores (Qdrant, Weaviate, Milvus, Pinecone) earn their keep — pgvector
  handles millions, but they handle hundreds of millions with sharded ANN.
- **You don't already run Postgres.** The whole appeal of pgvector is "you
  already have the infrastructure". Standing up Postgres just for the vector
  store gets you most of the operational cost of Qdrant without the latency
  win.

For the production reference promised in the Overview's stack table, pgvector
is the right default. Swap it in when you're ready; keep the in-memory store
behind the `embedding.store.type=in-memory` toggle so the workshop chapters
still run without infrastructure.
