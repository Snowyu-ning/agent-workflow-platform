package com.zxr.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final ChatClient deepSeekChatClient;

    public TestController(@Qualifier("deepSeekChatClient") ChatClient deepSeekChatClient){
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @GetMapping("/llm")
    public String testLlm(@RequestParam String input){
        return deepSeekChatClient.prompt()
                .user(input)
                .call()
                .content();
    }

}
