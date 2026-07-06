-- V10: 新增本地 Embedding 厂商（bge-small-zh-v1.5）
-- 用户可在 AI 设置中独立选择 Embedding Provider，与 Chat Provider 解耦

INSERT INTO `ai_provider` (`name`, `provider_key`, `base_url`, `plugin_type`, `api_key`)
VALUES ('本地 Embedding', 'local-embedding', 'http://localhost:8081', 'openai_compatible', 'local');

INSERT INTO `ai_model` (`provider_key`, `model_name`, `model_type`, `is_default`)
VALUES ('local-embedding', 'bge-small-zh-v1.5', 'EMBEDDING', 1);
