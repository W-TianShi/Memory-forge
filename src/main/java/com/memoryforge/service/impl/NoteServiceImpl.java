package com.memoryforge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memoryforge.entity.Note;
import com.memoryforge.mapper.NoteMapper;
import com.memoryforge.service.NoteService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Override
    public List<Note> getUserNotes(Long userId) {
        return getBaseMapper().findByUserId(userId);
    }

    @Override
    public Note getNoteById(Long id) {
        return getById(id);
    }

    @Override
    public Note createNote(Long userId, String title, String content) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content != null ? content : "");
        note.setFreeBlocks("[]");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        save(note);
        return note;
    }

    @Override
    public Note updateNote(Long id, String title, String content, String freeBlocks) {
        Note note = getById(id);
        if (note == null) return null;
        if (title != null) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (freeBlocks != null) note.setFreeBlocks(freeBlocks);
        note.setUpdatedAt(LocalDateTime.now());
        updateById(note);
        return note;
    }

    @Override
    public void deleteNote(Long id) {
        removeById(id);
    }
}
