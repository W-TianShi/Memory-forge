package com.memoryforge.controller;

import com.memoryforge.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class WordController {

    @Autowired
    private WordService wordService;

    @GetMapping("/api/word/info")
    public Map<String, Object> getWordInfo(@RequestParam String word) {
        return wordService.getWordInfo(word);
    }

    @PostMapping("/api/word/batch")
    public List<Map<String, Object>> getWordInfoBatch(@RequestBody Map<String, String> body) {
        String words = body.get("words");
        List<String> wordList = Arrays.asList(words.split("[,，\\s]+"));
        return wordService.getWordInfoBatch(wordList);
    }
}