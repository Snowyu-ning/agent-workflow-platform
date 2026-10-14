package com.zxr.evaluation;

import com.zxr.task.entity.AgentTask;
import com.zxr.task.service.TaskService;
import com.zxr.worker.producer.TaskProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class EvalRunner {

    private final TaskService taskService;
    private final TaskProducer taskProducer;
    private final AgentEvaluationMapper evalMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 单条任务最长等待时间（毫秒） */
    private static final long TIMEOUT_MS = 30_000;

    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 500;

    private final ChatClient deepSeekChatClient;

    public EvalRunner(TaskService taskService,
                      TaskProducer taskProducer,
                      AgentEvaluationMapper evalMapper,
                      @Qualifier("deepSeekChatClient") ChatClient deepSeekChatClient) {
        this.taskService = taskService;
        this.taskProducer = taskProducer;
        this.evalMapper = evalMapper;
        this.deepSeekChatClient = deepSeekChatClient;
    }

    /** 跑一次完整评测，返回统计报告 */
    public Map<String, Object> runEvaluation() throws Exception {
        String batchId = "batch-" + System.currentTimeMillis();
        List<EvalCase> cases = loadTestCases();

        List<EvalRecord> records = new ArrayList<>();
        for (EvalCase c : cases) {
            EvalRecord record = runOneCase(c);
            records.add(record);

            EvalEntity entity = new EvalEntity();
            entity.setEvalBatchId(batchId);
            entity.setCaseId(record.getCaseId());
            entity.setInputText(record.getInput());
            entity.setExpectedTool(record.getExpectedTool());
            entity.setActualTool(record.getActualTool());
            entity.setStatus(record.getStatus());
            entity.setLatencyMs(record.getLatencyMs());
            entity.setFinalAnswer(record.getFinalAnswer());
            entity.setErrorMessage(record.getErrorMessage());
            entity.setJudgeScore(record.getJudgeScore());
            entity.setJudgeReason(record.getJudgeReason());
            entity.setCreatedAt(LocalDateTime.now());
            evalMapper.insert(entity);
        }

        return buildReport(batchId, records);
    }

    /** 读取 JSON 测试集 */
    private List<EvalCase> loadTestCases() throws Exception {
        ClassPathResource resource = new ClassPathResource("eval/test_cases.json");
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<EvalCase>>() {});
        }
    }

    /** 跑单条用例：走真实异步链路 */
    private EvalRecord runOneCase(EvalCase c) {
        EvalRecord record = new EvalRecord();
        record.setCaseId(c.getCaseId());
        record.setInput(c.getInput());
        record.setExpectedTool(c.getExpectedTool());

        long start = System.currentTimeMillis();

        try {
            // 1. 创建任务（走 TaskService，写MySQL）
            Long taskId = taskService.createTask(0L, c.getInput());

            // 2. 发MQ消息（让 Worker 异步执行）
            taskProducer.sendTask(taskId);

            // 3. 轮询等待任务执行完成
            AgentTask task = null;
            long deadline = start + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                task = taskService.getTask(taskId);
                if (task != null && ("SUCCESS".equals(task.getStatus())
                        || "FAILED".equals(task.getStatus()))) {
                    break;
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }

            long latency = System.currentTimeMillis() - start;
            record.setLatencyMs(latency);

            // 4. 超时判断
            if (task == null || "WAITING".equals(task.getStatus())
                    || "RUNNING".equals(task.getStatus())) {
                record.setStatus("FAIL");
                record.setErrorMessage("任务超时未完成");
                return record;
            }

            // 5. 失败判断
            if ("FAILED".equals(task.getStatus())) {
                record.setActualTool(task.getTaskType());
                record.setStatus("FAIL");
                record.setErrorMessage(task.getErrorMessage());
                return record;
            }

            // 6. 成功：判定 toolOk + hasAnswer
            record.setActualTool(task.getTaskType());
            record.setFinalAnswer(task.getResult());

            boolean toolOk = c.getExpectedTool().equals(task.getTaskType());
            boolean hasAnswer = task.getResult() != null && !task.getResult().isEmpty();

            // ★ 新增：调用 LLM-as-Judge 打分
            if (toolOk && hasAnswer) {
                JudgeResult judgeResult = judge(c.getInput(), task.getResult());
                record.setJudgeScore(judgeResult.score);
                record.setJudgeReason(judgeResult.reason);
            }

            // 判定：分支对 + 结果非空 + Judge 分数 ≥ 7
            boolean judgeOk = record.getJudgeScore() != null && record.getJudgeScore() >= 7;

            if (toolOk && hasAnswer && judgeOk) {
                record.setStatus("PASS");
            } else {
                record.setStatus("FAIL");
                record.setErrorMessage(
                        "toolOk=" + toolOk + ", hasAnswer=" + hasAnswer + ", judgeScore=" + record.getJudgeScore()
                );
            }

            return record;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            record.setLatencyMs(latency);
            record.setStatus("FAIL");
            record.setErrorMessage(e.getMessage());
            return record;
        }
    }

    /** 生成统计报告 */
    private Map<String, Object> buildReport(String batchId, List<EvalRecord> records) {
        int total = records.size();
        int success = 0;
        int toolCorrect = 0;
        long totalLatency = 0;
        int judgeTotal = 0;
        int judgeCount = 0;

        for (EvalRecord r : records) {
            if ("PASS".equals(r.getStatus())) success++;
            if (r.getExpectedTool().equals(r.getActualTool())) toolCorrect++;
            if (r.getLatencyMs() != null) totalLatency += r.getLatencyMs();
            // ★ 新增
            if (r.getJudgeScore() != null && r.getJudgeScore() > 0) {
                judgeTotal += r.getJudgeScore();
                judgeCount++;
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("batchId", batchId);
        report.put("total", total);
        report.put("success", success);
        report.put("successRate", String.format("%.2f%%", 100.0 * success / total));
        report.put("toolAccuracy", String.format("%.2f%%", 100.0 * toolCorrect / total));
        report.put("avgEndToEndLatencyMs", totalLatency / total);  // ← 改成端到端
        report.put("avgJudgeScore", judgeCount > 0
                ? String.format("%.2f", 1.0 * judgeTotal / judgeCount)
                : "N/A");
        return report;
    }

    /**
     * 用 LLM 当裁判，给 Agent 的回答打分
     * @return int[2]：第一个是分数，第二个是 1 表示成功，0 表示解析失败
     */
    private JudgeResult judge(String userInput, String agentAnswer) {
        String prompt = """
            你是一名严格的技术评审员。请评估下面这个 AI 回答的质量。
            
            【用户问题】
            %s
            
            【AI回答】
            %s
            
            【评分标准】
            1-3分：答非所问，或只有空洞的套话
            4-6分：回答了问题，但分析不够深入，缺少具体建议
            7-9分：分析准确，给出了具体可执行的建议
            10分：分析精准，建议全面，覆盖多个维度
            
            【输出格式】
            第一行：只输出一个数字（1-10）
            第二行：一句话说明理由
            
            示例：
            8
            回答准确指出了索引问题，并给出了可执行的优化建议。
            """.formatted(userInput, agentAnswer);

        try {
            String response = deepSeekChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            // 解析：第一行是分数，第二行是理由
            String[] lines = response.split("\n", 2);
            int score = Integer.parseInt(lines[0].trim().replaceAll("[^0-9]", ""));
            String reason = lines.length > 1 ? lines[1].trim() : "";

            return new JudgeResult(score, reason);
        } catch (Exception e) {
            return new JudgeResult(0, "解析失败：" + e.getMessage());
        }
    }

    /** 内部类：Judge 结果 */
    private static class JudgeResult {
        int score;
        String reason;
        JudgeResult(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }
}