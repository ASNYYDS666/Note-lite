package com.note.service.ai.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaEmbeddingFacade implements EmbeddingFacade {

    private final WebClient.Builder webClientBuilder;

    @Override
    public List<Float> embed(String text, EmbedAIConfig config) {
        return embedBatch(List.of(text), config).get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, EmbedAIConfig config) {
        String baseUrl = config.getBaseUrl();
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        try {
            JsonNode response = client.post()
                    .uri("/api/embeddings")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", config.getModel(),
                            "input", texts
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<List<Float>> results = new ArrayList<>();
            JsonNode embeddings = response.path("embeddings");
            for (JsonNode embedding : embeddings) {
                List<Float> vec = new ArrayList<>();
                embedding.forEach(node -> vec.add((float) node.asDouble()));
                results.add(vec);
            }
            log.info("Ollama Embedding 成功: model={}, count={}, dim={}",
                    config.getModel(), results.size(),
                    results.isEmpty() ? 0 : results.get(0).size());
            return results;

        } catch (Exception e) {
            log.error("Ollama Embedding 异常: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "Ollama Embedding 异常: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String provider) {
        return true;
    }
}
