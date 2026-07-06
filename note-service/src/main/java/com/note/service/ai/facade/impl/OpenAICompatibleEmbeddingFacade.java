package com.note.service.ai.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAICompatibleEmbeddingFacade implements EmbeddingFacade {

    private final ObjectMapper objectMapper;

    // 部分厂商 API 限制单次请求最多 N 条文本，超出需分批
    private static final int BATCH_SIZE = 100;
    // Embedding 响应可能很大（24条×1536维 ≈ 784KB），超过默认256KB限制
    private final WebClient largeBufferClient;

    public OpenAICompatibleEmbeddingFacade(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.largeBufferClient = webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    @Override
    public List<Float> embed(String text, EmbedAIConfig config) {
        return embedBatch(List.of(text), config).get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, EmbedAIConfig config) {
        if (texts.size() <= BATCH_SIZE) {
            return doEmbedBatch(texts, config);
        }
        List<List<Float>> all = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            all.addAll(doEmbedBatch(texts.subList(i, end), config));
        }
        return all;
    }

    private List<List<Float>> doEmbedBatch(List<String> texts, EmbedAIConfig config) {
        String baseUrl = ensureProtocol(config.getBaseUrl());
        String fullUrl = baseUrl + "/embeddings";
        WebClient client = largeBufferClient.mutate().baseUrl(baseUrl).build();

        log.info("Embedding 请求: url={}, model={}, textLen={}, batchSize={}",
                fullUrl, config.getModel(), texts.get(0).length(), texts.size());

        try {
            JsonNode response = client.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", config.getModel(),
                            "input", texts
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<List<Float>> result = new ArrayList<>();
            for (JsonNode item : response.get("data")) {
                List<Float> vec = new ArrayList<>();
                item.get("embedding").forEach(node -> vec.add((float) node.asDouble()));
                result.add(vec);
            }
            log.info("Embedding 成功: model={}, dim={}",
                    config.getModel(), result.isEmpty() ? 0 : result.get(0).size());
            return result;

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("Embedding Key 无效: url={}, model={}, status=401",
                    fullUrl, config.getModel());
            throw new BusinessException(ErrorCode.AI_API_KEY_INVALID);
        } catch (WebClientResponseException.NotFound e) {
            log.error("Embedding 端点不存在 (404): url={}, model={}",
                    fullUrl, config.getModel());
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "Embedding 端点不存在: " + fullUrl + " —— 请确认该服务商支持 Embedding API");
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Embedding API 返回错误: url={}, model={}, status={}, body={}",
                    fullUrl, config.getModel(), e.getStatusCode(), body);
            String detail = extractErrorDetail(body);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "Embedding 调用失败 [" + e.getStatusCode() + "]: " + detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding 网络异常: url={}, model={}, error={}",
                    fullUrl, config.getModel(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "Embedding 网络异常: " + e.getMessage());
        }
    }

    private String extractErrorDetail(String body) {
        if (body == null || body.isEmpty()) return "无详情";
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode error = node.path("error");
            if (!error.isMissingNode()) {
                String msg = error.path("message").asText();
                if (!msg.isEmpty()) return msg;
            }
        } catch (Exception ignored) {}
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    @Override
    public boolean supports(String provider) {
        return true;
    }

    private static String ensureProtocol(String url) {
        if (url == null || url.isEmpty()) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }
}
