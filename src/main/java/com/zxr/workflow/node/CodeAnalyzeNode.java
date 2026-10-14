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
public class CodeAnalyzeNode implements NodeAction {

    private final ToolCallback codeTool;

    public CodeAnalyzeNode(ToolCallbackProvider provider) {
        this.codeTool = Arrays.stream(provider.getToolCallbacks())
                .filter(t -> "code_analyze".equals(t.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool 'code_analyze' not found"));
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @AgentTrace(node = "codeAnalyze", type = "TOOL")
    public Map<String, Object> apply(OverAllState state) {
        String userInput = (String) state.value("userInput").orElse("");
        String result = callTool(codeTool, userInput);
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