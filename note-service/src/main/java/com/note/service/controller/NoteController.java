package com.note.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.service.common.vo.Result;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;  // 如果用 Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
@Validated
@Tag(name = "笔记管理", description = "笔记的增删改查与标签管理")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @Operation(summary = "创建笔记")
    public Result<Long> create(@RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        Long noteId = noteService.createNote(userId, dto);
        return Result.success(noteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取笔记详情")
    public Result<NoteEntity> detail(@PathVariable Long id,
                                     @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.getDetail(id, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新笔记")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        noteService.updateNote(id, userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除笔记（默认软删除，permanent=true 物理删除）")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam(defaultValue = "false") boolean permanent,
                               @AuthenticationPrincipal Long userId) {
        noteService.deleteNote(id, userId, permanent);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询笔记列表")
    public Result<Page<NoteEntity>> page(NoteQueryDTO query,
                                         @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.pageQuery(userId, query));
    }
}
