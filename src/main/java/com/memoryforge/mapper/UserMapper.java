package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);
}
