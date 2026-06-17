ALTER TABLE `user_ai_config`
  CHANGE `provider` `chat_provider` VARCHAR(20) NOT NULL DEFAULT 'deepseek' COMMENT 'Chat服务商',
  CHANGE `api_key` `chat_api_key` VARCHAR(255) NOT NULL COMMENT 'Chat API Key(AES加密)',
  ADD COLUMN `chat_url` VARCHAR(255) DEFAULT NULL COMMENT 'Chat自定义API地址' AFTER `chat_model`,
  ADD COLUMN `embed_provider` VARCHAR(20) NOT NULL DEFAULT 'openai' COMMENT 'Embedding服务商' AFTER `chat_url`,
  ADD COLUMN `embed_api_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Embedding API Key(AES加密)' AFTER `embed_provider`,
  ADD COLUMN `embed_url` VARCHAR(255) DEFAULT NULL COMMENT 'Embedding自定义API地址' AFTER `embed_model`;
