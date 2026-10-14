package com.zxr.worker.producer;

import com.zxr.worker.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskProducer {
    private final RabbitTemplate rabbitTemplate;

    public TaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送任务ID到队列
     */
    public void sendTask(Long taskId){
        // 参数1：队列名
        // 参数2：消息内容（这里把 taskId 转成字符串）
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_QUEUE,
                taskId.toString()
        );
    }
}
