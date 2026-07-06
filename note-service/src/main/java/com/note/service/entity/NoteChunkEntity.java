package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note_chunks")
public class NoteChunkEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;
    private Long userId;
    private Integer chunkIndex;
    private String chunkText;
    private String chunkId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
