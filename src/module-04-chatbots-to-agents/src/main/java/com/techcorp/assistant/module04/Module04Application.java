package com.techcorp.assistant.module04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 04: From Chatbots to Agents
 *
 * This module demonstrates:
 * - ReAct (Reason + Act) agent pattern
 * - Stateful conversation memory with Redis
 * - Multi-agent orchestration with routing
 * - Complex task decomposition
 */
@SpringBootApplication
public class Module04Application {
    public static void main(String[] args) {
        SpringApplication.run(Module04Application.class, args);
    }
}
