package com.note.service.controller;


import com.note.service.dto.UserRegisterDTO;
import com.note.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/ums")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRegisterDTO dto) {
        boolean ok = userService.register(dto);
        return ok ? ResponseEntity.ok("注册成功")
                : ResponseEntity.badRequest().body("用户名或邮箱已存在");
    }
}
