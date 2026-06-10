package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.common.constant.CacheConstants;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.dto.ShareAccessDTO;
import com.note.service.dto.ShareCreateDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.ShareEntity;
import com.note.service.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {
    private final ShareMapper shareMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NoteService noteService;

    private static final int CODE_LENGTH = 8;
    private static final int EXPIRE_DAYS = 7; // 分享码有效期7天（仅用于数据库 expire_at 计算）

    /**
     * 生成分享码（原子操作，无冲突）
     */
    @Transactional
    public String generateShareCode(ShareCreateDTO dto, Long userId) {
        // 校验笔记是否存在且属于当前用户
        NoteEntity note = noteService.getDetail(dto.getNoteId(), userId);
        if (note == null) {
            throw new BusinessException(ErrorCode.SHARE_NOTE_NOT_FOUND);
        }

        // 调用 Lua 脚本生成唯一分享码
        String shareCode = generateUniqueCode();

        // 保存到数据库
        ShareEntity share = new ShareEntity();
        share.setNoteId(dto.getNoteId());
        share.setCode(shareCode);
        share.setPermission(dto.getPermission());
        share.setExpireAt(LocalDateTime.now().plusDays(EXPIRE_DAYS));
        shareMapper.insert(share);

        // 写入 Redis（用于快速校验，TTL 加随机偏移防雪崩）
        String redisKey = CacheConstants.SHARE_CODE_PREFIX + shareCode;
        String value = share.getNoteId() + ":" + share.getPermission();
        stringRedisTemplate.opsForValue().set(redisKey, value,
                CacheConstants.ttlWithJitter(CacheConstants.SHARE_CODE_TTL_SECONDS), TimeUnit.SECONDS);

        log.info("生成分享码: noteId={}, code={}", note.getId(), shareCode);
        return shareCode;
    }

    /**
     * 通过分享码获取笔记内容（无需登录）
     */
    public ShareAccessDTO accessByCode(String code) {
        // 0. 检查空值缓存（防穿透）
        String nullKey = CacheConstants.SHARE_CODE_NULL_PREFIX + code;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullKey))) {
            log.info("分享码空值缓存命中: code={}", code);
            throw new BusinessException(ErrorCode.SHARE_CODE_INVALID);
        }

        // 1. 从 Redis 获取
        String redisKey = CacheConstants.SHARE_CODE_PREFIX + code;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            String[] parts = cached.split(":");
            Long noteId = Long.parseLong(parts[0]);
            String permission = parts[1];
            NoteEntity note = noteService.getDetailWithoutAuth(noteId); // 新方法，不校验用户
            return buildAccessDTO(note, permission);
        }

        // 2. Redis 未命中，查数据库
        LambdaQueryWrapper<ShareEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareEntity::getCode, code)
                .ge(ShareEntity::getExpireAt, LocalDateTime.now());
        ShareEntity share = shareMapper.selectOne(wrapper);
        if (share == null) {
            // 防穿透：对无效分享码缓存空值
            stringRedisTemplate.opsForValue().set(nullKey, "1",
                    CacheConstants.SHARE_CODE_NULL_TTL_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(ErrorCode.SHARE_CODE_INVALID);
        }

        NoteEntity note = noteService.getDetailWithoutAuth(share.getNoteId());
        // 回写 Redis（TTL 加随机偏移防雪崩）
        String value = share.getNoteId() + ":" + share.getPermission();
        stringRedisTemplate.opsForValue().set(redisKey, value,
                CacheConstants.ttlWithJitter(CacheConstants.SHARE_CODE_TTL_SECONDS), TimeUnit.SECONDS);

        return buildAccessDTO(note, share.getPermission());
    }

    private ShareAccessDTO buildAccessDTO(NoteEntity note, String permission) {
        ShareAccessDTO dto = new ShareAccessDTO();
        dto.setNote(noteService.convertToVO(note));
        dto.setPermission(permission);
        return dto;
    }

    /**
     * 生成唯一分享码（通过 Lua 脚本保证原子性）
     */
    private String generateUniqueCode() {
        // Lua 脚本：在 Redis 中尝试生成一个不存在的分享码，最多重试 10 次
        String luaScript =
                "local key_prefix = ARGV[1] " +
                        "local code_length = tonumber(ARGV[2]) " +
                        "local max_attempts = tonumber(ARGV[3]) " +
                        "local ttl = tonumber(ARGV[4]) " +
                        "for i = 1, max_attempts do " +
                        "   local code = '' " +
                        "   for j = 1, code_length do " +
                        "       local rand = math.random(48, 122) " +
                        "       if rand > 57 and rand < 65 then rand = 65 end " +
                        "       if rand > 90 and rand < 97 then rand = 97 end " +
                        "       code = code .. string.char(rand) " +
                        "   end " +
                        "   local exists = redis.call('EXISTS', key_prefix .. code) " +
                        "   if exists == 0 then " +
                        "       redis.call('SETEX', key_prefix .. code, ttl, 'temp') " +
                        "       return code " +
                        "   end " +
                        "end " +
                        "return nil";

        DefaultRedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);
        String code = stringRedisTemplate.execute(script, Collections.emptyList(),
                CacheConstants.SHARE_CODE_PREFIX, String.valueOf(CODE_LENGTH), "10",
                String.valueOf(CacheConstants.SHARE_CODE_TTL_SECONDS));
        if (code == null) {
            throw new BusinessException(ErrorCode.SHARE_GENERATE_FAILED);
        }
        return code;
    }
}
