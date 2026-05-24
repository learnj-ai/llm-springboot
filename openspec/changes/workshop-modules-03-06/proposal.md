## Why

The AI Workshop foundation (Modules 01-02) is complete with working vector search and RAG pipelines. The next phase must teach production-critical capabilities: tool integration, autonomous agents, security hardening, and enterprise deployment. Without these modules, developers cannot move from proof-of-concept to production AI systems. This change completes the workshop's progression from fundamentals to production-ready enterprise AI.

## What Changes

- **Module 03**: Implement MCP-compliant tools connecting LLMs to databases and external APIs
- **Module 04**: Build autonomous agents using ReAct pattern with stateful memory and multi-agent orchestration
- **Module 05**: Add security guardrails including prompt injection defense, PII masking, and output validation
- **Module 06**: Implement observability, caching, evaluation benchmarks, and OpenShift deployment

Each module includes:
- Complete Spring Boot application code with REST APIs
- Unit and integration tests
- AsciiDoc workshop instructions for hands-on labs
- Sample datasets and configuration
- Docker Compose services where needed

## Capabilities

### New Capabilities

- `module-03-tools-mcp`: Tool integration following Model Context Protocol standards. Includes database tools (JDBC), external API tools (REST), tool orchestration, and MCP server configuration.
- `module-04-agents`: Autonomous agent implementation with ReAct reasoning loop, stateful conversation memory (Redis-backed), multi-agent orchestration with routing, and task decomposition.
- `module-05-security`: Security guardrails including prompt injection detection/prevention, PII masking service, LLM-based output validation, role-based access control for RAG, and security audit logging.
- `module-06-production`: Production deployment infrastructure including LLM evaluation framework, distributed tracing (OpenTelemetry), semantic caching, token optimization, Prometheus/Grafana monitoring, and OpenShift deployment manifests.

### Modified Capabilities

<!-- No existing capabilities are being modified - this is additive work building on modules 01-02 -->

## Impact

- **Code**: New Maven modules under `src/module-03-tools-mcp/`, `src/module-04-chatbots-to-agents/`, `src/module-05-security-guardrails/`, `src/module-06-enterprise-production/`
- **Dependencies**: Adds JDBC (PostgreSQL), Redis, OpenTelemetry, Micrometer, Spring Security, and langchain4j-open-ai (for tool support)
- **Infrastructure**: Requires PostgreSQL, Redis (added to docker-compose.yml), and OpenShift cluster for Module 06
- **Documentation**: Four new AsciiDoc workshop modules under `site/content/modules/ROOT/pages/`
- **APIs**: New REST endpoints for tools (`/api/v1/assistant/chat`), agents (`/api/v1/agent/execute`), secure RAG (`/api/v1/secure/query`), and evaluation (`/api/v1/eval/run`)
- **Configuration**: Requires `OPENAI_API_KEY` environment variable (already used in Module 02), adds database and Redis connection configuration
