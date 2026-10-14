package com.zxr.worker.consumer;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.zxr.task.entity.AgentTask;
import com.zxr.task.service.TaskService;
import com.zxr.worker.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AgentWorker {
    private final TaskService taskService;
    private final CompiledGraph compiledGraph;

    public AgentWorker(TaskService taskService,CompiledGraph compiledGraph) throws Exception{
        this.taskService = taskService;
        this.compiledGraph = compiledGraph;
    }

    /**
     * 监听队列，一旦有消息就执行
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void handleTask(String taskIdStr){
        Long taskId = Long.valueOf(taskIdStr);

        try {
            //1.查询任务详情
            AgentTask task = taskService.getTask(taskId);
            if(task == null){
                return;
            }

            //2.标记任务为运行中
            taskService.markRunning(taskId,"intentClassify");

            // 3. 执行 Graph
            Optional<OverAllState> result = compiledGraph.invoke(
                    Map.of(
                            "userInput", task.getInputText(),
                            "traceId", "trace-" + taskId + "-" + System.currentTimeMillis(),
                            "taskId", taskId.toString()
                    )
            );

            // 4. 取出意图和最终回答
            String intent = result.map(state -> (String) state.value("intent").orElse("")).orElse("");
            String finalAnswer = result.map(state -> (String) state.value("finalAnswer").orElse("")).orElse("");
            // 5. 保存结果（把 intent 也写进去）
            taskService.markSuccessWithType(taskId, finalAnswer, intent);

        } catch (Exception e) {
            //6.万一失败，保存错误信息
            taskService.markFailed(taskId,e.getMessage());
        }
    }
}
