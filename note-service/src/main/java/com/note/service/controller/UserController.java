//day02
//package com.note.service.controller;
//
//
//import com.note.service.dto.UserRegisterDTO;
//import com.note.service.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//@RestController
//@RequestMapping("/api/ums")
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    @PostMapping("/register")
//    public ResponseEntity<String> register(@RequestBody UserRegisterDTO dto) {
//        boolean ok = userService.register(dto);
//        return ok ? ResponseEntity.ok("注册成功")
//                : ResponseEntity.badRequest().body("用户名或邮箱已存在");
//    }
//}

//day03
package com.note.service.controller;

import com.note.service.common.util.JwtUtils;
import com.note.service.common.vo.Result;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ums")
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
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO dto) {
        UserEntity user = userService.login(dto);

        // 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());

        return Result.success(result);
    }


    @GetMapping("/info")
    public Result<UserEntity> getUserInfo() {
        // 测试受保护接口，后续完善
        return Result.success(new UserEntity());
    }
}
