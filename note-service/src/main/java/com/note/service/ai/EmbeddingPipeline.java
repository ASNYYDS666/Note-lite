package com.note.service.ai;

import com.note.service.ai.chunker.Chunk;
import com.note.service.ai.chunker.MarkdownSplitter;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.UserApiProfileEntity;
import com.note.service.mapper.NoteMapper;
import com.note.service.service.AiProviderService;
import com.note.service.service.UserApiProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingPipeline {

    private final NoteMapper noteMapper;
    private final MarkdownSplitter splitter;
    private final AIFacadeFactory facadeFactory;
    private final AiProviderService aiProviderService;
    private final AISettingService aiSettingService;
    private final UserApiProfileService profileService;
    private final Executor embeddingExecutor;

    private static final String COLLECTION = "note_chunks";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoteCreated(NoteEmbeddingEvent event) {
        if (event.getType() == NoteEmbeddingEvent.EventType.CREATED
                || event.getType() == NoteEmbeddingEvent.EventType.UPDATED) {
            embeddingExecutor.execute(() -> doProcessNoteEmbedding(event.getNoteId()));
        } else if (event.getType() == NoteEmbeddingEvent.EventType.DELETED) {
            embeddingExecutor.execute(() -> doDeleteNoteVectors(event.getNoteId()));
        }
    }

    private void doProcessNoteEmbedding(Long noteId) {
        try {
            NoteEntity note = noteMapper.selectById(noteId);
            if (note == null) {
                log.warn("向量化跳过：笔记不存在 noteId={}", noteId);
                return;
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("noteId", note.getId());
            metadata.put("userId", note.getUserId());
            metadata.put("title", note.getTitle());
            metadata.put("type", "note");

            List<Chunk> chunks = splitter.chunk(note.getContent(), metadata);
            if (chunks.isEmpty()) {
                log.debug("向量化跳过：笔记无内容 noteId={}", noteId);
                return;
            }

            // Try Profile first, then fall back to legacy user_ai_config
            EmbedAIConfig config = null;

            // 1) Try Profile-based config
            List<UserApiProfileEntity> profiles = profileService.listByUser(note.getUserId());
            UserApiProfileEntity embedProfile = profiles.stream()
                    .filter(p -> p.getEnabledModels() != null)
                    .findFirst().orElse(null);

            if (embedProfile != null) {
                String apiKey = profileService.decryptApiKey(embedProfile);
                if (apiKey != null && !apiKey.isEmpty()) {
                    String providerKey = embedProfile.getProviderKey();
                    var provider = aiProviderService.getProvider(providerKey);
                    String embedModel = aiProviderService.getDefaultEmbedModel(providerKey);
                    if (embedModel == null) {
                        log.info("向量化跳过：厂商 {} 未配置 Embedding 模型", provider.getName());
                    } else {
                        config = EmbedAIConfig.builder()
                                .provider(providerKey)
                                .apiKey(apiKey)
                                .model(embedModel)
                                .baseUrl(embedProfile.getBaseUrl())
                                .pluginType(provider.getPluginType())
                                .build();
                        log.info("向量化使用 Profile: profileId={}, embedModel={}", embedProfile.getId(), embedModel);
                    }
                } else {
                    log.info("向量化跳过：Profile 未配置 API Key userId={}", note.getUserId());
                }
            }

            // 2) Fallback: legacy user_ai_config
            if (config == null) {
                var userConfig = aiSettingService.getByUserId(note.getUserId());
                if (userConfig == null) {
                    log.info("向量化跳过：用户未配置 AI userId={}", note.getUserId());
                    return;
                }
                try {
                    config = aiProviderService.buildEmbedConfig(
                            userConfig.getEmbedProvider(), userConfig.getEmbedModel(),
                            userConfig.getEmbedUrl());
                } catch (BusinessException e) {
                    if (e.getCode().equals(ErrorCode.AI_CONFIG_NOT_FOUND.getCode())
                            || e.getCode().equals(ErrorCode.AI_CONFIG_DISABLED.getCode())) {
                        log.info("向量化跳过：用户未配置或已禁用 AI userId={}", note.getUserId());
                        return;
                    }
                    throw e;
                }
            }

            EmbeddingFacade embeddingFacade = facadeFactory.getEmbedding(config.getPluginType());
            List<String> texts = chunks.stream().map(Chunk::getText).collect(Collectors.toList());
            List<List<Float>> vectors = embeddingFacade.embedBatch(texts, config);

            VectorStore vectorStore = facadeFactory.getVectorStore();
            List<VectorDoc> docs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Chunk chunk = chunks.get(i);
                chunk.getMetadata().put("chunkIndex", chunk.getIndex());
                chunk.getMetadata().put("text", chunk.getText());

                VectorDoc doc = new VectorDoc();
                String rawId = noteId + "_" + chunk.getIndex();
                doc.setId(UUID.nameUUIDFromBytes(rawId.getBytes()).toString());
                doc.setVector(vectors.get(i));
                doc.setPayload(chunk.getMetadata());
                docs.add(doc);
            }

            // 先清除旧向量，再写入新向量（避免 chunk 数量变化时残留旧数据）
            vectorStore.deleteByFilter(COLLECTION, Map.of("noteId", (Object) noteId));
            vectorStore.upsert(COLLECTION, docs);
            log.info("异步向量化完成: noteId={}, chunks={}", noteId, chunks.size());

        } catch (Exception e) {
            log.error("异步向量化异常: noteId={}", noteId, e);
        }
    }

    private void doDeleteNoteVectors(Long noteId) {
        try {
            VectorStore vectorStore = facadeFactory.getVectorStore();
            // QdrantVectorStore has a convenience method deleteByNoteId
            if (vectorStore instanceof com.note.service.ai.facade.impl.QdrantVectorStore qvs) {
                qvs.deleteByNoteId(COLLECTION, noteId);
            }
            log.info("异步删除向量完成: noteId={}", noteId);
        } catch (Exception e) {
            log.error("异步删除向量异常: noteId={}", noteId, e);
        }
    }
}
