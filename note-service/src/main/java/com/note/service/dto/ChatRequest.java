package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 对话请求")
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题最长2000字符")
    @Schema(description = "用户问题", example = "总结这篇笔记的要点")
    private String question;

    @Pattern(regexp = "^(NOTE|FOLDER|ALL)$", message = "范围类型只能为 NOTE/FOLDER/ALL")
    @Schema(description = "检索范围类型", example = "NOTE", allowableValues = {"NOTE", "FOLDER", "ALL"})
    private String scopeType;

    @Schema(description = "范围ID列表", example = "[1]")
    private List<Long> scopeIds;

    @Pattern(regexp = "^(concise|detailed|code-review)$",
             message = "对话风格仅支持 concise/detailed/code-review")
    @Schema(description = "对话风格", example = "detailed",
            allowableValues = {"concise", "detailed", "code-review"})
    private String style;

    @Schema(description = "对话ID（null 则自动创建新对话）", example = "1")
    private Long conversationId;

    @Schema(description = "API Profile ID（使用新的 Profile 制）")
    private Long profileId;

    @Schema(description = "指定使用的模型名（如 deepseek-chat）")
    private String modelName;
}
