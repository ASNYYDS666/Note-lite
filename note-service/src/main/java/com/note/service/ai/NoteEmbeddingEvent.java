package com.note.service.ai;

import lombok.Getter;

@Getter
public class NoteEmbeddingEvent {

    private final Long noteId;
    private final EventType type;

    public NoteEmbeddingEvent(Long noteId, EventType type) {
        this.noteId = noteId;
        this.type = type;
    }

    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}
