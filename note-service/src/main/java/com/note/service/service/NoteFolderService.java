package com.note.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.NoteFolderEntity;
import com.note.service.mapper.NoteFolderMapper;
import com.note.service.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteFolderService extends ServiceImpl<NoteFolderMapper, NoteFolderEntity> {

    private final NoteMapper noteMapper;
    private final NoteService noteService;

    // ==================== 创建文件夹 ====================

    @Transactional
    public Long createFolder(Long userId, Long parentId, String name) {
        // 校验名称
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
        // 校验父文件夹存在且属于当前用户
        if (parentId != null) {
            NoteFolderEntity parent = baseMapper.selectById(parentId);
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        NoteFolderEntity folder = new NoteFolderEntity();
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setName(name.trim());
        folder.setSortOrder(0);
        baseMapper.insert(folder);
        log.info("创建文件夹: userId={}, folderId={}, name={}", userId, folder.getId(), name);
        return folder.getId();
    }

    // ==================== 重命名/移动文件夹 ====================

    @Transactional
    public void updateFolder(Long userId, Long folderId, String name, Long parentId) {
        NoteFolderEntity folder = getFolderWithAuth(userId, folderId);

        if (name != null && !name.trim().isEmpty()) {
            folder.setName(name.trim());
        }

        if (parentId != null) {
            // 防循环引用：目标不能是自己或子孙节点
            if (parentId.equals(folderId) || isDescendantOf(userId, parentId, folderId)) {
                throw new BusinessException(ErrorCode.FOLDER_CIRCULAR_REFERENCE);
            }
            // 校验目标父文件夹存在
            NoteFolderEntity targetParent = baseMapper.selectById(parentId);
            if (targetParent == null || !targetParent.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
            }
            folder.setParentId(parentId);
        }

        baseMapper.updateById(folder);
        log.info("更新文件夹: folderId={}, name={}, parentId={}", folderId, name, parentId);
    }

    // ==================== 删除文件夹 ====================

    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        NoteFolderEntity folder = getFolderWithAuth(userId, folderId);

        // 1. 收集该文件夹及所有子孙文件夹ID
        Set<Long> allFolderIds = new HashSet<>();
        collectDescendantFoldIds(userId, folderId, allFolderIds);
        allFolderIds.add(folderId);

        // 2. 将所有这些文件夹下的笔记移入回收站
        List<NoteEntity> notesToDelete = new ArrayList<>();
        for (Long fid : allFolderIds) {
            notesToDelete.addAll(findNotesInFolder(fid));
        }
        for (NoteEntity note : notesToDelete) {
            note.setIsDeleted(1);
            note.setDeletedAt(LocalDateTime.now());
            noteMapper.updateById(note);
            // 清除该笔记的缓存（避免 Redis 脏数据）
            noteService.invalidateNoteCache(note.getId(), userId);
        }

        // 3. 删除所有子孙文件夹
        for (Long fid : allFolderIds) {
            if (!fid.equals(folderId)) {
                baseMapper.deleteById(fid);
            }
        }

        // 4. 删除当前文件夹
        baseMapper.deleteById(folderId);

        log.info("删除文件夹: folderId={}, 级联删除子文件夹数={}, 移入回收站笔记数={}",
                folderId, allFolderIds.size() - 1, notesToDelete.size());
    }

    // ==================== 工具方法 ====================

    private NoteFolderEntity getFolderWithAuth(Long userId, Long folderId) {
        NoteFolderEntity folder = baseMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    /**
     * 检查 targetId 是否是 sourceId 的子孙节点
     */
    private boolean isDescendantOf(Long userId, Long targetId, Long sourceId) {
        NoteFolderEntity target = baseMapper.selectById(targetId);
        if (target == null || !target.getUserId().equals(userId)) {
            return false;
        }
        Long currentId = target.getParentId();
        while (currentId != null) {
            if (currentId.equals(sourceId)) {
                return true;
            }
            NoteFolderEntity current = baseMapper.selectById(currentId);
            if (current == null) break;
            currentId = current.getParentId();
        }
        return false;
    }

    /**
     * 递归收集所有子孙文件夹ID
     */
    private void collectDescendantFoldIds(Long userId, Long folderId, Set<Long> result) {
        List<NoteFolderEntity> children = baseMapper.selectByParentId(folderId, userId);
        for (NoteFolderEntity child : children) {
            result.add(child.getId());
            collectDescendantFoldIds(userId, child.getId(), result);
        }
    }

    /**
     * 查找指定文件夹下的所有活跃笔记
     */
    private List<NoteEntity> findNotesInFolder(Long folderId) {
        return noteMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NoteEntity>()
                        .eq("folder_id", folderId)
                        .eq("is_deleted", 0));
    }
}
