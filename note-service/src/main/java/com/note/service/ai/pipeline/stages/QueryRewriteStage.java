package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.ChatToken;
import com.note.service.ai.facade.LLMFacade;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1)
public class QueryRewriteStage implements RAGStage {

    private final AIFacadeFactory facadeFactory;

    public QueryRewriteStage(AIFacadeFactory facadeFactory) {
        this.facadeFactory = facadeFactory;
    }

    @Override
    @MonitorStage("rewrite")
    public void process(RAGContext ctx) {
        String question = ctx.getQuestion();
        List<Map<String, String>> history = ctx.getConversationHistory();

        String rewritePrompt = buildRewritePrompt(question, history);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", rewritePrompt));

        try {
            LLMFacade llm = facadeFactory.getLLM(ctx.getChatConfig().getPluginType());
            String rewritten = llm.streamChat(messages, ctx.getChatConfig())
                    .filter(t -> !t.thinking())
                    .map(ChatToken::text)
                    .collectList()
                    .block()
                    .stream()
                    .takeWhile(t -> !"[DONE]".equals(t))
                    .collect(Collectors.joining())
                    .trim();

            if (!rewritten.isEmpty()) {
                ctx.setRewrittenQuery(rewritten);
                log.debug("Query rewritten: [{}] -> [{}]", question, rewritten);
                return;
            }
        } catch (Exception e) {
            log.warn("Query rewrite failed, fallback to original: {}", e.getMessage());
        }

        ctx.setRewrittenQuery(question);
    }

    @Override
    public boolean isEnabled(RAGContext ctx) {
        return ctx.getConversationHistory() != null
            && !ctx.getConversationHistory().isEmpty();
    }

    private String buildRewritePrompt(String question, List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Rewrite the user's latest question into a standalone, keyword-rich \
                search query suitable for semantic search. Resolve all pronouns and \
                implicit references using the conversation history. Output only the \
                rewritten query, no explanation or preamble.
                """);

        if (history != null && !history.isEmpty()) {
            sb.append("\n--- Conversation History ---\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                String role = "user".equals(msg.get("role")) ? "User" : "Assistant";
                sb.append(role).append(": ").append(msg.get("content")).append("\n");
            }
        }

        sb.append("\n--- Latest Question ---\n");
        sb.append(question);
        sb.append("\n\nRewritten query:");
        return sb.toString();
    }
}
