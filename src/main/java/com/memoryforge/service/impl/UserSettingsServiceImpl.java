package com.memoryforge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memoryforge.entity.UserSettings;
import com.memoryforge.mapper.UserSettingsMapper;
import com.memoryforge.service.UserSettingsService;
import org.springframework.stereotype.Service;


@Service
public class UserSettingsServiceImpl extends ServiceImpl<UserSettingsMapper, UserSettings>
        implements UserSettingsService {

    @Override
    public UserSettings getByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<UserSettings>()
                .eq(UserSettings::getUserId, userId));
    }

    @Override
    public UserSettings saveOrUpdatePath(Long userId, String savePath) {
        UserSettings settings = this.getByUserId(userId);
        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
        }
        settings.setSavePath(savePath);
        this.saveOrUpdate(settings);
        return settings;
    }
}