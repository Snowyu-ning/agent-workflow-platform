package com.zxr.controller;

import com.zxr.task.entity.AgentTask;
import com.zxr.task.service.TaskService;
import com.zxr.worker.producer.TaskProducer;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentTaskController {
    private final TaskService taskService;
    private final TaskProducer taskProducer;

    public AgentTaskController(TaskService taskService, TaskProducer taskProducer) {
        this.taskService = taskService;
        this.taskProducer = taskProducer;
    }

    /**
     * 提交任务：立即返回taskId
     */
    @PostMapping("/task")
    public Map<String,Object> createTask(@RequestBody Map<String,Object> request){
        Long userId = Long.valueOf(request.get("userId").toString());
        String inputText = (String)request.get("inputText");

        //1.创建任务记录
        Long taskId = taskService.createTask(userId,inputText);

        //2.发送MQ信息，通知Worker执行
        taskProducer.sendTask(taskId);

        //3.立即返回，不等执行结果
        Map<String, Object> response = new HashMap<>();
        response.put("taskId",taskId);
        response.put("status","WAITING");
        return response;
    }

    /**
     *查询任务状态
     */
    @GetMapping("/task/{taskId}/status")
    public Map<String, Object> getStatus(@PathVariable Long taskId) {
        AgentTask task = taskService.getTask(taskId);
        Map<String, Object> response = new HashMap<>();
        if (task == null) {
            response.put("error", "task not found");
            return response;
        }
        response.put("taskId", task.getId());
        response.put("status", task.getStatus());
        response.put("currentNode", task.getCurrentNode());
        return response;
    }

    /**
     *查询任务结果
     */
    @GetMapping("/task/{taskId}/result")
    public Map<String, Object> getResult(@PathVariable Long taskId) {
        AgentTask task = taskService.getTask(taskId);
        Map<String, Object> response = new HashMap<>();
        if (task == null) {
            response.put("error", "task not found");
            return response;
        }
        response.put("taskId", task.getId());
        response.put("status", task.getStatus());
        response.put("result", task.getResult());
        response.put("errorMessage", task.getErrorMessage());
        return response;
    }
}
