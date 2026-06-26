package com.note.service.ai.pipeline.stages;

import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class QueryRewriteStage implements RAGStage {

    @Override
    public void process(RAGContext ctx) {
        ctx.setRewrittenQuery(ctx.getQuestion());
    }

    @Override
    public boolean isEnabled(RAGContext ctx) {
        return false;
    }
}
