package com.note.service.ai.pipeline.stages;

import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.ai.pipeline.RAGContext;
import com.note.service.ai.retrieval.KeywordRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetrievalStage 单元测试")
class RetrievalStageTest {

    private static final String COLLECTION = "note_chunks";
    private static final int TOP_K = 5;
    private static final double SCORE_THRESHOLD = 0.3;

    private static final int FETCH_K = 10; // max(TOP_K * 2, 10)

    private VectorStore vectorStore;
    private KeywordRetriever keywordRetriever;
    private RetrievalStage stage;
    private RAGContext ctx;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        AIFacadeFactory factory = mock(AIFacadeFactory.class);
        when(factory.getVectorStore()).thenReturn(vectorStore);

        keywordRetriever = mock(KeywordRetriever.class);
        when(keywordRetriever.search(anyString(), anyLong(), anyInt())).thenReturn(Map.of());

        stage = new RetrievalStage(factory, keywordRetriever, COLLECTION, TOP_K, SCORE_THRESHOLD);

        ctx = new RAGContext();
        ctx.setUserId(1L);
        ctx.setQuestion("test question");
        ctx.setQueryVector(List.of(0.1f, 0.2f));
    }

    @Test
    @DisplayName("调用 vectorStore.search()")
    void testCallsVectorStoreSearch() {
        when(vectorStore.search(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());

        stage.process(ctx);

        verify(vectorStore).search(eq(COLLECTION), any(), any(), eq(FETCH_K));
    }

    @Nested
    @DisplayName("Scope Filter")
    class ScopeFilter {

        @Test
        @DisplayName("scopeType=NOTE 时 filter 含 noteId")
        void testNoteScopeFilterIncludesNoteId() {
            ctx.setScopeType("NOTE");
            ctx.setScopeIds(List.of(42L));
            when(vectorStore.search(anyString(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            stage.process(ctx);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> filterCaptor = ArgumentCaptor.forClass(Map.class);
            verify(vectorStore).search(anyString(), any(), filterCaptor.capture(), anyInt());

            Map<String, Object> filter = filterCaptor.getValue();
            assertThat(filter).containsEntry("noteId", 42L);
            assertThat(filter).containsEntry("userId", 1L);
        }

        @Test
        @DisplayName("scopeType=ALL 时 filter 仅含 userId")
        void testAllScopeFilterOnlyUserId() {
            ctx.setScopeType("ALL");
            ctx.setScopeIds(null);
            when(vectorStore.search(anyString(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            stage.process(ctx);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> filterCaptor = ArgumentCaptor.forClass(Map.class);
            verify(vectorStore).search(anyString(), any(), filterCaptor.capture(), anyInt());

            Map<String, Object> filter = filterCaptor.getValue();
            assertThat(filter).containsEntry("userId", 1L);
            assertThat(filter).doesNotContainKey("noteId");
        }
    }

    @Nested
    @DisplayName("分数过滤")
    class ScoreFiltering {

        @Test
        @DisplayName("score 低于阈值的文档被过滤")
        void testLowScoreDocsFilteredOut() {
            VectorDoc highScore = new VectorDoc();
            highScore.setId("high");
            highScore.setScore(0.8);
            highScore.setPayload(Map.of("title", "相关笔记"));
            VectorDoc lowScore = new VectorDoc();
            lowScore.setId("low");
            lowScore.setScore(0.1);
            lowScore.setPayload(Map.of("title", "无关笔记"));

            when(vectorStore.search(anyString(), any(), any(), anyInt()))
                    .thenReturn(List.of(highScore, lowScore));

            stage.process(ctx);

            assertThat(ctx.getRetrieved()).hasSize(2);
            assertThat(ctx.getFilteredDocs()).hasSize(1);
            // Score is blended by RRF: 0.6*vector + 0.4*kw, kw=0 → 0.48
            assertThat(ctx.getFilteredDocs().get(0).getScore()).isEqualTo(0.48);
            assertThat(ctx.getFilteredDocs().get(0).getId()).isEqualTo("high");
        }

        @Test
        @DisplayName("score 为 null 的文档被过滤")
        void testNullScoreDocsFilteredOut() {
            VectorDoc withScore = new VectorDoc();
            withScore.setId("a");
            withScore.setScore(0.5);
            VectorDoc nullScore = new VectorDoc();
            nullScore.setId("b");
            nullScore.setScore(null);

            when(vectorStore.search(anyString(), any(), any(), anyInt()))
                    .thenReturn(List.of(withScore, nullScore));

            stage.process(ctx);

            assertThat(ctx.getFilteredDocs()).hasSize(1);
        }
    }
}
