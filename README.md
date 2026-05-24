# Architecting Intelligent Enterprise Systems: From Scratch

A hands-on workshop that transforms developers into AI architects using Spring Boot, Langchain4J, and Java 25. Students progress from embedding fundamentals through RAG pipelines, tool integration, autonomous agents, security hardening, and production deployment on OpenShift.

## Modules

| # | Module | Topic |
|---|--------|-------|
| 1 | Vectors & Embeddings | Embedding generation, distance metrics, chunking, vector store |
| 2 | Advanced RAG | Hybrid search, re-ranking, query transformation |
| 3 | Tools & MCP | Model Context Protocol, database and API tools |
| 4 | From Chatbots to Agents | ReAct pattern, memory, multi-agent orchestration |
| 5 | Security & Guardrails | Prompt injection defense, PII masking, output validation |
| 6 | Enterprise & Production | Evals, observability, caching, OpenShift deployment |

## Prerequisites

- Java 25
- Maven 3.9+
- Docker (or Podman)

## Quick Start

### 1. Set Environment Variables

```bash
export OPENAI_API_KEY=your-api-key-here
export OPENAI_MODEL_NAME=gpt-4o-mini
```

### 2. Start Infrastructure Services

```bash
docker compose up -d
```

This starts:
- PostgreSQL (port 5432) - for Module 03 tools
- Redis (port 6379) - for Module 04 & 06 caching/memory
- ChromaDB (port 8000) - for Module 02 vector search
- Prometheus (port 9090) - for Module 06 metrics
- Grafana (port 3000) - for Module 06 dashboards

### 3. Build All Modules

From this directory:

```bash
mvn clean install
```

To build a single module:

```bash
mvn clean install -pl src/module-03-tools-mcp
```

### 4. Run a Module

Each module runs on a different port:
- Module 01: 8081
- Module 02: 8082
- Module 03: 8083
- Module 04: 8084
- Module 05: 8085
- Module 06: 8086

```bash
cd src/module-03-tools-mcp
mvn spring-boot:run
```

### 5. Test the Endpoints

```bash
# Module 03: Tools & MCP
curl -X POST http://localhost:8083/api/v1/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me customer with ID 1"}'

# Module 04: Agents
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Help me with a product question", "sessionId": "test123"}'

# Module 05: Security
curl -X POST http://localhost:8085/api/v1/secure/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What security features do you offer?", "userId": "user1", "userRoles": ["user"], "department": "engineering"}'

# Module 06: Production
curl -X POST http://localhost:8086/api/v1/production/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Tell me about your product"}'
```

## Module Details

### Module 03: Tools & MCP
- **Endpoint**: `/api/v1/assistant/chat`
- **Features**: Database tools, external API tools, tool orchestration
- **Dependencies**: PostgreSQL

### Module 04: Chatbots to Agents
- **Endpoint**: `/api/v1/agent/execute`
- **Features**: ReAct agents, conversation memory, multi-agent orchestration, task decomposition
- **Dependencies**: Redis, PostgreSQL

### Module 05: Security & Guardrails
- **Endpoint**: `/api/v1/secure/query`
- **Features**: Prompt injection defense, PII masking, output validation, access control, audit logging
- **Dependencies**: Redis

### Module 06: Enterprise & Production
- **Endpoints**: `/api/v1/production/query`, `/api/v1/evaluation/run`
- **Features**: Evaluation framework, distributed tracing, metrics, caching, token optimization
- **Dependencies**: Redis
- **Monitoring**: Prometheus (http://localhost:9090), Grafana (http://localhost:3000, admin/admin)

## Documentation

- **Sample Queries**: [docs/sample-queries.md](docs/sample-queries.md)
- **Troubleshooting**: [docs/troubleshooting.md](docs/troubleshooting.md)
- **Architecture**: [docs/architecture.md](docs/architecture.md)
- **ADR**: [ADR.md](ADR.md)

## Running the Workshop Site Locally

The workshop instructions are in `docs/tutorials`

```bash
cd docs/tutorials
npm ci
npx honkit serve
```

Then visit http://localhost:4000.

