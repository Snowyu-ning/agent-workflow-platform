package com.zxr.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxr.trace.annotation.AgentTrace;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class SqlAnalyzeNode implements NodeAction {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ToolCallback sqlTool;

    public SqlAnalyzeNode(ToolCallbackProvider provider) {
        this.sqlTool = Arrays.stream(provider.getToolCallbacks())
                .filter(t -> "sql_explain".equals(t.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool 'sql_explain' not found"));
    }

    @Override
    @AgentTrace(node = "sqlAnalyze", type = "TOOL")
    public Map<String, Object> apply(OverAllState state) {
        String userInput = (String) state.value("userInput").orElse("");
        String result = callTool(sqlTool, userInput);
        return Map.of("toolOutput", result);
    }

    /** 把参数包装成 JSON 格式再调用 MCP 工具 */
    private String callTool(ToolCallback tool, String input) {
        try {
            String jsonArgs = objectMapper.writeValueAsString(Map.of("input", input));
            return tool.call(jsonArgs);
        } catch (Exception e) {
            throw new RuntimeException("调用 MCP 工具失败", e);
        }
    }
}