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
    @Select("SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
            "GROUP BY n.id ORDER BY n.updated_at DESC")
    Page<NoteEntity> selectPageWithTags(Page<NoteEntity> page,
                                        @Param("userId") Long userId,
                                        @Param("isDeleted") Integer isDeleted);
}