package com.memoryforge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memoryforge.entity.User;
import com.memoryforge.mapper.UserMapper;
import com.memoryforge.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}