package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_api_profile")
public class UserApiProfileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String profileName;
    private String providerKey;
    private String apiKey;
    private String baseUrl;
    private String enabledModels;  // JSON array string
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
