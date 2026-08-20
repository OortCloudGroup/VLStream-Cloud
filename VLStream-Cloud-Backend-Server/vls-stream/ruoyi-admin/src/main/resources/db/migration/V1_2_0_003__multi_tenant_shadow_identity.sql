-- Multi-tenant shadow identity and tenant-scoped role uniqueness.
-- Every statement is guarded because older installations may already have
-- tenant_id/login_id from the legacy initialization runner.

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `tenant_id` char(40) NULL DEFAULT ''000000'' COMMENT ''租户id'' AFTER `user_id`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'tenant_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `login_id` varchar(64) NULL DEFAULT NULL COMMENT ''登录ID'' AFTER `user_name`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'login_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `platform_user_id` varchar(64) NULL DEFAULT NULL COMMENT ''统一平台用户ID'' AFTER `tenant_id`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'platform_user_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'CREATE UNIQUE INDEX `uk_sys_user_tenant_platform` ON `sys_user` (`tenant_id`, `platform_user_id`)',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_sys_user_tenant_platform');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'CREATE UNIQUE INDEX `uk_sys_role_tenant_key` ON `sys_role` (`tenant_id`, `role_key`)',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND INDEX_NAME = 'uk_sys_role_tenant_key');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
