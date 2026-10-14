package com.zxr.task.service;

import com.zxr.task.entity.AgentTask;
import com.zxr.task.mapper.AgentTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final AgentTaskMapper taskMapper;

    public TaskService(AgentTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 创建任务：返回任务ID
     */
    public Long createTask(Long userId,String inputText){
        AgentTask task = new AgentTask();
        task.setUserId(userId);
        task.setInputText(inputText);
        task.setStatus("WAITING");
        task.setRetryCount(0);
        task.setMaxRetry(3);
        taskMapper.insert(task);
        return task.getId();//mp插入后自动回填
    }

    /**
     * 根据任务ID查询任务
     */
    public AgentTask getTask(Long taskId){
        return taskMapper.selectById(taskId);
    }

    /**
     * 把任务标记为运行中
     */
    public void markRunning(Long taskId,String currentNode){
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setStatus("RUNNING");
        task.setCurrentNode(currentNode);
        taskMapper.updateById(task);
    }

    /**
     * 把任务标记为成功，并保存结果
     */
    public void markSuccess(Long taskId,String result){
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setStatus("SUCCESS");
        task.setResult(result);
        taskMapper.updateById(task);
    }

    /**
     * 把任务标记为失败，保存错误信息
     */
    public void markFailed(Long taskId,String errorMsg){
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setStatus("FAILED");
        task.setErrorMessage(errorMsg);
        taskMapper.updateById(task);
    }

    /** 标记成功，同时保存任务类型（评测用） */
    public void markSuccessWithType(Long taskId, String result, String taskType) {
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setStatus("SUCCESS");
        task.setResult(result);
        task.setTaskType(taskType);   // ← 关键：把 intent 存进 task_type
        taskMapper.updateById(task);
    }
}
