package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "笔记分页查询参数")
public class NoteQueryDTO {
    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "标题关键词搜索")
    private String keyword;

    @Schema(description = "删除标记：0=正常, 1=回收站")
    private Integer isDeleted = 0;

    @Schema(description = "标签筛选列表", example = "[\"java\"]")
    private List<String> tags;

    @Schema(description = "标签匹配模式：ANY=任意标签, ALL=全部标签", example = "ANY", allowableValues = {"ANY", "ALL"})
    private String tagMatch = "ANY";
}
