package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.Word;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WordMapper extends BaseMapper<Word> {

    @Select("SELECT * FROM tb_words WHERE `word` = #{word} LIMIT 1")
    Word selectByWord(@Param("word") String word);

    @Select("<script>" +
            "SELECT * FROM tb_words WHERE `word` IN " +
            "<foreach collection='words' item='w' open='(' separator=',' close=')'>#{w}</foreach>" +
            "</script>")
    List<Word> selectByWords(@Param("words") List<String> words);
}