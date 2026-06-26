package com.note.service.ai.pipeline;

import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.VectorDoc;
import lombok.Data;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Data
public class RAGContext {

    // ====== 输入（由 ChatService 门面设置） ======
    private Long userId;
    private String question;
    private String scopeType;
    private List<Long> scopeIds;
    private String style;

    // ====== 配置（由 ChatService 门面获取后注入） ======
    private EmbedAIConfig embedConfig;
    private ChatAIConfig chatConfig;

    // ====== 多轮对话（由 ChatService 设置） ======
    private Long conversationId;
    private List<Map<String, String>> conversationHistory;

    // ====== 各阶段中间产物 ======
    private String rewrittenQuery;               // Stage 1: 改写后的查询
    private List<Float> queryVector;             // Stage 2: 查询向量
    private List<VectorDoc> retrieved;           // Stage 3: 检索结果（原始）
    private List<VectorDoc> filteredDocs;        // Stage 3: 分数过滤后的结果
    private String systemPrompt;                 // Stage 5: System Prompt
    private String userPrompt;                   // Stage 5: User Prompt
    private List<Map<String, String>> messages;  // Stage 5: 最终消息列表

    // ====== 输出（由 Stage 6 设置） ======
    private Flux<String> responseStream;         // Stage 6: LLM 流式响应
}
