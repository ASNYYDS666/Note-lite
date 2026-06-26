package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model")
public class AiModelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerKey;
    private String modelName;
    private String modelType;
    private Integer isDefault;

    private LocalDateTime createdAt;
}
