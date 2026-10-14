package com.zxr.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /** 主力模型：DeepSeek，用于所有关键任务 */
    @Bean
    public ChatClient deepSeekChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    /** 本地模型：Ollama qwen3:4b，用于 Judge 评分 */
    @Bean
    public ChatClient localChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

//    /**
//     * 验证工具注册成功
//     */
//    @Bean
//    public CommandLineRunner listMcpTools(ToolCallbackProvider provider) {
//        return args -> {
//            System.out.println("===== MCP Client registered tools =====");
//            for (var cb : provider.getToolCallbacks()) {
//                System.out.println("  - " + cb.getToolDefinition().name());
//            }
//            System.out.println("=======================================");
//        };
//    }
}