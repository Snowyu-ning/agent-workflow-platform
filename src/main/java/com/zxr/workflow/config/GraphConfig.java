package com.zxr.workflow.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.zxr.workflow.node.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class GraphConfig {

    @Bean
    public StateGraph agentWorkflowGraph(IntentClassifyNode intentNode,
                                         SqlAnalyzeNode sqlNode,
                                         RedisAnalyzeNode redisNode,
                                         CodeAnalyzeNode codeNode,
                                         AnswerGenerateNode answerNode) throws Exception {

        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("userInput", new ReplaceStrategy());
            strategies.put("intent", new ReplaceStrategy());
            strategies.put("toolOutput", new ReplaceStrategy());
            strategies.put("finalAnswer", new ReplaceStrategy());
            strategies.put("traceId", new ReplaceStrategy());  // ★ 新增
            return strategies;
        };

        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("intentClassify", node_async(intentNode))
                .addNode("sqlAnalyze", node_async(sqlNode))
                .addNode("redisAnalyze", node_async(redisNode))
                .addNode("codeAnalyze", node_async(codeNode))
                .addNode("answerGenerate", node_async(answerNode))
                .addEdge(StateGraph.START, "intentClassify")
                .addConditionalEdges(
                        "intentClassify",
                        state -> {
                            String intent = (String) state.value("intent").orElse("CODE_ANALYZE");
                            return CompletableFuture.completedFuture(intent.trim());
                        },
                        Map.of(
                                "SQL_ANALYZE", "sqlAnalyze",
                                "REDIS_ANALYZE", "redisAnalyze",
                                "CODE_ANALYZE", "codeAnalyze"
                        )
                )
                .addEdge("sqlAnalyze", "answerGenerate")
                .addEdge("redisAnalyze", "answerGenerate")
                .addEdge("codeAnalyze", "answerGenerate")
                .addEdge("answerGenerate", StateGraph.END);

        return graph;
    }

    @Bean
    public CompiledGraph compiledGraph(StateGraph agentWorkflowGraph) throws Exception {
        return agentWorkflowGraph.compile();
    }
}