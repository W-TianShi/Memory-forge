package com.memoryforge.service;

import com.memoryforge.entity.Word;

import java.util.Map;

public interface WordService {
    Word getByWord(String word);
    String getEnglishPhonetic(String word);
    Map<String, Object> getWordInfo(String word);
}