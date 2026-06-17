package com.note.service.ai;

import com.note.service.ai.chunker.Chunk;
import com.note.service.ai.chunker.MarkdownSplitter;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.EmbeddingService;
import com.note.service.ai.facade.QdrantVectorStore;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.NoteEntity;
import com.note.service.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final EmbeddingService embeddingService;
    private final QdrantVectorStore qdrantVectorStore;
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

            EmbedAIConfig config;
            try {
                config = aiSettingService.getDecryptedEmbedConfig(note.getUserId());
            } catch (BusinessException e) {
                if (e.getCode().equals(ErrorCode.AI_CONFIG_NOT_FOUND.getCode())
                        || e.getCode().equals(ErrorCode.AI_CONFIG_DISABLED.getCode())) {
                    log.info("向量化跳过：用户未配置或已禁用 AI userId={}", note.getUserId());
                    return;
                }
                throw e;
            }

            List<String> texts = chunks.stream().map(Chunk::getText).collect(Collectors.toList());
            List<List<Float>> vectors = embeddingService.embedBatch(texts, config);

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

            qdrantVectorStore.upsert(COLLECTION, docs);
            log.info("异步向量化完成: noteId={}, chunks={}", noteId, chunks.size());

        } catch (Exception e) {
            log.error("异步向量化异常: noteId={}", noteId, e);
        }
    }

    private void doDeleteNoteVectors(Long noteId) {
        try {
            qdrantVectorStore.deleteByNoteId(COLLECTION, noteId);
            log.info("异步删除向量完成: noteId={}", noteId);
        } catch (Exception e) {
            log.error("异步删除向量异常: noteId={}", noteId, e);
        }
    }
}
