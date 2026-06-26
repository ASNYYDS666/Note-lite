package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.prompt.PromptTemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PromptAssemblyStage 单元测试")
class PromptAssemblyStageTest {

    private static final int MAX_CHUNKS = 6;
    private static final int MAX_CHARS = 8000;

    private PromptTemplateEngine templateEngine;
    private PromptAssemblyStage stage;
    private RAGContext ctx;

    @BeforeEach
    void setUp() {
        templateEngine = mock(PromptTemplateEngine.class);
        when(templateEngine.render(anyString(), any())).thenReturn("渲染后的 Prompt");

        stage = new PromptAssemblyStage(templateEngine, MAX_CHUNKS, MAX_CHARS);

        ctx = new RAGContext();
        ctx.setQuestion("测试问题");
        ctx.setStyle("concise");
    }

    @Nested
    @DisplayName("System Prompt 风格")
    class SystemPrompt {

        @Test
        @DisplayName("style=concise → systemPrompt 包含'简洁'")
        void testConciseStyleSystemPrompt() {
            stage.process(ctx);
            assertThat(ctx.getSystemPrompt()).contains("简洁");
        }

        @Test
        @DisplayName("style=detailed → systemPrompt 包含'详细'")
        void testDetailedStyleSystemPrompt() {
            ctx.setStyle("detailed");
            stage.process(ctx);
            assertThat(ctx.getSystemPrompt()).contains("详细");
        }

        @Test
        @DisplayName("style=code-review → systemPrompt 包含'代码审查'")
        void testCodeReviewStyleSystemPrompt() {
            ctx.setStyle("code-review");
            stage.process(ctx);
            assertThat(ctx.getSystemPrompt()).contains("代码审查");
        }

        @Test
        @DisplayName("未知 style → 默认 detailed")
        void testUnknownStyleDefaultsToDetailed() {
            ctx.setStyle(null);
            stage.process(ctx);
            assertThat(ctx.getSystemPrompt()).contains("详细");
        }
    }

    @Nested
    @DisplayName("模板变量")
    class TemplateVariables {

        @Test
        @DisplayName("filteredDocs 非空 → hasChunks=true")
        void testHasChunksTrueWhenDocsPresent() {
            VectorDoc doc = new VectorDoc();
            doc.setPayload(Map.of("title", "笔记1", "text", "内容"));
            ctx.setFilteredDocs(List.of(doc));

            stage.process(ctx);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(templateEngine).render(eq("concise"), varsCaptor.capture());

            assertThat(varsCaptor.getValue()).containsEntry("hasChunks", true);
        }

        @Test
        @DisplayName("filteredDocs 为空 → hasChunks=false")
        void testHasChunksFalseWhenNoDocs() {
            ctx.setFilteredDocs(List.of());

            stage.process(ctx);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(templateEngine).render(eq("concise"), varsCaptor.capture());

            assertThat(varsCaptor.getValue()).containsEntry("hasChunks", false);
        }
    }

    @Nested
    @DisplayName("消息组装")
    class MessageAssembly {

        @Test
        @DisplayName("ctx.messages 含 system + user 两条消息")
        void testMessagesHasSystemAndUser() {
            stage.process(ctx);

            assertThat(ctx.getMessages()).hasSize(2);
        }

        @Test
        @DisplayName("messages 第一条 role=system")
        void testFirstMessageIsSystem() {
            stage.process(ctx);

            assertThat(ctx.getMessages().get(0)).containsEntry("role", "system");
        }

        @Test
        @DisplayName("messages 第二条 role=user")
        void testSecondMessageIsUser() {
            stage.process(ctx);

            assertThat(ctx.getMessages().get(1)).containsEntry("role", "user");
        }

        @Test
        @DisplayName("user message content 为模板渲染结果")
        void testUserMessageContentIsRenderedTemplate() {
            when(templateEngine.render(eq("concise"), any())).thenReturn("自定义渲染结果");

            stage.process(ctx);

            assertThat(ctx.getMessages().get(1)).containsEntry("content", "自定义渲染结果");
        }
    }
}
