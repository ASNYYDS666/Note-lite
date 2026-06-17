package com.note.service.ai.controller;

import com.note.service.ai.ChatService;
import com.note.service.dto.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Tag(name = "AI 对话", description = "RAG 知识库问答")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（SSE 流式）")
    public SseEmitter chat(@AuthenticationPrincipal Long userId,
                           @RequestBody @Valid ChatRequest request) {

        SseEmitter emitter = new SseEmitter(120_000L);

        new Thread(() -> {
            try {
                chatService.ask(userId, request.getQuestion(),
                        request.getScopeType(), request.getScopeIds())
                        .subscribe(
                                token -> {
                                    try {
                                        if ("[DONE]".equals(token)) {
                                            emitter.send(SseEmitter.event()
                                                    .data("{\"done\":true}"));
                                        } else {
                                            emitter.send(SseEmitter.event()
                                                    .data("{\"token\":\"" + escapeJson(token)
                                                            + "\",\"done\":false}"));
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
                                () -> emitter.complete()
                        );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\",\"done\":true}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, "sse-chat").start();

        return emitter;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
