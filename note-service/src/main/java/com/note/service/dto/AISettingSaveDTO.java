package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 配置保存请求")
public class AISettingSaveDTO {

    // ========== Chat 配置 ==========

    @NotBlank(message = "Chat 服务商不能为空")
    @Schema(description = "Chat 服务商", example = "deepseek")
    private String chatProvider;

    @NotBlank(message = "Chat API Key 不能为空")
    @Size(max = 255, message = "API Key 最长255字符")
    @Schema(description = "Chat API Key")
    private String chatApiKey;

    @Size(max = 50, message = "模型名最长50字符")
    @Schema(description = "Chat 模型", example = "deepseek-chat")
    private String chatModel;

    @Schema(description = "Chat 自定义 API 地址（中转站用）", example = "https://api.openai.com/v1")
    private String chatUrl;

    // ========== Embedding 配置 ==========

    @Schema(description = "Embedding 服务商（可选）", example = "openai")
    private String embedProvider;

    @Size(max = 255, message = "API Key 最长255字符")
    @Schema(description = "Embedding API Key（可选）")
    private String embedApiKey;

    @Size(max = 50, message = "模型名最长50字符")
    @Schema(description = "Embedding 模型", example = "text-embedding-3-small")
    private String embedModel;

    @Schema(description = "Embedding 自定义 API 地址（中转站用）", example = "https://api.openai.com/v1")
    private String embedUrl;
}
