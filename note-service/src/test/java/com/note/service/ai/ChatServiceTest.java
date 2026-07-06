package com.note.service.ai;

import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.ChatToken;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGPipeline;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.AiProviderEntity;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.entity.ConversationEntity;
import com.note.service.entity.UserApiProfileEntity;
import com.note.service.service.AiProviderService;
import com.note.service.service.ConversationService;
import com.note.service.service.UserApiProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 编排层单元测试")
class ChatServiceTest {

    @Mock private AISettingService aiSettingService;
    @Mock private AiProviderService aiProviderService;
    @Mock private UserApiProfileService profileService;
    @Mock private ConversationService conversationService;
    @Mock private RAGPipeline pipeline;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(aiSettingService, aiProviderService,
                profileService, conversationService, pipeline);
    }

    // ========================================
    // Profile 模式（新版）
    // ========================================

    @Nested
    @DisplayName("Profile 模式 (profileId != null)")
    class ProfileMode {

        @Test
        @DisplayName("正常流程：构建配置 → 创建对话 → 执行 RAG")
        void shouldOrchestrateWithProfile() {
            UserApiProfileEntity profile = profile("openai", "sk-xxx", "https://api.openai.com");
            AiProviderEntity provider = provider("openai", "openai_compatible");
            ConversationEntity conv = new ConversationEntity();
            conv.setId(10L);

            when(profileService.getById(1L, 100L)).thenReturn(profile);
            when(profileService.decryptApiKey(profile)).thenReturn("sk-xxx");
            when(aiProviderService.getProvider("openai")).thenReturn(provider);
            when(aiProviderService.getDefaultEmbedModel("openai")).thenReturn("text-embedding-3-small");
            when(conversationService.createConversation(eq(100L), anyString())).thenReturn(conv);
            when(pipeline.execute(any())).thenReturn(Flux.just(ChatToken.answer("回答"), ChatToken.DONE));

            Flux<ChatToken> result = chatService.ask(100L, "问题", "NOTE", List.of(1L),
                    "detailed", null, new AtomicLong(), new String[1], 1L, "gpt-4o");

            StepVerifier.create(result)
                    .assertNext(token -> assertThat(token.text()).isEqualTo("回答"))
                    .assertNext(token -> assertThat(token.isDone()).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Profile API Key 为空 → Flux.error AI_CONFIG_NOT_FOUND")
        void shouldFailWhenApiKeyEmpty() {
            UserApiProfileEntity profile = profile("openai", "", "https://api.openai.com");
            when(profileService.getById(1L, 100L)).thenReturn(profile);
            when(profileService.decryptApiKey(profile)).thenReturn("");

            Flux<ChatToken> result = chatService.ask(100L, "问题", "NOTE", List.of(),
                    "concise", null, new AtomicLong(), null, 1L, null);

            StepVerifier.create(result)
                    .expectErrorMatches(err -> err instanceof BusinessException
                            && ((BusinessException) err).getCode().equals(ErrorCode.AI_CONFIG_NOT_FOUND.getCode())
                            && ((BusinessException) err).getMessage().contains("未配置 API Key"))
                    .verify();
        }

        @Test
        @DisplayName("Profile API Key 解密结果为 null → Flux.error")
        void shouldFailWhenApiKeyDecryptNull() {
            UserApiProfileEntity profile = profile("openai", "encrypted", "https://api.openai.com");
            when(profileService.getById(1L, 100L)).thenReturn(profile);
            when(profileService.decryptApiKey(profile)).thenReturn(null);

            Flux<ChatToken> result = chatService.ask(100L, "问题", "NOTE", List.of(),
                    "concise", null, new AtomicLong(), null, 1L, null);

            StepVerifier.create(result)
                    .expectError(BusinessException.class)
                    .verify();
        }

        @Test
        @DisplayName("厂商未配置 Embedding 模型 → Flux.error")
        void shouldFailWhenNoEmbeddingModel() {
            UserApiProfileEntity profile = profile("openai", "sk-xxx", "https://api.openai.com");
            AiProviderEntity provider = provider("openai", "openai_compatible");

            when(profileService.getById(1L, 100L)).thenReturn(profile);
            when(profileService.decryptApiKey(profile)).thenReturn("sk-xxx");
            when(aiProviderService.getProvider("openai")).thenReturn(provider);
            when(aiProviderService.getDefaultEmbedModel("openai")).thenReturn(null);

            Flux<ChatToken> result = chatService.ask(100L, "问题", "NOTE", List.of(),
                    "concise", null, new AtomicLong(), null, 1L, null);

            StepVerifier.create(result)
                    .expectErrorMatches(err -> err instanceof BusinessException
                            && ((BusinessException) err).getMessage().contains("Embedding"))
                    .verify();
        }
    }

    // ========================================
    // Legacy 模式（旧版兼容）
    // ========================================

    @Nested
    @DisplayName("Legacy 模式 (profileId == null)")
    class LegacyMode {

        @Test
        @DisplayName("正常流程：从 user_ai_config 构建配置 → 执行 RAG")
        void shouldOrchestrateWithLegacyConfig() {
            UserAIConfigVO userConfig = new UserAIConfigVO();
            userConfig.setChatProvider("openai");
            userConfig.setChatModel("gpt-4o");
            userConfig.setChatUrl("https://api.openai.com");
            userConfig.setEmbedProvider("openai");
            userConfig.setEmbedModel("text-embedding-3-small");
            userConfig.setEmbedUrl("https://api.openai.com");
            userConfig.setIsEnabled(1);

            ChatAIConfig chatConfig = ChatAIConfig.builder()
                    .provider("openai").apiKey("sk-xxx").model("gpt-4o")
                    .baseUrl("https://api.openai.com").pluginType("openai_compatible").build();
            EmbedAIConfig embedConfig = EmbedAIConfig.builder()
                    .provider("openai").apiKey("sk-xxx").model("text-embedding-3-small")
                    .baseUrl("https://api.openai.com").pluginType("openai_compatible").build();

            ConversationEntity conv = new ConversationEntity();
            conv.setId(10L);

            when(aiSettingService.getByUserId(100L)).thenReturn(userConfig);
            when(aiProviderService.buildEmbedConfig("openai", "text-embedding-3-small", "https://api.openai.com"))
                    .thenReturn(embedConfig);
            when(aiProviderService.buildChatConfig("openai", "gpt-4o", "https://api.openai.com"))
                    .thenReturn(chatConfig);
            when(conversationService.createConversation(eq(100L), anyString())).thenReturn(conv);
            when(pipeline.execute(any())).thenReturn(Flux.just(ChatToken.answer("回答"), ChatToken.DONE));

            Flux<ChatToken> result = chatService.ask(100L, "问题", "ALL", List.of(),
                    "concise", null, new AtomicLong(), new String[1], null, null);

            StepVerifier.create(result)
                    .assertNext(token -> assertThat(token.text()).isEqualTo("回答"))
                    .assertNext(token -> assertThat(token.isDone()).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("user_ai_config 不存在 → Flux.error AI_CONFIG_NOT_FOUND")
        void shouldFailWhenNoLegacyConfig() {
            when(aiSettingService.getByUserId(100L)).thenReturn(null);

            Flux<ChatToken> result = chatService.ask(100L, "问题", "ALL", List.of(),
                    "concise", null, new AtomicLong(), null, null, null);

            StepVerifier.create(result)
                    .expectErrorMatches(err -> err instanceof BusinessException
                            && ((BusinessException) err).getCode().equals(ErrorCode.AI_CONFIG_NOT_FOUND.getCode()))
                    .verify();
        }
    }

    // ========================================
    // 对话管理
    // ========================================

    @Nested
    @DisplayName("对话管理")
    class ConversationManagement {

        @Test
        @DisplayName("conversationId == null → 自动创建新对话，标题截取前50字")
        void shouldCreateNewConversation() {
            setupProfileMode();
            ConversationEntity conv = new ConversationEntity();
            conv.setId(99L);
            when(conversationService.createConversation(eq(100L), anyString())).thenReturn(conv);
            when(pipeline.execute(any())).thenReturn(Flux.just(ChatToken.DONE));

            AtomicLong cidHolder = new AtomicLong();
            String[] qidHolder = new String[1];
            chatService.ask(100L, "这是一个非常长的".repeat(10) + "问题标题", "ALL", List.of(),
                    "concise", null, cidHolder, qidHolder, 1L, null);

            assertThat(cidHolder.get()).isEqualTo(99L);
            assertThat(qidHolder[0]).isNotNull();

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(conversationService).createConversation(eq(100L), titleCaptor.capture());
            assertThat(titleCaptor.getValue().length()).isLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("conversationId != null → 加载历史记录，复用对话")
        void shouldLoadExistingConversation() {
            setupProfileMode();
            List<Map<String, String>> history = List.of(
                    Map.of("role", "user", "content", "之前的问题"),
                    Map.of("role", "assistant", "content", "之前的回答")
            );
            when(conversationService.loadHistory(eq(5L), eq(100L), anyString())).thenReturn(history);
            when(pipeline.execute(any())).thenReturn(Flux.just(ChatToken.DONE));

            AtomicLong cidHolder = new AtomicLong(5L);
            String[] qidHolder = new String[1];
            chatService.ask(100L, "新问题", "NOTE", List.of(1L), "detailed",
                    5L, cidHolder, qidHolder, 1L, null);

            assertThat(cidHolder.get()).isEqualTo(5L);
            verify(conversationService, never()).createConversation(anyLong(), anyString());

            ArgumentCaptor<RAGContext> ctxCaptor = ArgumentCaptor.forClass(RAGContext.class);
            verify(pipeline).execute(ctxCaptor.capture());
            assertThat(ctxCaptor.getValue().getConversationId()).isEqualTo(5L);
            assertThat(ctxCaptor.getValue().getConversationHistory()).hasSize(2);
        }
    }

    // ========================================
    // RAGContext 组装正确性
    // ========================================

    @Nested
    @DisplayName("RAGContext 组装")
    class ContextAssembly {

        @Test
        @DisplayName("所有字段正确传递到 RAGContext")
        void shouldAssembleCompleteContext() {
            setupProfileMode();
            ConversationEntity conv = new ConversationEntity();
            conv.setId(42L);
            when(conversationService.createConversation(eq(100L), anyString())).thenReturn(conv);
            when(pipeline.execute(any())).thenReturn(Flux.just(ChatToken.DONE));

            chatService.ask(100L, "测试问题", "NOTE", List.of(1L, 2L),
                    "code-review", null, new AtomicLong(), new String[1], 1L, "gpt-4o");

            ArgumentCaptor<RAGContext> captor = ArgumentCaptor.forClass(RAGContext.class);
            verify(pipeline).execute(captor.capture());
            RAGContext ctx = captor.getValue();

            assertThat(ctx.getUserId()).isEqualTo(100L);
            assertThat(ctx.getQuestion()).isEqualTo("测试问题");
            assertThat(ctx.getScopeType()).isEqualTo("NOTE");
            assertThat(ctx.getScopeIds()).containsExactly(1L, 2L);
            assertThat(ctx.getStyle()).isEqualTo("code-review");
            assertThat(ctx.getConversationId()).isEqualTo(42L);
            assertThat(ctx.getChatConfig()).isNotNull();
            assertThat(ctx.getEmbedConfig()).isNotNull();
            assertThat(ctx.getChatConfig().getModel()).isEqualTo("gpt-4o");
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    private void setupProfileMode() {
        UserApiProfileEntity profile = profile("openai", "sk-xxx", "https://api.openai.com");
        AiProviderEntity provider = provider("openai", "openai_compatible");
        when(profileService.getById(1L, 100L)).thenReturn(profile);
        when(profileService.decryptApiKey(profile)).thenReturn("sk-xxx");
        when(aiProviderService.getProvider("openai")).thenReturn(provider);
        when(aiProviderService.getDefaultEmbedModel("openai")).thenReturn("text-embedding-3-small");
    }

    private UserApiProfileEntity profile(String providerKey, String apiKey, String baseUrl) {
        UserApiProfileEntity p = new UserApiProfileEntity();
        p.setId(1L);
        p.setUserId(100L);
        p.setProfileName("测试Profile");
        p.setProviderKey(providerKey);
        p.setApiKey(apiKey);
        p.setBaseUrl(baseUrl);
        return p;
    }

    private AiProviderEntity provider(String key, String pluginType) {
        AiProviderEntity p = new AiProviderEntity();
        p.setProviderKey(key);
        p.setName(key.toUpperCase());
        p.setPluginType(pluginType);
        return p;
    }
}
