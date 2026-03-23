package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.NoteTagEntity;
import com.note.service.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTagEntity> {


    @Select("SELECT tag_name FROM note_tag WHERE note_id = #{noteId}")
    List<String> selectTagsByNoteId(@Param("noteId") Long noteId);
    // 根据用户ID查询所有不重复的标签（Day 5 步骤 14 需要的）- 这个可能没添加
    @Select("SELECT DISTINCT tag_name FROM note_tag WHERE note_id IN " +
            "(SELECT id FROM note WHERE user_id = #{userId})")
    List<String> selectDistinctTagsByUserId(@Param("userId") Long userId);

    //day05-添加标签筛选的分页查询
    @Select("<script>" +
            "SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND n.title LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            "<if test='tags != null and tags.size() > 0'>" +
            " AND n.id IN (" +
            "   SELECT note_id FROM note_tag WHERE tag_name IN " +
            "   <foreach collection='tags' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "   GROUP BY note_id " +
            "   <if test='tagMatch == \"ALL\"'>" +
            "   HAVING COUNT(DISTINCT tag_name) = #{tags.size}" +
            "   </if>" +
            " )" +
            "</if>" +
            "GROUP BY n.id ORDER BY n.updated_at DESC" +
            "</script>")
    Page<NoteEntity> selectPageWithTags(@Param("page") Page<NoteEntity> page,
                                        @Param("userId") Long userId,
                                        @Param("isDeleted") Integer isDeleted,
                                        @Param("keyword") String keyword,
                                        @Param("tags") List<String> tags,
                                        @Param("tagMatch") String tagMatch);
}