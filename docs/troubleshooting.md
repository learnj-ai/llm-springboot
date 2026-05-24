# Troubleshooting Guide

Common issues and solutions for the LLM Spring Boot Workshop.

## Environment Setup Issues

### Docker Services Won't Start

**Problem**: `docker compose up` fails with port conflicts

**Solution**:
```bash
# Check if ports are already in use
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :8000  # ChromaDB

# Stop conflicting services or use different ports in docker-compose.yml
```

### OPENAI_API_KEY Not Set

**Problem**: Application fails with "API key not configured"

**Solution**:
```bash
# Set environment variable
export OPENAI_API_KEY=your-key-here

# Or add to .env file (don't commit!)
echo "OPENAI_API_KEY=your-key-here" > .env

# For production, use secrets management
```

## Compilation Issues

### Java 25 Preview Features

**Problem**: Compilation errors with record patterns or other preview features

**Solution**:
```bash
# Verify Java version
java --version  # Should be 25 or higher

# Ensure --enable-preview is set in maven-compiler-plugin
# Already configured in parent pom.xml
```

### Langchain4J API Changes

**Problem**: `Cannot find symbol ChatLanguageModel`

**Solution**:
- Langchain4J 1.11.0 uses `ChatModel` interface, not `ChatLanguageModel`
- Use `.chat()` method instead of `.generate()`
- Import `dev.langchain4j.model.chat.ChatModel`

### Spring Boot 4.0.5 Issues

**Problem**: Spring Boot dependencies not resolving

**Solution**:
```bash
# Clean and rebuild
mvn clean install -U

# Verify parent POM inheritance
# Check spring-boot-starter-parent version is 4.0.5
```

## Runtime Issues

### Redis Connection Errors

**Problem**: `Could not connect to Redis at localhost:6379`

**Solution**:
```bash
# Start Redis
docker compose up -d redis

# Check Redis is running
docker ps | grep redis

# Test connection
redis-cli ping
```

### PostgreSQL Connection Errors

**Problem**: Module 03 can't connect to database

**Solution**:
```bash
# Verify PostgreSQL is running
docker compose up -d postgres

# Check logs
docker logs llm-workshop-postgres

# Test connection
psql -h localhost -U workshop -d workshop_module03  # or workshop_module04
# Password: workshop123
```

### ChromaDB Not Responding

**Problem**: Vector search operations fail

**Solution**:
```bash
# Restart ChromaDB
docker compose restart chromadb

# Check health
curl http://localhost:8000/api/v1/heartbeat

# Clear data if needed
docker compose down -v chromadb
```

## Module-Specific Issues

### Module 03: Tool Execution Fails

**Problem**: Tools not being discovered or invoked

**Solutions**:
- Ensure `@Tool` annotation is present on methods
- Check that tools are registered in `AiServices.builder().tools(...)`
- Verify tool method signatures match expected format
- Enable debug logging: `logging.level.dev.langchain4j=DEBUG`

### Module 04: Memory Not Persisting

**Problem**: Agent forgets conversation history

**Solutions**:
- Check Redis is running and accessible
- Verify session ID is being passed correctly
- Check TTL settings in application.yml
- Test with: `redis-cli KEYS "chat:memory:*"`

### Module 05: All Queries Rejected

**Problem**: Legitimate queries being flagged as injection attempts

**Solutions**:
- Adjust `max-special-char-ratio` in application.yml (default 0.30)
- Review regex patterns in `PromptInjectionGuard`
- Check logs for specific rejection reasons
- Temporarily disable guards for testing (not for production!)

### Module 06: Metrics Not Showing

**Problem**: Prometheus/Grafana not displaying metrics

**Solutions**:
```bash
# Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets

# Verify actuator endpoint
curl http://localhost:8086/actuator/prometheus

# Check Prometheus config
cat docker/prometheus/prometheus.yml

# Restart services
docker compose restart prometheus grafana
```

## Performance Issues

### Slow LLM Responses

**Problem**: Queries taking >10 seconds

**Solutions**:
- Enable semantic caching in Module 06
- Reduce context size with token optimization
- Use faster model (`gpt-4o-mini` instead of `gpt-4o`)
- Check network latency to OpenAI API

### High Memory Usage

**Problem**: Application consuming >2GB RAM

**Solutions**:
- Enable JVM heap size limits: `-Xmx1g`
- Clear embedding cache periodically
- Reduce Redis memory usage
- Monitor with: `jconsole` or `VisualVM`

## Testing Issues

### Tests Fail with NullPointerException

**Problem**: Mocked dependencies not initialized

**Solutions**:
- Use `@ExtendWith(MockitoExtension.class)`
- Ensure `@Mock` and `@InjectMocks` are properly annotated
- Initialize mocks in `@BeforeEach` method
- Check constructor injection vs field injection

### Integration Tests Can't Connect to Services

**Problem**: Tests fail with connection errors

**Solutions**:
- Use Testcontainers for isolated testing
- Or ensure Docker services are running
- Use `@SpringBootTest` with proper profiles
- Mock external dependencies for unit tests

## Debugging Tips

### Enable Debug Logging

Add to application.yml:
```yaml
logging:
  level:
    com.techcorp.assistant: DEBUG
    dev.langchain4j: DEBUG
    org.springframework.ai: DEBUG
```

### Trace HTTP Requests

```bash
# Enable request/response logging
logging.level.org.springframework.web=DEBUG

# Or use network inspector
# In browser DevTools or Postman
```

### Inspect Redis Data

```bash
# Connect to Redis CLI
docker exec -it llm-workshop-redis redis-cli

# List all keys
KEYS *

# Get specific value
GET key-name

# Monitor operations
MONITOR
```

### Check Database State

```bash
# Connect to PostgreSQL
docker exec -it llm-workshop-postgres psql -U workshop -d workshop_module03  # or workshop_module04

# List tables
\dt

# Query data
SELECT * FROM customers;
SELECT * FROM support_tickets;
```

## Getting Help

1. Check the module-specific documentation in `site/content/modules/ROOT/pages/`
2. Review implementation in `src/module-XX-*/`
3. Search existing issues: https://github.com/learnj-ai/llm-springboot-workshop/issues
4. Ask in workshop discussions or create a new issue

## Common Error Messages

| Error | Module | Cause | Solution |
|-------|--------|-------|----------|
| `ChatModel cannot be resolved` | All | Wrong import | Use `dev.langchain4j.model.chat.ChatModel` |
| `Connection refused: localhost:6379` | 04, 05, 06 | Redis not running | `docker compose up -d redis` |
| `Prompt injection detected` | 05 | Security guard triggered | Review query content |
| `No embeddings found` | 02, 06 | Empty vector store | Initialize with seed data |
| `Rate limit exceeded` | All | Too many OpenAI requests | Implement backoff/retry |

## Performance Benchmarks

Expected performance metrics:

- **Module 03 (Tools)**: <3s per query
- **Module 04 (Agents)**: 3-8s per query (depends on iterations)
- **Module 05 (Security)**: <4s per query (includes validation)
- **Module 06 (Production)**: <2s with cache hit, <5s cache miss

If you're seeing >2x these times, check network latency and enable caching.
