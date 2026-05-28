package com.memoryforge.controller;

import com.memoryforge.dto.ChatRequest;
import com.memoryforge.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        return aiService.chat(request);
    }

    @GetMapping("/thinking-rules")
    public String getThinkingRules() {
        return aiService.getThinkingRules();
    }
}
