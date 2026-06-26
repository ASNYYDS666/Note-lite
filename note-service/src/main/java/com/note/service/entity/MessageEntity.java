package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class MessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String questionId;
    private String role;
    private String content;
    private String status;
    private String sources;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
