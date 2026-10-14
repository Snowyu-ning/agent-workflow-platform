package com.zxr.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.zxr.trace.annotation.AgentTrace;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IntentClassifyNode implements NodeAction {

    private final ChatClient deepSeekChatClient;

    public IntentClassifyNode(@Qualifier("deepSeekChatClient")ChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @Override
    @AgentTrace(node = "intentClassify", type = "ROUTER")
    public Map<String, Object> apply(OverAllState state) {
        String userInput = (String) state.value("userInput").orElse("");

        String prompt = """
                你是一个意图分类器。请判断用户输入的意图类型，只输出以下三种之一：
                
                - SQL_ANALYZE：用户输入包含 SQL 语句（如 SELECT、UPDATE、EXPLAIN 等关键词），
                  或明确要求分析 SQL 性能
                - REDIS_ANALYZE：用户输入包含 Redis Key 特征（形如 xxx:xxx、xxx:xxx:xxx 的冒号分隔格式），
                  或明确要求分析 Redis 缓存
                - CODE_ANALYZE：用户输入是代码片段、方法名、类名，或明确要求分析代码
                
                判断原则：
                1. 优先看用户输入的关键词（如"Redis"、"SQL"、"代码"）
                2. 如果输入形如 aaa:bbb 的冒号分隔字符串，优先判断为 REDIS_ANALYZE
                3. 如果输入是一条完整的 SQL 语句，判断为 SQL_ANALYZE
                
                用户输入：%s
                
                只输出意图类型，不要输出其他内容。
                """.formatted(userInput);

        String intent = deepSeekChatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim();

        return Map.of("intent", intent);
    }
}