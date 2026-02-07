package com.example.monitor.controller;

import com.example.monitor.service.McpChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final McpChatService mcpChatService;

    @PostMapping("/query")
    public ChatResponse query(@RequestBody ChatRequest request) {
        String response = mcpChatService.processQuery(request.getQuery());
        return new ChatResponse(response);
    }

    @Data
    public static class ChatRequest {
        private String query;
    }

    @Data
    public static class ChatResponse {
        private String response;

        public ChatResponse(String response) {
            this.response = response;
        }
    }
}
