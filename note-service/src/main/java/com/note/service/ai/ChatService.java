package com.note.service.ai;

import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGPipeline;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.UserApiProfileEntity;
import com.note.service.entity.AiProviderEntity;
import com.note.service.service.AiProviderService;
import com.note.service.service.ConversationService;
import com.note.service.service.UserApiProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ChatService {

    private final AISettingService aiSettingService;
    private final AiProviderService aiProviderService;
    private final UserApiProfileService profileService;
    private final ConversationService conversationService;
    private final RAGPipeline pipeline;

    public ChatService(AISettingService aiSettingService,
                       AiProviderService aiProviderService,
                       UserApiProfileService profileService,
                       ConversationService conversationService,
                       RAGPipeline pipeline) {
        this.aiSettingService = aiSettingService;
        this.aiProviderService = aiProviderService;
        this.profileService = profileService;
        this.conversationService = conversationService;
        this.pipeline = pipeline;
    }

    /**
     * RAG 对话入口（新版：使用 Profile，支持指定模型）。
     * @param profileId        API Profile ID（非 null 则使用 Profile 制）
     * @param modelName        指定使用的模型名（如 deepseek-chat）
     */
    public Flux<com.note.service.ai.facade.ChatToken> ask(Long userId, String question,
                            String scopeType, List<Long> scopeIds, String style,
                            Long conversationId,
                            AtomicLong conversationIdOut,
                            String[] questionIdOut,
                            Long profileId, String modelName) {

        ChatAIConfig chatConfig;
        EmbedAIConfig embedConfig;

        if (profileId != null) {
            // 新版：从 Profile 构建配置
            UserApiProfileEntity profile = profileService.getById(profileId, userId);
            String apiKey = profileService.decryptApiKey(profile);
            if (apiKey == null || apiKey.isEmpty()) {
                return Flux.error(new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "Profile「" + profile.getProfileName() + "」未配置 API Key，请在设置中填写"));
            }
            String baseUrl = profile.getBaseUrl();
            String providerKey = profile.getProviderKey();

            AiProviderEntity provider = aiProviderService.getProvider(providerKey);
            String pluginType = provider.getPluginType();
            String actualModel = (modelName != null && !modelName.isEmpty()) ? modelName : "default";

            chatConfig = ChatAIConfig.builder()
                    .provider(providerKey).apiKey(apiKey).model(actualModel)
                    .baseUrl(baseUrl).pluginType(pluginType).build();

            // 用户显式配置的 Embedding Provider，未配置则直接拒绝
            embedConfig = resolveEmbedConfig(userId);
            if (embedConfig == null) {
                return Flux.error(new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "未配置 Embedding 服务商，请在 AI 设置中启用并选择 Embedding 服务"));
            }
        } else {
            // 旧版兼容：从 user_ai_config + ai_provider 构建
            var userConfig = aiSettingService.getByUserId(userId);
            if (userConfig == null) {
                return Flux.error(new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND));
            }
            embedConfig = aiProviderService.buildEmbedConfig(
                    userConfig.getEmbedProvider(), userConfig.getEmbedModel(),
                    userConfig.getEmbedUrl());
            chatConfig = aiProviderService.buildChatConfig(
                    userConfig.getChatProvider(), userConfig.getChatModel(),
                    userConfig.getChatUrl());
        }

        // 2. 对话管理
        Long cid = conversationId;
        String qid = UUID.randomUUID().toString().replace("-", "");
        List<Map<String, String>> history = List.of();

        if (cid != null) {
            history = conversationService.loadHistory(cid, userId, qid);
        } else {
            String title = question != null && question.length() > 50
                    ? question.substring(0, 50) : question;
            var conv = conversationService.createConversation(userId, title);
            cid = conv.getId();
        }
        conversationIdOut.set(cid);
        if (questionIdOut != null && questionIdOut.length > 0) {
            questionIdOut[0] = qid;
        }

        // 3. 组装上下文
        RAGContext ctx = new RAGContext();
        ctx.setUserId(userId);
        ctx.setQuestion(question);
        ctx.setScopeType(scopeType);
        ctx.setScopeIds(scopeIds);
        ctx.setStyle(style);
        ctx.setEmbedConfig(embedConfig);
        ctx.setChatConfig(chatConfig);
        ctx.setConversationId(cid);
        ctx.setConversationHistory(history);

        return pipeline.execute(ctx);
    }

    /**
     * 解析用户的 Embedding 配置。从 user_ai_config 读取用户显式选择的 Embedding Provider，
     * 未配置则返回 null，调用方应直接拒绝请求（不再回退到 Chat 厂商）。
     */
    private EmbedAIConfig resolveEmbedConfig(Long userId) {
        var userConfig = aiSettingService.getByUserId(userId);
        if (userConfig == null || userConfig.getEmbedProvider() == null
                || userConfig.getEmbedProvider().isEmpty()) {
            return null;
        }
        try {
            return aiProviderService.buildEmbedConfig(
                    userConfig.getEmbedProvider(),
                    userConfig.getEmbedModel(),
                    userConfig.getEmbedUrl());
        } catch (Exception e) {
            log.warn("用户 Embedding 配置无效，回退到默认: userId={}, error={}",
                    userId, e.getMessage());
            return null;
        }
    }
}
