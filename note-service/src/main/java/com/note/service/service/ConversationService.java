package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.ConversationEntity;
import com.note.service.entity.MessageEntity;
import com.note.service.mapper.ConversationMapper;
import com.note.service.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    @Value("${note.conversation.max-history-rounds:5}")
    private int maxHistoryRounds;

    @Value("${note.conversation.max-history-hours:24}")
    private int maxHistoryHours;

    // ==================== 对话 CRUD ====================

    public List<ConversationEntity> listUserConversations(Long userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<ConversationEntity>()
                        .eq(ConversationEntity::getUserId, userId)
                        .orderByDesc(ConversationEntity::getUpdatedAt));
    }

    public ConversationEntity createConversation(Long userId, String titleHint) {
        String title = titleHint;
        if (title == null || title.trim().isEmpty()) {
            title = "新对话";
        } else if (title.length() > 50) {
            title = title.substring(0, 50);
        }
        ConversationEntity conv = new ConversationEntity();
        conv.setUserId(userId);
        conv.setTitle(title);
        conversationMapper.insert(conv);
        log.info("对话已创建: id={}, userId={}, title={}", conv.getId(), userId, title);
        return conv;
    }

    public ConversationEntity getConversation(Long id, Long userId) {
        ConversationEntity conv = conversationMapper.selectById(id);
        if (conv == null) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NO_PERMISSION);
        }
        return conv;
    }

    @Transactional
    public void deleteConversation(Long id, Long userId) {
        getConversation(id, userId);
        conversationMapper.deleteById(id);
        log.info("对话已删除: id={}, userId={}", id, userId);
    }

    // ==================== 消息 ====================

    public MessageEntity saveMessage(Long conversationId, String questionId,
                                      String role, String content, String sources) {
        MessageEntity msg = new MessageEntity();
        msg.setConversationId(conversationId);
        msg.setQuestionId(questionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setStatus("SUCCESS");
        msg.setSources(sources);
        msg.setTokenCount(estimateTokens(content));
        messageMapper.insert(msg);
        return msg;
    }

    public List<MessageEntity> getConversationMessages(Long conversationId, Long userId) {
        getConversation(conversationId, userId);
        return messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntity::getCreatedAt));
    }

    // ==================== 历史加载（参考 OpenChat get_history_list） ====================

    /**
     * 加载最近 N 轮成功 Q&A 对，用于注入 Prompt 上下文。
     * 参考 OpenChat process_chat.get_history_list()：
     * - 限定 24h 内
     * - 仅 status=SUCCESS
     * - 排除当前 question_id
     * - 按 question_id 分组为 Q&A 对
     * - 返回最近 N 轮（每轮 {question, answer}）
     */
    public List<Map<String, String>> loadHistory(Long conversationId, Long userId,
                                                   String currentQuestionId) {
        getConversation(conversationId, userId);

        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxHistoryHours);

        // 1. 查出成功的 user 消息（排除当前 question_id）
        List<MessageEntity> userMsgs = messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getRole, "user")
                        .eq(MessageEntity::getStatus, "SUCCESS")
                        .ne(currentQuestionId != null, MessageEntity::getQuestionId, currentQuestionId)
                        .ge(MessageEntity::getCreatedAt, cutoff)
                        .orderByDesc(MessageEntity::getCreatedAt));

        if (userMsgs.isEmpty()) return List.of();

        // 2. 收集 question_id 列表
        List<String> questionIds = userMsgs.stream()
                .map(MessageEntity::getQuestionId)
                .distinct()
                .limit(maxHistoryRounds)
                .toList();

        if (questionIds.isEmpty()) return List.of();

        // 3. 查出对应的 assistant 消息
        List<MessageEntity> assistantMsgs = messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getRole, "assistant")
                        .eq(MessageEntity::getStatus, "SUCCESS")
                        .in(MessageEntity::getQuestionId, questionIds));

        // 4. 按 question_id 分组
        Map<String, String> questionMap = new LinkedHashMap<>();
        Map<String, String> answerMap = new LinkedHashMap<>();
        for (MessageEntity m : assistantMsgs) {
            answerMap.putIfAbsent(m.getQuestionId(), m.getContent());
        }
        for (MessageEntity m : userMsgs) {
            questionMap.putIfAbsent(m.getQuestionId(), m.getContent());
        }

        // 5. 按时间倒序取 maxHistoryRounds 轮，再翻转为正序
        List<Map<String, String>> result = new ArrayList<>();
        List<String> orderedQids = userMsgs.stream()
                .map(MessageEntity::getQuestionId)
                .filter(questionMap::containsKey)
                .filter(answerMap::containsKey)
                .distinct()
                .limit(maxHistoryRounds)
                .toList();

        for (int i = orderedQids.size() - 1; i >= 0; i--) {
            String qid = orderedQids.get(i);
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("question", questionMap.get(qid));
            entry.put("answer", answerMap.get(qid));
            result.add(entry);
        }
        return result;
    }

    // ==================== 工具 ====================

    /**
     * 批量查询用户所有会话的消息数量，替代 N+1 的逐条 COUNT。
     */
    public Map<Long, Long> countMessagesForUser(Long userId) {
        List<Map<String, Object>> rows = messageMapper.countMessagesGroupByConversation(userId);
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("conversation_id"),
                        row -> ((Number) row.get("cnt")).longValue()
                ));
    }

    public long countMessages(Long conversationId) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId));
        return count != null ? count : 0;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 2.0);
    }
}
