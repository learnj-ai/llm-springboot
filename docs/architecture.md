# Architecture Documentation

This document describes the architecture of each workshop module.

## Module 03: Tools & MCP Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Request                        │
│                      POST /api/v1/assistant/chat            │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   AssistantController                        │
│  - Receives chat requests                                    │
│  - Delegates to ToolOrchestrator                            │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    ToolOrchestrator                          │
│  - Manages AI Services with tools                           │
│  - Detects tool execution requests                          │
│  - Processes tool results                                   │
└─────────────┬───────────────────────────┬───────────────────┘
              │                           │
              ▼                           ▼
    ┌──────────────────┐        ┌──────────────────┐
    │ CustomerDataTool │        │   WeatherTool    │
    │ @Tool annotated  │        │ @Tool annotated  │
    └────────┬─────────┘        └────────┬─────────┘
             │                           │
             ▼                           ▼
    ┌──────────────────┐        ┌──────────────────┐
    │   PostgreSQL     │        │  External API    │
    │  (Customers DB)  │        │ (Weather data)   │
    └──────────────────┘        └──────────────────┘
```

**Key Components**:
- **MCP Client**: Tool discovery and invocation layer
- **Tools**: Annotated methods that LLM can call
- **Tool Orchestrator**: Manages multi-tool workflows

## Module 04: Agent Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Request                          │
│                   POST /api/v1/agent/execute                │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     AgentController                          │
│  - Session management                                        │
│  - Memory retrieval/persistence                             │
└─────────┬────────────────────────────────┬──────────────────┘
          │                                │
          ▼                                ▼
┌──────────────────────┐        ┌───────────────────────────┐
│   ReActAgent         │        │ MultiAgentOrchestrator   │
│  - Thought loop      │        │  - Agent routing          │
│  - Action execution  │        │  - Response synthesis     │
│  - Observation       │        └────────┬──────────────────┘
└──────────────────────┘                 │
                                         ▼
                            ┌────────────────────────────────┐
                            │    Specialized Agents:         │
                            │  - CustomerSupportAgent        │
                            │  - TechnicalDocAgent          │
                            │  - ProductExpertAgent         │
                            └────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  ConversationMemoryService                   │
│  - Redis-backed chat memory                                 │
│  - 24-hour TTL                                              │
│  - Session-based isolation                                  │
└─────────────────────────────────────────────────────────────┘
```

**Key Patterns**:
- **ReAct**: Reasoning + Acting in iterative loop
- **Multi-Agent**: Specialized agents for different domains
- **Task Decomposition**: Breaking complex tasks into subtasks
- **Stateful Memory**: Conversation history in Redis

## Module 05: Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Request                          │
│                  POST /api/v1/secure/query                  │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   SecureRAGController                        │
│                    Security Layers ↓                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
        Layer 1: Input Validation
                            ▼
            ┌────────────────────────────┐
            │  PromptInjectionGuard      │
            │  - Pattern detection       │
            │  - Special char analysis   │
            │  - Input sanitization      │
            └────────────┬───────────────┘
                         │ [Approved]
        Layer 2: PII Masking (Input)
                         ▼
            ┌────────────────────────────┐
            │   PIIMaskingService        │
            │  - Email masking           │
            │  - Phone masking           │
            │  - SSN/Card masking        │
            └────────────┬───────────────┘
                         │ [Masked Input]
        Layer 3: RAG Execution
                         ▼
            ┌────────────────────────────┐
            │    SimpleRAGService        │
            │  - Query processing        │
            │  - Response generation     │
            └────────────┬───────────────┘
                         │ [Raw Response]
        Layer 4: Access Control
                         ▼
            ┌────────────────────────────┐
            │  DocumentAccessControl     │
            │  - Role-based filtering    │
            │  - Department restrictions │
            └────────────┬───────────────┘
                         │ [Filtered Response]
        Layer 5: Output Validation
                         ▼
            ┌────────────────────────────┐
            │    OutputValidator         │
            │  - LLM-as-judge safety     │
            │  - Hallucination detection │
            └────────────┬───────────────┘
                         │ [Validated]
        Layer 6: PII Masking (Output)
                         ▼
            ┌────────────────────────────┐
            │   PIIMaskingService        │
            │  - Final PII redaction     │
            └────────────┬───────────────┘
                         │ [Secure Response]
                         ▼
            ┌────────────────────────────┐
            │   SecurityAuditService     │
            │  - Event logging (Redis)   │
            │  - Severity-based alerts   │
            └────────────────────────────┘
```

**Defense-in-Depth Strategy**:
1. Input validation before processing
2. PII masking before LLM
3. Access control on retrieved data
4. Output validation before response
5. PII masking before user
6. Complete audit trail

## Module 06: Production Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Production Request                       │
│                 POST /api/v1/production/query               │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              ProductionRAGController (@Traced)               │
│  - Metrics collection                                        │
│  - Distributed tracing                                       │
└───────────────────────────┬─────────────────────────────────┘
                            │
        Layer 1: Caching
                            ▼
            ┌────────────────────────────┐
            │     CachingService         │
            │  - Semantic similarity     │
            │  - Embedding cache         │
            │  - Redis storage           │
            └────────┬───────────────────┘
                     │ [Cache Miss]
        Layer 2: Optimization
                     ▼
            ┌────────────────────────────┐
            │    TokenOptimizer          │
            │  - Context selection       │
            │  - Token budget mgmt       │
            │  - Prompt compression      │
            └────────┬───────────────────┘
                     │ [Optimized]
        Layer 3: RAG Execution
                     ▼
            ┌────────────────────────────┐
            │   SimpleRAGService         │
            │  - Query processing        │
            └────────┬───────────────────┘
                     │ [Response]
        Layer 4: Observability
                     ▼
    ┌──────────────────────────────────────────┐
    │           MetricsCollector               │
    │  - rag.queries.total (Counter)           │
    │  - rag.response.time (Timer)             │
    │  - rag.tokens.used (Gauge)               │
    └──────────────┬───────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────────┐
    │         Prometheus + Grafana             │
    │  - Metrics scraping                      │
    │  - Dashboard visualization               │
    └──────────────────────────────────────────┘

    ┌──────────────────────────────────────────┐
    │        OpenTelemetry Tracing             │
    │  - RAGTracingAspect (@Around)            │
    │  - Span creation & context propagation   │
    │  - Exception recording                   │
    └──────────────────────────────────────────┘
```

**Production Features**:
- **Evaluation Framework**: Automated quality metrics
- **Distributed Tracing**: Request flow visibility
- **Metrics**: Prometheus-compatible metrics
- **Caching**: Semantic and exact-match caching
- **Optimization**: Token budget management

## Deployment Architecture (OpenShift)

```
┌────────────────────────────────────────────────────────────┐
│                     OpenShift Cluster                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Route (TLS Edge Termination)             │  │
│  │       https://module-06-prod.apps.cluster.com        │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │                    Service                            │  │
│  │              ClusterIP (port 8080)                    │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │               Deployment (3 replicas)                 │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐              │  │
│  │  │  Pod 1  │  │  Pod 2  │  │  Pod 3  │              │  │
│  │  │ App:8086│  │ App:8086│  │ App:8086│              │  │
│  │  │         │  │         │  │         │              │  │
│  │  │ CPU:    │  │ CPU:    │  │ CPU:    │              │  │
│  │  │ 500m-1  │  │ 500m-1  │  │ 500m-1  │              │  │
│  │  │ Mem:    │  │ Mem:    │  │ Mem:    │              │  │
│  │  │ 1Gi-2Gi │  │ 1Gi-2Gi │  │ 1Gi-2Gi │              │  │
│  │  │         │  │         │  │         │              │  │
│  │  │Probes:  │  │Probes:  │  │Probes:  │              │  │
│  │  │Liveness │  │Liveness │  │Liveness │              │  │
│  │  │Readiness│  │Readiness│  │Readiness│              │  │
│  │  └────┬────┘  └────┬────┘  └────┬────┘              │  │
│  │       │            │            │                    │  │
│  └───────┼────────────┼────────────┼────────────────────┘  │
│          │            │            │                       │
│  ┌───────▼────────────▼────────────▼────────────────────┐  │
│  │                  Secret (openai-secret)               │  │
│  │         OPENAI_API_KEY: [base64 encoded]             │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘

External Dependencies:
    ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
    │ PostgreSQL   │     │    Redis     │     │  ChromaDB    │
    │  (External)  │     │  (External)  │     │  (External)  │
    └──────────────┘     └──────────────┘     └──────────────┘
```

**Deployment Features**:
- **High Availability**: 3 replicas with load balancing
- **Resource Management**: CPU/memory limits
- **Health Checks**: Liveness and readiness probes
- **Security**: TLS termination, secrets management
- **Scalability**: Horizontal pod autoscaling ready

## Data Flow Across Modules

```
Module 03: Request → Tools → Database/API → Response

Module 04: Request → Agent Router → Specialized Agent →
           Memory Store → Response

Module 05: Request → Security Layers (6 stages) →
           Audit Log → Response

Module 06: Request → Cache Check → Token Optimization →
           RAG → Metrics/Traces → Response
```

## Technology Stack Summary

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 4.0.5 |
| **Language** | Java 25 (preview features) |
| **LLM Integration** | Langchain4J 1.11.0 |
| **LLM Provider** | OpenAI (GPT-4, GPT-3.5) |
| **Database** | PostgreSQL 16 |
| **Cache/Memory** | Redis 7 |
| **Vector Store** | ChromaDB |
| **Metrics** | Micrometer + Prometheus |
| **Tracing** | OpenTelemetry |
| **Visualization** | Grafana |
| **Container** | Docker + Docker Compose |
| **Orchestration** | Kubernetes/OpenShift |
| **Build Tool** | Maven 3.x |

## Design Principles

1. **Modularity**: Each module is independent and can run standalone
2. **Production-Ready**: Real-world patterns, not toy implementations
3. **Security-First**: Defense-in-depth, PII protection, audit logging
4. **Observability**: Metrics, traces, and structured logging
5. **Testability**: Comprehensive unit and integration tests
6. **Scalability**: Stateless design, caching, resource optimization
