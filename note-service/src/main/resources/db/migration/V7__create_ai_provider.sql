-- V7: AI 厂商/模型两级配置表（Day 09 Part A）
-- 替代 user_ai_config 表中的 provider/api_key/model/url 硬编码字段

CREATE TABLE IF NOT EXISTS `ai_provider` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '厂商显示名',
    `key`         VARCHAR(30)  NOT NULL COMMENT '厂商标识（唯一）',
    `base_url`    VARCHAR(255) NOT NULL COMMENT 'API 基础地址',
    `api_key`     VARCHAR(255) DEFAULT '' COMMENT 'API Key（AES 加密存储，用户填入）',
    `plugin_type` VARCHAR(30)  NOT NULL DEFAULT 'openai_compatible' COMMENT '插件类型：openai_compatible / ollama',
    `is_enabled`  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key`),
    KEY `idx_plugin_type` (`plugin_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 厂商配置表';

CREATE TABLE IF NOT EXISTS `ai_model` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `provider_key` VARCHAR(30)  NOT NULL COMMENT '关联厂商 key',
    `model_name`   VARCHAR(100) NOT NULL COMMENT '模型名',
    `model_type`   VARCHAR(20)  NOT NULL COMMENT 'CHAT / EMBEDDING',
    `is_default`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否该类型的默认模型',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_model` (`provider_key`, `model_name`),
    KEY `idx_provider_key` (`provider_key`),
    KEY `idx_model_type` (`model_type`),
    CONSTRAINT `fk_model_provider` FOREIGN KEY (`provider_key`)
        REFERENCES `ai_provider` (`key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型列表';

-- ============================================
-- 厂商预置数据（api_key 为空，由用户填入）
-- ============================================

-- 1. DeepSeek 官方
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('DeepSeek 官方', 'deepseek', 'https://api.deepseek.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('deepseek', 'deepseek-chat', 'CHAT', 1),
('deepseek', 'deepseek-reasoner', 'CHAT', 0);

-- 2. OpenAI
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('OpenAI', 'openai', 'https://api.openai.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('openai', 'gpt-4o', 'CHAT', 1),
('openai', 'gpt-4o-mini', 'CHAT', 0),
('openai', 'text-embedding-3-small', 'EMBEDDING', 1);

-- 3. 硅基流动
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('硅基流动', 'siliconflow', 'https://api.siliconflow.cn/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('siliconflow', 'Qwen/Qwen3-32B', 'CHAT', 1),
('siliconflow', 'Qwen/Qwen2.5-7B-Instruct', 'CHAT', 0),
('siliconflow', 'deepseek-ai/DeepSeek-R1', 'CHAT', 0),
('siliconflow', 'deepseek-ai/DeepSeek-V3', 'CHAT', 0),
('siliconflow', 'BAAI/bge-m3', 'EMBEDDING', 1),
('siliconflow', 'BAAI/bge-large-zh-v1.5', 'EMBEDDING', 0),
('siliconflow', 'netease-youdao/bce-embedding-base_v1', 'EMBEDDING', 0);

-- 4. 阿里云百炼
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('阿里云百炼', 'aliyun', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('aliyun', 'qwen-plus', 'CHAT', 1),
('aliyun', 'qwen-max', 'CHAT', 0),
('aliyun', 'qwen3-32b', 'CHAT', 0),
('aliyun', 'qwen3-8b', 'CHAT', 0),
('aliyun', 'text-embedding-v3', 'EMBEDDING', 1);

-- 5. 智谱AI
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('智谱AI', 'zhipu', 'https://open.bigmodel.cn/api/paas/v4', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('zhipu', 'glm-4-plus', 'CHAT', 1),
('zhipu', 'glm-4-air', 'CHAT', 0);

-- 6. 腾讯混元
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('腾讯混元', 'hunyuan', 'https://api.hunyuan.cloud.tencent.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('hunyuan', 'hunyuan-pro', 'CHAT', 1),
('hunyuan', 'hunyuan-lite', 'CHAT', 0),
('hunyuan', 'hunyuan-embedding', 'EMBEDDING', 1);

-- 7. 火山引擎
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('火山引擎', 'volcengine', 'https://ark.cn-beijing.volces.com/api/v3', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('volcengine', 'deepseek-v3-250324', 'CHAT', 1),
('volcengine', 'deepseek-r1-250120', 'CHAT', 0),
('volcengine', 'doubao-1-5-pro-32k-250115', 'CHAT', 0),
('volcengine', 'doubao-embedding-large-text-250515', 'EMBEDDING', 1);

-- 8. 月之暗面
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('月之暗面', 'kimi', 'https://api.moonshot.cn/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('kimi', 'kimi-latest', 'CHAT', 1),
('kimi', 'moonshot-v1-8k', 'CHAT', 0),
('kimi', 'moonshot-v1-32k', 'CHAT', 0);

-- 9. 无问芯穹
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('无问芯穹', 'infini', 'https://cloud.infini-ai.com/maas/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('infini', 'qwen3-32b', 'CHAT', 1),
('infini', 'deepseek-r1', 'CHAT', 0),
('infini', 'qwen2.5-32b-instruct', 'CHAT', 0),
('infini', 'bge-m3', 'EMBEDDING', 1);

-- 10. 百川智能
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('百川智能', 'baichuan', 'https://api.baichuan-ai.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('baichuan', 'Baichuan4-Air', 'CHAT', 1),
('baichuan', 'Baichuan2-Turbo', 'CHAT', 0),
('baichuan', 'Baichuan-Text-Embedding', 'EMBEDDING', 1);

-- 11. 阶跃星辰
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('阶跃星辰', 'stepfun', 'https://api.stepfun.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('stepfun', 'step-2-16k', 'CHAT', 1),
('stepfun', 'step-1-32k', 'CHAT', 0);

-- 12. 腾讯云
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('腾讯云', 'tencent', 'https://api.lkeap.cloud.tencent.com/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('tencent', 'deepseek-v3', 'CHAT', 1),
('tencent', 'deepseek-r1', 'CHAT', 0);

-- 13. 百度千帆
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('百度千帆', 'baidu', 'https://qianfan.baidubce.com/v2', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('baidu', 'deepseek-v3', 'CHAT', 1),
('baidu', 'deepseek-r1', 'CHAT', 0),
('baidu', 'qwen3-32b', 'CHAT', 0);

-- 14. OpenRouter
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('OpenRouter', 'openrouter', 'https://openrouter.ai/api/v1', 'openai_compatible');
INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`) VALUES
('openrouter', 'deepseek/deepseek-chat', 'CHAT', 1),
('openrouter', 'mistralai/mistral-7b-instruct:free', 'CHAT', 0),
('openrouter', 'qwen/qwen3-235b-a22b:free', 'CHAT', 0);

-- 15. Ollama 本地
INSERT INTO `ai_provider` (`name`, `key`, `base_url`, `plugin_type`) VALUES
('Ollama 本地', 'ollama', 'http://localhost:11434', 'ollama');
-- Ollama 无预置模型，由用户手动添加
