package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "笔记创建/更新请求")
public class NoteDTO {

    @Schema(description = "笔记ID，新建时不传，更新时必传")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    @Schema(description = "笔记标题，最长200字符", example = "今日学习笔记")
    private String title;

    @Schema(description = "笔记正文，Markdown 格式", example = "# 学习笔记\n今天学了...")
    private String content;

    @Size(max = 10, message = "最多10个标签")
    @Schema(description = "标签列表，最多10个", example = "[\"java\", \"spring\"]")
    private List<String> tags;
}
