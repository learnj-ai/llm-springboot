# Infrastructure Requirements by Module

Quick reference for infrastructure services required by each workshop module.

## Summary Table

| Module | PostgreSQL | Redis | Prometheus | Grafana | ChromaDB | OpenAI API |
|--------|-----------|-------|------------|---------|----------|------------|
| **01** Vector Embeddings | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **02** Advanced RAG | ❌ | ❌ | ❌ | ❌ | ⚠️ | ✅ |
| **03** Tools & MCP | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **04** Chatbots to Agents | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| **05** Security & Guardrails | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ |
| **06** Enterprise Production | ❌ | ✅ | ✅ | ✅ | ❌ | ✅ |

- ✅ = Required
- ⚠️ = Optional
- ❌ = Not used

## Module Details

### Module 01: Vector Embeddings
**Infrastructure**: None

Pure Java implementation demonstrating embeddings and vector operations without external dependencies.

```bash
cd src/module-01-vector-embeddings
mvn spring-boot:run
```

---

### Module 02: Advanced RAG
**Infrastructure**: ChromaDB (optional)

- **ChromaDB** (port 8000): Vector store for semantic search
  - Optional: Can use in-memory store
  - Start: `docker run -p 8000:8000 chromadb/chroma`

```bash
cd src/module-02-advanced-rag
mvn spring-boot:run
```

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL_NAME=gpt-4
OPENAI_API_BASE=https://api.openai.com/
```

---

### Module 03: Tools & MCP
**Infrastructure**: PostgreSQL

- **PostgreSQL** (port 5432): Customer and support ticket data
  - Database: `workshop_module03`
  - User: `workshop` / Password: `workshop123`
  - Auto-initialized with schema and sample data

```bash
cd src/module-03-tools-mcp
docker-compose up -d
mvn spring-boot:run
```

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL_NAME=gpt-4o-mini
OPENAI_API_BASE=https://api.openai.com/
```

**Verify Database:**
```bash
docker exec -it module03-postgres psql -U workshop -d workshop_module03
\dt  # List tables
SELECT * FROM customers;
```

---

### Module 04: Chatbots to Agents
**Infrastructure**: PostgreSQL, Redis

- **PostgreSQL** (port 5432): Customer and ticket data
  - Database: `workshop_module04`
  - User: `workshop` / Password: `workshop123`
  
- **Redis** (port 6379): Conversation memory storage
  - Max memory: 256MB
  - Eviction: LRU policy
  - Persistence: AOF enabled

```bash
cd src/module-04-chatbots-to-agents
docker-compose up -d
mvn spring-boot:run
```

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL_NAME=gpt-4o-mini
OPENAI_API_BASE=https://api.openai.com/
```

**Verify Services:**
```bash
docker exec -it module04-postgres psql -U workshop -d workshop_module04
docker exec -it module04-redis redis-cli ping
```

---

### Module 05: Security & Guardrails
**Infrastructure**: Redis

- **Redis** (port 6379): Security audit event storage
  - Purpose: Security event logging
  - List key: `security-events`
  - Max events: 1000

```bash
cd src/module-05-security-guardrails
docker-compose up -d
mvn spring-boot:run
```

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL_NAME=gpt-4o
OPENAI_VALIDATOR_MODEL=gpt-4o-mini
REDIS_HOST=localhost
REDIS_PORT=6379
```

**View Security Events:**
```bash
docker exec -it module05-redis redis-cli LRANGE security-events 0 -1
```

---

### Module 06: Enterprise & Production
**Infrastructure**: Redis, Prometheus, Grafana

- **Redis** (port 6379): Response and semantic caching
  - Max memory: 512MB
  - Eviction: LRU policy
  - Cache TTL: 1 hour
  
- **Prometheus** (port 9090): Metrics collection
  - Scrapes: `/actuator/prometheus`
  - Retention: Default 15 days
  
- **Grafana** (port 3000): Metrics visualization
  - Credentials: admin/admin
  - Data source: Prometheus

```bash
cd src/module-06-enterprise-production
docker-compose up -d
mvn spring-boot:run
```

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL_NAME=gpt-4o-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
DOKIMOS_JUDGE_MODEL=gpt-4o
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Access Monitoring:**
- Application: http://localhost:8086
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

---

## Port Allocation

| Service | Port | Used By |
|---------|------|---------|
| PostgreSQL (Module 03) | 5432 | Module 03 |
| PostgreSQL (Module 04) | 5432 | Module 04 |
| Redis (Module 04) | 6379 | Module 04 |
| Redis (Module 05) | 6379 | Module 05 |
| Redis (Module 06) | 6379 | Module 06 |
| ChromaDB | 8000 | Module 02 (optional) |
| Prometheus | 9090 | Module 06 |
| Grafana | 3000 | Module 06 |
| Module 01 App | 8081 | - |
| Module 02 App | 8082 | - |
| Module 03 App | 8083 | - |
| Module 04 App | 8084 | - |
| Module 05 App | 8085 | - |
| Module 06 App | 8086 | - |

**Note**: Modules 03 and 04 can run simultaneously as they use different databases. Modules 04, 05, and 06 share port 6379 for Redis, so only one can run at a time (or configure different ports).

## Running Multiple Modules

### Sequential Approach (Recommended)

Run one module at a time, stopping services before moving to the next:

```bash
# Module 03
cd src/module-03-tools-mcp
docker-compose up -d
mvn spring-boot:run
# ... test and explore ...
mvn spring-boot:stop
docker-compose down

# Module 04
cd ../module-04-chatbots-to-agents
docker-compose up -d
mvn spring-boot:run
```

### Parallel Approach (Advanced)

To run multiple modules simultaneously, modify `docker-compose.yml` to use different ports:

**Module 04** - Keep Redis on 6379
**Module 05** - Change Redis port to 6380
**Module 06** - Change Redis port to 6381

Example for Module 05:
```yaml
services:
  redis:
    ports:
      - "6380:6379"  # Changed from 6379:6379
```

Then set in `.env`:
```bash
REDIS_PORT=6380  # For Module 05
```

## Docker Compose Cheat Sheet

### Start Services
```bash
docker-compose up -d           # Start in background
docker-compose up              # Start with logs
docker-compose up -d postgres  # Start specific service
```

### Monitor Services
```bash
docker-compose ps              # List containers
docker-compose logs            # View logs
docker-compose logs -f redis   # Follow logs for service
docker-compose top             # Show running processes
```

### Stop Services
```bash
docker-compose stop            # Stop containers
docker-compose down            # Stop and remove containers
docker-compose down -v         # Stop, remove containers and volumes
```

### Troubleshooting
```bash
docker-compose restart redis   # Restart service
docker-compose pull            # Pull latest images
docker-compose config          # Validate compose file
```

### Execute Commands
```bash
# PostgreSQL
docker exec -it module03-postgres psql -U workshop -d workshop_module03

# Redis
docker exec -it module04-redis redis-cli
docker exec -it module05-redis redis-cli MONITOR

# Check logs
docker logs module03-postgres
docker logs module04-redis
```

## Data Persistence

Each module's docker-compose uses named volumes for data persistence:

```bash
# List volumes
docker volume ls | grep module

# Inspect volume
docker volume inspect module03-postgres_postgres_data

# Backup volume
docker run --rm -v module03-postgres_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .

# Restore volume
docker run --rm -v module03-postgres_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /data

# Remove volume (WARNING: deletes all data)
docker-compose down -v
```

## Resource Requirements

### Minimum System Resources

- **RAM**: 4GB available
  - PostgreSQL: ~256MB per instance
  - Redis: ~256-512MB per instance
  - Prometheus: ~512MB
  - Grafana: ~256MB
  - Spring Boot apps: ~512MB each

- **Disk**: 2GB available
  - Docker images: ~1GB
  - Data volumes: ~500MB
  - Maven dependencies: ~500MB

- **CPU**: 2 cores recommended

### Production Recommendations

For production deployments:

1. **Increase Memory Limits**: See each `docker-compose.yml`
2. **Configure Persistence**: Use bind mounts or named volumes
3. **Enable Authentication**: Set passwords for Redis, PostgreSQL
4. **Network Isolation**: Use separate networks per module
5. **Resource Limits**: Add `mem_limit` and `cpus` to compose files
6. **Monitoring**: Enable health checks and alerts
7. **Backups**: Regular backups of databases and Redis

## Next Steps

1. Read [SETUP.md](SETUP.md) for detailed setup instructions
2. Check [.env.example](.env.example) for environment configuration
3. Review module-specific READMEs for feature details
4. Start with Module 01 and progress sequentially
5. Experiment with the infrastructure components

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
