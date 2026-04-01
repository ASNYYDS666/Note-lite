package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share")
public class ShareEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;
    private String code;
    private String permission; // READ / WRITE
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
