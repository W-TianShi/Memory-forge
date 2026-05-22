package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}