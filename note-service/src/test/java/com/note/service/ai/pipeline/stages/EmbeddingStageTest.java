package com.note.service.ai.pipeline.stages;

import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.ai.pipeline.RAGContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingStage 单元测试")
class EmbeddingStageTest {

    @Mock
    private AIFacadeFactory factory;
    @Mock
    private EmbeddingFacade embedding;

    private EmbeddingStage stage;
    private RAGContext ctx;

    @BeforeEach
    void setUp() {
        stage = new EmbeddingStage(factory);
        ctx = new RAGContext();
        ctx.setQuestion("测试问题");
        ctx.setEmbedConfig(EmbedAIConfig.builder().provider("deepseek").pluginType("openai_compatible").build());

        when(factory.getEmbedding(eq("openai_compatible"))).thenReturn(embedding);
        when(embedding.embed(any(), any())).thenReturn(List.of(0.1f, 0.2f, 0.3f));
    }

    @Test
    @DisplayName("从 factory 获取正确的 Embedding Provider")
    void testGetsEmbeddingProviderFromFactory() {
        stage.process(ctx);
        verify(factory).getEmbedding(eq("openai_compatible"));
    }

    @Test
    @DisplayName("process() 后 ctx.queryVector 非空")
    void testQueryVectorIsSet() {
        stage.process(ctx);
        assertThat(ctx.getQueryVector()).isNotNull().hasSize(3);
    }

    @Test
    @DisplayName("ctx 没有 rewrittenQuery 时使用 question")
    void testUsesQuestionWhenNoRewrittenQuery() {
        stage.process(ctx);
        verify(embedding).embed(eq("测试问题"), any());
    }

    @Test
    @DisplayName("有 rewrittenQuery 时优先使用")
    void testUsesRewrittenQueryWhenPresent() {
        ctx.setRewrittenQuery("改写的查询");
        stage.process(ctx);
        verify(embedding).embed(eq("改写的查询"), any());
    }
}
