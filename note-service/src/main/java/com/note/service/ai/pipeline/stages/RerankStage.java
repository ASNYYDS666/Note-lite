package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.ChatToken;
import com.note.service.ai.facade.LLMFacade;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.metrics.MonitorStage;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.pipeline.RAGStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(4)
public class RerankStage implements RAGStage {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final AIFacadeFactory facadeFactory;

    public RerankStage(AIFacadeFactory facadeFactory) {
        this.facadeFactory = facadeFactory;
    }

    @Override
    @MonitorStage("rerank")
    public void process(RAGContext ctx) {
        List<VectorDoc> docs = ctx.getFilteredDocs();
        if (docs == null || docs.size() <= 1) {
            return;
        }

        String query = ctx.getRewrittenQuery() != null
                ? ctx.getRewrittenQuery() : ctx.getQuestion();

        String prompt = buildRerankPrompt(query, docs);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt));

        try {
            LLMFacade llm = facadeFactory.getLLM(ctx.getChatConfig().getPluginType());
            String response = llm.streamChat(messages, ctx.getChatConfig())
                    .filter(t -> !t.thinking())
                    .map(ChatToken::text)
                    .collectList()
                    .block()
                    .stream()
                    .takeWhile(t -> !"[DONE]".equals(t))
                    .collect(Collectors.joining())
                    .trim();

            List<Integer> ranking = parseRanking(response, docs.size());
            if (ranking != null) {
                List<VectorDoc> reranked = ranking.stream()
                        .map(docs::get)
                        .toList();
                ctx.setFilteredDocs(reranked);
                ctx.setRetrieved(reranked);
                log.debug("Reranked {} docs: {}", reranked.size(), ranking);
            }
        } catch (Exception e) {
            log.warn("Rerank failed, keeping original order: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEnabled(RAGContext ctx) {
        return true;
    }

    private String buildRerankPrompt(String query, List<VectorDoc> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Rank the following documents by their relevance to the query. \
                Return only the document numbers in order of relevance, separated by \
                commas. Do not include any explanation or other text.\n
                """);
        sb.append("Query: ").append(query).append("\n\n");

        for (int i = 0; i < docs.size(); i++) {
            String title = Objects.toString(
                    docs.get(i).getPayload().getOrDefault("title", "Unknown"), "");
            String text = Objects.toString(
                    docs.get(i).getPayload().getOrDefault("text", ""), "");
            if (text.length() > 400) {
                text = text.substring(0, 400) + "...";
            }
            sb.append("--- Doc ").append(i + 1).append(" ---\n");
            sb.append("Title: ").append(title).append("\n");
            sb.append("Content: ").append(text).append("\n\n");
        }

        sb.append("Ranking (e.g., 3,1,5,2,4):");
        return sb.toString();
    }

    private List<Integer> parseRanking(String response, int docCount) {
        Matcher m = NUMBER_PATTERN.matcher(response);
        List<Integer> indices = new ArrayList<>();
        while (m.find()) {
            int idx = Integer.parseInt(m.group()) - 1;
            if (idx >= 0 && idx < docCount && !indices.contains(idx)) {
                indices.add(idx);
            }
        }
        for (int i = 0; i < docCount; i++) {
            if (!indices.contains(i)) {
                indices.add(i);
            }
        }
        return indices.isEmpty() ? null : indices;
    }
}
