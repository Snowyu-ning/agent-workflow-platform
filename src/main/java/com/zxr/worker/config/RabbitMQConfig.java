package com.zxr.worker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /** 队列名称，发送方和接收方都用这个常量 */
    public static final String TASK_QUEUE = "agent.task.queue";

    @Bean
    public Queue taskQueue() {
        // 参数 true 表示队列要持久化（RabbitMQ 重启后队列不丢）
        return new Queue(TASK_QUEUE, true);
    }
}