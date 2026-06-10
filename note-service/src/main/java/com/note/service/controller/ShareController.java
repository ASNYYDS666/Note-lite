package com.note.service.controller;

import com.note.service.common.vo.Result;
import com.note.service.common.vo.ShareCodeVO;
import com.note.service.dto.ShareAccessDTO;
import com.note.service.dto.ShareCreateDTO;
import com.note.service.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "分享管理", description = "生成分享码、通过分享码访问笔记")
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping
    @Operation(summary = "生成分享码")
    public Result<ShareCodeVO> createShare(@RequestBody @Valid ShareCreateDTO dto,
                                                   @AuthenticationPrincipal Long userId) {
        String code = shareService.generateShareCode(dto, userId);
        ShareCodeVO result = new ShareCodeVO();
        result.setShareCode(code);
        return Result.success(result);
    }

    @GetMapping("/{code}")
    @Operation(summary = "通过分享码访问笔记（无需登录）")
    public Result<ShareAccessDTO> accessByCode(@Parameter(description = "分享码") @PathVariable String code) {
        ShareAccessDTO access = shareService.accessByCode(code);
        return Result.success(access);
    }
}
