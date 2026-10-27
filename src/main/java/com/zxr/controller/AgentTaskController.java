package com.zxr.controller;

import com.zxr.task.entity.AgentTask;
import com.zxr.task.service.TaskService;
import com.zxr.worker.producer.TaskProducer;
import jakarta.servlet.http.HttpServletRequest;
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
    public Map<String,Object> createTask(@RequestBody Map<String,Object> request, HttpServletRequest httpRequest){
        //1.创建任务记录
        // 服务端从可信上下文获取 userId，忽略前端传的
        Long userId = (Long) httpRequest.getAttribute("userId");
        String inputText = (String) request.get("inputText");

        Long taskId = taskService.createTask(userId, inputText);

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
    public Map<String, Object> getStatus(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        // 1. 从拦截器放进去的上下文中取 userId
        Long userId = (Long) httpRequest.getAttribute("userId");

        AgentTask task = taskService.getTask(taskId);
        Map<String, Object> response = new HashMap<>();

        // 2. 校验：如果任务不存在，或者任务不属于当前用户，拒绝访问
        if (task == null || !task.getUserId().equals(userId)) {
            response.put("error", "Task not found or access denied");
            return response;
        }

        // 3. 通过校验，正常返回状态
        response.put("taskId", task.getId());
        response.put("status", task.getStatus());
        response.put("currentNode", task.getCurrentNode());
        return response;
    }

    /**
     *查询任务结果
     */
    @GetMapping("/task/{taskId}/result")
    public Map<String, Object> getResult(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        // 从请求上下文获取当前登录用户ID
        Long userId = (Long) httpRequest.getAttribute("userId");
        AgentTask task = taskService.getTask(taskId);
        Map<String, Object> response = new HashMap<>();

        // 校验：任务不存在 OR 任务不属于当前用户
        if (task == null || !task.getUserId().equals(userId)) {
            response.put("error", "Task not found or access denied");
            return response;
        }
        response.put("taskId", task.getId());
        response.put("status", task.getStatus());
        response.put("result", task.getResult());
        response.put("errorMessage", task.getErrorMessage());
        return response;
    }
}
