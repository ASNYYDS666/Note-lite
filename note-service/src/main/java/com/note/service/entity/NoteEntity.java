package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

@Data
@TableName("note")
public class NoteEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String summary;

    private Integer isDeleted;

    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // 非数据库字段，用于接收 GROUP_CONCAT 结果
    @TableField(exist = false)
    private String tagNames;

    @TableField(exist = false)
    private List<String> tags;

    // 解析 tagNames 到 tags 列表的方法
    public void parseTagNames() {
        if (this.tagNames != null && !this.tagNames.isEmpty()) {
            this.tags = Arrays.asList(this.tagNames.split(","));
        } else {
            this.tags = new ArrayList<>();
        }
    }
}
