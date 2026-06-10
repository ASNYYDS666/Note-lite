package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "生成分享码请求")
public class ShareCreateDTO {
    @NotNull(message = "笔记ID不能为空")
    @Schema(description = "要分享的笔记ID", example = "1")
    private Long noteId;

    @Pattern(regexp = "^(READ|WRITE)$", message = "权限值只能为 READ 或 WRITE")
    @Schema(description = "分享权限：READ=只读, WRITE=可编辑", example = "READ", allowableValues = {"READ", "WRITE"})
    private String permission = "READ";
}
