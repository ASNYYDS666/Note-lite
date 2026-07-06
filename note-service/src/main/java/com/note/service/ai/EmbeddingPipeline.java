package com.note.service.ai;

import com.note.service.ai.chunker.Chunk;
import com.note.service.ai.chunker.MarkdownSplitter;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.entity.NoteChunkEntity;
import com.note.service.entity.NoteEntity;
import com.note.service.mapper.NoteChunkMapper;
import com.note.service.mapper.NoteMapper;
import com.note.service.service.AiProviderService;
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
    private final NoteChunkMapper noteChunkMapper;
    private final MarkdownSplitter splitter;
    private final AIFacadeFactory facadeFactory;
    private final AiProviderService aiProviderService;
    private final AISettingService aiSettingService;
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

            // 用户显式选择 Embedding 服务商，不做降级
            var embedPref = aiSettingService.getByUserId(note.getUserId());
            if (embedPref == null || embedPref.getEmbedProvider() == null
                    || embedPref.getEmbedProvider().isEmpty()) {
                log.info("向量化跳过：用户未配置 Embedding 服务商 userId={}", note.getUserId());
                return;
            }

            EmbedAIConfig config;
            try {
                config = aiProviderService.buildEmbedConfig(
                        embedPref.getEmbedProvider(),
                        embedPref.getEmbedModel(),
                        embedPref.getEmbedUrl());
            } catch (Exception e) {
                log.warn("向量化跳过：Embedding 配置无效 userId={}, error={}",
                        note.getUserId(), e.getMessage());
                return;
            }

            log.debug("向量化使用: provider={}, model={}",
                    embedPref.getEmbedProvider(), embedPref.getEmbedModel());

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

            vectorStore.deleteByFilter(COLLECTION, Map.of("noteId", (Object) noteId));
            vectorStore.upsert(COLLECTION, docs);

            // Sync chunks to MySQL for keyword retrieval (best-effort, Qdrant is primary)
            syncChunksToMySQL(noteId, note.getUserId(), chunks, docs);
            log.info("异步向量化完成: noteId={}, chunks={}", noteId, chunks.size());

        } catch (Exception e) {
            log.error("异步向量化异常: noteId={}", noteId, e);
        }
    }

    private void doDeleteNoteVectors(Long noteId) {
        try {
            VectorStore vectorStore = facadeFactory.getVectorStore();
            vectorStore.deleteByNoteId(COLLECTION, noteId);
            // Best-effort MySQL cleanup — Qdrant is the source of truth
            noteChunkMapper.deleteByNoteId(noteId);
            log.info("异步删除向量完成: noteId={}", noteId);
        } catch (Exception e) {
            log.error("异步删除向量异常: noteId={}", noteId, e);
        }
    }

    /**
     * Sync chunk metadata to MySQL for keyword retrieval.
     * MySQL is a read-only cache — Qdrant is the primary store.
     * Failure here does NOT roll back Qdrant writes.
     */
    private void syncChunksToMySQL(Long noteId, Long userId, List<Chunk> chunks, List<VectorDoc> docs) {
        try {
            noteChunkMapper.deleteByNoteId(noteId);
            List<NoteChunkEntity> entities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                NoteChunkEntity entity = new NoteChunkEntity();
                entity.setNoteId(noteId);
                entity.setUserId(userId);
                entity.setChunkIndex(chunks.get(i).getIndex());
                entity.setChunkText(chunks.get(i).getText());
                entity.setChunkId(docs.get(i).getId());
                entities.add(entity);
            }
            noteChunkMapper.batchInsert(entities);
        } catch (Exception e) {
            log.warn("MySQL chunk sync failed (keyword retrieval will fallback to pure vector): "
                    + "noteId={}, error={}", noteId, e.getMessage());
        }
    }
}
