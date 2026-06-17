package com.note.service.ai;

import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.EmbeddingService;
import com.note.service.ai.facade.LLMService;
import com.note.service.ai.facade.QdrantVectorStore;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final AISettingService aiSettingService;
    private final EmbeddingService embeddingService;
    private final LLMService llmService;
    private final QdrantVectorStore qdrantVectorStore;
    private final String collection;
    private final int topK;
    private final double scoreThreshold;

    public ChatService(AISettingService aiSettingService,
                       EmbeddingService embeddingService,
                       LLMService llmService,
                       QdrantVectorStore qdrantVectorStore,
                       @Value("${qdrant.collection}") String collection,
                       @Value("${note.retrieval.top-k:5}") int topK,
                       @Value("${note.retrieval.score-threshold:0.3}") double scoreThreshold) {
        this.aiSettingService = aiSettingService;
        this.embeddingService = embeddingService;
        this.llmService = llmService;
        this.qdrantVectorStore = qdrantVectorStore;
        this.collection = collection;
        this.topK = topK;
        this.scoreThreshold = scoreThreshold;
    }

    public Flux<String> ask(Long userId, String question,
                            String scopeType, List<Long> scopeIds) {

        // Phase 1: Retrieve context (blocking — must complete before LLM prompt is built)
        EmbedAIConfig embedConfig;
        ChatAIConfig chatConfig;
        try {
            embedConfig = aiSettingService.getDecryptedEmbedConfig(userId);
            chatConfig = aiSettingService.getDecryptedChatConfig(userId);
        } catch (BusinessException e) {
            return Flux.error(e);
        }

        long t0 = System.currentTimeMillis();
        List<Float> queryVector;
        try {
            queryVector = embeddingService.embed(question, embedConfig);
        } catch (BusinessException e) {
            return Flux.error(e);
        }
        long embedMs = System.currentTimeMillis() - t0;

        Map<String, Object> filter = buildScopeFilter(userId, scopeType, scopeIds);
        long t1 = System.currentTimeMillis();
        List<VectorDoc> docs;
        try {
            docs = qdrantVectorStore.search(collection, queryVector, filter, topK);
        } catch (BusinessException e) {
            return Flux.error(e);
        }
        long searchMs = System.currentTimeMillis() - t1;

        // Filter low-score results
        List<VectorDoc> filteredDocs = docs.stream()
                .filter(d -> d.getScore() != null && d.getScore() >= scoreThreshold)
                .collect(Collectors.toList());
        log.info("RAG prep: embed={}ms, search={}ms, hits={}, filtered={}",
                embedMs, searchMs, docs.size(), filteredDocs.size());

        String prompt = buildPrompt(question, filteredDocs);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", prompt)
        );

        // Phase 2: Real streaming LLM — tokens flow to frontend as they arrive
        return llmService.streamChat(messages, chatConfig)
                .doOnNext(token -> {
                    if ("[DONE]".equals(token)) {
                        log.info("RAG stream complete");
                    }
                })
                .concatWithValues("[DONE]");
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

    private String buildPrompt(String question, List<VectorDoc> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下笔记片段回答问题。如果片段不足以回答，直接说'笔记中没有相关信息'。\n\n");

        if (docs.isEmpty()) {
            sb.append("（无相关笔记片段）\n");
        } else {
            sb.append("相关笔记片段：\n");
            for (int i = 0; i < docs.size(); i++) {
                VectorDoc doc = docs.get(i);
                sb.append("\n---\n");
                sb.append("[来源 ").append(i + 1).append(": ");
                sb.append(doc.getPayload().getOrDefault("title", "未知笔记")).append("]\n");
                sb.append(doc.getPayload().getOrDefault("text", ""));
                sb.append("\n");
            }
        }

        sb.append("\n问题：").append(question);
        return sb.toString();
    }

    private String buildSystemPrompt() {
        return "你是笔记助手。回答要准确、有条理。使用 Markdown 格式组织回答。";
    }
}
