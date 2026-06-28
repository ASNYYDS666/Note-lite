package com.note.service.ai.facade;

import com.note.service.ai.config.ChatAIConfig;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

public interface LLMFacade {

    /** 流式对话，返回逐 token 的 Flux。thinking=true 表示推理过程，false 表示正式回答 */
    Flux<ChatToken> streamChat(List<Map<String, String>> messages, ChatAIConfig config);

    /** 是否支持指定服务商（工厂路由依据） */
    boolean supports(String provider);
}
