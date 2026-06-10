-- 优化 note 表索引：用复合索引替代独立低区分度索引
-- 目标：覆盖最高频查询 WHERE user_id = ? AND is_deleted = ? ORDER BY updated_at DESC

-- 新增复合索引：等值条件(user_id, is_deleted) + 排序字段(updated_at)
ALTER TABLE `note` ADD INDEX `idx_user_deleted_updated` (`user_id`, `is_deleted`, `updated_at`);

-- 删除低区分度冗余索引：is_deleted 只有 0/1，独立索引优化器几乎不选
ALTER TABLE `note` DROP INDEX `idx_is_deleted`;
