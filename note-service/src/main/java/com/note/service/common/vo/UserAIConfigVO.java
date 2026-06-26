package com.note.service.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AI 配置响应")
public class UserAIConfigVO {

    // Chat 配置
    @Schema(description = "Chat 服务商", example = "deepseek")
    private String chatProvider;

    @Schema(description = "Chat 模型", example = "deepseek-chat")
    private String chatModel;

    @Schema(description = "Chat 自定义 API 地址")
    private String chatUrl;

    // Embedding 配置
    @Schema(description = "Embedding 服务商", example = "openai")
    private String embedProvider;

    @Schema(description = "Embedding 模型", example = "text-embedding-3-small")
    private String embedModel;

    @Schema(description = "Embedding 自定义 API 地址")
    private String embedUrl;

    @Schema(description = "是否启用")
    private Integer isEnabled;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
