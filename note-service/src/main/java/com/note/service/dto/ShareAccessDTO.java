package com.note.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.note.service.common.vo.NoteDetailVO;
import lombok.Data;

@Data
@Schema(description = "通过分享码访问笔记的返回结果")
public class ShareAccessDTO {
    @Schema(description = "笔记详情")
    private NoteDetailVO note;

    @Schema(description = "分享权限：READ=只读, WRITE=可编辑")
    private String permission;
}
