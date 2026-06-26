-- V8: 多轮对话支持（Day 09 Part B）

CREATE TABLE IF NOT EXISTS `conversation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `title`       VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '对话标题（首条提问前50字）',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_updated` (`user_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id` BIGINT       NOT NULL COMMENT '所属对话ID',
    `question_id`     VARCHAR(36)  NOT NULL COMMENT '问答对分组UUID',
    `role`            VARCHAR(20)  NOT NULL COMMENT '角色：user/assistant/system',
    `content`         MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/WAIT/ERROR（预留流式增量更新）',
    `sources`         TEXT         DEFAULT NULL COMMENT '引用来源JSON [{noteId,title}]',
    `token_count`     INT          DEFAULT NULL COMMENT 'Token估算数',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_conv_status` (`conversation_id`, `status`),
    CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`)
        REFERENCES `conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';
