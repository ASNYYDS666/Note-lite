package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import com.note.service.ai.prompt.ChatStyle;
import com.note.service.ai.prompt.PromptTemplateEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(5)
public class PromptAssemblyStage implements RAGStage {

    private final PromptTemplateEngine templateEngine;
    private final int maxChunks;
    private final int maxTotalChars;

    public PromptAssemblyStage(PromptTemplateEngine templateEngine,
                               @Value("${note.retrieval.max-chunks:6}") int maxChunks,
                               @Value("${note.retrieval.max-chars:8000}") int maxTotalChars) {
        this.templateEngine = templateEngine;
        this.maxChunks = maxChunks;
        this.maxTotalChars = maxTotalChars;
    }

    @Override
    @MonitorStage("prompt")
    public void process(RAGContext ctx) {
        ChatStyle chatStyle = ChatStyle.fromCode(ctx.getStyle());
        ctx.setSystemPrompt(chatStyle.getSystemPrompt());

        Map<String, Object> vars = buildTemplateVars(
                ctx.getQuestion(), ctx.getFilteredDocs());
        ctx.setUserPrompt(templateEngine.render(chatStyle.getCode(), vars));

        // 构建消息列表：[system] + [历史 user/assistant 交替] + [当前 user]
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", ctx.getSystemPrompt()));

        if (ctx.getConversationHistory() != null && !ctx.getConversationHistory().isEmpty()) {
            for (Map<String, String> entry : ctx.getConversationHistory()) {
                messages.add(Map.of("role", "user", "content", entry.get("question")));
                messages.add(Map.of("role", "assistant", "content", entry.get("answer")));
            }
        }

        messages.add(Map.of("role", "user", "content", ctx.getUserPrompt()));
        ctx.setMessages(messages);
    }

    private Map<String, Object> buildTemplateVars(String question, List<VectorDoc> filteredDocs) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("hasChunks", filteredDocs != null && !filteredDocs.isEmpty());
        vars.put("question", question != null ? question : "");

        if (filteredDocs == null || filteredDocs.isEmpty()) {
            vars.put("chunks", List.of());
            vars.put("truncated", false);
            return vars;
        }

        boolean truncated = false;
        int totalChars = 0;
        List<Map<String, Object>> chunks = new ArrayList<>();

        for (int i = 0; i < filteredDocs.size(); i++) {
            VectorDoc doc = filteredDocs.get(i);

            if (i >= maxChunks) {
                truncated = true;
                break;
            }

            String rawText = Objects.toString(doc.getPayload().getOrDefault("text", ""), "");
            String title = Objects.toString(doc.getPayload().getOrDefault("title", "未知笔记"), "");

            String safeText = rawText
                    .replace("{{", "\\{\\{")
                    .replace("}}", "\\}\\}");

            totalChars += safeText.length();
            if (totalChars > maxTotalChars) {
                int excess = totalChars - maxTotalChars;
                safeText = safeText.substring(0, Math.max(0, safeText.length() - excess))
                        + "\n...(内容过长，已截断)";
                truncated = true;
            }

            Map<String, Object> chunk = new LinkedHashMap<>();
            chunk.put("index", i + 1);
            chunk.put("title", title);
            chunk.put("text", safeText);
            chunks.add(chunk);

            if (totalChars > maxTotalChars) break;
        }

        vars.put("chunks", chunks);
        vars.put("truncated", truncated);
        return vars;
    }
}
