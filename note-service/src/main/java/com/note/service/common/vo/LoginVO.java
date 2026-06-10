package com.note.service.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录成功返回")
public class LoginVO {
    @Schema(description = "JWT 访问令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;
}
