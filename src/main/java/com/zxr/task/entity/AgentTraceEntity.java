package com.zxr.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_trace")
public class AgentTraceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long taskId;

    private String nodeName;

    private String nodeType;

    private LocalDateTime startTime;

    private Long durationMs;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;
}
