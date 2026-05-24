package com.techcorp.assistant.module04.agent;

/**
 * Interface for specialized agents in the multi-agent system.
 *
 * Each specialized agent handles a specific domain of queries.
 */
public interface SpecializedAgent {

    /**
     * Processes a request within this agent's domain of expertise.
     *
     * @param request The user's request
     * @return The agent's response
     */
    String process(String request);

    /**
     * Returns the name of this agent.
     *
     * @return Agent name
     */
    String getName();

    /**
     * Returns a description of this agent's capabilities.
     *
     * @return Agent description
     */
    String getDescription();
}
