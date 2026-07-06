package com.note.service.controller;

import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.util.JwtUtils;
import com.note.service.common.vo.LoginVO;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.UserInfoVO;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/api/v1/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    @Operation(summary="用户注册")
    public Result<String> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");

    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        UserEntity user = userService.login(dto);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        LoginVO result = new LoginVO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());

        return Result.success(result);
    }


    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getUserInfo(@AuthenticationPrincipal Long userId) {
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCreatedAt(user.getCreatedAt());
        return Result.success(vo);
    }
}
