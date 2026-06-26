package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(3)
public class RetrievalStage implements RAGStage {

    private final VectorStore vectorStore;
    private final String collection;
    private final int topK;
    private final double scoreThreshold;

    public RetrievalStage(AIFacadeFactory factory,
                          @Value("${qdrant.collection}") String collection,
                          @Value("${note.retrieval.top-k:5}") int topK,
                          @Value("${note.retrieval.score-threshold:0.3}") double scoreThreshold) {
        this.vectorStore = factory.getVectorStore();
        this.collection = collection;
        this.topK = topK;
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    @MonitorStage("retrieval")
    public void process(RAGContext ctx) {
        Map<String, Object> filter = buildScopeFilter(
                ctx.getUserId(), ctx.getScopeType(), ctx.getScopeIds());

        List<VectorDoc> docs = vectorStore.search(
                collection, ctx.getQueryVector(), filter, topK);

        ctx.setRetrieved(docs);
        ctx.setFilteredDocs(docs.stream()
                .filter(d -> d.getScore() != null && d.getScore() >= scoreThreshold)
                .collect(Collectors.toList()));
    }

    private Map<String, Object> buildScopeFilter(Long userId, String scopeType,
                                                  List<Long> scopeIds) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("userId", userId);
        if ("NOTE".equals(scopeType) && scopeIds != null && scopeIds.size() == 1) {
            filter.put("noteId", scopeIds.get(0));
        }
        return filter;
    }
}
