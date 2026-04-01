package com.note.service.dto;

//通过分享码访问返回
import com.note.service.entity.NoteEntity;
import lombok.Data;

@Data
public class ShareAccessDTO {
    private NoteEntity note;
    private String permission;
}
