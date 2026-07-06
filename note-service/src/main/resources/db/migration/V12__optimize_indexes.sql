-- 复合索引：覆盖聊天历史查询（conversation_id + role + status + ORDER BY created_at）
ALTER TABLE message
  ADD INDEX idx_conv_role_status_created (conversation_id, role, status, created_at);

-- 复合索引：覆盖文件夹列表查询（parent_id + user_id + ORDER BY sort_order）
ALTER TABLE note_folder
  ADD INDEX idx_parent_user_sort (parent_id, user_id, sort_order);
