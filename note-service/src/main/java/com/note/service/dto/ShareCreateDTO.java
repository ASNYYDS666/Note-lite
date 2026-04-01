package com.note.service.dto;

//生成分享码请求
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareCreateDTO {
    @NotNull(message = "笔记ID不能为空")
    private Long noteId;

    private String permission = "READ"; // 默认可读 (保留接口write)
}
