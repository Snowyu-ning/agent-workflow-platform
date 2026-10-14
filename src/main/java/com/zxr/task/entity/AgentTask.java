package com.zxr.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_task")
public class AgentTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String taskType;

    private String inputText;

    private String status;

    private String currentNode;

    private Integer retryCount;

    private Integer maxRetry;

    private String result;

    private String errorMessage;

    private String traceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}