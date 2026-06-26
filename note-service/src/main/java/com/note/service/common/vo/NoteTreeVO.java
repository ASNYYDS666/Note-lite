package com.note.service.common.vo;

import com.note.service.entity.NoteEntity;
import com.note.service.entity.NoteFolderEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Schema(description = "文件夹+笔记树响应")
public class NoteTreeVO {

    @Schema(description = "文件夹树列表(根级)")
    private List<FolderNode> folders;

    @Schema(description = "根级笔记(不属于任何文件夹)")
    private List<NoteNode> notes;

    /**
     * 从扁平的文件夹列表和笔记列表构建树结构
     */
    public static NoteTreeVO build(List<NoteFolderEntity> allFolders,
                                   List<NoteEntity> allNotes) {
        NoteTreeVO vo = new NoteTreeVO();

        // 构建文件夹树：按 parentId 分组，递归构建子节点
        Map<Long, List<NoteFolderEntity>> byParent = allFolders.stream()
                .collect(Collectors.groupingBy(f ->
                        f.getParentId() != null ? f.getParentId() : 0L));

        // 根级文件夹: parentId IS NULL (key=0)
        List<NoteFolderEntity> roots = byParent.getOrDefault(0L, List.of());
        vo.folders = roots.stream()
                .map(f -> buildFolderNode(f, byParent))
                .collect(Collectors.toList());

        // 按 folderId 分组笔记
        Map<Long, List<NoteEntity>> notesByFolder = allNotes.stream()
                .collect(Collectors.groupingBy(n ->
                        n.getFolderId() != null ? n.getFolderId() : 0L));

        // 将笔记挂到文件夹树节点上
        attachNotes(vo.folders, notesByFolder);

        // 根级笔记: folderId IS NULL (key=0)
        List<NoteEntity> rootNotes = notesByFolder.getOrDefault(0L, List.of());
        vo.notes = rootNotes.stream()
                .map(NoteNode::from)
                .collect(Collectors.toList());

        return vo;
    }

    private static FolderNode buildFolderNode(NoteFolderEntity folder,
                                              Map<Long, List<NoteFolderEntity>> byParent) {
        FolderNode node = FolderNode.from(folder);
        List<NoteFolderEntity> children = byParent.getOrDefault(folder.getId(), List.of());
        node.children = children.stream()
                .map(f -> buildFolderNode(f, byParent))
                .collect(Collectors.toList());
        return node;
    }

    private static void attachNotes(List<FolderNode> nodes,
                                    Map<Long, List<NoteEntity>> notesByFolder) {
        for (FolderNode node : nodes) {
            List<NoteEntity> folderNotes = notesByFolder.getOrDefault(node.id, List.of());
            node.notes = folderNotes.stream()
                    .map(NoteNode::from)
                    .collect(Collectors.toList());
            // 递归处理子文件夹
            attachNotes(node.children, notesByFolder);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文件夹树节点")
    public static class FolderNode {
        @Schema(description = "文件夹ID")
        private Long id;

        @Schema(description = "父文件夹ID")
        private Long parentId;

        @Schema(description = "文件夹名称")
        private String name;

        @Schema(description = "排序序号")
        private Integer sortOrder;

        @Schema(description = "子文件夹")
        private List<FolderNode> children;

        @Schema(description = "该文件夹下的笔记")
        private List<NoteNode> notes;

        public static FolderNode from(NoteFolderEntity entity) {
            FolderNode node = new FolderNode();
            node.id = entity.getId();
            node.parentId = entity.getParentId();
            node.name = entity.getName();
            node.sortOrder = entity.getSortOrder();
            return node;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "笔记树节点（精简信息）")
    public static class NoteNode {
        @Schema(description = "笔记ID")
        private Long id;

        @Schema(description = "笔记标题")
        private String title;

        @Schema(description = "所属文件夹ID")
        private Long folderId;

        @Schema(description = "最后更新时间")
        private LocalDateTime updatedAt;

        public static NoteNode from(NoteEntity entity) {
            return new NoteNode(entity.getId(), entity.getTitle(),
                    entity.getFolderId(), entity.getUpdatedAt());
        }
    }
}
