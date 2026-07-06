package com.note.service.ai.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.facade.ChatToken;
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
    public Flux<ChatToken> streamChat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = ensureProtocol(config.getBaseUrl());
        String model = config.getModel();
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        log.info("Chat 请求: url={}/chat/completions, model={}", baseUrl, model);

        final java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger lineCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

        return client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .bodyValue(Map.of(
                        "model", model,
                        "messages", messages,
                        "stream", true,
                        "max_tokens", 4096,
                        "temperature", 1.0,
                        "top_p", 0.8
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawChunk -> {
                    int n = chunkCount.incrementAndGet();
                    String preview = rawChunk.length() > 300 ? rawChunk.substring(0, 300) + "..." : rawChunk;
                    log.info("Chat chunk#{} (len={}): {}", n, rawChunk.length(), preview);
                })
                .concatMap(chunk -> Flux.fromArray(chunk.split("\n", -1)))
                .map(line -> {
                    String t = line.trim();
                    if (t.startsWith("data: ")) t = t.substring(6);
                    if (t.startsWith("{") && (t.contains("\"error\"") || t.contains("\"code\""))) {
                        log.error("Chat API 返回错误: {}", t);
                    }
                    return t;
                })
                .filter(data -> !data.isEmpty())
                .concatMap(data -> {
                    if ("[DONE]".equals(data)) {
                        return Flux.just(ChatToken.DONE);
                    }
                    lineCount.incrementAndGet();
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode choices = node.path("choices");
                        if (choices.isEmpty()) {
                            return Flux.empty();
                        }
                        JsonNode delta = choices.get(0).path("delta");

                        // 1) content — 模型正式回答
                        JsonNode contentNode = delta.path("content");
                        if (!contentNode.isNull() && contentNode.isTextual() && !contentNode.asText().isEmpty()) {
                            return Flux.just(ChatToken.answer(contentNode.asText()));
                        }

                        // 2) reasoning_content — 推理模型的思考过程
                        JsonNode reasoningNode = delta.path("reasoning_content");
                        if (!reasoningNode.isNull() && reasoningNode.isTextual() && !reasoningNode.asText().isEmpty()) {
                            return Flux.just(ChatToken.think(reasoningNode.asText()));
                        }

                        // 3) 非流式回退：message.content
                        JsonNode message = choices.get(0).path("message");
                        JsonNode msgContent = message.path("content");
                        if (!msgContent.isNull() && msgContent.isTextual() && !msgContent.asText().isEmpty()) {
                            return Flux.just(ChatToken.answer(msgContent.asText()));
                        }

                        // 空 delta，静默跳过
                        return Flux.empty();
                    } catch (Exception e) {
                        log.warn("Chat SSE JSON 解析失败: {}", data.length() > 200 ? data.substring(0, 200) + "..." : data);
                        return Flux.empty();
                    }
                })
                .doOnNext(t -> { if (!t.isDone()) tokenCount.incrementAndGet(); })
                .doOnComplete(() -> {
                    int n = tokenCount.get();
                    log.info("Chat 完成: model={}, lines={}, chunks={}, tokenCount={}",
                            model, lineCount.get(), chunkCount.get(), n);
                    if (n == 0) {
                        log.warn("Chat 返回空响应: url={}/chat/completions, model={}", baseUrl, model);
                    }
                })
                .onErrorMap(e -> !(e instanceof BusinessException), e -> {
                    if (e.getMessage() != null && e.getMessage().contains("401")) {
                        log.warn("LLM API Key 无效: url={}", baseUrl);
                        return new BusinessException(ErrorCode.AI_API_KEY_INVALID);
                    }
                    log.error("LLM 调用异常: url={}/chat/completions, model={}, error={}",
                            baseUrl, model, e.getMessage());
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
                .takeUntil(ChatToken::isDone)
                .filter(t -> !t.isDone())
                .doOnNext(t -> sb.append(t.text()))
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

    private static String ensureProtocol(String url) {
        if (url == null || url.isEmpty()) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }
}
