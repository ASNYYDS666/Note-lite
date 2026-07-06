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

    @Select("WITH RECURSIVE ancestors AS ("
          + " SELECT id, parent_id FROM note_folder WHERE id = #{targetId} AND user_id = #{userId}"
          + " UNION ALL"
          + " SELECT f.id, f.parent_id FROM note_folder f INNER JOIN ancestors a ON f.id = a.parent_id"
          + ") SELECT COUNT(*) > 0 FROM ancestors WHERE id = #{sourceId}")
    boolean isAncestorOf(@Param("userId") Long userId,
                         @Param("targetId") Long targetId,
                         @Param("sourceId") Long sourceId);

    @Select("WITH RECURSIVE descendants AS ("
          + " SELECT id FROM note_folder WHERE parent_id = #{folderId} AND user_id = #{userId}"
          + " UNION ALL"
          + " SELECT f.id FROM note_folder f INNER JOIN descendants d ON f.parent_id = d.id"
          + ") SELECT id FROM descendants")
    List<Long> selectDescendantIds(@Param("userId") Long userId, @Param("folderId") Long folderId);
}
