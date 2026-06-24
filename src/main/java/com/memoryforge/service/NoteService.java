package com.memoryforge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memoryforge.entity.Note;
import java.util.List;

public interface NoteService extends IService<Note> {
    List<Note> getUserNotes(Long userId);
    Note getNoteById(Long id);
    Note createNote(Long userId, String title, String content);
    Note updateNote(Long id, String title, String content, String freeBlocks);
    void deleteNote(Long id);
}
