//day02
package com.note.service.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度 6-30 字符")
    private String password;

    @Email(message = "邮箱格式错误")
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
