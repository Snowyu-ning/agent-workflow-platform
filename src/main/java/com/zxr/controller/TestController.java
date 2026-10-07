package com.zxr.agent.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final ChatClient chatClient;

    public TestController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/llm")
    public String testLlm(@RequestParam String input){
        return chatClient.prompt()
                .user(input)
                .call()
                .content();
    }

}
