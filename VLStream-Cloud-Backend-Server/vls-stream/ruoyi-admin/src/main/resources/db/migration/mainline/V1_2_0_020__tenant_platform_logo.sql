CREATE TABLE IF NOT EXISTS `sys_platform_logo` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL DEFAULT '000000' COMMENT 'Owning tenant',
    `oss_id` bigint NOT NULL COMMENT 'sys_oss object ID',
    `description` varchar(500) NOT NULL DEFAULT '' COMMENT 'Configuration description',
    `active` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether this logo is currently active',
    `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NULL COMMENT 'Created at',
    `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NULL COMMENT 'Updated at',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
    PRIMARY KEY (`id`),
    KEY `idx_platform_logo_tenant_active` (`tenant_id`, `active`, `is_deleted`),
    KEY `idx_platform_logo_oss` (`oss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tenant platform logo configurations';

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000002000, '平台设置', 1, 12, 'platform-logo',
    'system/platformLogo/index', '', 1, 0, 'C', '0', '0', 'system:platformLogo:list', 'monitor',
    'admin', NOW(), '', NULL, '租户平台标识设置菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000002000);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000002001, '平台标识新增', 1900000000000002000, 1, '#', '', '', 1, 0, 'F', '0', '0',
    'system:platformLogo:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000002001);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000002002, '平台标识修改', 1900000000000002000, 2, '#', '', '', 1, 0, 'F', '0', '0',
    'system:platformLogo:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000002002);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000002003, '平台标识删除', 1900000000000002000, 3, '#', '', '', 1, 0, 'F', '0', '0',
    'system:platformLogo:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000002003);
