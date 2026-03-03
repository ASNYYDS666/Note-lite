package com.note.service.dto;

import lombok.Data;

@Data
public class NoteQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String keyword;      // 标题搜索（暂不实现内容搜索）
    private Integer isDeleted = 0;
}
