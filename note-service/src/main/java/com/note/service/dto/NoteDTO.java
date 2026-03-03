package com.note.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class NoteDTO {

    private Long id;  // 更新时用

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    private String content;  // Markdown原文

    @Size(max = 10, message = "最多10个标签")
    private List<String> tags;
}
