package com.note.service.service;

import com.note.service.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    @Async
    public void invalidate(Long userId) {
        stringRedisTemplate.delete(CacheConstants.NOTE_TREE_PREFIX + userId);
        log.debug("Tree缓存已异步失效, userId={}", userId);
    }
}
