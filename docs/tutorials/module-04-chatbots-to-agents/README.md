# From Chatbots to Agents: Building Intelligent AI Systems

![xkcd: The Difference](https://imgs.xkcd.com/comics/the_difference.png)
*When AI systems can think, act, and remember, they transcend simple chatbots.*

## Welcome to Module 04

This comprehensive tutorial guides you through the evolution from basic chatbots to sophisticated AI agents. You'll learn how to build agents that can reason about problems, use tools, maintain conversation memory, collaborate with other agents, and decompose complex tasks.

## What You'll Build

By the end of this tutorial, you'll have created a multi-faceted agent system including:

1. **ReAct Agents** - Agents that iteratively reason and act using the ReAct (Reason + Act) pattern
2. **Stateful Conversation Memory** - Redis-backed persistent conversation history
3. **Multi-Agent Orchestration** - Systems where specialized agents collaborate or route requests
4. **Task Decomposition** - Agents that break down complex tasks into manageable subtasks

## Prerequisites

Before starting this tutorial, you should:

- Complete Modules 01-03 or have equivalent knowledge
- Understand Spring Boot basics (dependency injection, REST controllers)
- Be familiar with LLM fundamentals and LangChain4j
- Have Java 25 installed
- Have Docker or access to PostgreSQL and Redis

## Learning Objectives

After completing this tutorial, you will be able to:

- Implement the ReAct pattern for iterative agent reasoning
- Integrate external tools and APIs into agent workflows
- Build stateful conversation systems with Redis memory
- Create specialized agents for different domains
- Orchestrate multiple agents with routing and collaboration
- Decompose complex tasks into dependency graphs
- Design robust error handling for agent systems

## Module Architecture

```
module-04-chatbots-to-agents/
├── agent/                    # Agent implementations
│   ├── ReActAgent           # Core ReAct pattern implementation
│   ├── SpecializedAgent     # Interface for domain agents
│   ├── CustomerSupportAgent # Customer service specialist
│   ├── TechnicalDocAgent    # Technical documentation expert
│   └── ProductExpertAgent   # Product knowledge specialist
├── orchestrator/            # Multi-agent coordination
│   └── MultiAgentOrchestrator
├── memory/                  # Conversation persistence
│   ├── ConversationMemoryService
│   └── RedisChatMemoryStore
├── service/                 # Task management
│   └── TaskDecomposer
├── tool/                    # External integrations
│   ├── CustomerDataTool
│   └── WeatherTool
└── controller/              # REST API
    └── AgentController
```

## Key Concepts

### ReAct Pattern
The ReAct (Reason + Act) pattern enables agents to solve problems through iterative cycles:
1. **THOUGHT**: Reasoning about what to do next
2. **ACTION**: Executing a tool or making a decision
3. **OBSERVATION**: Receiving and analyzing results
4. Repeat until reaching a final answer

### Stateful Conversation
Unlike stateless chatbots, agents maintain conversation history across interactions, enabling:
- Context-aware responses
- Multi-turn problem solving
- Personalized interactions
- Session continuity

### Multi-Agent Systems
Complex problems often require multiple specialized agents:
- **Routing**: Direct requests to the most appropriate agent
- **Collaboration**: Combine perspectives from multiple agents
- **Specialization**: Each agent focuses on its domain expertise

### Task Decomposition
Breaking complex tasks into subtasks with dependency management:
- Automatic subtask generation
- Topological sorting based on dependencies
- Parallel execution where possible
- Result synthesis

## Tutorial Structure

This tutorial is organized into the following chapters:

1. **Getting Started** - Environment setup and running your first agent
2. **Understanding the ReAct Pattern** - Building agents that reason and act
3. **Integrating External Tools** - Connecting agents to databases and APIs
4. **Implementing Conversation Memory** - Building stateful agents with Redis
5. **Building Specialized Agents** - Creating domain-specific agent implementations
6. **Multi-Agent Orchestration** - Coordinating multiple agents effectively
7. **Complex Task Decomposition** - Breaking down and executing complex workflows
8. **Testing and Production Deployment** - Best practices and deployment strategies

## How to Use This Tutorial

Each chapter includes:
- **Conceptual Overview**: Understanding the "why" before the "how"
- **Architecture Diagrams**: Visual representation of components and flows
- **Code Walkthrough**: Step-by-step explanation of implementations
- **Practice Exercises**: Hands-on challenges to reinforce learning
- **Real-World Examples**: Practical applications and use cases

## Getting Help

If you encounter issues or have questions:

1. Check the practice exercises and solutions in each chapter
2. Review the test files in `src/test/java` for usage examples
3. Consult the LangChain4j documentation
4. Open an issue in the workshop repository

## Technical Stack

- **Spring Boot 4.0** - Application framework
- **LangChain4j** - LLM orchestration and agent framework
- **OpenAI GPT-4o-mini** - Default chat model (workshop standard); upgrade to GPT-4o per `OPENAI_MODEL_NAME`
- **PostgreSQL** - Persistent data storage
- **Redis** - Conversation memory cache
- **Maven** - Build and dependency management

## Time Commitment

Plan for approximately 4-6 hours to complete this tutorial, including:
- Reading and understanding concepts: 1.5 hours
- Implementing practice exercises: 2-3 hours
- Exploration and experimentation: 1-1.5 hours

## Ready to Begin?

Let's start building intelligent agents! Proceed to [Getting Started](chapters/01-getting-started.md) to set up your environment and run your first agent.

---

**Next**: [Chapter 1: Getting Started](chapters/01-getting-started.md)
