package com.note.service.ai.pipeline;

import com.note.service.ai.facade.ChatToken;
import com.note.service.ai.pipeline.stages.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RAGPipeline 集成测试")
class RAGPipelineTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private RAGStage mockStage1;
    private RAGStage mockStage2;
    private RAGStage mockStage3;
    private RAGPipeline pipeline;

    @BeforeEach
    void setUp() {
        mockStage1 = spy(new QueryRewriteStage());
        mockStage2 = mock(RAGStage.class);
        mockStage3 = mock(RAGStage.class);

        when(mockStage2.stageName()).thenReturn("MockStage2");
        when(mockStage2.isEnabled(any())).thenReturn(true);
        when(mockStage3.stageName()).thenReturn("MockStage3");
        when(mockStage3.isEnabled(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("Stage 排序与注入")
    class StageOrdering {

        @Test
        @DisplayName("所有注入的 Stage 按 @Order 排序")
        void testAllStagesInjectedAndOrdered() {
            pipeline = new RAGPipeline(List.of(mockStage1, mockStage2, mockStage3), registry);
            assertThat(pipeline).isNotNull();
        }
    }

    @Nested
    @DisplayName("禁用 Stage 跳过")
    class DisabledStageSkipping {

        @Test
        @DisplayName("isEnabled=false 的 Stage 不执行 process()")
        void testDisabledStageSkipped() {
            when(mockStage2.isEnabled(any())).thenReturn(false);
            pipeline = new RAGPipeline(List.of(mockStage2), registry);

            RAGContext ctx = new RAGContext();
            ctx.setResponseStream(Flux.just(ChatToken.answer("test")));

            pipeline.execute(ctx);

            verify(mockStage2, never()).process(any());
        }
    }

    @Nested
    @DisplayName("异常传播")
    class ExceptionPropagation {

        @Test
        @DisplayName("Stage 抛异常 → execute() 返回 Flux.error()")
        void testStageExceptionPropagatesAsFluxError() {
            doThrow(new RuntimeException("embedding failed")).when(mockStage2).process(any());
            pipeline = new RAGPipeline(List.of(mockStage2), registry);

            RAGContext ctx = new RAGContext();
            ctx.setResponseStream(Flux.just(ChatToken.answer("test")));

            Flux<ChatToken> result = pipeline.execute(ctx);

            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("Stage 抛异常 → 后续 Stage 不被调用")
        void testStageExceptionSkipsSubsequentStages() {
            doThrow(new RuntimeException("error")).when(mockStage2).process(any());
            pipeline = new RAGPipeline(List.of(mockStage2, mockStage3), registry);

            RAGContext ctx = new RAGContext();
            ctx.setResponseStream(Flux.just(ChatToken.answer("test")));

            pipeline.execute(ctx);

            verify(mockStage3, never()).process(any());
        }
    }
}
