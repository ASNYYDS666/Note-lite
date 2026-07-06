package com.note.service.ai.controller;

import com.note.service.ai.ChatService;
import com.note.service.ai.facade.ChatToken;
import com.note.service.common.metrics.SSEConnectionMetrics;
import com.note.service.dto.ChatRequest;
import com.note.service.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Tag(name = "AI 对话", description = "RAG 知识库问答")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final Executor chatExecutor;
    private final SSEConnectionMetrics sseMetrics;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（SSE 流式）")
    public SseEmitter chat(@AuthenticationPrincipal Long userId,
                           @RequestBody @Valid ChatRequest request) {

        SseEmitter emitter = new SseEmitter(120_000L);

        SecurityContext securityContext = SecurityContextHolder.getContext();
        sseMetrics.onConnectionOpen();

        emitter.onCompletion(sseMetrics::onConnectionClose);
        emitter.onError(e -> sseMetrics.onConnectionError());
        emitter.onTimeout(sseMetrics::onConnectionClose);

        chatExecutor.execute(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                AtomicLong cidHolder = new AtomicLong();
                String[] qidHolder = new String[1];
                StringBuilder responseBuffer = new StringBuilder();

                chatService.ask(userId, request.getQuestion(),
                        request.getScopeType(), request.getScopeIds(), request.getStyle(),
                        request.getConversationId(), cidHolder, qidHolder,
                        request.getProfileId(), request.getModelName())
                        .subscribe(
                                token -> {
                                    try {
                                        if (token.isDone()) {
                                            emitter.send(SseEmitter.event()
                                                    .data("{\"done\":true,\"conversationId\":"
                                                            + cidHolder.get() + "}"));
                                        } else {
                                            responseBuffer.append(token.text());
                                            emitter.send(SseEmitter.event()
                                                    .data("{\"token\":\"" + escapeJson(token.text())
                                                            + "\",\"thinking\":" + token.thinking()
                                                            + ",\"done\":false}"));
                                        }
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                },
                                error -> {
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .data("{\"error\":\"" + escapeJson(error.getMessage())
                                                        + "\",\"done\":true}"));
                                        emitter.complete();
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                },
                                () -> {
                                    // 流完成后保存消息（失败不影响响应）
                                    saveMessages(cidHolder.get(), qidHolder[0],
                                            request.getQuestion(), responseBuffer.toString());
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\",\"done\":true}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    private void saveMessages(Long conversationId, String questionId,
                               String question, String answer) {
        if (conversationId == null || questionId == null) return;
        try {
            conversationService.saveMessage(conversationId, questionId,
                    "user", question, null);
            conversationService.saveMessage(conversationId, questionId,
                    "assistant", answer, null);
            log.info("消息已保存: conversationId={}, questionId={}", conversationId, questionId);
        } catch (Exception e) {
            log.error("消息保存失败: conversationId={}, questionId={}",
                    conversationId, questionId, e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
