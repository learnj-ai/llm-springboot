package com.techcorp.assistant.module03.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Real MCP (Model Context Protocol) client wiring.
 *
 * <p>Unlike the {@code @Tool}-annotated database/weather helpers in this module
 * (which are loaded directly into the LLM's tool list at JVM-startup time and
 * speak no protocol), this client connects to a separate <b>MCP server process</b>
 * over JSON-RPC and discovers its tools at runtime via {@code tools/list}. That's
 * what the Model Context Protocol actually is: a protocol for an LLM host to
 * negotiate with an external server about which tools, resources, and prompts
 * are available, and to invoke them through a structured channel.
 *
 * <p>The default configuration here launches the official filesystem MCP server
 * via {@code npx} and gives it a single root directory. To enable, set
 * {@code mcp.filesystem.enabled=true} and {@code mcp.filesystem.root=/some/path}.
 * Disabled by default so the workshop's existing chapters still run on a machine
 * without Node.js installed.
 *
 * <p>See {@code docs/tutorials/module-03-tools-mcp/09-real-mcp.md} for the full
 * walkthrough.
 */
@Configuration
@ConditionalOnProperty(name = "mcp.filesystem.enabled", havingValue = "true")
public class FilesystemMcpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpClientConfig.class);

    @Value("${mcp.filesystem.root:${user.home}/mcp-demo}")
    private String filesystemRoot;

    @Bean(destroyMethod = "close")
    public McpClient filesystemMcpClient() {
        log.info("Starting filesystem MCP server via stdio, root={}", filesystemRoot);

        // The official `@modelcontextprotocol/server-filesystem` server runs over stdio:
        // we spawn it as a child process and talk JSON-RPC across its stdin/stdout.
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("npx", "-y", "@modelcontextprotocol/server-filesystem", filesystemRoot))
                .logEvents(true)
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .clientName("module-03-tools-mcp")
                .clientVersion("1.0.0")
                .initializationTimeout(Duration.ofSeconds(20))
                .toolExecutionTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Adapts the MCP client into LangChain4J's {@link ToolProvider} so its tools
     * can be plugged into an {@code AiServices} builder alongside (or instead of)
     * the local {@code @Tool}-annotated services.
     */
    @Bean
    public ToolProvider mcpToolProvider(McpClient filesystemMcpClient) {
        return McpToolProvider.builder()
                .mcpClients(filesystemMcpClient)
                .build();
    }
}
