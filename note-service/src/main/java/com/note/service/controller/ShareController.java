package com.note.service.controller;

import com.note.service.common.vo.Result;
import com.note.service.dto.ShareAccessDTO;
import com.note.service.dto.ShareCreateDTO;
import com.note.service.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping
    @Operation(summary = "生成分享码")
    public Result<Map<String, String>> createShare(@RequestBody @Valid ShareCreateDTO dto,
                                                   @AuthenticationPrincipal Long userId) {
        String code = shareService.generateShareCode(dto, userId);
        Map<String, String> result = new HashMap<>();
        result.put("code", code);
        return Result.success(result);
    }

    @GetMapping("/{code}")
    @Operation(summary = "通过分享码访问笔记（无需登录）")
    public Result<ShareAccessDTO> accessByCode(@PathVariable String code) {
        ShareAccessDTO access = shareService.accessByCode(code);
        return Result.success(access);
    }
}
