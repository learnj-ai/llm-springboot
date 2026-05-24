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

The workshop instructions are an [Antora](https://antora.org/) site using the RHDP Showroom theme. To build and preview it locally, run the following from the **repository root** (one level up from this directory):

```bash
cd ..

docker run --user=$(id -u) --rm \
  -v $(pwd):/showroom/repo \
  --mount 'type=tmpfs,dst=/showroom/repo/.cache,tmpfs-mode=1777' \
  --entrypoint antora \
  -w /showroom/repo \
  ghcr.io/rhpds/showroom-content:latest \
  default-site.yml
```

If your shell prompt still shows `llm-springboot %`, you are in the wrong directory for this command. The playbook file is at the repository root, not inside `llm-springboot/`.

> **Note:** Replace `docker` with `podman` if that is your container runtime.

The generated site will be in the `output/` directory at the repository root. Open it in your browser:

```bash
open output/index.html
```

Or serve it locally:

```bash
npx http-server output -p 8443
```

Then visit http://localhost:8443.

### Rebuilding After Changes

There is no long-running dev server or file watcher. The container starts, builds the site, and exits. After editing any `.adoc` file, re-run the same `docker run` (or `podman run`) command above and refresh your browser. No containers need to be restarted -- each run is a clean, one-shot build that overwrites `output/`.

You can use fswatch to watch for changed for example

```bash

  # Install once
  brew install fswatch
  # Terminal 1 — serve
  npx http-server output -p 8443
  # Terminal 2 — watch and rebuild on save
  fswatch -o llm-springboot/site/content/ | while read; do
    docker run --user=$(id -u) --rm \
      -v $(pwd):/showroom/repo \
      --mount 'type=tmpfs,dst=/showroom/repo/.cache,tmpfs-mode=1777' \
      --entrypoint antora -w /showroom/repo \
      ghcr.io/rhpds/showroom-content:latest default-site.yml
  done

```

## Contributing

### Project Structure

```
llm-springboot/
├── pom.xml                        Parent POM (module declarations only)
├── README.md
├── .gitignore
│
├── site/                          Workshop instructions (Antora)
│   ├── www/                       Build output (git-ignored)
│   └── content/
│       ├── antora.yml             Component descriptor
│       ├── lib/                   Antora extensions
│       └── modules/ROOT/
│           ├── nav.adoc           Left-hand navigation
│           ├── assets/images/     Screenshots and diagrams
│           ├── examples/          Downloadable assets
│           ├── partials/          Reusable content fragments
│           └── pages/             AsciiDoc pages (one per module)
│
├── pm/                            Project management
│   ├── ADR.md                     Architecture Decision Records
│   ├── spec.md                    Technical specification
│   └── workshop-abstract.md       Workshop abstract and module overview
│
└── src/                           Source code (Maven modules)
    ├── module-01-vector-embeddings/
    ├── module-02-advanced-rag/
    ├── module-03-tools-mcp/
    ├── module-04-chatbots-to-agents/
    ├── module-05-security-guardrails/
    └── module-06-enterprise-production/
```

**Three directories, three concerns:**

- **`site/`** — All Antora/AsciiDoc content for the workshop instructions. Edit pages under `site/content/modules/ROOT/pages/`. The GitHub Actions workflow at the repo root builds and deploys this to GitHub Pages.
- **`pm/`** — Planning and process documents. The ADR log tracks all architecture decisions. Update `ADR.md` when making significant technical choices.
- **`src/`** — Java source code organized as Maven modules, one per workshop module. Each module has its own `pom.xml` referencing the parent. Module naming follows `module-NN-slug` to match the corresponding `.adoc` page filename.

### Adding Content

- **New instruction page:** Create a `.adoc` file in `site/content/modules/ROOT/pages/` and add an `xref` entry to `site/content/modules/ROOT/nav.adoc`.
- **New image:** Place it in `site/content/modules/ROOT/assets/images/` and reference it with `image::filename.png[alt text]`.
- **New source code:** Add classes under the appropriate module in `src/module-NN-*/src/main/java/com/techcorp/assistant/`.
- **New decision:** Append an ADR entry to `pm/ADR.md`.
