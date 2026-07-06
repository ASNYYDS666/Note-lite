package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import com.note.service.ai.retrieval.KeywordRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(3)
public class RetrievalStage implements RAGStage {

    private final VectorStore vectorStore;
    private final KeywordRetriever keywordRetriever;
    private final String collection;
    private final int topK;
    private final double scoreThreshold;
    private final double vectorWeight;

    public RetrievalStage(AIFacadeFactory factory,
                          KeywordRetriever keywordRetriever,
                          @Value("${qdrant.collection}") String collection,
                          @Value("${note.retrieval.top-k:5}") int topK,
                          @Value("${note.retrieval.score-threshold:0.3}") double scoreThreshold) {
        this.vectorStore = factory.getVectorStore();
        this.keywordRetriever = keywordRetriever;
        this.collection = collection;
        this.topK = topK;
        this.scoreThreshold = scoreThreshold;
        // RRF weight: vector weight (keyword weight = 1 - vectorWeight)
        this.vectorWeight = 0.6;
    }

    @Override
    @MonitorStage("retrieval")
    public void process(RAGContext ctx) {
        Map<String, Object> filter = buildScopeFilter(
                ctx.getUserId(), ctx.getScopeType(), ctx.getScopeIds());

        // Double-fetch: ask Qdrant for more candidates so RRF has room to re-rank
        int fetchK = Math.max(topK * 2, 10);
        List<VectorDoc> vectorDocs = vectorStore.search(
                collection, ctx.getQueryVector(), filter, fetchK);

        // Keyword path: MySQL FULLTEXT as complementary signal
        String query = ctx.getRewrittenQuery() != null
                ? ctx.getRewrittenQuery() : ctx.getQuestion();
        Map<String, Float> keywordScores = keywordRetriever.search(query, ctx.getUserId(), fetchK);

        // RRF fusion: merge vector and keyword scores
        List<VectorDoc> merged = rrfMerge(vectorDocs, keywordScores, vectorWeight, topK);

        ctx.setRetrieved(merged);
        ctx.setFilteredDocs(merged.stream()
                .filter(d -> d.getScore() != null && d.getScore() >= scoreThreshold)
                .collect(Collectors.toList()));
    }

    /**
     * Reciprocal Rank Fusion: combine vector similarity scores with keyword BM25 scores.
     * Chunks that appear in both result sets get boosted.
     */
    private List<VectorDoc> rrfMerge(List<VectorDoc> vectorDocs, Map<String, Float> kwScores,
                                     double vecWeight, int resultTopK) {
        double kwWeight = 1.0 - vecWeight;

        List<VectorDoc> merged = new ArrayList<>(vectorDocs);
        Set<String> seenIds = vectorDocs.stream().map(VectorDoc::getId).collect(Collectors.toSet());

        // Add keyword-only chunks as new entries (not in vector results)
        for (Map.Entry<String, Float> entry : kwScores.entrySet()) {
            if (!seenIds.contains(entry.getKey())) {
                VectorDoc doc = new VectorDoc();
                doc.setId(entry.getKey());
                doc.setScore((double) entry.getValue());
                doc.setPayload(new LinkedHashMap<>());
                merged.add(doc);
            }
        }

        // Re-score: blend vector + keyword
        for (VectorDoc doc : merged) {
            float kwScore = kwScores.getOrDefault(doc.getId(), 0f);
            double vecScore = doc.getScore() != null ? doc.getScore() : 0.0;
            doc.setScore(vecWeight * vecScore + kwWeight * kwScore);
        }

        // Sort by blended score descending and return top-K
        merged.sort(Comparator.comparingDouble(VectorDoc::getScore).reversed());
        return merged.subList(0, Math.min(resultTopK, merged.size()));
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
