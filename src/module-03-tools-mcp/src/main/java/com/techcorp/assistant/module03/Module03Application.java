package com.techcorp.assistant.module03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 03: Tools and Model Context Protocol
 *
 * This module demonstrates:
 * - Database tools with @Tool annotations
 * - External API integration (Weather)
 * - MCP-compliant tool registration
 * - Tool orchestration with LLMs
 */
@SpringBootApplication
public class Module03Application {
    public static void main(String[] args) {
        SpringApplication.run(Module03Application.class, args);
    }
}
