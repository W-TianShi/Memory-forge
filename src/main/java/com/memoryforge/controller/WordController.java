package com.memoryforge.controller;

import com.memoryforge.entity.Word;
import com.memoryforge.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin // 解决前后端跨域问题
public class WordController {

    @Autowired
    private WordService wordService;

    // 核心接口：用户输入单词，返回释义
    @GetMapping("/api/word/search")
    public Word searchWord(@RequestParam String word) {

        return wordService.getByWord(word);
    }

    // 单独查询英式音标（只返回音标）
    @GetMapping("/api/word/phonetic")
    public String getPhonetic(@RequestParam String word) {
        return wordService.getEnglishPhonetic(word);
    }
    //查询释义和音标
    @GetMapping("/info")
    public Map<String, Object> getWordInfo(@RequestParam String word) {
        return wordService.getWordInfo(word);
    }


}
