package com.zxr.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.zxr.trace.annotation.AgentTrace;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Map;

@Component
public class RedisAnalyzeNode implements NodeAction {

    private final ToolCallback redisTool;

    public RedisAnalyzeNode(ToolCallbackProvider provider) {
        this.redisTool = Arrays.stream(provider.getToolCallbacks())
                .filter(t -> "redis_analyze".equals(t.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool 'redis_analyze' not found"));
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @AgentTrace(node = "redisAnalyze", type = "TOOL")
    public Map<String, Object> apply(OverAllState state) {
        String userInput = (String) state.value("userInput").orElse("");
        String result = callTool(redisTool, userInput);
        return Map.of("toolOutput", result);
    }

    private String callTool(ToolCallback tool, String input) {
        try {
            String jsonArgs = objectMapper.writeValueAsString(Map.of("input", input));
            return tool.call(jsonArgs);
        } catch (Exception e) {
            throw new RuntimeException("调用 MCP 工具失败", e);
        }
    }
}