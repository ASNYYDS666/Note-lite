-- V9: 重命名 ai_provider.key → provider_key（key 是 MySQL 保留字）
-- 同时修复 ai_model 外键引用

ALTER TABLE `ai_model` DROP FOREIGN KEY `fk_model_provider`;

ALTER TABLE `ai_provider` CHANGE COLUMN `key` `provider_key` VARCHAR(30) NOT NULL;
ALTER TABLE `ai_provider` DROP INDEX `uk_key`;
ALTER TABLE `ai_provider` ADD UNIQUE INDEX `uk_provider_key` (`provider_key`);

-- 重建 ai_model 外键
ALTER TABLE `ai_model`
    ADD CONSTRAINT `fk_model_provider` FOREIGN KEY (`provider_key`)
        REFERENCES `ai_provider` (`provider_key`) ON DELETE CASCADE;
