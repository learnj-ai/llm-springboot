# Workshop Quick Start Guide

## Prerequisites

- Java 25
- Maven 3.9+
- Docker & Docker Compose
- OpenAI API Key

## Infrastructure Setup

Start all required services (PostgreSQL, Redis, ChromaDB):

```bash
docker-compose up -d
```

Verify services are running:
```bash
docker-compose ps
```

## Environment Configuration

Set your OpenAI API key:

```bash
export OPENAI_API_KEY="your-api-key-here"
```

## Module 03: Tools & MCP

Build and run:
```bash
cd src/module-03-tools-mcp
mvn clean spring-boot:run
```

Test the endpoint:
```bash
curl -X POST http://localhost:8083/api/v1/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is customer 12345'\''s email?"}'
```

## Module 04: Chatbots to Agents

Build and run:
```bash
cd src/module-04-chatbots-to-agents
mvn clean spring-boot:run
```

Test the ReAct agent:
```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the weather in Boston?", "mode": "react"}'
```

## Database Access

Connect to PostgreSQL:
```bash
docker exec -it llm-workshop-postgres psql -U workshop -d workshop_module04  # or workshop_module03
```

Sample queries:
```sql
SELECT * FROM customers;
SELECT * FROM support_tickets WHERE status = 'open';
```

## Redis Access

Connect to Redis CLI:
```bash
docker exec -it llm-workshop-redis redis-cli
```

## Troubleshooting

### Services won't start
```bash
docker-compose down -v
docker-compose up -d
```

### Port conflicts
Check if ports 5432, 6379, or 8000 are already in use:
```bash
lsof -i :5432
lsof -i :6379
lsof -i :8000
```

### Database schema not initialized
The schema is automatically loaded from `src/module-03-tools-mcp/src/main/resources/db/` on first startup.
If needed, reload:
```bash
docker-compose down -v
docker-compose up -d
```

## Compilation

Compile all modules:
```bash
mvn clean compile
```

Compile specific module:
```bash
mvn clean compile -pl :module-03-tools-mcp
mvn clean compile -pl :module-04-chatbots-to-agents
```

## Notes

- Module 03 runs on port 8083
- Module 04 runs on port 8084
- PostgreSQL data persists in Docker volume `postgres_data`
- Redis data persists in Docker volume `redis_data`
