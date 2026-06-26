package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.NoteFolderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteFolderMapper extends BaseMapper<NoteFolderEntity> {

    @Select("SELECT * FROM note_folder WHERE user_id = #{userId} ORDER BY sort_order, id")
    List<NoteFolderEntity> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM note_folder WHERE parent_id = #{parentId} AND user_id = #{userId} ORDER BY sort_order, id")
    List<NoteFolderEntity> selectByParentId(@Param("parentId") Long parentId,
                                             @Param("userId") Long userId);
}
