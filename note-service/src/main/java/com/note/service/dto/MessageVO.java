package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "对话消息")
public class MessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "角色：user/assistant/system")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "引用来源")
    private List<SourceVO> sources;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
