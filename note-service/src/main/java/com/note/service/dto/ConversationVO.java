package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "对话摘要")
public class ConversationVO {

    @Schema(description = "对话ID")
    private Long id;

    @Schema(description = "对话标题")
    private String title;

    @Schema(description = "消息数")
    private int messageCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
