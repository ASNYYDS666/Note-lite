package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.common.constant.CacheConstants;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.metrics.MicrometerMetrics;
import com.note.service.common.vo.NoteDetailVO;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.NoteTagEntity;
import com.note.service.mapper.NoteMapper;
import com.note.service.mapper.NoteTagMapper;
import com.note.service.mapper.ShareMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NoteService 单元测试")
class NoteServiceTest {

    @Mock
    private NoteMapper noteMapper;
    @Mock
    private NoteTagMapper noteTagMapper;
    @Mock
    private ShareMapper shareMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MicrometerMetrics metrics;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteTagMapper, shareMapper, stringRedisTemplate,
                objectMapper, metrics);
        ReflectionTestUtils.setField(noteService, "baseMapper", noteMapper);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(metrics.recordQuery(any())).thenAnswer(invocation -> {
            MicrometerMetrics.CheckedSupplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    // ========================================
    // 组 1: createNote
    // ========================================

    @Nested
    @DisplayName("创建笔记")
    class CreateNote {

        @Test
        @DisplayName("含标签创建 — 标签去重、小写、trim")
        void shouldCreateWithTags() {
            NoteDTO dto = new NoteDTO();
            dto.setTitle(" 测试笔记 ");
            dto.setContent("# Hello");
            dto.setTags(Arrays.asList("Java", " SPRING ", "java"));

            when(noteMapper.insert(ArgumentMatchers.<NoteEntity>any())).thenAnswer(inv -> {
                NoteEntity n = inv.getArgument(0);
                n.setId(100L);
                return 1;
            });

            Long noteId = noteService.createNote(1L, dto);

            assertThat(noteId).isEqualTo(100L);
            // 标签去重、小写、trim 后只剩 "java" 和 "spring" 两个
            verify(noteTagMapper, times(2)).insert(ArgumentMatchers.<NoteTagEntity>any());
        }

        @Test
        @DisplayName("无标签创建")
        void shouldCreateWithoutTags() {
            NoteDTO dto = new NoteDTO();
            dto.setTitle("无标签笔记");
            dto.setContent("内容");

            when(noteMapper.insert(ArgumentMatchers.<NoteEntity>any())).thenAnswer(inv -> {
                NoteEntity n = inv.getArgument(0);
                n.setId(101L);
                return 1;
            });

            noteService.createNote(1L, dto);

            verify(noteTagMapper, never()).insert(ArgumentMatchers.<NoteTagEntity>any());
        }

        @Test
        @DisplayName("空标签列表创建")
        void shouldCreateWithEmptyTags() {
            NoteDTO dto = new NoteDTO();
            dto.setTitle("空标签");
            dto.setContent("内容");
            dto.setTags(Collections.emptyList());

            when(noteMapper.insert(ArgumentMatchers.<NoteEntity>any())).thenAnswer(inv -> {
                NoteEntity n = inv.getArgument(0);
                n.setId(102L);
                return 1;
            });

            noteService.createNote(1L, dto);

            verify(noteTagMapper, never()).insert(ArgumentMatchers.<NoteTagEntity>any());
        }
    }

    // ========================================
    // 组 2: getDetail（缓存链路）
    // ========================================

    @Nested
    @DisplayName("获取笔记详情（含缓存链路）")
    class GetDetail {

        @Test
        @DisplayName("缓存命中直接返回")
        void shouldReturnFromCache() throws Exception {
            NoteEntity note = createTestNote(100L, 1L, "标题", "内容", 0);
            note.setTags(Arrays.asList("java"));
            String json = objectMapper.writeValueAsString(note);
            String cacheKey = "note:detail:1:100";

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get(cacheKey)).thenReturn(json);

            NoteEntity result = noteService.getDetail(100L, 1L);

            assertThat(result.getTitle()).isEqualTo("标题");
            assertThat(result.getTags()).containsExactly("java");
            verify(metrics).recordCacheHit();
            verify(noteMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("空值缓存命中 → 抛 NOTE_NO_PERMISSION")
        void shouldThrowWhenNullCacheHit() {
            when(stringRedisTemplate.hasKey("note:null:200")).thenReturn(true);

            assertThatThrownBy(() -> noteService.getDetail(200L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NO_PERMISSION.getCode());

            verify(metrics).recordNullCacheHit();
            verify(noteMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("缓存未命中 + DB 查到 → 回写缓存")
        void shouldQueryDbAndWriteCacheWhenCacheMiss() {
            NoteEntity note = createTestNote(300L, 1L, "DB标题", "DB内容", 0);
            String cacheKey = "note:detail:1:300";

            when(stringRedisTemplate.hasKey("note:null:300")).thenReturn(false);
            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(noteMapper.selectById(300L)).thenReturn(note);
            when(noteTagMapper.selectTagsByNoteId(300L)).thenReturn(Arrays.asList("java"));

            NoteEntity result = noteService.getDetail(300L, 1L);

            assertThat(result.getTitle()).isEqualTo("DB标题");
            assertThat(result.getTags()).containsExactly("java");
            verify(metrics).recordCacheMiss();
            verify(valueOperations).set(eq(cacheKey), anyString(),
                    anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存未命中 + DB 无记录 → 写空值缓存 + 抛异常")
        void shouldWriteNullCacheAndThrowWhenNoteNotFound() {
            when(stringRedisTemplate.hasKey("note:null:999")).thenReturn(false);
            when(valueOperations.get("note:detail:1:999")).thenReturn(null);
            when(noteMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> noteService.getDetail(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NO_PERMISSION.getCode());

            verify(valueOperations).set(eq("note:null:999"), eq("1"),
                    anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存未命中 + DB 存在但 userId 不匹配")
        void shouldWriteNullCacheWhenWrongUser() {
            NoteEntity note = createTestNote(100L, 2L, "他人笔记", "内容", 0);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:1:100")).thenReturn(null);
            when(noteMapper.selectById(100L)).thenReturn(note);

            assertThatThrownBy(() -> noteService.getDetail(100L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NO_PERMISSION.getCode());

            verify(valueOperations).set(eq("note:null:100"), eq("1"),
                    anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存未命中 + DB 查到但 isDeleted=1")
        void shouldThrowWhenNoteInRecycle() {
            NoteEntity note = createTestNote(100L, 1L, "回收站笔记", "内容", 1);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:1:100")).thenReturn(null);
            when(noteMapper.selectById(100L)).thenReturn(note);

            assertThatThrownBy(() -> noteService.getDetail(100L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_IN_RECYCLE.getCode());
        }

        @Test
        @DisplayName("缓存 JSON 损坏 → 删 key 后回退 DB")
        void shouldDeleteCorruptCacheAndFallbackToDb() {
            NoteEntity note = createTestNote(100L, 1L, "DB笔记", "内容", 0);
            note.setTags(Arrays.asList("java"));
            String cacheKey = "note:detail:1:100";

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get(cacheKey)).thenReturn("not-valid-json{{{");
            when(noteMapper.selectById(100L)).thenReturn(note);
            when(noteTagMapper.selectTagsByNoteId(100L)).thenReturn(Arrays.asList("java"));

            NoteEntity result = noteService.getDetail(100L, 1L);

            assertThat(result.getTitle()).isEqualTo("DB笔记");
            verify(stringRedisTemplate).delete(cacheKey);
            // recordCacheHit 在反序列化前调用，即使 JSON 损坏也算一次命中
            verify(metrics).recordCacheHit();
        }

        @Test
        @DisplayName("缓存命中但 isDeleted=1")
        void shouldThrowWhenCachedNoteIsDeleted() throws Exception {
            NoteEntity note = createTestNote(100L, 1L, "已删笔记", "内容", 1);
            String json = objectMapper.writeValueAsString(note);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:1:100")).thenReturn(json);

            assertThatThrownBy(() -> noteService.getDetail(100L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_IN_RECYCLE.getCode());

            verify(metrics).recordCacheHit();
        }
    }

    // ========================================
    // 组 3: updateNote / deleteNote
    // ========================================

    @Nested
    @DisplayName("更新与删除笔记")
    class UpdateAndDelete {

        @Test
        @DisplayName("更新成功 → 清除三个缓存 key")
        void shouldUpdateAndClearCache() throws Exception {
            NoteEntity exist = createTestNote(100L, 1L, "旧标题", "旧内容", 0);
            exist.setTags(Arrays.asList("old"));
            String json = objectMapper.writeValueAsString(exist);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:1:100")).thenReturn(json);

            NoteDTO dto = new NoteDTO();
            dto.setTitle("新标题");
            dto.setContent("新内容");
            dto.setTags(Arrays.asList("java"));

            noteService.updateNote(100L, 1L, dto);

            verify(noteMapper).updateById(ArgumentMatchers.<NoteEntity>any());
            verify(stringRedisTemplate).delete("note:detail:1:100");
            verify(stringRedisTemplate).delete("note:detail:share:100");
            verify(stringRedisTemplate).delete("note:null:100");
        }

        @Test
        @DisplayName("更新无权限的笔记 → 抛异常")
        void shouldNotUpdateWhenNoPermission() {
            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:2:100")).thenReturn(null);
            when(noteMapper.selectById(100L)).thenReturn(null);

            NoteDTO dto = new NoteDTO();
            dto.setTitle("新标题");
            dto.setContent("新内容");

            assertThatThrownBy(() -> noteService.updateNote(100L, 2L, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NO_PERMISSION.getCode());

            verify(noteMapper, never()).updateById(ArgumentMatchers.<NoteEntity>any());
        }

        @Test
        @DisplayName("软删除 → isDeleted=1，不清除标签")
        void shouldSoftDelete() {
            NoteEntity note = createTestNote(100L, 1L, "笔记", "内容", 0);
            when(noteMapper.selectById(100L)).thenReturn(note);

            noteService.deleteNote(100L, 1L, false);

            verify(noteMapper).updateById(ArgumentMatchers.<NoteEntity>any());
            verify(noteTagMapper, never()).delete(any());
            verify(shareMapper, never()).delete(any());
            verify(stringRedisTemplate).delete("note:detail:1:100");
            verify(stringRedisTemplate).delete("note:detail:share:100");
            verify(stringRedisTemplate).delete("note:null:100");
        }

        @Test
        @DisplayName("物理删除 → 删笔记、标签、分享记录")
        void shouldPermanentDelete() {
            NoteEntity note = createTestNote(100L, 1L, "笔记", "内容", 0);
            when(noteMapper.selectById(100L)).thenReturn(note);

            noteService.deleteNote(100L, 1L, true);

            verify(noteMapper).deleteById(100L);
            verify(noteTagMapper).delete(any());
            verify(shareMapper).delete(any());
            verify(stringRedisTemplate).delete("note:detail:1:100");
            verify(stringRedisTemplate).delete("note:detail:share:100");
            verify(stringRedisTemplate).delete("note:null:100");
        }

        @Test
        @DisplayName("删除无权限笔记 → 抛异常")
        void shouldNotDeleteWhenNoPermission() {
            when(noteMapper.selectById(100L)).thenReturn(null);

            assertThatThrownBy(() -> noteService.deleteNote(100L, 1L, false))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NO_PERMISSION.getCode());

            verify(noteMapper, never()).updateById(ArgumentMatchers.<NoteEntity>any());
            verify(noteMapper, never()).deleteById(anyLong());
        }
    }

    // ========================================
    // 组 4: pageQuery
    // ========================================

    @Nested
    @DisplayName("分页查询")
    class PageQuery {

        @Test
        @DisplayName("无标签筛选的简单分页")
        void shouldSimplePageQuery() {
            NoteQueryDTO query = new NoteQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);
            query.setIsDeleted(0);

            NoteEntity n1 = createTestNote(1L, 1L, "笔记1", "内容1", 0);
            NoteEntity n2 = createTestNote(2L, 1L, "笔记2", "内容2", 0);
            Page<NoteEntity> entityPage = new Page<>(1, 10, 2);
            entityPage.setRecords(Arrays.asList(n1, n2));

            when(noteMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(entityPage);
            when(noteTagMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            Page<NoteDetailVO> result = noteService.pageQuery(1L, query);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getRecords().get(0)).isInstanceOf(NoteDetailVO.class);
        }

        @Test
        @DisplayName("带标签筛选的分页")
        void shouldTagFilterPageQuery() {
            NoteQueryDTO query = new NoteQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);
            query.setIsDeleted(0);
            query.setTags(Arrays.asList("java", "spring"));
            query.setTagMatch("ALL");

            NoteEntity n1 = createTestNote(1L, 1L, "笔记1", "内容1", 0);
            when(noteMapper.selectWithTags(eq(1L), eq(0), isNull(),
                    anyList(), eq("ALL"), eq(0L), eq(10L)))
                    .thenReturn(Arrays.asList(n1));
            when(noteMapper.countWithTags(eq(1L), eq(0), isNull(),
                    anyList(), eq("ALL")))
                    .thenReturn(1L);

            Page<NoteDetailVO> result = noteService.pageQuery(1L, query);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            verify(noteMapper).selectWithTags(anyLong(), anyInt(), any(),
                    anyList(), anyString(), anyLong(), anyLong());
            verify(noteMapper).countWithTags(anyLong(), anyInt(), any(),
                    anyList(), anyString());
        }
    }

    // ========================================
    // 组 5: 回收站操作
    // ========================================

    @Nested
    @DisplayName("回收站操作")
    class Recycle {

        @Test
        @DisplayName("从回收站恢复 → isDeleted=0, deletedAt=null")
        void shouldRestoreFromRecycle() {
            NoteEntity note = createTestNote(100L, 1L, "回收笔记", "内容", 1);
            note.setDeletedAt(java.time.LocalDateTime.now());
            when(noteMapper.selectById(100L)).thenReturn(note);

            noteService.restoreFromRecycle(100L, 1L);

            verify(noteMapper).updateById(ArgumentMatchers.<NoteEntity>any());
            verify(stringRedisTemplate).delete("note:detail:1:100");
            verify(stringRedisTemplate).delete("note:detail:share:100");
            verify(stringRedisTemplate).delete("note:null:100");
        }

        @Test
        @DisplayName("恢复未在回收站的笔记 → 抛 NOTE_NOT_IN_RECYCLE")
        void shouldNotRestoreWhenNotInRecycle() {
            NoteEntity note = createTestNote(100L, 1L, "正常笔记", "内容", 0);
            when(noteMapper.selectById(100L)).thenReturn(note);

            assertThatThrownBy(() -> noteService.restoreFromRecycle(100L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NOT_IN_RECYCLE.getCode());
        }

        @Test
        @DisplayName("清空回收站 → 物理删除所有已删笔记")
        void shouldClearRecycle() {
            NoteEntity n1 = createTestNote(1L, 1L, "回收1", "", 1);
            NoteEntity n2 = createTestNote(2L, 1L, "回收2", "", 1);
            when(noteMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(Arrays.asList(n1, n2));

            noteService.clearRecycle(1L);

            verify(noteMapper, times(2)).deleteById(anyLong());
            verify(noteTagMapper, times(2)).delete(any(QueryWrapper.class));
            verify(shareMapper, times(2)).delete(any(QueryWrapper.class));
            verify(stringRedisTemplate, times(6)).delete(anyString());
        }
    }

    // ========================================
    // 组 6: getDetailWithoutAuth（分享）
    // ========================================

    @Nested
    @DisplayName("分享笔记访问（无需认证）")
    class GetDetailWithoutAuth {

        @Test
        @DisplayName("分享缓存命中")
        void shouldReturnFromShareCache() throws Exception {
            NoteEntity note = createTestNote(100L, 1L, "分享笔记", "内容", 0);
            note.setTags(Arrays.asList("public"));
            String json = objectMapper.writeValueAsString(note);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:share:100")).thenReturn(json);

            NoteEntity result = noteService.getDetailWithoutAuth(100L);

            assertThat(result.getTitle()).isEqualTo("分享笔记");
            verify(noteMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("分享笔记不存在 → 写空值缓存 + NOTE_NOT_FOUND")
        void shouldThrowWhenShareNoteNotFound() {
            when(stringRedisTemplate.hasKey("note:null:999")).thenReturn(false);
            when(valueOperations.get("note:detail:share:999")).thenReturn(null);
            when(noteMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> noteService.getDetailWithoutAuth(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());

            verify(valueOperations).set(eq("note:null:999"), eq("1"),
                    anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("分享笔记已软删除 → NOTE_NOT_FOUND（不写空值缓存）")
        void shouldThrowWhenSharedNoteIsDeleted() {
            NoteEntity note = createTestNote(100L, 1L, "已删分享笔记", "内容", 1);

            when(stringRedisTemplate.hasKey("note:null:100")).thenReturn(false);
            when(valueOperations.get("note:detail:share:100")).thenReturn(null);
            when(noteMapper.selectById(100L)).thenReturn(note);

            assertThatThrownBy(() -> noteService.getDetailWithoutAuth(100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());

            // isDeleted=1 时不写空值缓存（笔记可能恢复）
            verify(valueOperations, never()).set(eq("note:null:100"), anyString(),
                    anyLong(), any());
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    private NoteEntity createTestNote(Long id, Long userId, String title, String content,
                                       Integer isDeleted) {
        NoteEntity note = new NoteEntity();
        note.setId(id);
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setSummary(content != null && content.length() > 200
                ? content.substring(0, 200) + "..." : content);
        note.setIsDeleted(isDeleted);
        return note;
    }
}
