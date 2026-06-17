package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.ai.NoteEmbeddingEvent;
import com.note.service.common.constant.CacheConstants;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.metrics.MicrometerMetrics;
import com.note.service.common.vo.NoteDetailVO;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.NoteTagEntity;
import com.note.service.entity.ShareEntity;
import com.note.service.mapper.NoteMapper;
import com.note.service.mapper.NoteTagMapper;
import com.note.service.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService extends ServiceImpl<NoteMapper, NoteEntity> {

    private final NoteTagMapper noteTagMapper;
    private final ShareMapper shareMapper;

    private final StringRedisTemplate stringRedisTemplate;  // 通过构造器注入
    private final ObjectMapper objectMapper;
    private final MicrometerMetrics metrics; //day08新增监控指标工具
    private final ApplicationEventPublisher eventPublisher;

    // ========== 核心 CRUD ==========

    @Transactional
    public Long createNote(Long userId, NoteDTO dto) {
        // 1. 保存笔记主表
        NoteEntity note = new NoteEntity();
        note.setUserId(userId);
        note.setTitle(dto.getTitle().trim());
        note.setContent(dto.getContent());
        note.setSummary(generateSummary(dto.getContent()));
        note.setIsDeleted(0);

        baseMapper.insert(note);

        // 2. 保存标签（去重、小写、 trim）
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(note.getId(), dto.getTags());
        }

        log.info("创建笔记成功: userId={}, noteId={}", userId, note.getId());
        eventPublisher.publishEvent(new NoteEmbeddingEvent(note.getId(), NoteEmbeddingEvent.EventType.CREATED));
        return note.getId();
    }

    public NoteEntity getDetail(Long noteId, Long userId) {

        // 监控：使用recordQuery包裹整段代码，自动记录整个方法的耗时
        return metrics.recordQuery(() ->{
            // 1.检查空值缓存（防穿透）
            String nullKey = CacheConstants.NOTE_NULL_PREFIX + noteId;
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullKey))) {
                log.info("空值缓存命中，直接返回: noteId={}", noteId);
                metrics.recordNullCacheHit(); // 穿透防护命中 +1
                throw new BusinessException(ErrorCode.NOTE_NO_PERMISSION);
            }

            // 2.尝试从缓存中获取（Key 含 userId，天然隔离子权限）
            String cacheKey = CacheConstants.NOTE_DETAIL_PREFIX + userId + ":" + noteId;
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("缓存命中: userId={}, noteId={}", userId, noteId);
                metrics.recordCacheHit(); // 命中计数器 +1
                try {
                    NoteEntity note = objectMapper.readValue(cachedJson, NoteEntity.class);
                    if (note.getIsDeleted() == 1) {
                        throw new BusinessException(ErrorCode.NOTE_IN_RECYCLE);
                    }
                    return note;
                } catch (JsonProcessingException e) {
                    log.error("缓存反序列化失败: noteId={}", noteId, e);
                    // 解析失败，删除缓存并继续查数据库
                    stringRedisTemplate.delete(cacheKey);
                }
            } else {
                log.info("缓存未命中: userId={}, noteId={}", userId, noteId);
                metrics.recordCacheMiss(); // 未命中计数器 +1
            }

            // 3.缓存未命中，查数据库
            NoteEntity note = baseMapper.selectById(noteId);
            if (note == null || !note.getUserId().equals(userId)) {
                // 防穿透：对不存在的 noteId 缓存空值
                stringRedisTemplate.opsForValue().set(nullKey, "1",
                        CacheConstants.NOTE_NULL_TTL_SECONDS, TimeUnit.SECONDS);
                throw new BusinessException(ErrorCode.NOTE_NO_PERMISSION);
            }
            if (note.getIsDeleted() == 1) {
                throw new BusinessException(ErrorCode.NOTE_IN_RECYCLE);
            }

            // 组装标签
            List<String> tags = noteTagMapper.selectTagsByNoteId(noteId);
            note.setTags(tags);

            // 4.写入缓存 day07（TTL 加随机偏移防雪崩）
            try {
                String json = objectMapper.writeValueAsString(note);
                stringRedisTemplate.opsForValue().set(cacheKey, json,
                        CacheConstants.ttlWithJitter(CacheConstants.NOTE_DETAIL_TTL_SECONDS), TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.error("缓存写入失败: noteId={}", noteId, e);
            }
            return note;
        });
    }

    //新增清除缓存的方法：同时清除认证用户缓存和分享缓存
    private void clearNoteCache(Long noteId, Long userId) {
        String userCacheKey = CacheConstants.NOTE_DETAIL_PREFIX + userId + ":" + noteId;
        String shareCacheKey = CacheConstants.NOTE_SHARE_DETAIL_PREFIX + noteId;
        String nullKey = CacheConstants.NOTE_NULL_PREFIX + noteId;
        stringRedisTemplate.delete(userCacheKey);
        stringRedisTemplate.delete(shareCacheKey);
        stringRedisTemplate.delete(nullKey);
        log.debug("清除缓存: userKey={}, shareKey={}, nullKey={}", userCacheKey, shareCacheKey, nullKey);
    }

    @Transactional
    public void updateNote(Long noteId, Long userId, NoteDTO dto) {
        // 1. 校验权限
        NoteEntity exist = getDetail(noteId, userId);  // 会抛异常如果无权限

        // 2. 更新主表
        exist.setTitle(dto.getTitle().trim());
        exist.setContent(dto.getContent());
        exist.setSummary(generateSummary(dto.getContent()));
        baseMapper.updateById(exist);

        // 3. 更新标签：先删后插（简单策略）
        noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", noteId));
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(noteId, dto.getTags());
        }

        // 清除缓存
        clearNoteCache(noteId, userId);
        eventPublisher.publishEvent(new NoteEmbeddingEvent(noteId, NoteEmbeddingEvent.EventType.UPDATED));
        log.info("更新笔记成功，已清除缓存: noteId={}", noteId);
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId, boolean permanent) {
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_NO_PERMISSION);
        }

        if (permanent) {
            // 物理删除
            baseMapper.deleteById(noteId);
            noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", noteId));
            shareMapper.delete(new QueryWrapper<ShareEntity>().eq("note_id", noteId));
            eventPublisher.publishEvent(new NoteEmbeddingEvent(noteId, NoteEmbeddingEvent.EventType.DELETED));
            log.info("物理删除笔记: noteId={}", noteId);
        } else {
            // 软删除（移入回收站）
            note.setIsDeleted(1);
            note.setDeletedAt(LocalDateTime.now());
            baseMapper.updateById(note);
            eventPublisher.publishEvent(new NoteEmbeddingEvent(noteId, NoteEmbeddingEvent.EventType.DELETED));
            log.info("移入回收站: noteId={}", noteId);
        }
        // 新增无论是软删除还是物理删除都需要清除缓存
        clearNoteCache(noteId, userId);
    }

    // ========== 分页查询 ==========

    /**
     * 分页查询笔记列表（支持标签筛选）
     */
    public Page<NoteDetailVO> pageQuery(Long userId, NoteQueryDTO query) {
        // 如果没有标签筛选，走原来的简单查询（性能更好）
        Page<NoteEntity> entityPage;
        if (query.getTags() == null || query.getTags().isEmpty()) {
            entityPage = simplePageQuery(userId, query);
        } else {
            // 有标签筛选：使用手动分页
            entityPage = tagFilterPageQuery(userId, query);
        }

        Page<NoteDetailVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 简单分页查询（无标签筛选）- 复用原有逻辑
     */
    private Page<NoteEntity> simplePageQuery(Long userId, NoteQueryDTO query) {
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", query.getIsDeleted());

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like("title", query.getKeyword());
        }

        wrapper.orderByDesc("updated_at");
        page = baseMapper.selectPage(page, wrapper);

        // 批量组装标签
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            List<Long> noteIds = page.getRecords().stream()
                    .map(NoteEntity::getId)
                    .collect(Collectors.toList());
            Map<Long, List<String>> tagMap = noteTagMapper.selectList(
                    new QueryWrapper<NoteTagEntity>().in("note_id", noteIds))
                    .stream()
                    .collect(Collectors.groupingBy(
                            NoteTagEntity::getNoteId,
                            Collectors.mapping(NoteTagEntity::getTagName, Collectors.toList())
                    ));
            page.getRecords().forEach(note ->
                    note.setTags(tagMap.getOrDefault(note.getId(), Collections.emptyList())));
        }

        return page;
    }

    /**
     * 标签筛选分页查询（手动分页解决 COUNT 问题）
     */
    private Page<NoteEntity> tagFilterPageQuery(Long userId, NoteQueryDTO query) {
        // 计算分页参数
        long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
        long limit = query.getPageSize();

        // 1. 查询当前页的数据
        List<NoteEntity> records = baseMapper.selectWithTags(
                userId,
                query.getIsDeleted(),
                query.getKeyword(),
                query.getTags(),
                query.getTagMatch(),
                offset,
                limit
        );

        // 2. 解析 tagNames 到 tags 列表
        if (records != null) {
            records.forEach(NoteEntity::parseTagNames);
        }

        // 3. 查询总记录数
        long total = baseMapper.countWithTags(
                userId,
                query.getIsDeleted(),
                query.getKeyword(),
                query.getTags(),
                query.getTagMatch()
        );

        // 4. 手动组装 Page 对象
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize(), total);
        page.setRecords(records != null ? records : new ArrayList<>());

        return page;
    }
//    // 这是原有的分页查询方法（day05步骤二指出有错误，替换为以下新的）
//    public Page<NoteEntity> pageQuery(Long userId, NoteQueryDTO query) {
//        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
//
//        // 简单实现：先查主表，再组装标签（避免复杂 SQL）
//        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
//        wrapper.eq("user_id", userId)
//                .eq("is_deleted", query.getIsDeleted());
//
//        // 关键词搜索（仅标题）
//        if (StringUtils.hasText(query.getKeyword())) {
//            wrapper.like("title", query.getKeyword());
//        }
//
//        wrapper.orderByDesc("updated_at");
//        page = baseMapper.selectPage(page, wrapper);
//
//        // 批量组装标签（可优化为批量查询，当前逐条）
//        page.getRecords().forEach(note -> {
//            List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
//            note.setTags(tags);
//        });
//
//        return page;
//    }

    public NoteDetailVO convertToVO(NoteEntity entity) {
        NoteDetailVO vo = new NoteDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setSummary(entity.getSummary());
        vo.setTags(entity.getTags());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    public NoteDetailVO getDetailVO(Long noteId, Long userId) {
        return convertToVO(getDetail(noteId, userId));
    }

    public List<String> getUserTags(Long userId) {
        return noteTagMapper.selectDistinctTagsByUserId(userId);
    }

    // ========== 工具方法 ==========

    private void saveTags(Long noteId, List<String> tags) {
        // 清洗：去重、小写、trim、过滤空串
        List<String> cleanedTags = tags.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .limit(10)  // 最多10个
                .collect(Collectors.toList());

        for (String tagName : cleanedTags) {
            NoteTagEntity tag = new NoteTagEntity();
            tag.setNoteId(noteId);
            tag.setTagName(tagName);
            noteTagMapper.insert(tag);
        }
    }

    private String generateSummary(String content) {
        if (!StringUtils.hasText(content)) return "";
        // 简单清洗 Markdown 标记，取前200字符
        String plain = content
                .replaceAll("#", "")
                .replaceAll("\\*", "")
                .replaceAll("`", "")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .replaceAll("\n", " ")
                .trim();

        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }

    // ========== 回收站相关 ==========

    /**
     * 从回收站恢复笔记
     */
    @Transactional
    public void restoreFromRecycle(Long noteId, Long userId) {
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_NO_PERMISSION);
        }
        if (note.getIsDeleted() != 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_IN_RECYCLE);
        }

        note.setIsDeleted(0);
        note.setDeletedAt(null);
        baseMapper.updateById(note);
        clearNoteCache(noteId, userId);
        eventPublisher.publishEvent(new NoteEmbeddingEvent(noteId, NoteEmbeddingEvent.EventType.UPDATED));
        log.info("恢复笔记，已清除缓存: noteId={}", noteId);
    }

    /**
     * 清空回收站（物理删除所有回收站笔记）
     */
    @Transactional
    public void clearRecycle(Long userId) {
        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_deleted", 1);

        List<NoteEntity> recycleNotes = baseMapper.selectList(wrapper);
        for (NoteEntity note : recycleNotes) {
            // 物理删除笔记（同时删除标签和分享记录）
            baseMapper.deleteById(note.getId());
            noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", note.getId()));
            shareMapper.delete(new QueryWrapper<ShareEntity>().eq("note_id", note.getId()));
            // 清除缓存
            clearNoteCache(note.getId(), userId);
            eventPublisher.publishEvent(new NoteEmbeddingEvent(note.getId(), NoteEmbeddingEvent.EventType.DELETED));
        }
        log.info("清空回收站: userId={}, count={}", userId, recycleNotes.size());
    }

    /**
     * 获取回收站列表
     */
    public Page<NoteDetailVO> pageRecycle(Long userId, NoteQueryDTO query) {
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 1)
                .orderByDesc("updated_at");  // 按更新时间倒序（删除时 updated_at 同步更新），利用复合索引消除 filesort

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like("title", query.getKeyword());
        }

        page = baseMapper.selectPage(page, wrapper);

        // 批量组装标签
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            List<Long> noteIds = page.getRecords().stream()
                    .map(NoteEntity::getId)
                    .collect(Collectors.toList());
            Map<Long, List<String>> tagMap = noteTagMapper.selectList(
                    new QueryWrapper<NoteTagEntity>().in("note_id", noteIds))
                    .stream()
                    .collect(Collectors.groupingBy(
                            NoteTagEntity::getNoteId,
                            Collectors.mapping(NoteTagEntity::getTagName, Collectors.toList())
                    ));
            page.getRecords().forEach(note ->
                    note.setTags(tagMap.getOrDefault(note.getId(), Collections.emptyList())));
        }

        Page<NoteDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 获取笔记详情（无需权限，用于分享）
     */
    public NoteEntity getDetailWithoutAuth(Long noteId) {

        // 检查空值缓存（防穿透）
        String nullKey = CacheConstants.NOTE_NULL_PREFIX + noteId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullKey))) {
            log.info("分享-空值缓存命中: noteId={}", noteId);
            metrics.recordNullCacheHit(); // 穿透防护命中 +1
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        String cacheKey = CacheConstants.NOTE_SHARE_DETAIL_PREFIX + noteId;
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cachedJson != null) {
            log.info("分享-缓存命中: noteId={}", noteId);
            try {
                NoteEntity note = objectMapper.readValue(cachedJson, NoteEntity.class);
                // 分享访问只需校验笔记未被删除
                if (note.getIsDeleted() == 1) {
                    throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
                }
                return note;
            } catch (JsonProcessingException e) {
                log.error("分享-缓存反序列化失败，将删除缓存 key={}", cacheKey, e);
                stringRedisTemplate.delete(cacheKey);
                // 继续查库
            }
        } else {
            log.info("分享-缓存未命中: noteId={}", noteId);
        }

        // 缓存未命中或反序列化失败，查询数据库
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || note.getIsDeleted() == 1) {
            // 防穿透：缓存空值
            if (note == null) {
                stringRedisTemplate.opsForValue().set(nullKey, "1",
                        CacheConstants.NOTE_NULL_TTL_SECONDS, TimeUnit.SECONDS);
            }
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        // 组装标签
        List<String> tags = noteTagMapper.selectTagsByNoteId(noteId);
        note.setTags(tags);

        // 写入缓存（TTL 加随机偏移防雪崩）
        try {
            String json = objectMapper.writeValueAsString(note);
            stringRedisTemplate.opsForValue().set(cacheKey, json,
                    CacheConstants.ttlWithJitter(CacheConstants.NOTE_DETAIL_TTL_SECONDS), TimeUnit.SECONDS);
            log.debug("分享-缓存写入: noteId={}", noteId);
        } catch (JsonProcessingException e) {
            log.error("分享-序列化笔记失败，noteId={}", noteId, e);
        }
        return note;
    }

}
