package com.zxr.evaluation;

import lombok.Data;

@Data
public class EvalRecord {
    private Integer caseId;
    private String input;
    private String expectedTool;
    private String actualTool;
    private String status;      // PASS / FAIL
    private Long latencyMs;
    private String finalAnswer;
    private String errorMessage;
    private Integer judgeScore;//1-10(7pass)
    private String judgeReason;
}