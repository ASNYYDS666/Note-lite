package com.note.service.controller;

import com.note.service.common.vo.Result;
import com.note.service.entity.AiModelEntity;
import com.note.service.entity.AiProviderEntity;
import com.note.service.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "AI 厂商管理", description = "查询厂商列表、模型列表、更新 API Key")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;

    @GetMapping("/providers")
    @Operation(summary = "列出所有已启用厂商（含模型）")
    public Result<List<ProviderVO>> listProviders() {
        List<AiProviderEntity> providers = providerService.listEnabledProviders();
        List<ProviderVO> vos = providers.stream()
                .map(p -> {
                    List<AiModelEntity> models = providerService.listModels(p.getProviderKey(), null);
                    List<ModelVO> chatModels = models.stream()
                            .filter(m -> "CHAT".equals(m.getModelType()))
                            .map(m -> new ModelVO(m.getModelName(), m.getIsDefault() == 1))
                            .collect(Collectors.toList());
                    List<ModelVO> embedModels = models.stream()
                            .filter(m -> "EMBEDDING".equals(m.getModelType()))
                            .map(m -> new ModelVO(m.getModelName(), m.getIsDefault() == 1))
                            .collect(Collectors.toList());
                    ProviderVO vo = new ProviderVO();
                    vo.setKey(p.getProviderKey());
                    vo.setName(p.getName());
                    vo.setPluginType(p.getPluginType());
                    vo.setHasApiKey(p.getApiKey() != null && !p.getApiKey().isEmpty());
                    vo.setBaseUrl(p.getBaseUrl());
                    vo.setChatModels(chatModels);
                    vo.setEmbedModels(embedModels);
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @GetMapping("/providers/{key}/models")
    @Operation(summary = "获取某厂商的模型列表")
    public Result<Map<String, List<ModelVO>>> listModels(
            @PathVariable String key,
            @RequestParam(required = false) String type) {
        List<AiModelEntity> models = providerService.listModels(key, type);
        Map<String, List<ModelVO>> grouped = models.stream()
                .collect(Collectors.groupingBy(
                        AiModelEntity::getModelType,
                        Collectors.mapping(
                                m -> new ModelVO(m.getModelName(), m.getIsDefault() == 1),
                                Collectors.toList()
                        )
                ));
        return Result.success(grouped);
    }

    @PutMapping("/providers/{key}/apikey")
    @Operation(summary = "更新厂商 API Key")
    public Result<Void> updateApiKey(@AuthenticationPrincipal Long userId,
                                     @PathVariable String key,
                                     @RequestBody Map<String, String> body) {
        providerService.updateApiKey(key, body.get("apiKey"));
        return Result.success();
    }

    // ==================== VO ====================

    @Data
    public static class ProviderVO {
        private String key;
        private String name;
        private String pluginType;
        private boolean hasApiKey;
        private String baseUrl;
        private List<ModelVO> chatModels;
        private List<ModelVO> embedModels;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelVO {
        private String modelName;
        private boolean isDefault;
    }
}
