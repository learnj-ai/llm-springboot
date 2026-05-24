# Workshop Accuracy Review

**Branch:** `workshop-review-fixes`
**Head:** `7a82138` (fix: standardize LLM model selection across workshop)
**Date:** 2026-05-12
**Scope:** All tutorial documents under `docs/tutorials/`, plus overview docs (`README.md`, `QUICKSTART.md`, `docs/troubleshooting.md`, `docker-compose.yml`), cross-referenced against the actual Java sources under `src/module-0[1-6]-*`.

This review is a layer on top of the existing `TASKS.md`. It does three things:
1. **Verifies** which `[x]` items in `TASKS.md` actually landed in the working tree.
2. **Confirms** which `[ ]` items are still real issues.
3. **Surfaces NEW findings** not in `TASKS.md`.

Severity legend: **BLOCKER** (code won't compile or example will fail at runtime) · **MAJOR** (misleads learners about behavior or security) · **MINOR** (inaccurate or out-of-date but not breaking) · **NIT** (stylistic / future polish).

---

## 1. Status of `TASKS.md` items

### Marked `[x]` — verified

| Item | Verdict | Notes |
|------|---------|-------|
| 1.1 Java 25 + `--enable-preview` | Mostly applied | Most chapters consistent. Stragglers: `module-02-advanced-rag/08-structured-concurrency.md` is actually fine now (correctly requires Java 25 + preview, only its "virtual threads since Java 21" prose remains, which is factually accurate). |
| 1.8 Ports & DB | Partial — Module 04 done, Module 03 still pending (matches TASKS note) | `application.yml` present at module-04, port 8084, DB `workshop_module04`. Module 03 still has `application.properties` and `workshop_db`. |
| 1.9 LLM model selection | Done inside tutorial chapters | Tutorial chapters all use `gpt-4o-mini` / `gpt-4o`. The README's Ollama softening landed at `docs/tutorials/README.md:151`. **But** several top-level and openspec files were missed — see §2. |

### Marked `[ ]` — confirmed still open

All of 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.10, 2.1, 2.3, 2.5, 2.7 (see caveat below), 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.17 verified as still real issues. Quick confirmations:

- **1.4 BM25 length bug**: `KeywordSearchService.java:90-91` still uses `Set<String>.size()` for `docLength`. Doc at `03-keyword-search.md:56,197` still teaches "total tokens".
- **1.5 RedisChatMemoryStore**: `RedisChatMemoryStore.java:60-65` still writes a `"stored"` marker; messages remain in the in-memory `ConcurrentHashMap`. Doc at `chapters/04-conversation-memory.md:176` still uses bare `TypeReference<List<ChatMessage>>(){}` with no `@JsonTypeInfo` resolver — that snippet will throw `InvalidTypeIdException`.
- **1.6 CachingService**: `CachingService.java` still uses `HashMap` (not `ConcurrentHashMap`); `cosineSimilarity` still has no zero-norm guard.
- **1.7 Dokimos**: `dokimos-core`, `dokimos-spring-ai`, `dokimos-junit` still referenced in `module-06`'s pom; not on Maven Central.
- **1.10 Prompt injection**: `02-prompt-injection-guard.md:463-465` still shows the dangerous `@Cacheable(key = "#input.hashCode()")` snippet.
- **2.1 SimpleSpanProcessor**: still used in `TracingConfig.java`; doc has no `BatchSpanProcessor`/OTLP/sampler section.
- **2.3 Metrics units**: `06-metrics-monitoring.md` `Timer` description still says "milliseconds"; Prometheus exports `_seconds`.
- **2.14 Architecture diagram**: `docs/tutorials/README.md:95` still has an unlabeled `MCP[MCP Server]` node with no implemented-vs-reference legend.

### Corrections to `TASKS.md`

- **2.4 Grafana/Prometheus setup** — partially done already. `docker-compose.yml` provisions both services, `docker/prometheus/prometheus.yml` has a scrape job for `module-06-production` on port 8086, and `docker/grafana/dashboards/rag-dashboard.json` is a starter dashboard. What's still missing is the OpenShift `ServiceMonitor` example. Reword the task to that scope.
- **2.7 `IndexedSegment` / `searchSegments` forward-reference** — these are defined within Module 02's own code, not actually imported from Module 01. The doc cross-reference is what's misleading, not the API existence. Re-scope to "explain in Module 02 how the vector-store interface extends".
- **2.16 `StringBuilder.isEmpty()` callout** — moot under the Java 25 baseline. `isEmpty()` exists since Java 15; mark `[-]` (skipped).

---

## 2. NEW findings not in `TASKS.md`

### BLOCKER

1. **`docker-compose.yml:9` provisions only `workshop_db`** but Module 04's `application.yml:7` now points to `workshop_module04` (per the completed §1.8 work). A learner running `docker-compose up` then `./mvnw -pl module-04 spring-boot:run` will hit `database "workshop_module04" does not exist`. The compose file also only mounts Module 03's `schema.sql` / `data.sql` as init scripts. When 1.8 finishes for Module 03 (`workshop_module03`), this will break Module 03 too.
2. **`docs/tutorials/module-01-vector-embeddings/02-embedding-service.md:106-116`** shows a `@PostConstruct public void post()` snippet on `EmbeddingService` that doesn't exist in the real code. Initialization actually happens in `VectorStoreService`. The snippet will not compile if copied.
3. **`docs/tutorials/module-04-chatbots-to-agents/QUICKSTART.md:50, 84, 125-126`** uses customer IDs `12345` / `67890` (John Doe, Jane Smith), but `init-db.sql` for this module inserts `CUST001`–`CUST004` (Alice/Bob/Carol/David). Every curl example in the quickstart will hit "customer not found". This refines and partially supersedes `TASKS.md` 2.10 — the cross-module mismatch is one issue, but the more pressing one is the quickstart is internally inconsistent with its own DB seed.

### MAJOR

4. **`llm-springboot/README.md:28`** still tells learners `export OPENAI_MODEL_NAME=gpt-4  # or gpt-3.5-turbo`. The §1.9 sweep missed this top-level README.
5. **`docs/troubleshooting.md:184`** recommends "Use faster model (gpt-3.5-turbo instead of gpt-4)". Same regression: §1.9 sweep missed this file.
6. **`docs/tutorials/module-01-vector-embeddings/01-getting-started.md:35`** says "Spring Boot 3.x" but `module-01`'s pom declares `spring-boot-starter-parent` 4.0.5. Same for **`module-03-tools-mcp/01-getting-started.md:38`**.
7. **`docs/tutorials/module-04-chatbots-to-agents/book.json:53`** and **`docs/tutorials/module-05-security-guardrails/.tutorial-metadata.json:23,51`** still list `"Java 17+"`. The §1.1 sweep covered chapter prose but not these metadata files.
8. **`docs/tutorials/module-03-tools-mcp/01-getting-started.md`** and **`06-rest-controller.md`** show example responses with `"toolsUsed": [...]` (TASKS 2.5 flagged this is always empty), but the underlying issue is structural: `ToolOrchestrator` only returns a `String response`, and the `ChatResponse` convenience constructor (`ChatResponse.java:11-12`) initialises `toolsUsed` to an empty list. The `toolsUsed` field is permanently unobservable — not just empty in the sample. The chapter implies tool-call observability that the architecture cannot provide.
9. **`docs/tutorials/module-06-enterprise-production/01-getting-started.md:299`** documents `POST /api/v1/rag/query` but `ProductionRAGController` actually exposes `/api/v1/production/query`. Learners following the chapter's curl examples will hit 404.
10. **`docs/tutorials/module-06-enterprise-production/06-security-audit-service.md`** (in Module 05 path — `06-security-audit-service.md`) has no production-hardening subsection for Redis ACLs/TLS/immutable sink. Real `SecurityAuditService.java` writes a Redis list with no auth/TLS configuration. Same as TASKS 2.13.

### MINOR

11. **Directory name vs. tutorial name mismatch**: `src/module-01-vectors-embeddings/` (extra **s**) but `docs/tutorials/module-01-vector-embeddings/` (no s). Anyone running `cd src/module-01-vector-embeddings` from the docs will hit "no such file or directory". Either rename the directory or correct every `module-01-vector-embeddings` reference in the docs.
12. **`docs/tutorials/module-05-security-guardrails/10-testing-validation.md`** CI workflow sets `java-version: '25'` but does not pass `--enable-preview` to Maven. With Spring Boot 4.0 using `StructuredTaskScope` (JEP 505 preview), tests may fail in CI.
13. **`docs/tutorials/module-02-advanced-rag/08-structured-concurrency.md` Exercise 3 (ll. 339-349)** shows `StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(), Duration.ofSeconds(5))` — but the actual `RAGController.java:71` uses the single-arg form. The Duration overload referenced in the exercise isn't part of the API the rest of the module uses.
14. **`docs/tutorials/module-03-tools-mcp/04-mcp-configuration.md:283`** shows a `.maxRetries(3)` snippet that doesn't appear in `MCPServerConfig.java`. Either remove or mark as an exercise.
15. **`docs/tutorials/module-03-tools-mcp/06-rest-controller.md` (l. ~269-290)** documents a `@RestControllerAdvice` GlobalExceptionHandler that isn't actually wired up in code. Default Spring error responses differ from the chapter's examples.

### NIT

16. **`docs/tutorials/module-05-security-guardrails/04-output-validator.md`** matches `OutputValidator.java` correctly, but doesn't document what happens if the validator's LLM returns invalid JSON (code catches and fails-safe at `:81`).

---

## 3. Items verified accurate (no change needed)

So the consolidation isn't just a list of complaints, these are good:

- Module 01: AllMiniLM-L6-v2 (384-dim), in-memory store, chunking constants (300/30/500), three similarity metrics, two chunking strategies, `POST /api/v1/search/vector` endpoint — all match code exactly.
- Module 02: structured-concurrency chapter correctly aligned to Java 25 + preview; pom `java.version=25`; `POST /api/v1/rag/query` and `/api/v1/rag/compare` match `RAGController`.
- Module 03: `CustomerDataTool`, `WeatherTool`, `ToolOrchestrator` snippets match code.
- Module 04: `workshop_module04` migration complete; port 8084 consistent; `application.yml` present.
- Module 05: PII regex matches code byte-for-byte; `SecureRAGController` mapping at `/api/v1/secure/query` matches code; `gpt-4o` / `gpt-4o-mini` pair correct in `application.yml`.
- Module 06: liveness/readiness probes use the split paths; `Dockerfile` is `openjdk-25`; Dokimos judge `gpt-4o`; default model `gpt-4o-mini`.
- Overall: Ollama softening correctly applied in overview README.

---

## 4. Recommended next moves

Cheap wins first, then heavier work, mirroring the `TASKS.md` ordering but with the new findings folded in:

1. **§2 #4, #5, #7** — top-level model/Java grep sweeps. Add `README.md`, `docs/troubleshooting.md`, `book.json`, `.tutorial-metadata.json` to the §1.1 / §1.9 sweep checklists in `TASKS.md`.
2. **§2 #1** — fix `docker-compose.yml`: either provision multiple databases (init script that runs `CREATE DATABASE workshop_module03; CREATE DATABASE workshop_module04;`) or document the manual step. Block on this before §1.8 for Module 03 lands.
3. **§2 #3** — replace `12345`/`67890` in `module-04/QUICKSTART.md` with `CUST001`/`CUST002` so it actually works against the seeded DB.
4. **§2 #2** — fix the `02-embedding-service.md` `@PostConstruct` snippet (either remove or move to `VectorStoreService`).
5. **§2 #9** — fix `module-06/01-getting-started.md` endpoint path (`/api/v1/production/query`).
6. **§2 #6, #11, #14, #15** — single-file doc corrections.
7. **§2 #12** — add `--enable-preview` to the CI YAML in `module-05/10-testing-validation.md`.
8. Mark `TASKS.md` 2.4 as substantially done (Grafana+Prometheus shipped; only OpenShift `ServiceMonitor` remains). Mark 2.16 as `[-]` skipped (moot under Java 25).
9. Then proceed with the existing `TASKS.md` heavy items (1.4 BM25 fix, 1.5 Redis serialization, 1.6 CachingService, 1.10 prompt-injection, 1.2 MCP labeling, 1.7 Dokimos).

---

## 5. Methodology & confidence

Each module was audited by reading every tutorial `.md` and cross-checking claims against:
- The corresponding `src/module-0X-*/` Java sources
- `pom.xml` (Spring Boot / Java version, dependencies actually declared)
- `application.yml` / `application.properties` (ports, DB names, model IDs, prefixes)
- `Dockerfile` (Module 06)
- `docker-compose.yml`, top-level `README.md`, `QUICKSTART.md`, `docs/troubleshooting.md`

Pgvector / ChromaDB / OllamaChatModel / NeMo Guardrails claims were not exercised at runtime — they were assessed only by reading the code and config.

A handful of agent reports made claims I could not independently verify (e.g., specific line numbers in chapters I didn't open directly). Those were filtered out before this consolidation; only findings I either confirmed by direct grep/read or that match an existing `TASKS.md` item are included above. Where a finding is line-number-precise, treat it as direct evidence; where it's file-level only, treat it as a strong signal worth a second look.
