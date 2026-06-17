package com.note.service.controller;

import com.note.service.ai.AISettingService;
import com.note.service.ai.facade.EmbeddingService;
import com.note.service.ai.facade.LLMService;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.dto.AISettingSaveDTO;
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
    private final LLMService llmService;
    private final EmbeddingService embeddingService;

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
        ChatAIConfig config = aiSettingService.getDecryptedChatConfig(userId);
        long t0 = System.currentTimeMillis();
        String reply = llmService.chat(
                List.of(Map.of("role", "user", "content", "hi")),
                config);
        long latency = System.currentTimeMillis() - t0;
        return Result.success(Map.of(
                "success", true,
                "model", config.getModel(),
                "baseUrl", config.getBaseUrl() != null ? config.getBaseUrl()
                        : ("openai".equals(config.getProvider())
                            ? "https://api.openai.com/v1" : "https://api.deepseek.com/v1"),
                "latencyMs", latency,
                "reply", reply.length() > 200 ? reply.substring(0, 200) + "..." : reply
        ));
    }

    @PostMapping("/ai-config/test-embed")
    @Operation(summary = "测试 Embedding 连接")
    public Result<Map<String, Object>> testEmbed(@AuthenticationPrincipal Long userId) {
        EmbedAIConfig config = aiSettingService.getDecryptedEmbedConfig(userId);
        long t0 = System.currentTimeMillis();
        List<Float> vector = embeddingService.embed("test", config);
        long latency = System.currentTimeMillis() - t0;
        return Result.success(Map.of(
                "success", true,
                "model", config.getModel(),
                "baseUrl", config.getBaseUrl() != null ? config.getBaseUrl()
                        : ("openai".equals(config.getProvider())
                            ? "https://api.openai.com/v1" : "https://api.deepseek.com/v1"),
                "dimension", vector.size(),
                "latencyMs", latency
        ));
    }
}
