package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长50字符")
    @Schema(description = "用户名，全局唯一", example = "nysanka")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度 6-30 字符")
    @Schema(description = "密码，长度 6-30 字符", example = "123456")
    private String password;

    @Email(message = "邮箱格式错误")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 100, message = "邮箱最长100字符")
    @Schema(description = "邮箱地址", example = "nysanka@example.com")
    private String email;
}
