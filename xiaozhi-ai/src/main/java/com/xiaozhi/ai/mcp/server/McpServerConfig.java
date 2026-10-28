package com.xiaozhi.ai.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把 {@link McpServerTools} 注册成 MCP 工具；SSE 端点由 spring-ai-starter-mcp-server-webmvc 提供。
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider xiaozhiMcpTools(McpServerTools tools,
                                                com.xiaozhi.ai.homeassistant.HomeAssistantTools haTools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools, haTools).build();
    }
}