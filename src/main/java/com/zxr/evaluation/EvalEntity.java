package com.zxr.evaluation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_evaluation")
public class EvalEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String evalBatchId;

    private Integer caseId;

    private String inputText;

    private String expectedTool;

    private String actualTool;

    private String status;

    private Integer judgeScore;

    private String judgeReason;

    private Long latencyMs;

    private String finalAnswer;

    private String errorMessage;

    private LocalDateTime createdAt;
}