-- V10: 新增文件夹表 + note 表添加 folder_id 字段
-- 说明: 标签降级为 frontmatter 元数据，文件夹成为笔记的主要组织方式

-- 1. 创建文件夹表
CREATE TABLE IF NOT EXISTS note_folder (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '所属用户ID',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父文件夹ID，NULL表示根目录',
    name        VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    sort_order  INT          DEFAULT 0 COMMENT '排序序号，同层内升序排列',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记文件夹';

-- 2. note 表新增 folder_id 字段
ALTER TABLE note ADD COLUMN folder_id BIGINT DEFAULT NULL COMMENT '所属文件夹ID，NULL表示根目录';
ALTER TABLE note ADD INDEX idx_folder_id (folder_id);
