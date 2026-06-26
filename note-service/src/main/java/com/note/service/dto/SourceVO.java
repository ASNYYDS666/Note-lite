package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "引用来源")
public class SourceVO {

    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "笔记标题")
    private String title;
}
