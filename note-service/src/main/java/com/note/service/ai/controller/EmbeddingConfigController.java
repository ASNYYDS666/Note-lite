package com.note.service.ai.controller;

import com.note.service.ai.AISettingService;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.entity.AiModelEntity;
import com.note.service.entity.AiProviderEntity;
import com.note.service.service.AiProviderService;
import com.note.service.service.UserApiProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Embedding 配置管理", description = "用户独立选择 Embedding 服务商，与 Chat 解耦")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class EmbeddingConfigController {

    private final AISettingService aiSettingService;
    private final AiProviderService aiProviderService;
    private final UserApiProfileService profileService;

    @GetMapping("/embedding-config")
    @Operation(summary = "获取当前用户的 Embedding 配置")
    public Result<Map<String, Object>> getConfig(@AuthenticationPrincipal Long userId) {
        UserAIConfigVO config = aiSettingService.getEmbeddingConfig(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("embedProvider", config != null ? config.getEmbedProvider() : null);
        result.put("embedModel", config != null ? config.getEmbedModel() : null);
        result.put("embedUrl", config != null ? config.getEmbedUrl() : null);
        return Result.success(result);
    }

    @PutMapping("/embedding-config")
    @Operation(summary = "保存 Embedding 配置")
    public Result<Void> saveConfig(@AuthenticationPrincipal Long userId,
                                    @RequestBody EmbeddingConfigRequest req) {
        aiSettingService.saveEmbeddingConfig(userId,
                req.getEmbedProvider(), req.getEmbedModel(), req.getEmbedUrl());
        return Result.success();
    }

    @GetMapping("/embedding-providers")
    @Operation(summary = "获取可用的 Embedding Provider 列表（本地 + 已配置 Key 的远程厂商）")
    public Result<List<Map<String, Object>>> getEmbeddingProviders() {
        var grouped = aiProviderService.listModelsGroupedByProvider();
        List<Map<String, Object>> result = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            List<AiModelEntity> embedModels = entry.getValue().stream()
                    .filter(m -> "EMBEDDING".equals(m.getModelType()))
                    .toList();
            if (embedModels.isEmpty()) continue;

            AiProviderEntity provider = aiProviderService.getProvider(entry.getKey());
            boolean isLocal = "local-embedding".equals(provider.getProviderKey());
            boolean hasApiKey = provider.getApiKey() != null && !provider.getApiKey().isEmpty();

            // 只展示本地 Embedding 或已配置 API Key 的远程厂商
            if (!isLocal && !hasApiKey) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", provider.getProviderKey());
            item.put("name", provider.getName());
            item.put("isLocal", isLocal);
            item.put("models", embedModels.stream().map(m -> Map.of(
                    "modelName", m.getModelName(),
                    "isDefault", m.getIsDefault() == 1
            )).toList());
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/embedding-config/test")
    @Operation(summary = "测试 Embedding 连接")
    public Result<Map<String, Object>> testConnection(@AuthenticationPrincipal Long userId,
                                                       @RequestBody TestEmbedRequest req) {
        return Result.success(profileService.testEmbedding(
                req.getProviderKey(), null, req.getBaseUrl()));
    }

    @Data
    public static class EmbeddingConfigRequest {
        private String embedProvider;
        private String embedModel;
        private String embedUrl;
    }

    @Data
    public static class TestEmbedRequest {
        private String providerKey;
        private String baseUrl;
    }
}
