package com.memoryforge.service.impl;

import com.memoryforge.entity.Word;
import com.memoryforge.mapper.WordMapper;
import com.memoryforge.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private WordMapper wordMapper;

    @Override
    @Cacheable(value = "wordInfo", key = "#word.toLowerCase()")
    public Map<String, Object> getWordInfo(String word) {
        Word w = wordMapper.selectByWord(word);
        Map<String, Object> result = new HashMap<>();
        if (w != null) {
            result.put("word", w.getWord());
            result.put("en_pronunciation", w.getEnPronunciation());
            result.put("us_pronunciation", w.getUsPronunciation());
            result.put("desc", w.getDesc());
        }
        return result;
    }

    @Override
    @Cacheable(value = "wordBatch", key = "#words.hashCode()")
    public List<Map<String, Object>> getWordInfoBatch(List<String> words) {
        List<Word> list = wordMapper.selectByWords(words);
        Map<String, Word> map = list.stream()
                .collect(Collectors.toMap(w -> w.getWord().toLowerCase(), w -> w, (a, b) -> a));
        return words.stream().map(w -> {
            Map<String, Object> result = new HashMap<>();
            Word found = map.get(w.toLowerCase().trim());
            if (found != null) {
                result.put("word", found.getWord());
                result.put("en_pronunciation", found.getEnPronunciation());
                result.put("us_pronunciation", found.getUsPronunciation());
                result.put("desc", found.getDesc());
            }
            return result;
        }).collect(Collectors.toList());
    }
}