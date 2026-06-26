package com.note.service.controller;

import com.note.service.ai.AISettingService;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.entity.UserApiProfileEntity;
import com.note.service.service.UserApiProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "API Profile 管理", description = "用户 API Key Profile 的增删改查 + 远程模型发现")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class ProfileController {

    private final UserApiProfileService profileService;
    private final AISettingService aiSettingService;

    // ==================== Profile CRUD ====================

    @GetMapping("/profiles")
    @Operation(summary = "获取用户所有 Profile")
    public Result<List<UserApiProfileEntity>> list(@AuthenticationPrincipal Long userId) {
        return Result.success(profileService.listByUser(userId));
    }

    @PostMapping("/profiles")
    @Operation(summary = "创建或更新 Profile")
    public Result<UserApiProfileEntity> save(@AuthenticationPrincipal Long userId,
                                              @RequestBody ProfileSaveRequest req) {
        UserApiProfileEntity entity = profileService.saveOrUpdate(
                userId, req.getId(), req.getProfileName(), req.getProviderKey(),
                req.getApiKey(), req.getBaseUrl(), req.getEnabledModels());
        return Result.success(entity);
    }

    @DeleteMapping("/profiles/{id}")
    @Operation(summary = "删除 Profile")
    public Result<Void> delete(@AuthenticationPrincipal Long userId,
                                @Parameter(description = "Profile ID") @PathVariable Long id) {
        profileService.delete(id, userId);
        return Result.success();
    }

    // ==================== 远程模型刷新 ====================

    @PostMapping("/profiles/migrate-legacy")
    @Operation(summary = "将旧版 user_ai_config 迁移为 Profile（API Key 需用户重新输入）")
    public Result<UserApiProfileEntity> migrateLegacy(@AuthenticationPrincipal Long userId) {
        UserAIConfigVO legacy = aiSettingService.getByUserId(userId);
        if (legacy == null) {
            return Result.error(404, "没有旧配置可迁移");
        }

        List<String> models = new java.util.ArrayList<>();
        if (legacy.getChatModel() != null) models.add(legacy.getChatModel());
        if (legacy.getEmbedModel() != null) models.add(legacy.getEmbedModel());

        // Create Profile without API key — user enters it in editor
        UserApiProfileEntity profile = profileService.saveOrUpdate(
                userId, null,
                legacy.getChatProvider(),
                legacy.getChatProvider(),
                "",  // empty — user re-enters in Profile editor
                legacy.getChatUrl(),
                models);
        return Result.success(profile);
    }

    @PostMapping("/profiles/refresh-models")
    @Operation(summary = "调用远程 /v1/models 发现模型")
    public Result<List<Map<String, Object>>> refreshModels(
            @AuthenticationPrincipal Long userId,
            @RequestBody RefreshModelsRequest req) {
        return Result.success(profileService.refreshRemoteModels(req.getBaseUrl(), req.getApiKey()));
    }

    // ==================== 请求体 ====================

    @Data
    public static class ProfileSaveRequest {
        private Long id;
        private String profileName;
        private String providerKey;
        private String apiKey;
        private String baseUrl;
        private List<String> enabledModels;
    }

    @Data
    public static class RefreshModelsRequest {
        private String baseUrl;
        private String apiKey;
    }

    @PostMapping("/profiles/test-embed")
    @Operation(summary = "测试 Embedding 连接（从 ai_model 表自动识别 embedding 模型）")
    public Result<Map<String, Object>> testEmbed(@AuthenticationPrincipal Long userId,
                                                  @RequestBody TestEmbedRequest req) {
        return Result.success(profileService.testEmbedding(
                req.getProviderKey(), req.getApiKey(), req.getBaseUrl()));
    }

    @Data
    public static class TestEmbedRequest {
        private String providerKey;
        private String apiKey;
        private String baseUrl;
    }
}
