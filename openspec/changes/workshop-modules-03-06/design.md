## Context

Modules 01 (Vectors & Embeddings) and 02 (Advanced RAG) are complete, providing foundational vector search and hybrid retrieval capabilities. The workshop now requires production-grade features that enterprise developers need to deploy AI systems: tool integration with databases/APIs, autonomous agents with memory, security guardrails, and production deployment infrastructure.

Current state:
- Two independent Maven modules (`module-01-vector-embeddings`, `module-02-advanced-rag`)
- Each module is self-contained with copied dependencies (no cross-module references)
- In-memory vector store in Module 01, ChromaDB deferred to Module 02 (per ADR-010)
- Spring Boot 4.0.5, Java 25, Langchain4J 1.11 (per ADR-011)
- OpenAI API integration established in Module 02
- Docker Compose with Chroma, PostgreSQL (pgvector), Redis already defined

Constraints:
- Each module must be independently runnable for workshop flow
- Workshop students progress sequentially through modules
- All code must be educational-quality with clear teaching moments
- OpenShift deployment target (RHDP environment) for Module 06

## Goals / Non-Goals

**Goals:**
- Implement 4 new Maven modules with complete Spring Boot applications and REST APIs
- Provide production-ready patterns for tools, agents, security, and deployment
- Create comprehensive AsciiDoc workshop instructions with hands-on labs
- Ensure each module builds on prior concepts without requiring code rewrites
- Demonstrate enterprise best practices (tracing, metrics, caching, access control)

**Non-Goals:**
- Replacing existing modules 01-02 (additive only)
- Cross-module Maven dependencies (keep modules self-contained)
- Full production-scale infrastructure (workshop focus, not real production)
- Multi-cloud deployment (OpenShift only per ADR-005)
- Advanced agent frameworks (LangGraph, CrewAI) - stick to foundational patterns

## Decisions

### Decision 1: Multi-Module Maven Structure with Self-Contained Modules

**Choice:** Create 4 new Maven modules under `src/` following the existing pattern. Each module is self-contained with its own `pom.xml` and does NOT depend on other modules via Maven `<dependency>`.

**Rationale:**
- **Workshop Flow**: Students can start any module without building all prior modules
- **Isolation**: Changes to earlier modules don't break later ones during workshop development
- **Clarity**: Each module's `pom.xml` shows exactly what that module needs
- **Trade-off**: Code duplication (e.g., Module 03 copies embedding code from Module 02) is acceptable for educational clarity

**Alternatives Considered:**
- Shared library module: Rejected - adds complexity and breaks module independence
- Monolithic single module: Rejected - loses progressive learning structure

### Decision 2: Tool Architecture Following Model Context Protocol

**Choice:** Use Langchain4J's `@Tool` annotation system with Spring-managed beans. Tools are registered with `ChatLanguageModel` via builder pattern. Tool execution is handled automatically by Langchain4J's tool execution loop.

**Rationale:**
- **MCP Alignment**: Langchain4J's tool abstraction maps cleanly to MCP concepts (tool discovery, parameter schemas, execution)
- **Spring Integration**: `@Component` beans with `@Tool` methods integrate naturally with Spring DI
- **Type Safety**: Java method signatures provide compile-time parameter validation
- **Educational Value**: Students see tool definitions alongside business logic (e.g., `CustomerDataTool` with JDBC queries)

**Implementation Pattern:**
```java
@Component
public class CustomerDataTool {
    @Tool("Retrieves customer info by ID")
    public String getCustomerInfo(@P("Customer ID") String id) { ... }
}

// Configuration
ChatLanguageModel model = OpenAiChatModel.builder()
    .tools(customerDataTool, weatherTool)
    .build();
```

**Alternatives Considered:**
- Custom MCP server implementation: Rejected - over-engineered for workshop scope
- Function calling without abstractions: Rejected - loses type safety and discoverability

### Decision 3: ReAct Agent with Explicit Thought-Action-Observation Loop

**Choice:** Implement ReAct agent with string-based parsing of thought/action markers. Use a loop with configurable max iterations (default 5) and explicit "FINAL ANSWER:" detection.

**Rationale:**
- **Transparency**: Students see the entire reasoning chain in logs
- **Debuggability**: Each iteration's thought, action, observation is clearly separated
- **Educational**: Explicit loop structure teaches the ReAct pattern better than black-box frameworks
- **Control**: Max iterations prevent infinite loops and token waste

**Implementation Pattern:**
```java
for (int i = 0; i < maxIterations; i++) {
    String thought = llm.generate(reactPrompt);
    if (thought.contains("FINAL ANSWER:")) return extractAnswer(thought);
    String action = extractAction(thought);  // Parse "ACTION: ..."
    String observation = toolOrchestrator.execute(action);
    currentThought = observation;  // Feed into next iteration
}
```

**Alternatives Considered:**
- LangGraph or similar frameworks: Rejected - adds dependency and hides mechanics
- Single-shot tool calling: Rejected - not truly agentic (no iterative reasoning)

### Decision 4: Redis-Backed Conversation Memory with 24-Hour TTL

**Choice:** Use Langchain4J's `MessageWindowChatMemory` with custom `RedisChatMemoryStore`. Session IDs are client-provided. Memory expires after 24 hours via Redis TTL.

**Rationale:**
- **Persistence**: Survives application restarts (unlike in-memory)
- **Scalability**: Multiple app instances can share Redis
- **Cost Control**: 24-hour TTL prevents unbounded growth
- **Workshop Simplicity**: Redis is already in docker-compose.yml

**Implementation Pattern:**
```java
ChatMemory memory = MessageWindowChatMemory.builder()
    .id(sessionId)
    .maxMessages(20)  // Sliding window
    .chatMemoryStore(new RedisChatMemoryStore(redisTemplate))
    .build();
```

**Alternatives Considered:**
- PostgreSQL storage: Rejected - overkill for workshop (querying not needed)
- No persistence: Rejected - can't demonstrate stateful agents

### Decision 5: Multi-Agent Orchestration with LLM-Based Routing

**Choice:** Create specialized agent interfaces (CustomerSupportAgent, TechnicalDocAgent, ProductExpertAgent). Use a coordinator LLM to analyze requests and route to appropriate agent.

**Rationale:**
- **Simplicity**: No hardcoded routing rules to maintain
- **Flexibility**: LLM adapts to ambiguous requests
- **Educational**: Students see prompt engineering for routing
- **Extensibility**: New agents can be added without changing coordinator logic

**Implementation Pattern:**
```java
String routingPrompt = """
    Analyze the request and select: support, documentation, or product
    Request: %s
    Respond with just the agent name.
    """.formatted(userRequest);
String selectedAgent = coordinatorLLM.generate(routingPrompt);
return agents.get(selectedAgent).process(userRequest);
```

**Alternatives Considered:**
- Keyword-based routing: Rejected - brittle, requires constant rule updates
- Embedding similarity to agent descriptions: Rejected - over-engineered

### Decision 6: Layered Security Architecture

**Choice:** Security layers applied sequentially in `SecureRAGController`:
1. Prompt injection validation (reject early)
2. Input sanitization (remove tags, normalize whitespace)
3. PII masking (redact sensitive data)
4. RAG execution
5. Output validation (LLM-as-judge)
6. Output PII masking

**Rationale:**
- **Defense in Depth**: Multiple layers catch different attack vectors
- **Fail Fast**: Injection detection rejects before expensive LLM calls
- **Auditability**: Each layer logs events for monitoring
- **Educational Clarity**: Students see how layers compose

**Alternatives Considered:**
- Single comprehensive filter: Rejected - harder to teach and debug
- Security at infrastructure layer (WAF): Rejected - workshop focuses on application-level controls

### Decision 7: Regex-Based PII Detection for Workshop Scope

**Choice:** Use Java regex patterns for email, phone, SSN, credit card detection. Replace matches with `[TYPE_REDACTED]` tokens.

**Rationale:**
- **No Dependencies**: Pure Java, no external libraries
- **Deterministic**: Same input always produces same output (good for testing)
- **Fast**: Regex is faster than ML-based NER models
- **Educational**: Students understand the patterns

**Limitations:**
- **False Negatives**: Won't catch all PII variants (e.g., international phone formats)
- **False Positives**: May redact non-PII (e.g., "555-1234" in documentation)
- **Not Production-Grade**: Real systems should use AWS Comprehend, Azure Text Analytics, or similar

**Alternatives Considered:**
- NER models (Hugging Face Transformers): Rejected - adds complexity, latency
- Cloud PII services: Rejected - requires API keys, costs

### Decision 8: LLM-as-Judge for Output Validation

**Choice:** Use separate gpt-3.5-turbo instance (temperature=0.0) to evaluate outputs for toxicity, confidentiality, and factuality. Return JSON with `{safe, violations, confidence}`.

**Rationale:**
- **Flexibility**: Can evaluate nuanced criteria that regex can't catch
- **Cost-Effective**: gpt-3.5-turbo is 10x cheaper than gpt-4
- **Deterministic**: temperature=0.0 for consistent judgments
- **Educational**: Shows dual-LLM pattern (generator + judge)

**Limitations:**
- **Latency**: Adds 200-500ms per response
- **Cost**: Doubles token usage (once for generation, once for validation)
- **Not 100% Reliable**: LLMs can miss violations or hallucinate them

**Alternatives Considered:**
- Rule-based filters: Rejected - can't handle nuanced safety criteria
- Dedicated safety models (Llama Guard): Accepted as future enhancement, but out of workshop scope

### Decision 9: Semantic Caching with Embedding Similarity Threshold

**Choice:** Cache RAG responses in Redis keyed by query text. For cache lookups, compute embedding of new query and check cosine similarity against all cached queries. If similarity > 0.95, return cached response.

**Rationale:**
- **Cost Savings**: Reduces LLM calls for paraphrased questions
- **User Experience**: Faster responses for common questions
- **Threshold Tuning**: 0.95 balances recall (finding similar queries) vs. precision (avoiding false matches)

**Limitations:**
- **Linear Scan**: O(n) similarity checks don't scale past ~1000 cached queries
- **Cold Start**: First query always misses cache
- **Stale Data**: Cached responses don't update if knowledge base changes

**Alternatives Considered:**
- Vector DB for cache (Chroma with separate collection): Rejected - over-engineered for workshop
- Exact match cache only: Rejected - misses paraphrased queries

### Decision 10: OpenShift Deployment with UBI-Based Images

**Choice:** Use `registry.access.redhat.com/ubi9/openjdk-21` as base image. Deploy with Kubernetes Deployment (3 replicas), Service, and OpenShift Route (TLS edge termination). Store secrets in OpenShift Secret resources.

**Rationale:**
- **Enterprise Standard**: UBI images are Red Hat's standard for production
- **Security**: Regular CVE patching, minimal attack surface
- **OpenShift Native**: Route resource for ingress (vs. raw Ingress)
- **RHDP Integration**: Matches workshop hosting environment

**Configuration:**
- CPU: 500m request, 1000m limit
- Memory: 1Gi request, 2Gi limit
- Liveness: `/actuator/health` (60s initial delay)
- Readiness: `/actuator/health/readiness` (30s initial delay)

**Alternatives Considered:**
- Eclipse Temurin images: Rejected - not enterprise-focused
- Kubernetes Ingress: Rejected - OpenShift Routes are idiomatic

### Decision 11: Evaluation Framework with Embeddings + LLM-as-Judge

**Choice:** Implement 3 metrics:
- **Accuracy**: Cosine similarity between expected and actual answer embeddings
- **Relevance**: Cosine similarity between combined context and response embeddings
- **Faithfulness**: LLM-as-judge rating (0.0-1.0) whether response is grounded in context

Load test cases from `data/eval-golden-set.json` with schema: `{id, query, context[], expectedAnswer}`.

**Rationale:**
- **Comprehensive**: Covers answer quality (accuracy), grounding (relevance), and hallucination prevention (faithfulness)
- **Automatable**: Can run in CI/CD pipelines
- **Educational**: Students learn evaluation is not just "looks good to me"

**Limitations:**
- **Golden Dataset Maintenance**: Requires manual curation of test cases
- **Subjectivity**: Embedding similarity doesn't capture semantic equivalence perfectly

**Alternatives Considered:**
- BLEU/ROUGE metrics: Rejected - poor for open-ended generation
- Human evaluation: Rejected - doesn't scale, not automatable

## Risks / Trade-offs

### Risk: OpenAI API Dependency
**Impact**: Workshop requires API key; costs vary with usage; rate limits can block students.
**Mitigation**:
- Provide clear API key setup instructions
- Use gpt-3.5-turbo where possible (cheaper)
- Implement semantic caching to reduce API calls
- Consider adding Ollama/local LLM option for cost-sensitive scenarios (future enhancement)

### Risk: Redis Single Point of Failure
**Impact**: If Redis goes down, memory and cache are lost.
**Mitigation**:
- Workshop scope: acceptable (students can restart Docker Compose)
- Production note: document Redis clustering for real deployments
- Graceful degradation: agents should handle missing memory (fall back to stateless)

### Risk: PII Detection False Negatives
**Impact**: Regex patterns won't catch all PII (non-US formats, creative obfuscation).
**Mitigation**:
- Document limitations clearly in workshop materials
- Mark as "educational implementation"
- Recommend production systems use dedicated PII services (AWS Comprehend, etc.)

### Risk: LLM-as-Judge Unreliability
**Impact**: Output validator may miss toxic content or hallucinate violations.
**Mitigation**:
- Use temperature=0.0 for determinism
- Combine with regex-based checks for known patterns
- Log all validation results for audit trail
- Document as one layer in defense-in-depth strategy

### Risk: Semantic Cache Scalability
**Impact**: Linear scan of cached queries doesn't scale past ~1000 entries.
**Mitigation**:
- Acceptable for workshop (limited query volume)
- Document scaling path: use vector DB (Chroma/pgvector) for production cache
- Implement cache size limits (e.g., Redis ZSET with score-based eviction)

### Risk: Module Independence vs. Code Duplication
**Impact**: Copying code across modules leads to drift; bug fixes don't propagate.
**Mitigation**:
- Acceptable trade-off for workshop (each module is a teaching artifact)
- Not intended for production use (students would refactor with shared libraries)
- Document clearly: "This module is self-contained for learning purposes"

### Trade-off: Educational Clarity vs. Production Patterns
**Decision**: Favor explicit, verbose implementations over abstractions.
**Examples**:
- BM25 implemented from scratch (vs. using Lucene)
- ReAct loop with string parsing (vs. LangGraph)
- Regex PII detection (vs. NER models)

**Rationale**: Students learn underlying mechanics before using libraries. Workshop materials note where production systems would differ.

## Migration Plan

### Phase 1: Module 03 (Tools & MCP)
1. Create `src/module-03-tools-mcp/` Maven module
2. Add PostgreSQL sample data (`customers`, `tickets` tables) to docker-compose.yml init script
3. Implement `CustomerDataTool` and `WeatherTool`
4. Configure `ChatLanguageModel` with tools in Spring `@Configuration`
5. Build `ToolOrchestrator` service
6. Create `/api/v1/assistant/chat` endpoint
7. Write unit tests for tools and integration tests for orchestration
8. Author AsciiDoc workshop instructions

### Phase 2: Module 04 (Agents)
1. Create `src/module-04-chatbots-to-agents/` Maven module
2. Implement `ReActAgent` with thought-action-observation loop
3. Build `ConversationMemoryService` with Redis-backed storage
4. Create `MultiAgentOrchestrator` with routing logic
5. Implement specialized agents (CustomerSupportAgent, TechnicalDocAgent, ProductExpertAgent)
6. Build `TaskDecomposer` for complex task handling
7. Create `/api/v1/agent/execute` endpoint with session management
8. Write tests covering ReAct iterations, memory persistence, routing
9. Author AsciiDoc workshop instructions

### Phase 3: Module 05 (Security)
1. Create `src/module-05-security-guardrails/` Maven module
2. Implement `PromptInjectionGuard` with pattern matching
3. Build `PIIMaskingService` with regex detection
4. Create `OutputValidator` with LLM-as-judge
5. Implement `DocumentAccessControl` with role/department filtering
6. Build `SecurityAuditService` with Redis logging
7. Create `/api/v1/secure/query` endpoint with layered security
8. Write tests for each security layer and integration tests for full pipeline
9. Author AsciiDoc workshop instructions with attack/defense scenarios

### Phase 4: Module 06 (Production)
1. Create `src/module-06-enterprise-production/` Maven module
2. Build `EvaluationService` with accuracy/relevance/faithfulness metrics
3. Create sample `data/eval-golden-set.json` with 10+ test cases
4. Implement OpenTelemetry tracing with `@Traced` aspect
5. Add Micrometer metrics (counter, timer, gauge)
6. Build `CachingService` with exact and semantic caching
7. Implement `TokenOptimizer` for context assembly
8. Create Dockerfile with UBI base image
9. Write OpenShift manifests (Deployment, Service, Route, Secret)
10. Add Prometheus scrape config and Grafana dashboard JSON
11. Update docker-compose.yml with Prometheus and Grafana services
12. Write tests for caching, optimization, evaluation
13. Author AsciiDoc workshop instructions for deployment

### Rollback Strategy
- Each module is independent; broken module doesn't affect others
- Workshop can continue with partial modules (e.g., skip Module 06 if OpenShift unavailable)
- Git branches: `module-03-complete`, `module-04-complete`, etc. for known-good states

## Open Questions

1. **Ollama Integration**: Should we add Ollama as an alternative to OpenAI for cost-sensitive students?
   - **Decision**: Defer to post-workshop enhancement. Adds setup complexity.

2. **Cross-Encoder Re-Ranking**: Module 02 uses embedding-based re-ranker. Should Module 03+ use cross-encoder (ms-marco-MiniLM)?
   - **Decision**: Keep embedding re-ranker for consistency. Note cross-encoder as enhancement in workshop materials.

3. **Agent State Persistence**: Should task decomposition state be persisted to Redis for resume-after-failure?
   - **Decision**: No - out of workshop scope. Document as production consideration.

4. **Evaluation UI**: Should Module 06 include a simple web UI for running evals?
   - **Decision**: No - keep focus on backend. Students use REST API or curl.

5. **Multi-Tenancy**: Should security module demonstrate tenant isolation (e.g., customer-scoped embeddings)?
   - **Decision**: No - single-tenant is sufficient for workshop. Note multi-tenancy as advanced topic.
