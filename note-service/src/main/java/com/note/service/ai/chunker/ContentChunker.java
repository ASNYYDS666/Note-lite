package com.note.service.ai.chunker;

import java.util.List;
import java.util.Map;

public interface ContentChunker {
    List<Chunk> chunk(String content, Map<String, Object> metadata);
}
