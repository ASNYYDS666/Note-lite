package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.mapper.UserMapper;
import org.springframework.stereotype.Service;
@Service
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    public boolean register(UserRegisterDTO dto) {
        // 1. 判重
        boolean exist = baseMapper.selectCount(
                new QueryWrapper<UserEntity>()
                        .eq("username", dto.getUsername())
                        .or()
                        .eq("email", dto.getEmail())
        ) > 0;
        if (exist) return false;

        // 2. 保存（密码先明文，Day3 再加密）
        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        return save(user);
    }
}
