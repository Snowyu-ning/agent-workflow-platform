package com.zxr.workflow.state;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AgentState {
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 用户原始输出
     */
    private String userInput;

    /**
     * 意图分类结果：SQL_ANALYZE / REDIS_ANALYZE / CODE_ANALYZE
     */
    private String intent;

    /**
     * 工具输入
     */
    private String toolInput;

    /**
     * 工具输出
     */
    private String toolOutput;

    /**
     * 结果校验：PASS / FAIL
     */
    private String validationResult;

    /**
     * 全局重试次数
     */
    private Integer retryCount = 0;

    /**
     * 节点级重试计数
     */
    private Map<String,Integer> nodeRetryCount = new HashMap<>();

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 失败节点
     */
    private String failedNode;

    /**
     * 是否需要重试
     */
    private Boolean needRetry = false;

    /**
     * 最终回答
     */
    private String finalAnswer;
}
