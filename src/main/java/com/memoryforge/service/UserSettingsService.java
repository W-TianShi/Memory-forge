package com.memoryforge.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.memoryforge.entity.UserSettings;

public interface UserSettingsService extends IService<UserSettings> {

    /**
     * 根据用户ID获取设置
     * @param userId 用户ID
     * @return 用户设置
     */
    UserSettings getByUserId(Long userId);

    /**
     * 保存或更新用户设置的保存路径
     * @param userId 用户ID
     * @param savePath 保存路径
     * @return 更新后的设置
     */
    UserSettings saveOrUpdatePath(Long userId, String savePath);
}