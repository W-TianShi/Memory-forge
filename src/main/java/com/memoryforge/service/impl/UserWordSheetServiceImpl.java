package com.memoryforge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memoryforge.entity.UserWordSheet;
import com.memoryforge.mapper.UserWordSheetMapper;
import com.memoryforge.service.UserWordSheetService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserWordSheetServiceImpl extends ServiceImpl<UserWordSheetMapper, UserWordSheet> implements UserWordSheetService {
    @Override
    public List<UserWordSheet> getUserSheets(Long userId) {
        return getBaseMapper().findByUserId(userId);
    }
}
