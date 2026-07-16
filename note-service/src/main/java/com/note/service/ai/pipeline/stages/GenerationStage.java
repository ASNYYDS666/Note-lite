package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.ChatToken;
import com.note.service.ai.facade.LLMFacade;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class GenerationStage implements RAGStage {

    private final AIFacadeFactory factory;

    @Override
    public void process(RAGContext ctx) {
        LLMFacade llm = factory.getLLM(ctx.getChatConfig().getPluginType());
        ctx.setResponseStream(
            llm.streamChat(ctx.getMessages(), ctx.getChatConfig())
                .doOnNext(token -> {
                    if (token.isDone()) {
                        log.info("RAG stream complete");
                    }
                })
                .concatWithValues(ChatToken.DONE)
        );
    }
}
