package com.note.service.ai.config;

import com.note.service.ai.facade.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QdrantCollectionInitializer {

    private final VectorStore vectorStore;
    private final String collectionName;
    private final int vectorSize;

    public QdrantCollectionInitializer(VectorStore vectorStore,
                                        @Value("${qdrant.collection:note_chunks}") String collectionName,
                                        @Value("${qdrant.vector-size:512}") int vectorSize) {
        this.vectorStore = vectorStore;
        this.collectionName = collectionName;
        this.vectorSize = vectorSize;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        vectorStore.createCollection(collectionName, vectorSize);
    }
}
