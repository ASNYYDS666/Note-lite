package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider")
public class AiProviderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String providerKey;
    private String baseUrl;
    private String apiKey;
    private String pluginType;
    private Integer isEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
