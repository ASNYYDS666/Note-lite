package com.note.service.ai.facade;

import com.note.service.ai.facade.impl.OpenAICompatibleChatFacade;
import com.note.service.ai.facade.impl.OpenAICompatibleEmbeddingFacade;
import com.note.service.ai.facade.impl.OllamaChatFacade;
import com.note.service.ai.facade.impl.OllamaEmbeddingFacade;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AIFacadeFactory {

    private final OpenAICompatibleChatFacade openAICompatibleChat;
    private final OllamaChatFacade ollamaChat;
    private final OpenAICompatibleEmbeddingFacade openAICompatibleEmbedding;
    private final OllamaEmbeddingFacade ollamaEmbedding;
    private final VectorStore vectorStore;

    public LLMFacade getLLM(String pluginType) {
        return switch (pluginType) {
            case "openai_compatible" -> openAICompatibleChat;
            case "ollama" -> ollamaChat;
            default -> throw new BusinessException(ErrorCode.AI_PROVIDER_UNSUPPORTED);
        };
    }

    public EmbeddingFacade getEmbedding(String pluginType) {
        return switch (pluginType) {
            case "openai_compatible" -> openAICompatibleEmbedding;
            case "ollama" -> ollamaEmbedding;
            default -> throw new BusinessException(ErrorCode.AI_PROVIDER_UNSUPPORTED);
        };
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }
}
