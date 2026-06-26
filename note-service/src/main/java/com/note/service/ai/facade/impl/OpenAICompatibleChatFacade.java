package com.note.service.ai.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.facade.LLMFacade;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleChatFacade implements LLMFacade {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> streamChat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = config.getBaseUrl();
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
                .concatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .map(line -> {
                    // 兼容有/无 "data: " 前缀的 SSE（OpenAI 有前缀，百炼等无前缀）
                    String t = line.trim();
                    if (t.startsWith("data: ")) t = t.substring(6);
                    return t;
                })
                .filter(data -> !data.isEmpty())
                .concatMap(data -> {
                    if ("[DONE]".equals(data)) {
                        return Flux.just("[DONE]");
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode choices = node.path("choices");
                        if (choices.isEmpty()) return Flux.empty();
                        JsonNode delta = choices.get(0).path("delta");

                        // 1) content — 最终输出文本。reasoning_content 是模型内心独白，不展示
                        JsonNode contentNode = delta.path("content");
                        if (!contentNode.isNull() && contentNode.isTextual() && !contentNode.asText().isEmpty()) {
                            return Flux.just(contentNode.asText());
                        }

                        // 2) 非流式回退：message.content
                        JsonNode message = choices.get(0).path("message");
                        JsonNode msgContent = message.path("content");
                        if (!msgContent.isNull() && msgContent.isTextual() && !msgContent.asText().isEmpty()) {
                            return Flux.just(msgContent.asText());
                        }

                        return Flux.empty();
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                })
                .onErrorMap(e -> !(e instanceof BusinessException), e -> {
                    if (e.getMessage() != null && e.getMessage().contains("401")) {
                        log.warn("LLM API Key 无效: url={}", baseUrl);
                        return new BusinessException(ErrorCode.AI_API_KEY_INVALID);
                    }
                    log.error("LLM 调用异常: {}", e.getMessage());
                    return new BusinessException(ErrorCode.AI_CHAT_FAILED,
                            "Chat 服务异常: " + e.getMessage());
                });
    }

    /**
     * 非流式 Chat（供测试连接等场景使用），内部复用流式实现收集完整回复。
     */
    public String chat(List<Map<String, String>> messages, ChatAIConfig config) {
        StringBuilder sb = new StringBuilder();
        streamChat(messages, config)
                .takeUntil("[DONE]"::equals)
                .filter(token -> !"[DONE]".equals(token))
                .doOnNext(sb::append)
                .then()
                .block();
        String result = sb.toString();
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CHAT_FAILED, "Chat 返回为空");
        }
        return result;
    }

    @Override
    public boolean supports(String provider) {
        return true;
    }
}
