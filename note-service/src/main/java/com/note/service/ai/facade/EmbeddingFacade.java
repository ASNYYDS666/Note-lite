package com.note.service.ai.facade;

import com.note.service.ai.config.EmbedAIConfig;
import java.util.List;

public interface EmbeddingFacade {

    /** 单文本向量化 */
    List<Float> embed(String text, EmbedAIConfig config);

    /** 批量文本向量化 */
    List<List<Float>> embedBatch(List<String> texts, EmbedAIConfig config);

    /** 是否支持指定服务商（工厂路由依据） */
    boolean supports(String provider);
}
