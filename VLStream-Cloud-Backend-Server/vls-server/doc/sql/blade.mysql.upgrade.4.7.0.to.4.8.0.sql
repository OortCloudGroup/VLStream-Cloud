-- ----------------------------
-- Added permission type field to role permission table
-- ----------------------------
ALTER TABLE `blade_role_scope`
    ADD COLUMN `scope_category` int NULL COMMENT 'Permission Type (1: Data Permission, 2: API Permission)' AFTER `id`,
    MODIFY COLUMN `scope_id` bigint NULL DEFAULT NULL COMMENT 'Permission ID' AFTER `id`;

-- ----------------------------
-- Set historical data permission type to 1 in role permission table
-- ----------------------------
UPDATE `blade_role_scope` SET `scope_category` = 1 WHERE `scope_category` IS NULL;

-- ----------------------------
-- Create interface permission table
-- ----------------------------
CREATE TABLE `blade_scope_api`  (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `menu_id` bigint NULL DEFAULT NULL COMMENT 'Menu primary key',
    `resource_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Resource Code',
    `scope_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Interface permission name',
    `scope_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Interface permission address',
    `scope_type` int NULL DEFAULT NULL COMMENT 'Interface permission type',
    `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Interface permission remarks',
    `create_user` bigint NULL DEFAULT NULL COMMENT 'Created by',
    `create_dept` bigint NULL DEFAULT NULL COMMENT 'Create department',
    `create_time` datetime NULL DEFAULT NULL COMMENT 'Creation time',
    `update_user` bigint NULL DEFAULT NULL COMMENT 'Modified by',
    `update_time` datetime NULL DEFAULT NULL COMMENT 'Modification time',
    `status` int NULL DEFAULT 1 COMMENT 'Status',
    `is_deleted` int NULL DEFAULT 0 COMMENT 'Whether deleted',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Interface permissions table';

-- ----------------------------
-- Dictionary table newly added interface permission type data
-- ----------------------------
INSERT INTO `blade_dict` (`id`, `parent_id`, `code`, `dict_key`, `dict_value`, `sort`, `remark`, `is_deleted`) VALUES (1123598814738675237, 0, 'api_scope_type', '-1', 'Interface permissions', 10, NULL, 0);
INSERT INTO `blade_dict` (`id`, `parent_id`, `code`, `dict_key`, `dict_value`, `sort`, `remark`, `is_deleted`) VALUES (1123598814738675238, 1123598814738675237, 'api_scope_type', '1', '系统Interface', 1, NULL,  0);
INSERT INTO `blade_dict` (`id`, `parent_id`, `code`, `dict_key`, `dict_value`, `sort`, `remark`, `is_deleted`) VALUES (1123598814738675239, 1123598814738675237, 'api_scope_type', '2', '业务Interface', 2, NULL,  0);

-- ----------------------------
-- Add interface permission menu in menu table
-- ----------------------------
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`) VALUES (1123598815738675311, 1123598815738675307, 'api_scope', 'Interface permissions', 'menu', '/authority/apiscope', 'iconfont iconicon_send', 3, 1, 0, 1, NULL, 0);
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`) VALUES (1123598815738675312, 1123598815738675311, 'api_scope_setting', 'Permission Configuration', 'setting', NULL, 'setting', 1, 2, 2, 1, NULL, 0);
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`) VALUES (2006703481530257413, 1123598815738675311, 1123598816738675201);
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`) VALUES (2006703481530257414, 1123598815738675312, 1123598816738675201);
