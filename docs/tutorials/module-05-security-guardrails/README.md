# Securing LLM Applications: Guardrails and Safety Patterns

Welcome to Module 05 of the LLM Spring Boot Workshop! This tutorial teaches you how to build comprehensive security guardrails for production LLM applications using Spring Boot and defensive programming patterns.

![Security XKCD](https://imgs.xkcd.com/comics/security.png)
*Source: [xkcd.com](https://xkcd.com/538/)*

## What You'll Build

A multi-layered security system that protects both inputs and outputs of LLM applications. You'll learn how to defend against prompt injection attacks, detect and redact sensitive data, validate AI outputs, implement access controls, and maintain security audit trails.

## Learning Objectives

By completing this tutorial, you will:

- Understand and defend against prompt injection attacks
- Implement PII detection and masking for compliance
- Validate AI outputs using LLM-as-judge patterns
- Detect hallucinations by grounding responses in source documents
- Build role-based and attribute-based access control systems
- Create comprehensive security audit trails
- Apply defense-in-depth security architecture
- Design fail-safe systems that reject on error

## Technologies Used

- Java 25
- Spring Boot 4.0
- LangChain4j 1.11.0
- Redis (for audit logging)
- OpenAI API (dual-model setup: primary + validator)
- Maven

## Prerequisites

### Required Knowledge

1. **Java Fundamentals**: Comfortable with classes, interfaces, generics, and modern Java features
2. **Spring Boot Basics**: Understanding of dependency injection, component scanning, and REST controllers
3. **LLM Concepts**: Familiarity with what LLMs are and how they generate responses
4. **Basic Security Awareness**: Understanding of authentication, authorization, and input validation

### Development Environment

- **Java 25** installed
- **Maven 3.6+** for building the project
- **Redis server** running locally (or Docker: `docker run -d -p 6379:6379 redis`)
- **OpenAI API key** (set as environment variable: `OPENAI_API_KEY`)
- **IDE** with Java support (IntelliJ IDEA, VS Code, or Eclipse)
- **curl** or **Postman** for testing REST endpoints

### System Requirements

- **RAM**: 8GB minimum
- **Disk Space**: ~500MB for dependencies
- **Network**: Internet connection for OpenAI API calls

## Tutorial Structure

This tutorial is organized into chapters that build on each other:

1. **Introduction** - Security landscape and architecture overview
2. **Prompt Injection Guard** - Defending against malicious inputs
3. **PII Masking Service** - Protecting sensitive data
4. **Output Validator** - Ensuring safe AI responses
5. **Document Access Control** - Implementing authorization
6. **Security Audit Service** - Logging and monitoring
7. **LLM Configuration** - Dual-model security setup
8. **Simple RAG Service** - Retrieval-augmented generation
9. **Secure RAG Controller** - Orchestrating the security pipeline
10. **Testing and Validation** - Verifying security controls
11. **Conclusion** - Best practices and next steps

## Getting Started

1. **Clone the repository** (if you haven't already)
2. **Navigate to the module** directory: `cd src/module-05-security-guardrails`
3. **Start Redis** server: `docker run -d -p 6379:6379 redis`
4. **Set environment variables**:
   ```bash
   export OPENAI_API_KEY=your_api_key_here
   export OPENAI_MODEL_NAME=gpt-4o
   ```
5. **Build the project**: `mvn clean install`
6. **Run the application**: `mvn spring-boot:run`
7. **Test the endpoint**:
   ```bash
   curl -X POST http://localhost:8085/api/v1/secure/query \
     -H "Content-Type: application/json" \
     -d '{
       "query": "What security features does your product offer?",
       "userId": "user123",
       "userRoles": ["user"],
       "department": "engineering"
     }'
   ```

## Estimated Time

**4-5 hours** to complete the full tutorial with exercises.

## Difficulty Level

**Advanced** - Requires solid Java and Spring Boot knowledge, plus understanding of security concepts.

## Support and Resources

- **Source Code**: `/src/module-05-security-guardrails`
- **Tests**: `/src/test/java/com/techcorp/assistant/module05`
- **Configuration**: `/src/main/resources/application.yml`

## Ready to Begin?

Start with [Chapter 1: Introduction](./01-introduction.md) to understand the security landscape and architecture.

---

**Next**: [Introduction](./01-introduction.md)
