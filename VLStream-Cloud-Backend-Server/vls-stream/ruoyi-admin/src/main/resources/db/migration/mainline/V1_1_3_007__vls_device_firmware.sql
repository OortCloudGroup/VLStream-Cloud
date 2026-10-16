CREATE TABLE IF NOT EXISTS `vls_device_firmware` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL DEFAULT '000000' COMMENT 'Owning tenant',
    `camera_model` varchar(128) NOT NULL COMMENT 'Camera model code',
    `firmware_version` varchar(64) NOT NULL COMMENT 'Semantic firmware version',
    `oss_config_key` varchar(64) NOT NULL COMMENT 'sys_oss_config.config_key',
    `object_key` varchar(512) NOT NULL COMMENT 'MinIO object key',
    `original_file_name` varchar(255) NOT NULL COMMENT 'Original upload file name',
    `content_type` varchar(128) NOT NULL COMMENT 'Upload Content-Type',
    `file_size` bigint NOT NULL COMMENT 'Firmware package size in bytes',
    `sha256` char(64) NULL COMMENT 'Verified package SHA-256',
    `upload_status` varchar(20) NOT NULL COMMENT 'UPLOADING/READY',
    `upload_expires_at` datetime NOT NULL COMMENT 'Presigned PUT URL expiration time',
    `create_user` varchar(64) NULL,
    `create_dept` varchar(64) NULL,
    `create_time` datetime NULL,
    `update_user` varchar(64) NULL,
    `update_time` datetime NULL,
    `status` int NOT NULL DEFAULT 1,
    `is_deleted` int NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vls_firmware_tenant_model_version` (`tenant_id`, `camera_model`, `firmware_version`),
    UNIQUE KEY `uk_vls_firmware_object` (`oss_config_key`, `object_key`),
    KEY `idx_vls_firmware_model_status` (`camera_model`, `upload_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VLS protocol device firmware repository';

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000700, 'VLS协议设备固件管理', 1, 11, 'device-firmware',
    'system/deviceFirmware/index', '', 1, 0, 'C', '0', '0', 'vls:firmware:list', 'upload',
    'admin', NOW(), '', NULL, 'VLS协议设备固件管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000700);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000701, '固件上传', 1900000000000000700, 1, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:firmware:upload', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000701);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000702, '固件下载', 1900000000000000700, 2, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:firmware:download', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000702);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000703, '固件删除', 1900000000000000700, 3, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:firmware:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000703);
