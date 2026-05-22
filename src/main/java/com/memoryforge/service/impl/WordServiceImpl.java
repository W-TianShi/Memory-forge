package com.memoryforge.service.impl;

import com.memoryforge.entity.Word;
import com.memoryforge.mapper.WordMapper;
import com.memoryforge.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private WordMapper wordMapper;
//只查询释义
    @Override
    public Word getByWord(String word) {
        return wordMapper.selectByWord(word);
    }
//只查询音标
    @Override
    public String getEnglishPhonetic(String word) {
        return wordMapper.selectEnPhoneticByWord(word);
    }
//查询释义和音标
    @Override
    public Map<String, Object> getWordInfo(String word) {
        return wordMapper.selectPhoneticAndDesc(word);
    }
}