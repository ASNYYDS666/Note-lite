package com.note.service.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户个人信息")
public class UserInfoVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
