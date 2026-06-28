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
     * RAG 对话入口（旧版兼容：使用 user_ai_config 表）。
     */
    public Flux<com.note.service.ai.facade.ChatToken> ask(Long userId, String question,
                            String scopeType, List<Long> scopeIds, String style,
                            Long conversationId,
                            AtomicLong conversationIdOut,
                            String[] questionIdOut) {
        return ask(userId, question, scopeType, scopeIds, style, conversationId,
                conversationIdOut, questionIdOut, null, null);
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

            // Embedding 使用厂商预置的默认 embedding 模型（无需用户手动选择）
            String embedModel = aiProviderService.getDefaultEmbedModel(providerKey);
            if (embedModel == null) {
                return Flux.error(new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "厂商「" + provider.getName() + "」未配置 Embedding 模型，无法进行知识检索"));
            }
            embedConfig = EmbedAIConfig.builder()
                    .provider(providerKey).apiKey(apiKey).model(embedModel)
                    .baseUrl(baseUrl).pluginType(pluginType).build();
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
}
