<!-- .slide: class="title-slide" -->

# Building Production-Ready LLM Applications with Spring Boot

## Architecting Intelligent Enterprise Systems

### From Vector Embeddings to Production Deployment

---

<!-- .slide: class="center middle" -->

## Workshop Authors

### Bazlur Rahman
Java Champion, Author, ...

### Shaaf Syed
Java, Architect, ML, Technical Editor, Open source Evangelist  
Red Hat Inc.

---

## Workshop Overview

**Transform from Java Developer to AI Architect**

- **Duration:** 25-30 hours of hands-on learning
- **Modules:** 6 progressive modules from beginner to advanced
- **Tech Stack:** Java 25, Spring Boot 4.0, LangChain4J 1.11
- **Approach:** Production-ready patterns, not toy examples

**What You'll Build:**

- Semantic search systems with vector embeddings
- Advanced RAG pipelines with hybrid search
- Tool-augmented LLM applications
- Autonomous agents with ReAct patterns
- Secure, monitored, production-grade systems

---

<!-- .slide: class="module-intro" -->

## Module 01: Vector Embeddings

### 🎯 Semantic Search Fundamentals

**Estimated Time:** 3.5 hours | **Difficulty:** Beginner

**Key Concepts:**

- Transform text into mathematical vectors
- Cosine similarity and distance metrics
- Document chunking strategies
- Vector stores and retrieval

---

## Module 01: What You'll Build

### Semantic Search API

**Implementation:**

- Embedding generation service
- In-memory vector store
- Similarity-based retrieval
- REST API endpoints

**Learning Outcomes:**

- Understand how embeddings capture meaning
- Implement efficient document chunking
- Build your first semantic search system
- Foundation for all RAG applications

---

<!-- .slide: class="module-intro" -->

## Module 02: Advanced RAG

### 🚀 Beyond Basic Retrieval

**Estimated Time:** 4-5 hours | **Difficulty:** Intermediate

**Key Concepts:**

- Query transformation and expansion
- Hybrid search (BM25 + Vector)
- Reciprocal Rank Fusion (RRF)
- Modern Java structured concurrency

---

## Module 02: What You'll Build

### Advanced RAG Pipeline

**Implementation:**

- Query rewriting service
- Hybrid search combining keyword + semantic
- Re-ranking with cross-encoders
- Parallel search with virtual threads

**Learning Outcomes:**

- Dramatically improve answer quality
- Combine multiple retrieval strategies
- Leverage Java 25 structured concurrency
- Production-grade RAG patterns

---

<!-- .slide: class="module-intro" -->

## Module 03: Tools & MCP

### 🔧 Connecting LLMs to the World

**Estimated Time:** 5 hours | **Difficulty:** Intermediate

**Key Concepts:**

- LLM function calling
- Model Context Protocol (MCP)
- Database tool integration
- External API orchestration

---

## Module 03: What You'll Build

### Tool-Augmented Assistant

**Implementation:**

- Database query tools
- REST API integration tools
- Tool execution service
- MCP-compliant tool definitions

**Learning Outcomes:**

- Enable LLMs to access live data
- Build reusable tool libraries
- Understand MCP architecture
- Real-world LLM applications

---

<!-- .slide: class="module-intro" -->

## Module 04: Chatbots to Agents

### 🤖 Autonomous Intelligence

**Estimated Time:** 4-6 hours | **Difficulty:** Advanced

**Key Concepts:**

- ReAct pattern (Reasoning + Acting)
- Agent execution loops
- Conversation memory management
- Multi-agent orchestration

---

## Module 04: What You'll Build

### Autonomous Agent System

**Implementation:**

- ReAct agent with tool access
- Redis-backed conversation memory
- Task decomposition service
- Multi-agent routing system

**Learning Outcomes:**

- Build agents that plan and act
- Implement persistent memory
- Orchestrate specialized agents
- Production agent architectures

---

<!-- .slide: class="module-intro" -->

## Module 05: Security & Guardrails

### 🔒 Hardening Your LLM Apps

**Estimated Time:** 4.5 hours | **Difficulty:** Intermediate

**Key Concepts:**

- Prompt injection defense
- PII detection and masking
- Output validation and filtering
- RBAC/ABAC access control

---

## Module 05: What You'll Build

### Secure RAG Service

**Implementation:**

- Prompt injection guard
- PII masking service
- Output validation filters
- Document access control
- Security audit logging

**Learning Outcomes:**

- Defend against prompt attacks
- Protect sensitive information
- Validate LLM outputs
- Enterprise security patterns

---

<!-- .slide: class="module-intro" -->

## Module 06: Enterprise Production

### 🏢 Taking LLMs to Production

**Estimated Time:** 6-8 hours | **Difficulty:** Advanced

**Key Concepts:**

- RAG evaluation frameworks (Dokimos)
- Distributed tracing (OpenTelemetry)
- Semantic caching strategies
- Cost optimization and monitoring

---

## Module 06: What You'll Build

### Production-Grade System

**Implementation:**

- Evaluation pipeline with metrics
- OpenTelemetry distributed tracing
- Redis semantic cache
- Prometheus metrics + Grafana dashboards
- Kubernetes deployment configs

**Learning Outcomes:**

- Measure RAG quality objectively
- Debug complex LLM systems
- Optimize cost and performance
- Deploy to Kubernetes/OpenShift

---

## Workshop Architecture

```
Client → Security → Agent → RAG Pipeline → LLM
           ↓          ↓         ↓
        Audit    Memory    Vector Store

        Observability Layer (Tracing, Metrics, Eval)
```

**Solid Foundation:**

- Spring Boot 4.0 + Java 25
- LangChain4J for LLM integration
- PostgreSQL + Redis + Vector stores
- Docker + Kubernetes deployment

---

<!-- .slide: class="center middle" -->

## Prerequisites

**Required Knowledge:**

- Java 25 (records, virtual threads, structured concurrency)
- Spring Boot (DI, REST, configuration)
- REST APIs and HTTP fundamentals
- Git basics

**Required Tools:**

- JDK 25 with preview features enabled
- Maven 3.8+
- Docker & Docker Compose
- OpenAI API key

---

<!-- .slide: class="center middle" -->

## Learning Outcomes

✅ Design production-ready LLM applications

✅ Build semantic search with vector embeddings

✅ Implement advanced RAG with hybrid search

✅ Integrate tools via Model Context Protocol

✅ Create autonomous agents with ReAct

✅ Secure apps with comprehensive guardrails

✅ Deploy with monitoring and evaluation

---

<!-- .slide: class="title-slide" -->

## Ready to Begin?

### Start with Module 01: Vector Embeddings

**Let's build something intelligent together!**

🚀 Happy Learning!
