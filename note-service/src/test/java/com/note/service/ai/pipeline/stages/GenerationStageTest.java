package com.note.service.ai.pipeline.stages;

import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.LLMFacade;
import com.note.service.ai.pipeline.RAGContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerationStage 单元测试")
class GenerationStageTest {

    @Mock
    private AIFacadeFactory factory;
    @Mock
    private LLMFacade llm;

    private GenerationStage stage;
    private RAGContext ctx;

    @BeforeEach
    void setUp() {
        stage = new GenerationStage(factory);
        ctx = new RAGContext();
        ctx.setChatConfig(ChatAIConfig.builder().provider("deepseek").pluginType("openai_compatible").build());
        ctx.setMessages(List.of(
                Map.of("role", "system", "content", "你是一个助手"),
                Map.of("role", "user", "content", "你好")
        ));

        when(factory.getLLM(eq("openai_compatible"))).thenReturn(llm);
        when(llm.streamChat(any(), any()))
                .thenReturn(Flux.just("你好", "世界"));
    }

    @Test
    @DisplayName("从 factory 获取正确的 LLM Provider")
    void testGetsLLMProviderFromFactory() {
        stage.process(ctx);
        verify(factory).getLLM(eq("openai_compatible"));
    }

    @Test
    @DisplayName("调用 llm.streamChat()")
    void testCallsLLMStreamChat() {
        stage.process(ctx);
        verify(llm).streamChat(any(), any());
    }

    @Test
    @DisplayName("ctx.responseStream 末尾包含 [DONE]")
    void testResponseStreamEndsWithDone() {
        stage.process(ctx);

        Flux<String> stream = ctx.getResponseStream();
        assertThat(stream).isNotNull();

        StepVerifier.create(stream)
                .expectNext("你好", "世界")
                .expectNext("[DONE]")
                .verifyComplete();
    }

    @Test
    @DisplayName("ctx.responseStream 不为空")
    void testResponseStreamIsNotNull() {
        stage.process(ctx);
        assertThat(ctx.getResponseStream()).isNotNull();
    }
}
