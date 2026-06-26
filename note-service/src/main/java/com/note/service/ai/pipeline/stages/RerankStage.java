package com.note.service.ai.pipeline.stages;

import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class RerankStage implements RAGStage {

    @Override
    public void process(RAGContext ctx) {
        // 透传：不做重排序
    }

    @Override
    public boolean isEnabled(RAGContext ctx) {
        return false;
    }
}
