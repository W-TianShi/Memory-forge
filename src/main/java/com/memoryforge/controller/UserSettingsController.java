package com.memoryforge.controller;
import com.memoryforge.entity.UserSettings;
import com.memoryforge.service.UserSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    /**
     * 根据用户ID获取设置
     */
    @GetMapping("/{userId}")
    public UserSettings getByUserId(@PathVariable Long userId) {
        return userSettingsService.getByUserId(userId);
    }

    /**
     * 更新用户保存路径（不存在则创建）
     */
    @PostMapping("/save-path")
    public UserSettings updateSavePath(
            @RequestParam Long userId,
            @RequestParam String savePath) {
        return userSettingsService.saveOrUpdatePath(userId, savePath);
    }
}