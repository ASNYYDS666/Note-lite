CREATE TABLE IF NOT EXISTS `note` (
                                      `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `user_id`       BIGINT       NOT NULL COMMENT '作者ID',
                                      `title`         VARCHAR(200) NOT NULL DEFAULT '未命名笔记',
    `content`       LONGTEXT     COMMENT 'Markdown内容',
    `summary`       VARCHAR(500) COMMENT '纯文本摘要',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-正常 1-回收站',
    `deleted_at`    DATETIME     COMMENT '删除时间',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_deleted` (`is_deleted`),
    INDEX `idx_updated_at` (`updated_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 标签关联表（简化版，不单独建标签字典表）

CREATE TABLE IF NOT EXISTS `note_tag` (
                                          `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `note_id`       BIGINT       NOT NULL,
                                          `tag_name`      VARCHAR(50)  NOT NULL COMMENT '标签名',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_note_tag` (`note_id`, `tag_name`),
    INDEX `idx_tag_name` (`tag_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记标签关联表';
