package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.Word;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface WordMapper extends BaseMapper<Word> {

    // 根据单词查询（完全匹配 tb_words 表）
    @Select("SELECT * FROM tb_words WHERE `word` = #{word} LIMIT 1")
    Word selectByWord(@Param("word") String word);

    // 新增：只查英式音标（单独返回）
    @Select("SELECT en_pronunciation FROM tb_words WHERE `word` = #{word} LIMIT 1")
    String selectEnPhoneticByWord(@Param("word") String word);

    @Select("SELECT en_pronunciation, `desc` FROM tb_words WHERE `word` = #{word} LIMIT 1")
    Map<String, Object> selectPhoneticAndDesc(@Param("word") String word);
}