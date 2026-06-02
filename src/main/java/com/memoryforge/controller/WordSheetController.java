package com.memoryforge.controller;

import com.memoryforge.entity.UserWordSheet;
import com.memoryforge.service.UserWordSheetService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/word-sheets")
public class WordSheetController {

    private final UserWordSheetService sheetService;

    public WordSheetController(UserWordSheetService sheetService) {
        this.sheetService = sheetService;
    }

    @GetMapping
    public List<UserWordSheet> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return sheetService.getUserSheets(userId);
    }

    @GetMapping("/{id}")
    public UserWordSheet get(@PathVariable Long id) {
        return sheetService.getById(id);
    }

    @PostMapping
    public UserWordSheet save(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        UserWordSheet sheet = id != null ? sheetService.getById(id) : new UserWordSheet();
        sheet.setUserId(userId);
        sheet.setTitle((String) body.getOrDefault("title", "未命名"));
        sheet.setColCount(Integer.valueOf(body.getOrDefault("colCount", 2).toString()));
        sheet.setData((String) body.get("data"));
        sheet.setUpdatedAt(LocalDateTime.now());
        if (id == null) { sheet.setCreatedAt(LocalDateTime.now()); sheetService.save(sheet); }
        else { sheetService.updateById(sheet); }
        return sheet;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        sheetService.removeById(id);
        return "ok";
    }
}
