//day02
//package com.note.service.service;
//
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.note.service.dto.UserRegisterDTO;
//import com.note.service.entity.UserEntity;
//import com.note.service.mapper.UserMapper;
//import org.springframework.stereotype.Service;
//@Service
//public class UserService extends ServiceImpl<UserMapper, UserEntity> {
//
//    public boolean register(UserRegisterDTO dto) {
//        // 1. 判重
//        boolean exist = baseMapper.selectCount(
//                new QueryWrapper<UserEntity>()
//                        .eq("username", dto.getUsername())
//                        .or()
//                        .eq("email", dto.getEmail())
//        ) > 0;
//        if (exist) return false;
//
//        // 2. 保存（密码先明文，Day3 再加密）
//        UserEntity user = new UserEntity();
//        user.setUsername(dto.getUsername());
//        user.setPassword(dto.getPassword());
//        user.setEmail(dto.getEmail());
//        return save(user);
//    }
//}

//day03-登录
package com.note.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.common.exception.BusinessException;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(UserRegisterDTO dto) {
        // 检查用户名
        if (lambdaQuery().eq(UserEntity::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 检查邮箱
        if (lambdaQuery().eq(UserEntity::getEmail, dto.getEmail()).count() > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 创建用户（密码加密）
        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());

        save(user);
    }

    public UserEntity login(LoginDTO dto) {
        UserEntity user = lambdaQuery()
                .eq(UserEntity::getUsername, dto.getUsername())
                .one();

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        return user;
    }
}
