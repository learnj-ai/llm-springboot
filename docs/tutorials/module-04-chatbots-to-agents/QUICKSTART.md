# Module 04 Quick Start Guide

This quick start guide helps you get the module running in 5 minutes.

## Prerequisites

- Java 25 (workshop uses `--enable-preview`)
- Maven 3.9+
- Docker and Docker Compose
- OpenAI API key

## 1. Start Infrastructure

```bash
# Start PostgreSQL and Redis
cd src/module-04-chatbots-to-agents
docker-compose up -d postgres redis

# Wait for services to be ready (about 10 seconds)
sleep 10
```

## 2. Configure Environment

```bash
# Set your OpenAI API key
export OPENAI_API_KEY="sk-your-api-key-here"
```

## 3. Build and Run

```bash
# Build the module
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8084`

## 4. Test the Agent

### ReAct Mode (Reasoning with Tools)

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What subscription plan is customer CUST001 on?",
    "mode": "react"
  }'
```

### Multi-Agent Mode (Routing)

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How do I integrate with your API?",
    "mode": "multiagent"
  }'
```

### Task Decomposition Mode

```bash
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Plan a complete product launch strategy",
    "mode": "decompose"
  }'
```

### Stateful Conversation

```bash
# First message (get session ID from response)
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tell me about customer CUST001",
    "mode": "react"
  }'

# Follow-up message (use session ID from previous response)
curl -X POST http://localhost:8084/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What was that customer ID again?",
    "sessionId": "abc-123-def-456",
    "mode": "react"
  }'
```

## 5. View Tutorial

```bash
# Install HonKit
cd docs/tutorials/module-04-chatbots-to-agents
npm install

# Serve tutorial locally
npx honkit serve

# Open browser to http://localhost:4000
```

## Agent Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `react` | ReAct pattern with tool use | Questions requiring data access |
| `multiagent` | Route to specialized agent | Domain-specific queries |
| `collaborative` | All agents collaborate | Complex multi-domain questions |
| `decompose` | Break down complex tasks | Multi-step workflows |

## Sample Test Data

The module includes sample customers and support tickets:

**Customers:**
- `CUST001`: Alice Johnson, premium
- `CUST002`: Bob Smith, standard
- `CUST003`: Carol White, premium
- `CUST004`: David Brown, basic

**Support Tickets:**
- `TKT001` — CUST001, "Cannot access premium features" (open)
- `TKT002` — CUST002, "Billing question" (pending)
- `TKT003` — CUST003, "Feature request: Dark mode" (open)
- `TKT004` — CUST001, "Integration help needed" (closed)

## Architecture Overview

```
User Request
    ↓
AgentController (REST API)
    ↓
┌──────────────┬──────────────┬─────────────┐
│  ReActAgent  │  MultiAgent  │  TaskDecomp │
│   (tools)    │  (routing)   │  (subtasks) │
└──────┬───────┴──────┬───────┴──────┬──────┘
       ↓              ↓              ↓
  [Tools]      [Specialized]    [LLM Tasks]
  - Database    Agents          - Breakdown
  - Weather    - Support        - Execute
  - etc.       - Tech Doc       - Synthesize
               - Product
    ↓              ↓              ↓
ConversationMemoryService (Redis)
```

## Common Issues

### Port Already in Use

```bash
# Change port in application.yml
# server:
#   port: 8085
```

### Database Connection Failed

```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs module-04-postgres
```

### Redis Connection Failed

```bash
# Verify Redis is running
docker ps | grep redis

# Test connection
redis-cli ping
```

### OpenAI API Errors

```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Check API key validity at platform.openai.com
```

## Next Steps

1. Read the full tutorial: [README.md](README.md)
2. Explore the code in `src/main/java/com/techcorp/assistant/module04/`
3. Run the tests: `mvn test`
4. Try the practice exercises in each chapter
5. Build your own custom agents

## Monitoring

Check application health:

```bash
curl http://localhost:8084/api/v1/agent/health
```

View metrics (if actuator enabled):

```bash
curl http://localhost:8084/actuator/metrics
```

## Cleanup

```bash
# Stop the application (Ctrl+C)

# Stop infrastructure
docker-compose down

# Remove volumes (optional)
docker-compose down -v
```

## Documentation

- Full Tutorial: [README.md](README.md)
- API Reference: `src/main/java/com/techcorp/assistant/module04/controller/AgentController.java`
- Configuration: `src/main/resources/application.yml`
- Tests: `src/test/java/`

## Support

- Review test files for usage examples
- Check chapter exercises for common patterns
- Consult LangChain4j docs: https://docs.langchain4j.dev/
- Open issues in the workshop repository

Happy building!
