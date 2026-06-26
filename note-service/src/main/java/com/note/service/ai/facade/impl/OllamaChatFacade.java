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
public class OllamaChatFacade implements LLMFacade {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> streamChat(List<Map<String, String>> messages, ChatAIConfig config) {
        String baseUrl = config.getBaseUrl();
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        return client.post()
                .uri("/api/chat")
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "model", config.getModel(),
                        "messages", messages,
                        "stream", true
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> reactor.core.publisher.Flux.fromArray(
                        chunk.replace("\r", "").split("\n")))
                .filter(line -> !line.trim().isEmpty())
                .map(line -> {
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        if (node.has("done") && node.get("done").asBoolean()) {
                            return "[DONE]";
                        }
                        JsonNode message = node.path("message");
                        JsonNode content = message.path("content");
                        return content.isMissingNode() ? "" : content.asText();
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(token -> !token.isEmpty())
                .onErrorMap(e -> !(e instanceof BusinessException), e -> {
                    log.error("Ollama Chat 异常: {}", e.getMessage());
                    return new BusinessException(ErrorCode.AI_CHAT_FAILED,
                            "Ollama 对话异常: " + e.getMessage());
                });
    }

    @Override
    public boolean supports(String provider) {
        return true;
    }
}
