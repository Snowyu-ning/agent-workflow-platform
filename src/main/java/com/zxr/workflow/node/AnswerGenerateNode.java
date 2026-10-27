package com.zxr.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.zxr.trace.annotation.AgentTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;

import java.util.Map;

@Component
@Slf4j
public class AnswerGenerateNode implements NodeAction {

    private final ChatClient deepSeekChatClient;

    public AnswerGenerateNode(@Qualifier("deepSeekChatClient")ChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @Override
    @AgentTrace(node = "answerGenerate", type = "LLM")
    public Map<String, Object> apply(OverAllState state) {
        String toolOutput = (String) state.value("toolOutput").orElse("");
        String userInput = (String) state.value("userInput").orElse("");

        String prompt = """
        你是一名资深 Java 后端工程师。
        用户的问题是：%s

        工具分析结果如下：
        %s

        请按以下要求回答：
        1. 如果工具输出中包含有效的分析内容，请基于它生成回答，包含：问题总结、原因分析、优化建议、预期收益。
        2. 如果工具输出是错误信息（比如"Key不存在"、"SQL执行失败"），请：
           - 如实说明失败原因
           - 基于用户输入本身，尝试给出你能提供的分析或建议
           - 如果确实无法分析，直接说明"当前信息不足，无法给出具体分析"，不要编造数据或结论
        3. ★ 如果工具输出中包含 EXPLAIN 的执行计划数据，请明确说明"根据 EXPLAIN 预估"，
           不要把预估行数说成"实际扫描行数"。预估不等于实际。
        4. 不要输出技术细节的原始数据，用人类能听懂的话说。
        """.formatted(userInput, toolOutput);

        ChatResponse response = deepSeekChatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();

        // 获取当前的 taskId（前面 Worker 存进 State 的）
        String taskId = (String) state.value("taskId").orElse("unknown");

        Usage usage = response.getMetadata().getUsage();
        if (usage != null) {
            log.info("Token消耗统计】taskId={}, 输入: {}, 输出: {}, 总计: {}",
                    taskId,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }  else {
            log.info("【Token消耗统计】taskId={}, 当前模型未返回 Usage 数据", taskId);
        }

        return Map.of("finalAnswer", answer);
    }
}