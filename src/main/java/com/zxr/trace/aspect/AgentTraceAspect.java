package com.zxr.trace.aspect;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.zxr.task.entity.AgentTraceEntity;
import com.zxr.task.mapper.AgentTraceMapper;
import com.zxr.trace.annotation.AgentTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AgentTraceAspect {

    private final AgentTraceMapper traceMapper;

    public AgentTraceAspect(AgentTraceMapper traceMapper) {
        this.traceMapper = traceMapper;
    }

    @Around("@annotation(agentTrace)")
    public Object around(ProceedingJoinPoint joinPoint, AgentTrace agentTrace) throws Throwable {
        long start = System.currentTimeMillis();

        // 1. 从方法参数中取出 OverAllState
        Object[] args = joinPoint.getArgs();
        String traceId = null;
        String taskId = null;
        for (Object arg : args) {
            if (arg instanceof OverAllState state) {
                traceId = (String) state.value("traceId").orElse(null);
                taskId = (String) state.value("taskId").orElse(null);
                break;
            }
        }

        // 2. 执行目标方法
        Object result = null;
        String status = "SUCCESS";
        String errorMsg = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "FAILED";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 3. 无论成功失败，都记录 Trace
            long duration = System.currentTimeMillis() - start;
            try {
                AgentTraceEntity trace = new AgentTraceEntity();
                trace.setTraceId(traceId);
                trace.setTaskId(taskId != null ? Long.valueOf(taskId) : null);
                trace.setNodeName(agentTrace.node());
                trace.setNodeType(agentTrace.type());
                trace.setStartTime(LocalDateTime.now().minusNanos(duration * 1_000_000));
                trace.setDurationMs(duration);
                trace.setStatus(status);
                trace.setErrorMessage(errorMsg);
                trace.setCreatedAt(LocalDateTime.now());
                traceMapper.insert(trace);
            } catch (Exception ex) {
                // Trace 落库失败不影响主流程
            }
        }
    }
}