package com.note.service.dto;

import lombok.Data;
//day05-添加标签筛选字段
import java.util.List;

@Data
public class NoteQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String keyword;      // 标题搜索（暂不实现内容搜索）
    private Integer isDeleted = 0;
    private List<String> tags;  //day05-标签筛选-新增
    private String tagMatch="ANY";  //ANY-任意标签 ALL-全部标签-新增
}
