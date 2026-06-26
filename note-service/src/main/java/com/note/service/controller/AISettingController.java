package com.note.service.controller;

import com.note.service.ai.AISettingService;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.dto.AISettingSaveDTO;
import com.note.service.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI 配置", description = "用户 AI 服务商配置管理")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class AISettingController {

    private final AISettingService aiSettingService;
    private final AiProviderService aiProviderService;
    private final AIFacadeFactory facadeFactory;

    @GetMapping("/ai-config")
    @Operation(summary = "获取 AI 配置")
    public Result<UserAIConfigVO> get(@AuthenticationPrincipal Long userId) {
        return Result.success(aiSettingService.getByUserId(userId));
    }

    @PostMapping("/ai-config")
    @Operation(summary = "保存 AI 配置")
    public Result<Void> save(@AuthenticationPrincipal Long userId,
                             @RequestBody @Valid AISettingSaveDTO dto) {
        aiSettingService.saveOrUpdate(userId, dto);
        return Result.success();
    }

    @DeleteMapping("/ai-config")
    @Operation(summary = "删除 AI 配置")
    public Result<Void> delete(@AuthenticationPrincipal Long userId) {
        aiSettingService.delete(userId);
        return Result.success();
    }

    @PostMapping("/ai-config/test-chat")
    @Operation(summary = "测试 Chat 连接")
    public Result<Map<String, Object>> testChat(@AuthenticationPrincipal Long userId) {
        var userConfig = aiSettingService.getByUserId(userId);
        if (userConfig == null) {
            return Result.error(ErrorCode.AI_CONFIG_NOT_FOUND.getCode(),
                    ErrorCode.AI_CONFIG_NOT_FOUND.getDefaultMessage());
        }
        ChatAIConfig config = aiProviderService.buildChatConfig(
                userConfig.getChatProvider(), userConfig.getChatModel(),
                userConfig.getChatUrl());

        var facade = facadeFactory.getLLM(config.getPluginType());
        if (!(facade instanceof com.note.service.ai.facade.impl.OpenAICompatibleChatFacade oaiChat)) {
            return Result.success(Map.of("success", true, "model", config.getModel(),
                    "baseUrl", config.getBaseUrl(), "note", "非 OpenAI 兼容格式，跳过快速测试"));
        }

        long t0 = System.currentTimeMillis();
        String reply = oaiChat.chat(
                List.of(Map.of("role", "user", "content", "hi")), config);
        long latency = System.currentTimeMillis() - t0;
        return Result.success(Map.of(
                "success", true,
                "model", config.getModel(),
                "baseUrl", config.getBaseUrl(),
                "latencyMs", latency,
                "reply", reply.length() > 200 ? reply.substring(0, 200) + "..." : reply
        ));
    }

    @PostMapping("/ai-config/test-embed")
    @Operation(summary = "测试 Embedding 连接")
    public Result<Map<String, Object>> testEmbed(@AuthenticationPrincipal Long userId) {
        var userConfig = aiSettingService.getByUserId(userId);
        if (userConfig == null) {
            return Result.error(ErrorCode.AI_CONFIG_NOT_FOUND.getCode(),
                    ErrorCode.AI_CONFIG_NOT_FOUND.getDefaultMessage());
        }
        EmbedAIConfig config = aiProviderService.buildEmbedConfig(
                userConfig.getEmbedProvider(), userConfig.getEmbedModel(),
                userConfig.getEmbedUrl());

        var facade = facadeFactory.getEmbedding(config.getPluginType());
        long t0 = System.currentTimeMillis();
        List<Float> vector = facade.embed("test", config);
        long latency = System.currentTimeMillis() - t0;
        return Result.success(Map.of(
                "success", true,
                "model", config.getModel(),
                "baseUrl", config.getBaseUrl(),
                "dimension", vector.size(),
                "latencyMs", latency
        ));
    }
}
