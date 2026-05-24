## 1. Module 03 - Tools & MCP Setup

- [x] 1.1 Create Maven module `src/module-03-tools-mcp/` with pom.xml
- [x] 1.2 Add dependencies: spring-boot-starter-web, spring-boot-starter-data-jdbc, postgresql, langchain4j, langchain4j-open-ai
- [x] 1.3 Create PostgreSQL schema for customers and tickets tables
- [x] 1.4 Add sample data SQL script for customers and support tickets
- [x] 1.5 Update docker-compose.yml with PostgreSQL init script

## 2. Module 03 - Tool Implementation

- [x] 2.1 Implement CustomerDataTool with @Tool annotation for getCustomerInfo method
- [x] 2.2 Implement CustomerDataTool searchTickets method with status parameter
- [x] 2.3 Create WeatherTool with @Tool annotation for getCurrentWeather method
- [x] 2.4 Add error handling for non-existent customers and API failures
- [x] 2.5 Create MCPServerConfig with ChatLanguageModel bean registering all tools

## 3. Module 03 - Tool Orchestration

- [x] 3.1 Implement ToolOrchestrator service handling tool execution requests
- [x] 3.2 Add logic to detect tool execution requests in AI responses
- [x] 3.3 Implement tool result processing and final response generation
- [x] 3.4 Create AssistantController with POST /api/v1/assistant/chat endpoint
- [x] 3.5 Add request/response DTOs (ChatRequest, ChatResponse)

## 4. Module 03 - Tests

- [x] 4.1 Write unit tests for CustomerDataTool methods
- [x] 4.2 Write unit tests for WeatherTool with mocked API calls
- [x] 4.3 Write integration test for tool orchestration flow
- [x] 4.4 Write controller test for /api/v1/assistant/chat endpoint

## 5. Module 04 - Agents Setup

- [x] 5.1 Create Maven module `src/module-04-chatbots-to-agents/` with pom.xml
- [x] 5.2 Add dependencies: spring-boot-starter-web, spring-boot-starter-data-redis, langchain4j, langchain4j-open-ai
- [x] 5.3 Copy base services from Module 03 (tools, embeddings)
- [x] 5.4 Configure Redis connection in application.properties

## 6. Module 04 - ReAct Agent Implementation

- [x] 6.1 Create ReActAgent service with solve method and max iterations parameter
- [x] 6.2 Implement REACT_PROMPT template with THOUGHT/ACTION/OBSERVATION structure
- [x] 6.3 Add extractAction method parsing ACTION: pattern from LLM response
- [x] 6.4 Add extractFinalAnswer method parsing FINAL ANSWER: pattern
- [x] 6.5 Implement iterative loop executing thought-action-observation cycle
- [x] 6.6 Add logging for each iteration showing thought, action, observation

## 7. Module 04 - Conversation Memory

- [x] 7.1 Create ConversationMemoryService managing session memories
- [x] 7.2 Implement RedisChatMemoryStore implementing ChatMemoryStore interface
- [x] 7.3 Add getOrCreateMemory method creating MessageWindowChatMemory with session ID
- [x] 7.4 Implement Redis TTL (24 hours) for memory expiration
- [x] 7.5 Add methods: addMessage, getHistory, clearMemory

## 8. Module 04 - Multi-Agent Orchestration

- [x] 8.1 Create SpecializedAgent interface with process method
- [x] 8.2 Implement CustomerSupportAgent handling support queries
- [x] 8.3 Implement TechnicalDocAgent handling documentation queries
- [x] 8.4 Implement ProductExpertAgent handling product queries
- [x] 8.5 Create MultiAgentOrchestrator with routing logic
- [x] 8.6 Implement LLM-based routing prompt selecting appropriate agent
- [x] 8.7 Add collaborativeRequest method gathering all agent perspectives
- [x] 8.8 Implement response synthesis combining multi-agent outputs

## 9. Module 04 - Task Decomposition

- [x] 9.1 Create TaskDecomposer service with executeComplexTask method
- [x] 9.2 Implement decompose method generating subtasks via LLM
- [x] 9.3 Add JSON parsing for subtask structure (id, description, dependencies)
- [x] 9.4 Implement dependency-aware execution order
- [x] 9.5 Create synthesizeResults method combining subtask outputs
- [x] 9.6 Define Subtask, SubtaskResult, TaskExecutionResult records

## 10. Module 04 - Agent API & Tests

- [x] 10.1 Create AgentController with POST /api/v1/agent/execute endpoint
- [x] 10.2 Add session management loading/saving conversation history
- [x] 10.3 Create AgentRequest and AgentResponse DTOs
- [x] 10.4 Write unit tests for ReActAgent iterations
- [x] 10.5 Write integration tests for memory persistence with Redis
- [x] 10.6 Write tests for multi-agent routing scenarios
- [x] 10.7 Write tests for task decomposition with dependencies

## 11. Module 05 - Security Setup

- [x] 11.1 Create Maven module `src/module-05-security-guardrails/` with pom.xml
- [x] 11.2 Add dependencies: spring-boot-starter-web, spring-boot-starter-data-redis, spring-boot-starter-security, langchain4j
- [x] 11.3 Copy RAG services from Module 02 (vector search, hybrid search, RAG pipeline)
- [x] 11.4 Configure security logging to application.yml

## 12. Module 05 - Prompt Injection Defense

- [x] 12.1 Create PromptInjectionGuard component with validate method
- [x] 12.2 Add regex patterns for common injection attempts (ignore instructions, system override, role manipulation)
- [x] 12.3 Implement special character ratio check (>30% threshold)
- [x] 12.4 Create sanitizeInput method removing HTML/XML tags
- [x] 12.5 Add whitespace normalization to sanitization
- [x] 12.6 Define ValidationResult record with approved/rejected states

## 13. Module 05 - PII Masking

- [x] 13.1 Create PIIMaskingService with maskPII method
- [x] 13.2 Add regex patterns for email, phone, SSN, credit card
- [x] 13.3 Implement masking replacing matches with [TYPE_REDACTED] tokens
- [x] 13.4 Create detectPII method returning PIIMatch list
- [x] 13.5 Define PIIMatch and PIIDetectionResult records

## 14. Module 05 - Output Validation

- [x] 14.1 Create OutputValidator service with LLM-as-judge
- [x] 14.2 Configure separate ChatLanguageModel (gpt-3.5-turbo, temperature=0.0)
- [x] 14.3 Implement validateOutput method checking toxicity, confidentiality, tone
- [x] 14.4 Add JSON parsing for validation response (safe, violations, confidence)
- [x] 14.5 Implement containsHallucination method comparing output to sources
- [x] 14.6 Define ValidationCriteria record

## 15. Module 05 - Access Control & Audit

- [x] 15.1 Create DocumentAccessControl service with filterByPermissions method
- [x] 15.2 Implement hasAccess checking required_role and department metadata
- [x] 15.3 Add enrichWithACL method adding role/department to segment metadata
- [x] 15.4 Create SecurityAuditService with logSecurityEvent method
- [x] 15.5 Implement Redis storage for audit events
- [x] 15.6 Add severity-based alerting (HIGH/CRITICAL trigger ERROR logs)
- [x] 15.7 Define SecurityEvent, AuditEvent records and Severity enum

## 16. Module 05 - Secure RAG API & Tests

- [x] 16.1 Create SecureRAGController with POST /api/v1/secure/query endpoint
- [x] 16.2 Wire security layers in sequence: injection guard → sanitize → PII mask → RAG → validate → PII mask
- [x] 16.3 Add security audit logging at each layer
- [x] 16.4 Create SecureRequest and SecureResponse DTOs
- [x] 16.5 Write unit tests for each security layer independently
- [x] 16.6 Write integration tests for full secure pipeline
- [x] 16.7 Write tests for prompt injection scenarios
- [x] 16.8 Write tests for PII detection/masking accuracy

## 17. Module 06 - Production Setup

- [x] 17.1 Create Maven module `src/module-06-enterprise-production/` with pom.xml
- [x] 17.2 Add dependencies: spring-boot-starter-actuator, micrometer-registry-prometheus, opentelemetry-api, spring-boot-starter-aop
- [x] 17.3 Copy RAG services and security layers from Modules 02 and 05
- [x] 17.4 Create sample eval dataset `data/eval-golden-set.json` with 10+ test cases
- [x] 17.5 Update docker-compose.yml adding Prometheus and Grafana services

## 18. Module 06 - Evaluation Framework

- [x] 18.1 Create EvaluationService with runEvaluation method
- [x] 18.3 Add calculateAccuracy using embedding cosine similarity
- [x] 18.4 Add calculateRelevance comparing response to context embeddings
- [x] 18.5 Add calculateFaithfulness using LLM-as-judge (0.0-1.0 score)
- [x] 18.6 Implement calculateAverages aggregating metrics across all cases
- [x] 18.7 Define EvalCase, EvalResult, MetricAverages, EvaluationReport records

## 19. Module 06 - Distributed Tracing

- [x] 19.1 Create TracingConfig bean configuring OpenTelemetry Tracer
- [x] 19.2 Create @Traced annotation for method instrumentation
- [x] 19.3 Implement RAGTracingAspect with @Around advice
- [x] 19.4 Add span creation with method name and component attribute
- [x] 19.5 Implement exception recording in spans (status=ERROR)
- [x] 19.6 Add span.makeCurrent() for context propagation

## 20. Module 06 - Metrics & Monitoring

- [x] 20.1 Create MetricsCollector service with MeterRegistry
- [x] 20.2 Add counter "rag.queries.total" for query count
- [x] 20.3 Add timer "rag.response.time" for latency tracking
- [x] 20.4 Add gauge "rag.tokens.used" for token consumption
- [x] 20.5 Instrument RAGService with metric recording
- [x] 20.6 Create Prometheus scrape config targeting /actuator/prometheus
- [x] 20.7 Create Grafana dashboard JSON with query rate, latency, error panels

## 21. Module 06 - Caching & Optimization

- [x] 21.1 Create CachingService with exact match cache using @Cacheable
- [x] 21.2 Implement semanticCacheGet checking embedding similarity (threshold 0.95)
- [x] 21.3 Implement semanticCachePut storing query-response pairs in Redis
- [x] 21.4 Add embedding cache (in-memory Map) to avoid recomputing embeddings
- [x] 21.5 Create TokenOptimizer service with optimizeContext method
- [x] 21.6 Implement segment selection by token budget and relevance score
- [x] 21.7 Add compressPrompt removing redundant whitespace and filler words
- [x] 21.8 Define TokenizedSegment record

## 22. Module 06 - OpenShift Deployment

- [x] 22.1 Create Dockerfile using registry.access.redhat.com/ubi9/openjdk-21 base
- [x] 22.2 Add JAR copy and ENTRYPOINT instructions
- [x] 22.3 Create deployment.yaml with Deployment resource (3 replicas)
- [x] 22.4 Add resource requests (1Gi memory, 500m CPU) and limits (2Gi, 1000m)
- [x] 22.5 Configure liveness probe on /actuator/health (60s initial delay)
- [x] 22.6 Configure readiness probe on /actuator/health/readiness (30s initial delay)
- [x] 22.7 Create Service resource exposing port 8080
- [x] 22.8 Create Route resource with TLS edge termination
- [x] 22.9 Create Secret resource for OPENAI_API_KEY
- [x] 22.10 Add environment variable configuration loading secrets

## 23. Module 06 - Tests

- [x] 23.1 Write unit tests for evaluation metrics (accuracy, relevance, faithfulness)
- [x] 23.2 Write tests for semantic cache hit/miss scenarios
- [x] 23.3 Write tests for token optimization with budget constraints
- [x] 23.4 Write integration tests for distributed tracing span creation
- [x] 23.5 Write tests for metrics collection and Prometheus export

## 24. Workshop Documentation - Module 03

- [x] 24.1 Create site/content/modules/ROOT/pages/module-03-tools-mcp.adoc
- [x] 24.2 Write introduction section (learning objectives, prerequisites, time estimate)
- [x] 24.3 Write background section on MCP architecture and tool integration
- [x] 24.4 Create Lab 3.1: Implement database tool
- [x] 24.5 Create Lab 3.2: Implement external API tool
- [x] 24.6 Create Lab 3.3: Configure MCP server
- [x] 24.7 Create Lab 3.4: Build tool orchestration
- [x] 24.8 Add knowledge check questions
- [x] 24.9 Add challenge exercise (implement custom tool)

## 25. Workshop Documentation - Module 04

- [x] 25.1 Create site/content/modules/ROOT/pages/module-04-chatbots-to-agents.adoc
- [x] 25.2 Write introduction section (learning objectives, prerequisites, time estimate)
- [x] 25.3 Write background section on ReAct pattern and agent architectures
- [x] 25.4 Create Lab 4.1: Implement ReAct agent
- [x] 25.5 Create Lab 4.2: Add stateful memory
- [x] 25.6 Create Lab 4.3: Build multi-agent orchestration
- [x] 25.7 Create Lab 4.4: Implement task decomposition
- [x] 25.8 Add knowledge check questions
- [x] 25.9 Add challenge exercise (create specialized agent)

## 26. Workshop Documentation - Module 05

- [x] 26.1 Create site/content/modules/ROOT/pages/module-05-security-guardrails.adoc
- [x] 26.2 Write introduction section (learning objectives, prerequisites, time estimate)
- [x] 26.3 Write background section on AI security threats and defense strategies
- [x] 26.4 Create Lab 5.1: Implement prompt injection defense
- [x] 26.5 Create Lab 5.2: Build PII masking service
- [x] 26.6 Create Lab 5.3: Add output validation
- [x] 26.7 Create Lab 5.4: Implement access control
- [x] 26.8 Create Lab 5.5: Build security audit logging
- [x] 26.9 Add knowledge check questions with attack scenarios
- [x] 26.10 Add challenge exercise (implement additional security layer)

## 27. Workshop Documentation - Module 06

- [x] 27.1 Create site/content/modules/ROOT/pages/module-06-enterprise-production.adoc
- [x] 27.2 Write introduction section (learning objectives, prerequisites, time estimate)
- [x] 27.3 Write background section on production AI operations and observability
- [x] 27.4 Create Lab 6.1: Build evaluation framework
- [x] 27.5 Create Lab 6.2: Implement distributed tracing
- [x] 27.6 Create Lab 6.3: Add metrics and monitoring
- [x] 27.7 Create Lab 6.4: Implement caching strategies
- [x] 27.8 Create Lab 6.5: Deploy to OpenShift
- [x] 27.9 Add knowledge check questions
- [x] 27.10 Add challenge exercise (optimize performance)

## 28. Integration & Polish

- [x] 28.1 Update site navigation (nav.adoc) with new modules
- [x] 28.2 Add cross-references between modules
- [x] 28.3 Create sample query collections for testing each module
- [x] 28.4 Write troubleshooting guide for common issues
- [x] 28.5 Update main README with module overview and setup instructions
- [x] 28.6 Add architecture diagrams for tools, agents, security, deployment
- [x] 28.7 Review and update ADR.md with any additional decisions made during implementation
