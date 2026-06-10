CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    `email`      VARCHAR(100) COMMENT '邮箱',
    `avatar_url` VARCHAR(500) COMMENT '头像URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
