package com.memoryforge.dto;

import java.util.List;

public class ChatRequest {

    private Long problemId;
    private String problemContent;
    private String message;
    private String mode; // "full" or "step"
    private List<ChatMessage> history;

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public String getProblemContent() { return problemContent; }
    public void setProblemContent(String problemContent) { this.problemContent = problemContent; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }
}
