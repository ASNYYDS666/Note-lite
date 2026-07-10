package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.NoteChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteChunkMapper extends BaseMapper<NoteChunkEntity> {

    /** Full-text keyword search on chunk text, returning chunk IDs ranked by relevance */
    @Select("SELECT c.chunk_id, MATCH(c.chunk_text) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS bm25_score "
          + "FROM note_chunks c WHERE c.user_id = #{userId} "
          + "AND MATCH(c.chunk_text) AGAINST(#{query} IN NATURAL LANGUAGE MODE) > 0 "
          + "ORDER BY bm25_score DESC LIMIT #{topK}")
    List<ChunkScoreView> searchFulltext(@Param("query") String query,
                                         @Param("userId") Long userId,
                                         @Param("topK") int topK);

    /** Delete all chunks for a given note */
    @org.apache.ibatis.annotations.Delete("DELETE FROM note_chunks WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);

    /** Count chunks for a user (used for reconciliation) */
    @Select("SELECT COUNT(*) FROM note_chunks WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /** Batch insert chunk records. ON DUPLICATE KEY UPDATE makes it safe for concurrent updates to the same note. */
    @org.apache.ibatis.annotations.Insert("<script>"
            + "INSERT INTO note_chunks (note_id, user_id, chunk_index, chunk_text, chunk_id) VALUES "
            + "<foreach collection='chunks' item='c' separator=','>"
            + "(#{c.noteId}, #{c.userId}, #{c.chunkIndex}, #{c.chunkText}, #{c.chunkId})"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE chunk_text = VALUES(chunk_text), chunk_index = VALUES(chunk_index)"
            + "</script>")
    int batchInsert(@Param("chunks") List<NoteChunkEntity> chunks);

    /** View object for fulltext search results */
    class ChunkScoreView {
        private String chunkId;
        private double bm25Score;

        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        public double getBm25Score() { return bm25Score; }
        public void setBm25Score(double bm25Score) { this.bm25Score = bm25Score; }
    }
}
