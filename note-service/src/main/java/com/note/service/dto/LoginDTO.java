package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长50字符")
    @Schema(description = "用户名", example = "nysanka")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
