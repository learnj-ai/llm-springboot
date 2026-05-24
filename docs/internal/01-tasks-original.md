# Workshop Review — Tasks

Branch: `workshop-review-fixes`

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[-]` skipped/N/A

---

## 1. Major Issues

### [x] 1.1 — Standardize Java version across modules
**Reality:** poms use Java 25 with `--enable-preview` (Spring Boot 4.0.5, `StructuredTaskScope.open(...)` is JEP 505 preview).
**Decision:** align all docs to Java 25 + preview, not downgrade.
**Touch points:**
- `docs/tutorials/README.md` — done (prereqs, stack table, philosophy)
- `docs/tutorials/module-01-vector-embeddings/README.md` — done
- `docs/tutorials/module-01-vector-embeddings/01-getting-started.md` — done (prereq + troubleshooting)
- `docs/tutorials/module-02-advanced-rag/README.md` — done
- `docs/tutorials/module-02-advanced-rag/01-getting-started.md` — done
- `docs/tutorials/module-02-advanced-rag/08-structured-concurrency.md` — pending (add `--enable-preview` callout, drop "Java 21+" framing)
- `docs/tutorials/module-03-tools-mcp/README.md` — pending
- `docs/tutorials/module-03-tools-mcp/01-getting-started.md` — pending
- `docs/tutorials/module-04-chatbots-to-agents/README.md` — pending
- `docs/tutorials/module-04-chatbots-to-agents/QUICKSTART.md` — pending
- `docs/tutorials/module-04-chatbots-to-agents/chapters/01-getting-started.md` — pending
- `docs/tutorials/module-05-security-guardrails/README.md` — pending
- `docs/tutorials/module-05-security-guardrails/00-getting-started.md` — pending
- `docs/tutorials/module-05-security-guardrails/01-introduction.md` — pending
- `docs/tutorials/module-05-security-guardrails/10-testing-validation.md` — pending (GitHub Actions "Set up JDK 17")
- `docs/tutorials/module-06-enterprise-production/08-kubernetes-deployment.md` — pending (`openjdk-21` → `openjdk-25`, drop "compatible with Java 25 code" comment)
- `src/module-06-enterprise-production/Dockerfile` — pending (`openjdk-21` → `openjdk-25`)

### [ ] 1.2 — Fix MCP mislabeling in Module 03
Module 03 calls LangChain4J `@Tool` integration "MCP". Two options:
- **(a)** Rename module to "Tool Integration with LangChain4J", remove MCP language from chapter 04 + Overview.
- **(b)** Add a real MCP example via `langchain4j-mcp` (filesystem server + client config + `tools/list` over JSON-RPC).

### [ ] 1.3 — Resolve vector-store stack mismatch
Overview promises pgvector/ChromaDB; code only ships in-memory store.
- Update Overview stack table (already partially done: now reads "In-memory (workshop) / pgvector (production reference)").
- Decide: add real pgvector chapter (Module 06 addendum or Module 02 step) OR just keep the production-reference note.

### [ ] 1.4 — Fix BM25 length-normalization bug
File: `src/module-02-advanced-rag/.../KeywordSearchService.java` and `docs/tutorials/module-02-advanced-rag/03-keyword-search.md`.
Bug: `docLength` uses `Set<String>` (unique tokens) instead of total tokens for `b * dl / avgdl`.
Fix: introduce a `List<String>` tokenizer for length; keep set tokenizer for doc-frequency contains-check; precompute per-segment tokens at index time.

### [ ] 1.5 — RedisChatMemoryStore actually persists to Redis
File: `src/module-04-chatbots-to-agents/.../RedisChatMemoryStore.java` and `docs/tutorials/module-04-chatbots-to-agents/chapters/04-conversation-memory.md`.
Replace `ConcurrentHashMap` + `"stored"` marker with real serialization (LangChain4J `ChatMessageSerializer`/`ChatMessageDeserializer`, or Jackson with `@JsonTypeInfo`).
Fix the chapter's snippet that uses `new TypeReference<List<ChatMessage>>() {}` without subtype resolver.

### [ ] 1.6 — CachingService correctness/concurrency bugs
File: `src/module-06-enterprise-production/.../CachingService.java` and `docs/tutorials/module-06-enterprise-production/05-caching-strategies.md`.
- `HashMap` → `ConcurrentHashMap`
- `@Cacheable` add `unless = "#result == null"` (or restructure)
- Per-key Redis TTL instead of single `semantic:queries` hash TTL reset
- Zero-norm guard in `cosineSimilarity`
- Document O(n) lookup limitation; preview HNSW for "production"

### [ ] 1.7 — Resolve Dokimos framework reference
Module 06 imports `dokimos-core`, `dokimos-spring-ai`, `dokimos-junit` — not findable on Maven Central. Decide:
- **(a)** Ship as local Maven repo with clone/install instructions
- **(b)** Replace with Spring AI evaluators or hand-rolled minimal evaluator
- **(c)** Publish to Maven Central
Files: `docs/tutorials/module-06-enterprise-production/02-dokimos-evaluation.md`, `03-custom-evaluators.md`, module pom.

### [~] 1.8 — Port and config inconsistencies
- [x] `docs/tutorials/module-02-advanced-rag/01-getting-started.md`: "Module 01 (8081)" → "Module 01 (8080)"
- [x] Module 04: converted `application.properties` → `application.yml` (file + every doc reference)
- [x] Module 04: database renamed `workshop_db` → `workshop_module04` (yml + getting-started + external-tools + testing-deployment)
- [ ] **Module 03**: same migration is in user's stash (`application.yml` + Java code). When stash is applied, rename DB to `workshop_module03` and replace `workshop_db` in 8 spots in `docs/tutorials/module-03-tools-mcp/01-getting-started.md`.
- [-] customer_id format alignment — moot once each module has its own DB; each module can keep its own format (`12345` for Module 03, `CUST001` for Module 04).

### [x] 1.9 — Standardize LLM model selection
- [x] Default model unified to `gpt-4o-mini` (Module 02 yml + doc, Module 06 yml + doc + src README + workshop doc, all chat-model references in prose).
- [x] Module 05 dual-model pair upgraded: primary `gpt-4` → `gpt-4o`, validator `gpt-3.5-turbo` → `gpt-4o-mini` (yml, 4 chapter files, conclusion, intro stack table).
- [x] Module 06 Dokimos judge upgraded to `gpt-4o` (yml + config bean + integration test + src README + workshop doc).
- [x] Module 03 cost table now carries an "*as of 2026-05*" caveat and links to OpenAI's live pricing.
- [x] Overview's Ollama promise softened to "swap-in instructions" rather than a first-class supported provider (no full Ollama chapter — flagged as a future enhancement).
- [-] Azure `.deploymentName("gpt-4")` left as-is (deployment names are customer-chosen aliases, not model IDs).

### [ ] 1.10 — Strengthen prompt-injection guard guidance
File: `docs/tutorials/module-05-security-guardrails/02-prompt-injection-guard.md`.
- **Remove** `@Cacheable(value = "validationCache", key = "#input.hashCode()")` (hashCode collisions enable cache poisoning) — replace with full-string key or SHA-256
- Add an **indirect prompt injection** section (malicious instructions in retrieved RAG documents)
- Recommend defense-in-depth: separate system/user/tool_result channels, output filtering, tool allowlists, LLM-classifier (moderation endpoint / NeMo Guardrails / Prompt Guard)

---

## 2. Minor Issues

### [ ] 2.1 — Tracing uses `SimpleSpanProcessor`
`docs/tutorials/module-06-enterprise-production/04-distributed-tracing.md`.
Add a production subsection: `BatchSpanProcessor` + `OtlpGrpcSpanExporter` + `Sampler.parentBased(Sampler.traceIdRatioBased(0.1))`.

### [ ] 2.2 — Liveness probe path
`docs/tutorials/module-06-enterprise-production/08-kubernetes-deployment.md`.
Change liveness to `/actuator/health/liveness` and readiness to `/actuator/health/readiness` (Spring Boot 2.3+ split).

### [ ] 2.3 — Metrics units mislabeled (ms vs seconds)
`docs/tutorials/module-06-enterprise-production/06-metrics-monitoring.md`.
`Timer` description says "milliseconds" but Prometheus exports `_seconds`. Fix description.

### [ ] 2.4 — Grafana setup missing
Add docker-compose for Prometheus + Grafana, sample `prometheus.yml` scrape job, starter Grafana dashboard JSON. For OpenShift, show `ServiceMonitor` + user workload monitoring.

### [ ] 2.5 — `toolsUsed: []` always empty in Module 03
`docs/tutorials/module-03-tools-mcp/01-getting-started.md` example responses.
Either populate field (custom `ToolExecutor` wrapper / interceptor) or remove from response schema + examples.

### [ ] 2.6 — BM25 complexity claim understated
`docs/tutorials/module-02-advanced-rag/03-keyword-search.md`.
Real cost is closer to `O(n × m × q)` due to repeated tokenization. Either tighten basic impl (precompute tokens at index time) or be explicit about scale limit (~1k segments).

### [ ] 2.7 — `IndexedSegment` / `searchSegments` referenced before defined
Module 02 keyword chapter uses methods not shown in Module 01. Either show additions in Module 02 vector-store overview, or back-port to Module 01.

### [ ] 2.8 — `ContextAwareReActAgent` doesn't actually do ReAct loop
`docs/tutorials/module-04-chatbots-to-agents/chapters/04-conversation-memory.md`.
Show proper integration: inject history into ReAct prompt template OR attach `ChatMemory` to `AiServices` builder; preserve thought/action/observation loop.

### [ ] 2.9 — ReAct action parser is brittle
`docs/tutorials/module-04-chatbots-to-agents/chapters/02-react-pattern.md`.
Regex `(\\w+)\\(([^)]*)\\)` and `replaceAll("['\"]", "")` mangle real inputs (e.g., names with `'`). Migrate to LangChain4J `@Tool` (JSON tool calls) or JSON-formatted action + Jackson parse.

### [ ] 2.10 — Customer ID format mismatch across modules
Module 03 uses `12345`, Module 04 uses `CUST001` on same DB. Standardize OR use separate DBs with explicit teardown docs.

### [ ] 2.11 — Embedding model caching note is wrong
`docs/tutorials/module-01-vector-embeddings/01-getting-started.md` troubleshooting.
Model ships in `langchain4j-embeddings-all-minilm-l6-v2` JAR via Maven; `~/.cache/langchain4j/` is not where it lives. Rewrite paragraph.

### [ ] 2.12 — Practice-exercise APIs don't match real code
Module 02 re-ranking exercise uses `embeddingService.generateEmbeddings(...)`; Module 06 ANN exercise uses unverified Hnswlib coordinates. Fix or implement the referenced methods.

### [ ] 2.13 — Audit log Redis location not protectedcommit
`docs/tutorials/module-05-security-guardrails/06-security-audit-service.md` (and getting-started).
Add production-hardening subsection: Redis ACLs, TLS, ship to immutable sink (ELK/Splunk/Loki/S3 object-lock).

### [ ] 2.14 — Overview architecture diagram lists unbuilt components
`docs/tutorials/README.md`.
Add legend distinguishing "implemented in workshop" vs "production reference" for MCP server, vector store, multi-agent orchestrator, etc.

### [ ] 2.15 — Cosine-similarity practice note
`docs/tutorials/module-01-vector-embeddings/04-similarity-calculator.md` practice exercise.
Add caveat that `distance = √(2 − 2·cosine)` assumes unit vectors.

### [ ] 2.16 — `StringBuilder.isEmpty()` (Java 15+) callout
Module 01 document chunker code uses Java 15+ constructs without note. With Java 25 baseline, this is moot — verify chapter language doesn't claim older Java.

### [ ] 2.17 — Mermaid CSS leaks into rendered output
`docs/tutorials/book.json` (HonKit + Mermaid plugin config).
Configure plugin to inline styles into SVG instead of emitting `<style>` blocks. Post-process if needed.

---

## 3. Recommended fixes from accuracy review (2026-05-12)

Source: `WORKSHOP-ACCURACY-REVIEW.md`. Each item is grounded in a file/line cross-checked against the actual source.

### Corrections to existing items

- **2.4** — partially done already. `docker-compose.yml` provisions Prometheus + Grafana; `docker/prometheus/prometheus.yml` has a `module-06-production` scrape job targeting `host.docker.internal:8086`; `docker/grafana/dashboards/rag-dashboard.json` exists as a starter dashboard. **Re-scope** to: "Add OpenShift `ServiceMonitor` + user workload monitoring example."
- **2.7** — re-scope. `IndexedSegment` / `searchSegments` are defined within Module 02's own code, not imported from Module 01. The real issue is that the keyword chapter introduces them without a short "this extends the vector-store interface" paragraph. Fix is a 5-line callout in `module-02-advanced-rag/03-keyword-search.md`, not a back-port to Module 01.
- **2.16** — mark `[-]` skipped. `StringBuilder.isEmpty()` is Java 15+, well below the Java 25 baseline. Doc language in `03-document-chunker.md` doesn't claim older Java. Moot.

### BLOCKER

### [ ] 3.1 — `docker-compose.yml` only creates `workshop_db`
`docker-compose.yml:9` declares `POSTGRES_DB: workshop_db`. Module 04's `application.yml:7` now expects `workshop_module04` (from 1.8). `docker-compose up` followed by Module 04 startup will fail with `database "workshop_module04" does not exist`. When 1.8 lands for Module 03 (`workshop_module03`), the same will break Module 03.
Fix options: (a) replace the single `POSTGRES_DB` with an `init.sql` that runs `CREATE DATABASE workshop_module03; CREATE DATABASE workshop_module04;`, mounted into `/docker-entrypoint-initdb.d/`; (b) add a separate Postgres service per module (heavy); (c) keep the shared instance but document the manual `CREATE DATABASE` step in QUICKSTART. Option (a) is cheapest.
Also: `compose` only mounts Module 03's `schema.sql` and `data.sql` as init scripts. Once each module has its own DB, Module 04's seed needs to be wired in too.

### [ ] 3.2 — `02-embedding-service.md` shows a `@PostConstruct` that doesn't exist
`docs/tutorials/module-01-vector-embeddings/02-embedding-service.md:106-116` shows a `@PostConstruct public void post()` on `EmbeddingService` that initializes the model. The real `EmbeddingService.java` has no such method — initialization actually happens inside `VectorStoreService`'s `@PostConstruct`. Snippet will not compile if copied.
Fix: either delete the snippet, or move the description to the `VectorStoreService` chapter (and show the real code).

### [ ] 3.3 — Module 04 QUICKSTART uses customer IDs that aren't in the seed
`docs/tutorials/module-04-chatbots-to-agents/QUICKSTART.md:50, 84, 125-126` uses `customerId=12345` / `67890` in curl examples (and a sample-data table with John Doe / Jane Smith). The module's `init-db.sql` actually seeds `CUST001`–`CUST004` (Alice Johnson / Bob Smith / Carol White / David Brown). Every quickstart curl will return "customer not found".
Fix: replace IDs in QUICKSTART with `CUST001` etc. and rewrite the sample-data table to match the seed.
(This supersedes part of `2.10`: the in-module inconsistency is the more pressing issue than the cross-module format mismatch.)

### MAJOR

### [ ] 3.4 — Top-level `README.md` still preaches `gpt-4` / `gpt-3.5-turbo`
`llm-springboot/README.md:28`: `export OPENAI_MODEL_NAME=gpt-4  # or gpt-3.5-turbo`. The 1.9 sweep missed this. Replace with `gpt-4o-mini` and drop the gpt-3.5 alternative.

### [ ] 3.5 — `docs/troubleshooting.md` still recommends `gpt-3.5-turbo`
`docs/troubleshooting.md:184`: "Use faster model (gpt-3.5-turbo instead of gpt-4)". Replace recommendation with `gpt-4o-mini`.

### [ ] 3.6 — Two chapters say "Spring Boot 3.x" against a 4.0.5 pom
- `docs/tutorials/module-01-vector-embeddings/01-getting-started.md:35`
- `docs/tutorials/module-03-tools-mcp/01-getting-started.md:38`
Both module poms declare `spring-boot-starter-parent` 4.0.5. Update prose to "Spring Boot 4.0".

### [ ] 3.7 — Metadata files still claim "Java 17+"
The 1.1 sweep covered chapter prose but not metadata:
- `docs/tutorials/module-04-chatbots-to-agents/book.json:53`
- `docs/tutorials/module-05-security-guardrails/.tutorial-metadata.json:23, 51`
Update to `Java 25+`.

### [ ] 3.8 — `toolsUsed` field is architecturally unobservable
`docs/tutorials/module-03-tools-mcp/01-getting-started.md` and `06-rest-controller.md` show response examples with `"toolsUsed": [...]`. `ToolOrchestrator` only returns a `String response`, and the `ChatResponse` convenience constructor (`ChatResponse.java:11-12`) initializes `toolsUsed` to an empty list. Tools invoked by the LLM are never surfaced.
This is a structural extension of `2.5` (which says "always empty in examples"). Either thread tool-call metadata through `ToolOrchestrator` (via a `ToolExecutor` wrapper / interceptor) and populate the field for real, or remove the field from the DTO and from every example.

### [ ] 3.9 — Module 06 getting-started documents the wrong RAG endpoint
`docs/tutorials/module-06-enterprise-production/01-getting-started.md:299` documents `POST /api/v1/rag/query`. The actual `ProductionRAGController` exposes `/api/v1/production/query`. Learners following the curl example will hit 404.
Fix the path in the chapter (or rename the controller mapping if you prefer the documented path).

### MINOR

### [ ] 3.10 — `src/module-01-vectors-embeddings` directory has an extra "s"
Directory: `src/module-01-vectors-embeddings/` (note the extra `s` in `vectors`).
Tutorial folder and every doc reference: `module-01-vector-embeddings/` (no `s`).
Anyone following `cd src/module-01-vector-embeddings` from the docs will hit "No such file or directory". Either rename the directory to match, or correct the references (rename is cleaner — single change, all the prose stays).

### [ ] 3.11 — Module 05 CI workflow missing `--enable-preview`
`docs/tutorials/module-05-security-guardrails/10-testing-validation.md` sets `java-version: '25'` in the GitHub Actions workflow but doesn't pass `--enable-preview` to Maven. With Spring Boot 4.0 + `StructuredTaskScope` (JEP 505 preview), tests can fail in CI.
Fix: add `MAVEN_OPTS: --enable-preview` (or configure `maven-surefire-plugin` argLine) in the workflow YAML.

### [ ] 3.12 — Module 02 Exercise 3 references a `StructuredTaskScope` overload not used elsewhere
`docs/tutorials/module-02-advanced-rag/08-structured-concurrency.md` Exercise 3 (≈ ll. 339-349) shows `StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(), Duration.ofSeconds(5))`. The real `RAGController.java:71` uses the single-arg form. The two-arg `Duration` overload isn't what the module otherwise teaches.
Fix: either drop the Duration arg from the exercise, or add a short note that the timeout-aware overload is aspirational / from a later JDK preview cycle.

### [ ] 3.13 — `maxRetries(3)` snippet in MCP config chapter isn't in the real config
`docs/tutorials/module-03-tools-mcp/04-mcp-configuration.md:283` shows `.maxRetries(3)`. `MCPServerConfig.java` doesn't include it.
Fix: either add the call to the real config (genuine improvement — retries are a fine default), or label the snippet "Exercise: add retries" so readers know it's a stretch task.

### [ ] 3.14 — Module 03 chapter documents a `@RestControllerAdvice` that doesn't exist
`docs/tutorials/module-03-tools-mcp/06-rest-controller.md` (around ll. 269-290) walks through a `GlobalExceptionHandler` whose example responses learners will see "from the API". The real codebase has no such handler, so actual error responses come straight from Spring's defaults and look different.
Fix: implement the handler in `src/module-03-tools-mcp` (recommended) or recast the section as "exercise: add a global exception handler".

### NIT

### [ ] 3.15 — `OutputValidator` failure mode not documented
`docs/tutorials/module-05-security-guardrails/04-output-validator.md` correctly describes the LLM-as-judge flow but doesn't say what happens if the validator's JSON parse fails. The code catches and fails-closed (rejects the output) at `OutputValidator.java:81`. Add a one-line note.

---

## Working order

Recommended sequence (cheap wins first, then heavier work). Items added after the 2026-05-12 review are folded in where they fit best.

1. **3.4, 3.5, 3.6, 3.7** — top-level / metadata sweeps the previous 1.1 / 1.9 passes missed (single grep-and-replace).
2. **1.1** — finish Java 25 prose sweep on the chapters still listed pending.
3. **1.8 + 3.1** — Module 03 DB rename **and** docker-compose multi-DB init. Treat as a single unit so neither side ships in isolation.
4. **1.9** — already done; just verify `3.4`/`3.5` cleared the regression.
5. **3.2, 3.3, 3.9, 3.10, 3.12, 3.13, 3.14, 3.15** — single-file doc/code-path corrections.
6. **2.1, 2.2, 2.3, 2.5 (or 3.8 as the deeper fix), 2.11, 2.15** — doc-only minor fixes from the original review.
7. **3.11** — add `--enable-preview` to the Module 05 CI YAML.
8. **1.4** BM25 length bug (real code change, contained).
9. **1.6** CachingService bugs (real code change, contained).
10. **1.10** Prompt-injection guidance (doc + remove dangerous snippet).
11. **1.5** RedisChatMemoryStore (real code change, larger).
12. **2.8, 2.9** ReAct chapter fixes.
13. **1.3** Vector store decision (scope call).
14. **1.2** MCP labeling (scope call — rename or build real MCP).
15. **1.7** Dokimos resolution (scope call).
16. **2.4** (now re-scoped to OpenShift ServiceMonitor only), **2.13, 2.14, 2.17** — remaining production-hardening + doc polish.

Skipped: **2.16** (moot under Java 25). Re-scoped: **2.4**, **2.7** (see "Corrections to existing items" above).
