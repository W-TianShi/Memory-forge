package com.memoryforge.service;

import com.memoryforge.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    SseEmitter chat(ChatRequest request);
    String getThinkingRules();
}
