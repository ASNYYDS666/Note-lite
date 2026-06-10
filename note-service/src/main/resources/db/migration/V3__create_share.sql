CREATE TABLE IF NOT EXISTS `share` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `note_id`    BIGINT       NOT NULL COMMENT '被分享的笔记ID',
    `code`       VARCHAR(8)   NOT NULL COMMENT '分享码，8位字母数字',
    `permission` VARCHAR(10)  NOT NULL DEFAULT 'READ' COMMENT 'READ或WRITE',
    `expire_at`  DATETIME     NOT NULL COMMENT '过期时间',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_note_id` (`note_id`),
    INDEX `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表';
