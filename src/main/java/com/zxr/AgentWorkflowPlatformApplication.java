package com.zxr;

import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {ChatClientAutoConfiguration.class})
public class AgentWorkflowPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentWorkflowPlatformApplication.class, args);
    }

}
