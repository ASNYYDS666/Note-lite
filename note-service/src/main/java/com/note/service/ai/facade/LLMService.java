package com.note.service.ai.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/v1";
    private static final String OPENAI_URL = "https://api.openai.com/v1";

    public Flux<String> streamChat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = resolveBaseUrl(config);
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        return client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "model", config.getModel(),
                        "messages", messages,
                        "stream", true
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> log.debug("LLM raw chunk: {}", chunk.substring(0, Math.min(200, chunk.length()))))
                .flatMap(chunk -> reactor.core.publisher.Flux.fromArray(
                        chunk.replace("\r", "").split("\n")))
                .filter(line -> !line.trim().isEmpty())
                .doOnNext(line -> log.debug("LLM line: {}", line.substring(0, Math.min(200, line.length()))))
                .onErrorMap(WebClientResponseException.Unauthorized.class, e -> {
                    log.warn("LLM API Key 无效: provider={}", config.getProvider());
                    return new BusinessException(ErrorCode.AI_API_KEY_INVALID);
                })
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("LLM API 返回错误: status={}", e.getStatusCode());
                    return new BusinessException(ErrorCode.AI_CHAT_FAILED,
                            "Chat 服务返回异常: " + e.getStatusCode());
                })
                .onErrorMap(e -> !(e instanceof BusinessException), e -> {
                    log.error("LLM 调用异常: {}", e.getMessage());
                    return new BusinessException(ErrorCode.AI_CHAT_FAILED);
                })
                .filter(line -> line.startsWith("data: "))
                .map(line -> {
                    String jsonStr = line.substring(6).trim();
                    if ("[DONE]".equals(jsonStr)) {
                        return "[DONE]";
                    }
                    try {
                        JsonNode node = objectMapper.readTree(jsonStr);
                        JsonNode choices = node.path("choices");
                        if (choices.isEmpty()) {
                            log.warn("SSE: choices 为空, line={}", line.substring(0, 100));
                            return "";
                        }
                        JsonNode delta = choices.get(0).path("delta");
                        JsonNode content = delta.path("content");
                        if (content.isMissingNode()) {
                            log.warn("SSE: content missing, delta={}", delta);
                            return "";
                        }
                        String text = content.asText();
                        if (!text.isEmpty()) {
                            log.debug("SSE token: {}", text);
                        }
                        return text;
                    } catch (Exception e) {
                        log.warn("SSE 解析失败: {} raw={}", e.getMessage(), jsonStr.substring(0, 80));
                        return "";
                    }
                })
                .filter(token -> !token.isEmpty());
    }

    public String chat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = resolveBaseUrl(config);
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", config.getModel(),
                            "messages", messages,
                            "stream", false
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response.path("choices").get(0)
                    .path("message").path("content").asText();

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("LLM API Key 无效: provider={}", config.getProvider());
            throw new BusinessException(ErrorCode.AI_API_KEY_INVALID);
        } catch (WebClientResponseException e) {
            log.error("LLM API 返回错误: status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.AI_CHAT_FAILED,
                    "Chat 服务返回异常: " + e.getStatusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 调用异常: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_CHAT_FAILED);
        }
    }

    private String resolveBaseUrl(ChatAIConfig config) {
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
