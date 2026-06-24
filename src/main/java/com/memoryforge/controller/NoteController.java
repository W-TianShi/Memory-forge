package com.memoryforge.controller;

import com.memoryforge.entity.Note;
import com.memoryforge.service.NoteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<Note> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return noteService.getUserNotes(userId);
    }

    @GetMapping("/{id}")
    public Note get(@PathVariable Long id) {
        return noteService.getNoteById(id);
    }

    @PostMapping
    public Note create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String title = (String) body.getOrDefault("title", "未命名笔记");
        String content = (String) body.getOrDefault("content", "");
        return noteService.createNote(userId, title, content);
    }

    @PutMapping("/{id}")
    public Note update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String freeBlocks = body.get("freeBlocks") != null ? body.get("freeBlocks").toString() : null;
        return noteService.updateNote(id, title, content, freeBlocks);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        noteService.deleteNote(id);
        return "ok";
    }
}
