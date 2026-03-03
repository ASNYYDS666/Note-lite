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
}