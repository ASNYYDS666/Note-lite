package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.NoteTagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTagEntity> {


    @Select("SELECT tag_name FROM note_tag WHERE note_id = #{noteId}")
    List<String> selectTagsByNoteId(@Param("noteId") Long noteId);
    // 根据用户ID查询所有不重复的标签（Day 5 步骤 14 需要的）- 这个可能没添加
    @Select("SELECT DISTINCT tag_name FROM note_tag WHERE note_id IN " +
            "(SELECT id FROM note WHERE user_id = #{userId})")
    List<String> selectDistinctTagsByUserId(@Param("userId") Long userId);
}