package com.note.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.common.vo.Result;
import com.note.service.dto.ConversationVO;
import com.note.service.dto.MessageVO;
import com.note.service.dto.SourceVO;
import com.note.service.entity.ConversationEntity;
import com.note.service.entity.MessageEntity;
import com.note.service.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "AI 对话管理", description = "多轮对话会话 CRUD")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @GetMapping("/conversations")
    @Operation(summary = "获取用户对话列表")
    public Result<List<ConversationVO>> listConversations(@AuthenticationPrincipal Long userId) {
        List<ConversationEntity> list = conversationService.listUserConversations(userId);
        Map<Long, Long> countMap = conversationService.countMessagesForUser(userId);
        List<ConversationVO> vos = list.stream()
                .map(c -> {
                    ConversationVO vo = new ConversationVO();
                    vo.setId(c.getId());
                    vo.setTitle(c.getTitle());
                    vo.setCreatedAt(c.getCreatedAt());
                    vo.setUpdatedAt(c.getUpdatedAt());
                    vo.setMessageCount(countMap.getOrDefault(c.getId(), 0L).intValue());
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建新对话")
    public Result<ConversationVO> createConversation(@AuthenticationPrincipal Long userId,
                                                      @RequestBody Map<String, String> body) {
        String title = body != null ? body.getOrDefault("title", "新对话") : "新对话";
        ConversationEntity conv = conversationService.createConversation(userId, title);
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        vo.setTitle(conv.getTitle());
        vo.setCreatedAt(conv.getCreatedAt());
        vo.setUpdatedAt(conv.getUpdatedAt());
        return Result.success(vo);
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "获取对话历史消息")
    public Result<List<MessageVO>> getMessages(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long id) {
        List<MessageEntity> messages = conversationService.getConversationMessages(id, userId);
        List<MessageVO> vos = messages.stream()
                .map(m -> {
                    MessageVO vo = new MessageVO();
                    vo.setId(m.getId());
                    vo.setRole(m.getRole());
                    vo.setContent(m.getContent());
                    vo.setSources(parseSources(m.getSources()));
                    vo.setCreatedAt(m.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除对话及所有消息")
    public Result<Void> deleteConversation(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long id) {
        conversationService.deleteConversation(id, userId);
        return Result.success();
    }

    private List<SourceVO> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isEmpty()) return null;
        try {
            return objectMapper.readValue(sourcesJson, new TypeReference<List<SourceVO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
