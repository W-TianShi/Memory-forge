package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @Select("SELECT * FROM notes WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<Note> findByUserId(Long userId);
}
