package com.zxr.evaluation;

import lombok.Data;

@Data
public class EvalCase {
    private Integer caseId;
    private String input;
    private String expectedTool;
}