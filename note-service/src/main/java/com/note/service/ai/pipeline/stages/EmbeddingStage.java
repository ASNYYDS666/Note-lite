package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class EmbeddingStage implements RAGStage {

    private final AIFacadeFactory factory;

    @Override
    @MonitorStage("embedding")
    public void process(RAGContext ctx) {
        EmbeddingFacade embedding = factory.getEmbedding(
                ctx.getEmbedConfig().getPluginType());
        String query = ctx.getRewrittenQuery() != null
                ? ctx.getRewrittenQuery() : ctx.getQuestion();
        ctx.setQueryVector(embedding.embed(query, ctx.getEmbedConfig()));
    }
}
