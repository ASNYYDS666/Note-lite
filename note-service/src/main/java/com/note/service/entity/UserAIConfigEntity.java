package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ai_config")
public class UserAIConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String chatProvider;
    private String chatModel;
    private String chatUrl;
    private String embedProvider;
    private String embedModel;
    private String embedUrl;
    private Integer isEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
