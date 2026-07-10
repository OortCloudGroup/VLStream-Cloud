-- Local single-tenant RuoYi user system seed.
-- This script is idempotent and targets the local user/permission tables.

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `sys_user` ADD COLUMN `tenant_id` char(40) NULL DEFAULT ''000000'' COMMENT ''租户id'' AFTER `user_id`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'tenant_id');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `sys_user` ADD COLUMN `login_id` varchar(64) NULL DEFAULT NULL COMMENT ''登录ID'' AFTER `user_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'login_id');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `sys_user` ADD COLUMN `job_id` varchar(64) NULL DEFAULT NULL COMMENT ''职务ID'' AFTER `dept_id`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'job_id');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `sys_user` ADD COLUMN `post_id` varchar(64) NULL DEFAULT NULL COMMENT ''岗位ID'' AFTER `job_id`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'post_id');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `sys_user` ADD COLUMN `dept_code` varchar(128) NULL DEFAULT NULL COMMENT ''部门编码'' AFTER `dept_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'dept_code');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `sys_user` MODIFY COLUMN `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户昵称';

INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES (100, '000000', 0, '0', '默认组织', 0, 'admin', '', '', '0', '0', 'system', NOW())
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`), `dept_name` = VALUES(`dept_name`), `status` = '0', `del_flag` = '0';

INSERT INTO `sys_post` (`post_id`, `tenant_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`)
VALUES (1, '000000', 'admin', '管理员', 1, '0', 'system', NOW())
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`), `post_name` = VALUES(`post_name`), `status` = '0';

INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES (1, '000000', '超级管理员', 'admin', 1, '1', '0', '0', 'system', NOW())
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`), `role_name` = VALUES(`role_name`), `role_key` = VALUES(`role_key`), `status` = '0', `del_flag` = '0';

UPDATE `sys_user`
SET `user_name` = CONCAT('disabled_', LEFT(MD5(CAST(`user_id` AS CHAR)), 20)),
    `login_id` = CONCAT('disabled_', LEFT(MD5(CAST(`user_id` AS CHAR)), 20)),
    `status` = '1',
    `del_flag` = '2',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE (`user_name` = 'admin' OR `login_id` = 'admin')
  AND CAST(`user_id` AS CHAR) <> '1';

INSERT INTO `sys_user` (`user_id`, `tenant_id`, `dept_id`, `job_id`, `post_id`, `user_name`, `login_id`, `idcard`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `create_by`, `create_time`, `dept_name`, `dept_code`)
VALUES ('1', '000000', 100, NULL, '1', 'admin', 'admin', '', '管理员', 'sys_user', 'admin@example.local', '', '0', '', '$2a$10$3xdbUdL3XaiLCGmg8OOIseOgV1l6i/Nl8i10X84AOs7r2nlE3oVyS', '0', '0', 'system', NOW(), '默认组织', 'default')
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`), `dept_id` = VALUES(`dept_id`), `post_id` = VALUES(`post_id`), `login_id` = VALUES(`login_id`), `nick_name` = VALUES(`nick_name`), `password` = VALUES(`password`), `status` = '0', `del_flag` = '0';

INSERT INTO `sys_user_role` (`user_id`, `tenant_id`, `role_id`)
VALUES ('1', '000000', 1)
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`);

INSERT INTO `sys_user_post` (`user_id`, `tenant_id`, `post_id`)
VALUES ('1', '000000', 1)
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`);

INSERT INTO `sys_menu` (`menu_id`, `tenant_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(900000, '000000', '系统管理', 0, 90, 'system', NULL, NULL, 1, 0, 'M', '0', '0', 'system', 'setting', 'system', NOW(), '系统管理目录'),
(900100, '000000', '用户管理', 900000, 1, 'users', 'system/users', NULL, 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'system', NOW(), '用户管理'),
(900200, '000000', '角色管理', 900000, 2, 'roles', 'system/roles', NULL, 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'system', NOW(), '角色管理'),
(900300, '000000', '菜单管理', 900000, 3, 'menus', 'system/menus', NULL, 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'system', NOW(), '菜单管理'),
(900400, '000000', '部门管理', 900000, 4, 'depts', 'system/depts', NULL, 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'system', NOW(), '部门管理'),
(900500, '000000', '岗位管理', 900000, 5, 'posts', 'system/posts', NULL, 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'system', NOW(), '岗位管理'),
(901000, '000000', '主动安全', 0, 91, 'active-safety', NULL, NULL, 1, 0, 'M', '0', '0', 'active-safety:list', 'shield', 'system', NOW(), '主动安全目录'),
(901100, '000000', '主动安全事件', 901000, 1, 'events/secure', 'active-safety/events/secure', NULL, 1, 0, 'C', '0', '0', 'active-safety:events:list', 'shield', 'system', NOW(), '主动安全事件'),
(901200, '000000', '我的工单', 901000, 2, 'work-orders/my', 'active-safety/work-orders/my', NULL, 1, 0, 'C', '0', '0', 'active-safety:workorder:my', 'list', 'system', NOW(), '我的工单'),
(901300, '000000', '待办工单', 901000, 3, 'work-orders/pending', 'active-safety/work-orders/pending', NULL, 1, 0, 'C', '0', '0', 'active-safety:workorder:pending', 'list', 'system', NOW(), '待办工单'),
(901400, '000000', '已办工单', 901000, 4, 'work-orders/completed', 'active-safety/work-orders/completed', NULL, 1, 0, 'C', '0', '0', 'active-safety:workorder:completed', 'list', 'system', NOW(), '已办工单'),
(901500, '000000', '可接工单', 901000, 5, 'work-orders/claimable', 'active-safety/work-orders/claimable', NULL, 1, 0, 'C', '0', '0', 'active-safety:workorder:claimable', 'list', 'system', NOW(), '可接工单'),
(901600, '000000', '主动安全设置', 901000, 6, 'settings/secure', 'active-safety/settings/secure', NULL, 1, 0, 'C', '0', '0', 'active-safety:settings:secure', 'setting', 'system', NOW(), '主动安全设置'),
(901700, '000000', '工单设置', 901000, 7, 'settings/work-orders', 'active-safety/settings/work-orders', NULL, 1, 0, 'C', '0', '0', 'active-safety:settings:workorder', 'setting', 'system', NOW(), '工单设置')
ON DUPLICATE KEY UPDATE `tenant_id` = VALUES(`tenant_id`), `menu_name` = VALUES(`menu_name`), `path` = VALUES(`path`), `component` = VALUES(`component`), `visible` = '0', `status` = '0';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `tenant_id`, `menu_id`)
SELECT 1, '000000', `menu_id` FROM `sys_menu` WHERE `tenant_id` = '000000';
