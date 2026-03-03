package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note_tag")
public class NoteTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;
    private String tagName;
    private LocalDateTime createdAt;
}
