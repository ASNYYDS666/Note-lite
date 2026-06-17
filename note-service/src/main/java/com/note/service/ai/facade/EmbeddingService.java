package com.note.service.ai.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/v1";
    private static final String OPENAI_URL = "https://api.openai.com/v1";

    public List<Float> embed(String text, EmbedAIConfig config) {
        return embedBatch(List.of(text), config).get(0);
    }

    public List<List<Float>> embedBatch(List<String> texts, EmbedAIConfig config) {
        String baseUrl = resolveBaseUrl(config);
        String fullUrl = baseUrl + "/embeddings";
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        log.info("Embedding 请求: url={}, model={}, textLen={}",
                fullUrl, config.getModel(), texts.get(0).length());

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
            log.info("Embedding 成功: model={}, dim={}", config.getModel(),
                    result.isEmpty() ? 0 : result.get(0).size());
            return result;

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("Embedding Key 无效: url={}, model={}, status=401",
                    fullUrl, config.getModel());
            throw new BusinessException(ErrorCode.AI_API_KEY_INVALID);
        } catch (WebClientResponseException.NotFound e) {
            log.error("Embedding 端点不存在 (404): url={}, model={} —— 可能服务商不支持 Embedding，或 URL 拼写错误",
                    fullUrl, config.getModel());
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "Embedding 端点不存在: " + fullUrl + " —— 请确认该服务商支持 Embedding API");
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Embedding API 返回错误: url={}, model={}, status={}, body={}",
                    fullUrl, config.getModel(), e.getStatusCode(), body);
            // Try to extract the error message from response body
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

    public String resolveBaseUrl(EmbedAIConfig config) {
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            return config.getBaseUrl();
        }
        if ("openai".equals(config.getProvider())) {
            return OPENAI_URL;
        }
        if ("deepseek".equals(config.getProvider())) {
            return DEEPSEEK_URL;
        }
        throw new BusinessException(ErrorCode.AI_PROVIDER_UNSUPPORTED);
    }
}
