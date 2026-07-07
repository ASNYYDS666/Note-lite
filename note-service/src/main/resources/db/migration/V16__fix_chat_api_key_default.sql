-- V16: chat_api_key 列改为有默认值，兼容实体类不映射该列时的 INSERT
-- 该列已不再使用（API Key 由 user_api_profile 表管理），但 NOT NULL 无默认值导致新插入失败
ALTER TABLE `user_ai_config` MODIFY `chat_api_key` VARCHAR(255) NOT NULL DEFAULT '';
