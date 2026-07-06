package com.note.service.ai.retrieval;

import com.note.service.mapper.NoteChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keyword-based retrieval using MySQL FULLTEXT ngram index.
 * Returns chunkId -&gt; normalized BM25 score map for RRF fusion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordRetriever {

    private final NoteChunkMapper noteChunkMapper;

    /**
     * Search chunks by keyword relevance.
     *
     * @return map of chunkId → normalized score (0..1), empty if no matches
     */
    public Map<String, Float> search(String query, Long userId, int topK) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        try {
            List<NoteChunkMapper.ChunkScoreView> results =
                    noteChunkMapper.searchFulltext(query.trim(), userId, topK);

            if (results.isEmpty()) {
                return Map.of();
            }

            // Normalize scores to [0, 1] for RRF fusion, using the first (best) score as baseline
            double maxScore = results.get(0).getBm25Score();
            return results.stream()
                    .collect(Collectors.toMap(
                            NoteChunkMapper.ChunkScoreView::getChunkId,
                            r -> (float) (r.getBm25Score() / maxScore),
                            (a, b) -> a,
                            LinkedHashMap::new));

        } catch (Exception e) {
            log.warn("Keyword search failed, falling back to pure vector retrieval: {}", e.getMessage());
            return Map.of();
        }
    }
}
