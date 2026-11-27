-- The SysUserRoleView entity maps audit/display fields that the legacy
-- sys_user_role join table never had. MyBatis-Plus generated selects (for
-- example selectById used before role deletion) include these columns, so the
-- missing columns surface as "Unknown column 'created_at'".
-- Add nullable columns and backfill role_name; existing rows stay valid.

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_role` ADD COLUMN `created_at` datetime NULL DEFAULT NULL COMMENT ''创建时间'' AFTER `role_id`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'created_at');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_role` ADD COLUMN `updated_at` varchar(64) NULL DEFAULT NULL COMMENT ''更新时间'' AFTER `created_at`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'updated_at');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_role` ADD COLUMN `deleted_at` bigint NULL DEFAULT NULL COMMENT ''删除时间'' AFTER `updated_at`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'deleted_at');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_role` ADD COLUMN `role_name` varchar(30) NULL DEFAULT NULL COMMENT ''角色名称'' AFTER `deleted_at`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'role_name');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Existing bindings keep their role name for the delete guard's user-facing
-- message. Inserts after this migration may leave it NULL; custom queries
-- continue to join sys_role for the authoritative name.
UPDATE sys_user_role sur
JOIN sys_role r ON r.role_id = sur.role_id
SET sur.role_name = r.role_name
WHERE sur.role_name IS NULL;
