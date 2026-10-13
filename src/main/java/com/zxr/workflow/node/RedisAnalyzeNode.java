package com.zxr.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

public class SqlAnalyzeNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userInput = (String)state.value("userInput").orElse("");
        //先模拟，后续替换为真实的SQL分析逻辑
        String result =  "【SQL分析节点】收到输入：" + userInput + "，正在分析执行计划...";
        return Map.of("toolOutput",result);
    }
}
