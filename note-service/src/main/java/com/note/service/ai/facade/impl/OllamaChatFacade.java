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
public class OllamaChatFacade implements LLMFacade {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<ChatToken> streamChat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = config.getBaseUrl();
        String model = config.getModel();
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        log.info("Ollama Chat 请求: url={}/api/chat, model={}", baseUrl, model);

        final java.util.concurrent.atomic.AtomicInteger tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

        return client.post()
                .uri("/api/chat")
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "model", config.getModel(),
                        "messages", messages,
                        "stream", true,
                        "max_tokens", 4096
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .concatMap(chunk -> Flux.fromArray(chunk.replace("\r", "").split("\n", -1)))
                .filter(line -> !line.trim().isEmpty())
                .map(line -> {
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        if (node.has("done") && node.get("done").asBoolean()) {
                            return ChatToken.DONE;
                        }
                        if (node.has("error")) {
                            String msg = node.get("error").asText();
                            log.error("Ollama Chat API 返回错误: {}", msg);
                            throw new BusinessException(ErrorCode.AI_CHAT_FAILED, "Ollama: " + msg);
                        }
                        JsonNode message = node.path("message");
                        JsonNode content = message.path("content");
                        String text = content.isMissingNode() ? "" : content.asText();
                        return text.isEmpty() ? ChatToken.answer("") : ChatToken.answer(text);
                    } catch (Exception e) {
                        log.warn("Ollama Chat JSON 解析失败: {}", line.length() > 200 ? line.substring(0, 200) + "..." : line);
                        return ChatToken.answer("");
                    }
                })
                .filter(t -> !t.text().isEmpty() || t.isDone())
                .doOnNext(t -> { if (!t.isDone()) tokenCount.incrementAndGet(); })
                .doOnComplete(() -> log.info("Ollama Chat 完成: model={}, tokenCount={}", model, tokenCount.get()))
                .onErrorMap(e -> !(e instanceof BusinessException), e -> {
                    log.error("Ollama Chat 异常: url={}/api/chat, model={}, error={}",
                            baseUrl, model, e.getMessage());
                    return new BusinessException(ErrorCode.AI_CHAT_FAILED,
                            "Ollama 对话异常: " + e.getMessage());
                });
    }

    @Override
    public boolean supports(String provider) {
        return true;
    }
}
