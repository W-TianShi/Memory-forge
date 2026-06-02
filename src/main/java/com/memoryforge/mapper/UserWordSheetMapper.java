package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.UserWordSheet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserWordSheetMapper extends BaseMapper<UserWordSheet> {
    @Select("SELECT * FROM user_word_sheets WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<UserWordSheet> findByUserId(Long userId);
}
