CREATE TABLE IF NOT EXISTS `user_ai_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `provider`    VARCHAR(20)  NOT NULL DEFAULT 'deepseek' COMMENT 'AI服务商(deepseek/openai)',
    `api_key`     VARCHAR(255) NOT NULL COMMENT 'API Key(AES-128-ECB加密)',
    `chat_model`  VARCHAR(50)  NOT NULL DEFAULT 'deepseek-chat' COMMENT '对话模型',
    `embed_model` VARCHAR(50)  NOT NULL DEFAULT 'text-embedding-3-small' COMMENT '向量化模型',
    `is_enabled`  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用,1=启用)',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户AI配置表';
