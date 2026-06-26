package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.service.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {

    // 分页查询用户的笔记（带标签聚合）
    // 这个是原有的分页查询方法，在day05步骤二更改为 一下
//    @Select("SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
//            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
//            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
//            "GROUP BY n.id ORDER BY n.updated_at DESC")
//    Page<NoteEntity> selectPageWithTags(Page<NoteEntity> page,
//                                        @Param("userId") Long userId,
//                                        @Param("isDeleted") Integer isDeleted);

     //查询当前页数据（带标签筛选）- 手动分页
     //
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
            "GROUP BY n.id " +
            "ORDER BY n.updated_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<NoteEntity> selectWithTags(@Param("userId") Long userId,
                                    @Param("isDeleted") Integer isDeleted,
                                    @Param("keyword") String keyword,
                                    @Param("tags") List<String> tags,
                                    @Param("tagMatch") String tagMatch,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    /**
     * 查询总记录数（带标签筛选）- 使用 COUNT DISTINCT 确保准确
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT n.id) FROM note n " +
            "LEFT JOIN note_tag nt ON n.id = nt.note_id " +
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
            "</script>")
    long countWithTags(@Param("userId") Long userId,
                       @Param("isDeleted") Integer isDeleted,
                       @Param("keyword") String keyword,
                       @Param("tags") List<String> tags,
                       @Param("tagMatch") String tagMatch);

    @Select("SELECT id, title, folder_id, updated_at FROM note WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY updated_at DESC")
    List<NoteEntity> selectActiveByUserId(@Param("userId") Long userId);

}