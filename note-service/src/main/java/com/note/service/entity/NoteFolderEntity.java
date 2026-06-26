package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note_folder")
public class NoteFolderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long parentId;
    private String name;
    private Integer sortOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
