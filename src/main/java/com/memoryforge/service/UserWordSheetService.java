package com.memoryforge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memoryforge.entity.UserWordSheet;
import java.util.List;

public interface UserWordSheetService extends IService<UserWordSheet> {
    List<UserWordSheet> getUserSheets(Long userId);
}
