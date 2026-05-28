package com.memoryforge.service;

import java.util.List;
import java.util.Map;

public interface WordService {
    Map<String, Object> getWordInfo(String word);
    List<Map<String, Object>> getWordInfoBatch(List<String> words);
}