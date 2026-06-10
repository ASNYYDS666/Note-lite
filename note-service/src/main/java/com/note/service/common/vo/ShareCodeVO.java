package com.note.service.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分享码生成结果")
public class ShareCodeVO {
    @Schema(description = "分享码，8位字母数字组合", example = "aB3xK9mQ")
    private String shareCode;

    @Schema(description = "分享链接（预留）", example = "http://localhost:5173/share/aB3xK9mQ")
    private String shareUrl;
}
