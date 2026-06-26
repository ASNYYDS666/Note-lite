package com.note.service.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记详情")
public class NoteDetailVO {
    @Schema(description = "笔记ID")
    private Long id;

    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记正文，Markdown 格式")
    private String content;

    @Schema(description = "所属文件夹ID")
    private Long folderId;

    @Schema(description = "笔记摘要，从正文自动生成")
    private String summary;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
