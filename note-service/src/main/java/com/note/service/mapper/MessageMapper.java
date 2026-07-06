package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    @Select("SELECT conversation_id, COUNT(*) AS cnt FROM message WHERE conversation_id IN (SELECT id FROM conversation WHERE user_id = #{userId}) GROUP BY conversation_id")
    List<Map<String, Object>> countMessagesGroupByConversation(@Param("userId") Long userId);
}
