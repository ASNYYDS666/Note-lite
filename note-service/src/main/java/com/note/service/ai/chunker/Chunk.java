package com.note.service.ai.chunker;

import lombok.Data;

import java.util.Map;

@Data
public class Chunk {
    private int index;
    private String text;
    private Map<String, Object> metadata;
}
