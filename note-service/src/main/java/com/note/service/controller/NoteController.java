package com.note.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.service.common.constant.CacheConstants;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.NoteDetailVO;
import com.note.service.common.vo.NoteTreeVO;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.service.NoteFolderService;
import com.note.service.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;  // 如果用 Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/note")
@RequiredArgsConstructor
@Validated
@Tag(name = "笔记管理", description = "笔记的增删改查与标签管理")
public class NoteController {


    private final NoteService noteService;
    private final NoteFolderService noteFolderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "创建笔记")
    public Result<Long> create(@RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        Long noteId = noteService.createNote(userId, dto);
        return Result.success(noteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取笔记详情")
    public Result<NoteDetailVO> detail(@Parameter(description = "笔记ID") @PathVariable Long id,
                                     @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.getDetailVO(id, userId));
    }


    @PutMapping("/{id}")
    @Operation(summary = "更新笔记")
    public Result<Void> update(@Parameter(description = "笔记ID") @PathVariable Long id,
                               @RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        noteService.updateNote(id, userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除笔记（默认软删除，permanent=true 物理删除）")
    public Result<Void> delete(@Parameter(description = "笔记ID") @PathVariable Long id,
                               @Parameter(description = "true=物理删除, false=软删除移入回收站") @RequestParam(defaultValue = "false") boolean permanent,
                               @AuthenticationPrincipal Long userId) {
        noteService.deleteNote(id, userId, permanent);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询笔记列表")
    public Result<Page<NoteDetailVO>> page(NoteQueryDTO query,
                                         @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.pageQuery(userId, query));
    }

    // ==================== 回收站接口 ====================

    @GetMapping("/recycle/page")
    @Operation(summary = "获取回收站列表")
    public Result<Page<NoteDetailVO>> recyclePage(NoteQueryDTO query,
                                                @AuthenticationPrincipal Long userId) {
        query.setIsDeleted(1);
        return Result.success(noteService.pageRecycle(userId, query));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "从回收站恢复笔记")
    public Result<Void> restore(@Parameter(description = "笔记ID") @PathVariable Long id,
                                @AuthenticationPrincipal Long userId) {
        noteService.restoreFromRecycle(id, userId);
        return Result.success();
    }

    @DeleteMapping("/recycle/clear")
    @Operation(summary = "清空回收站")
    public Result<Void> clearRecycle(@AuthenticationPrincipal Long userId) {
        noteService.clearRecycle(userId);
        return Result.success();
    }

    // ==================== 标签接口 ====================

    @GetMapping("/tags")
    @Operation(summary = "获取当前用户所有标签")
    public Result<List<String>> getUserTags(@AuthenticationPrincipal Long userId) {
        return Result.success(noteService.getUserTags(userId));
    }

    // ==================== 草稿接口（Jackson 版）====================

    /**
     * 构建草稿 Redis Key
     * 新建笔记草稿：note:draft:{userId}:new
     * 已有笔记草稿：note:draft:{userId}:{noteId}
     */
    private String buildDraftKey(Long userId, Long noteId) {
        if (noteId != null) {
            return CacheConstants.NOTE_DRAFT_PREFIX + userId + ":" + noteId;
        }
        // 新建笔记用固定后缀 "new"，避免多 tab 歧义时 key 混乱
        // 已知限制：同用户同时新建多篇时会覆盖，可接受（面试可讲）
        return CacheConstants.NOTE_DRAFT_PREFIX + userId + ":new";
    }

    @PostMapping("/draft")
    @Operation(summary = "自动保存草稿（3秒防抖后前端触发）")
    public Result<Void> saveDraft(@RequestBody NoteDTO dto,
                                  @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, dto.getId());
        try {
            String json = objectMapper.writeValueAsString(dto);
            stringRedisTemplate.opsForValue().set(key, json,
                    CacheConstants.ttlWithJitter(CacheConstants.NOTE_DRAFT_TTL_SECONDS), TimeUnit.SECONDS);
            log.debug("草稿已保存: key={}", key);
        } catch (JsonProcessingException e) {
            // 序列化失败不影响主流程，只记录日志
            log.error("草稿序列化失败: userId={}, noteId={}", userId, dto.getId(), e);
        }
        return Result.success();
    }

    @GetMapping("/draft")
    @Operation(summary = "获取草稿（进入编辑页时查询）")
    public Result<NoteDTO> getDraft(@Parameter(description = "笔记ID，不传则获取新建笔记草稿") @RequestParam(required = false) Long noteId,
                                    @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, noteId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null) {
            return Result.success(null);  // 无草稿，返回 null，前端正常处理
        }

        try {
            NoteDTO draft = objectMapper.readValue(json, NoteDTO.class);
            log.debug("草稿已读取: key={}", key);
            return Result.success(draft);
        } catch (JsonProcessingException e) {
            // 草稿数据损坏，删除并返回 null
            log.error("草稿反序列化失败，已清除: key={}", key, e);
            stringRedisTemplate.delete(key);
            return Result.success(null);
        }
    }

    @DeleteMapping("/draft")
    @Operation(summary = "清除草稿（保存成功后前端调用）")
    public Result<Void> clearDraft(@Parameter(description = "笔记ID，不传则清除新建笔记草稿") @RequestParam(required = false) Long noteId,
                                   @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, noteId);
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("草稿已清除: key={}, existed={}", key, deleted);
        return Result.success();
    }

    // ==================== 文件夹+笔记树接口 ====================

    @GetMapping("/tree")
    @Operation(summary = "获取文件夹+笔记树（驱动前端目录树）")
    public Result<NoteTreeVO> getNoteTree(@AuthenticationPrincipal Long userId) {
        return Result.success(noteService.getNoteTree(userId));
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "移动笔记到指定文件夹")
    public Result<Void> moveNote(@Parameter(description = "笔记ID") @PathVariable Long id,
                                  @Parameter(description = "目标文件夹ID，null表示根目录") @RequestParam(required = false) Long folderId,
                                  @AuthenticationPrincipal Long userId) {
        noteService.moveNote(id, userId, folderId);
        return Result.success();
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "重命名笔记（仅更新标题）")
    public Result<Void> renameNote(@Parameter(description = "笔记ID") @PathVariable Long id,
                                    @Parameter(description = "新标题") @RequestParam String title,
                                    @AuthenticationPrincipal Long userId) {
        noteService.renameNote(id, userId, title);
        return Result.success();
    }

    // ==================== 文件夹CRUD接口 ====================

    @PostMapping("/folder")
    @Operation(summary = "创建文件夹")
    public Result<Long> createFolder(@Parameter(description = "父文件夹ID，null表示根目录") @RequestParam(required = false) Long parentId,
                                     @Parameter(description = "文件夹名称") @RequestParam String name,
                                     @AuthenticationPrincipal Long userId) {
        Long folderId = noteFolderService.createFolder(userId, parentId, name);
        return Result.success(folderId);
    }

    @PutMapping("/folder/{id}")
    @Operation(summary = "重命名或移动文件夹")
    public Result<Void> updateFolder(@Parameter(description = "文件夹ID") @PathVariable Long id,
                                      @Parameter(description = "新名称(可选)") @RequestParam(required = false) String name,
                                      @Parameter(description = "目标父文件夹ID(可选)") @RequestParam(required = false) Long parentId,
                                      @AuthenticationPrincipal Long userId) {
        noteFolderService.updateFolder(userId, id, name, parentId);
        return Result.success();
    }

    @DeleteMapping("/folder/{id}")
    @Operation(summary = "删除文件夹（内部笔记移入回收站）")
    public Result<Void> deleteFolder(@Parameter(description = "文件夹ID") @PathVariable Long id,
                                      @AuthenticationPrincipal Long userId) {
        noteFolderService.deleteFolder(userId, id);
        return Result.success();
    }
}

