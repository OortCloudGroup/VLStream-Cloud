-- Extend the firmware repository for target-aware OTA deployment.
-- V1_1_3_007 may already be applied, so this migration only moves the schema forward.

ALTER TABLE `vls_mqtt_device`
    ADD COLUMN `device_model` varchar(128) NULL COMMENT 'Hardware-reported model code' AFTER `device_serial`,
    ADD COLUMN `application_version` varchar(64) NULL COMMENT 'Current application firmware version' AFTER `firmware_version`,
    ADD COLUMN `rootfs_version` varchar(64) NULL COMMENT 'Current rootfs firmware version' AFTER `application_version`;

UPDATE `vls_mqtt_device`
SET `application_version` = `firmware_version`
WHERE `application_version` IS NULL
  AND `firmware_version` IS NOT NULL;

ALTER TABLE `vls_device_firmware`
    ADD COLUMN `target` varchar(16) NOT NULL DEFAULT 'application'
        COMMENT 'Upgrade target: application/rootfs' AFTER `camera_model`,
    DROP INDEX `uk_vls_firmware_tenant_model_version`,
    ADD UNIQUE KEY `uk_vls_firmware_tenant_model_target_version`
        (`tenant_id`, `camera_model`, `target`, `firmware_version`),
    ADD KEY `idx_vls_firmware_model_target_ready`
        (`camera_model`, `target`, `upload_status`, `create_time`);

CREATE TABLE IF NOT EXISTS `vls_firmware_deploy_task` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL DEFAULT '000000' COMMENT 'Owning tenant',
    `request_id` varchar(64) NOT NULL COMMENT 'OTA task id used by hardware replies',
    `mqtt_message_id` varchar(64) NOT NULL COMMENT 'Outbound MQTT message id',
    `device_row_id` bigint NOT NULL COMMENT 'vls_mqtt_device.id',
    `device_id` varchar(100) NOT NULL COMMENT 'Hardware device identifier',
    `device_model` varchar(128) NOT NULL COMMENT 'Model captured when dispatched',
    `target` varchar(16) NOT NULL COMMENT 'application/rootfs',
    `current_version` varchar(64) NOT NULL COMMENT 'Version captured when dispatched',
    `target_version` varchar(64) NOT NULL COMMENT 'Firmware version to install',
    `firmware_id` bigint NOT NULL COMMENT 'vls_device_firmware.id',
    `file_name` varchar(255) NOT NULL,
    `file_size` bigint NOT NULL,
    `sha256` char(64) NOT NULL,
    `rollback_enable` tinyint(1) NOT NULL DEFAULT 0,
    `reboot_after` tinyint(1) NOT NULL DEFAULT 0,
    `deploy_status` varchar(24) NOT NULL COMMENT 'CREATED/PUBLISHED/processing/terminal status',
    `mqtt_topic` varchar(255) NOT NULL,
    `download_expires_at` bigint NOT NULL COMMENT 'Signed URL expiry, epoch seconds',
    `published_at` datetime NULL,
    `last_reply_at` datetime NULL,
    `completed_at` datetime NULL,
    `failure_reason` varchar(2000) NULL,
    `reply_payload` text NULL,
    `create_user` varchar(64) NULL,
    `create_dept` varchar(64) NULL,
    `create_time` datetime NULL,
    `update_user` varchar(64) NULL,
    `update_time` datetime NULL,
    `status` int NOT NULL DEFAULT 1,
    `is_deleted` int NOT NULL DEFAULT 0,
    `active_slot` tinyint GENERATED ALWAYS AS (
        CASE
            WHEN `is_deleted` = 0 AND `deploy_status` IN
                ('CREATED', 'PUBLISHED', 'ACCEPTED', 'DOWNLOADING', 'VERIFYING', 'INSTALLING', 'REBOOTING')
            THEN 1
            ELSE NULL
        END
    ) STORED COMMENT 'Non-null only while an OTA task is active',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vls_firmware_task_request` (`request_id`),
    UNIQUE KEY `uk_vls_firmware_task_message` (`mqtt_message_id`),
    UNIQUE KEY `uk_vls_firmware_task_active`
        (`tenant_id`, `device_row_id`, `target`, `active_slot`),
    KEY `idx_vls_firmware_task_device_created` (`device_row_id`, `create_time`),
    KEY `idx_vls_firmware_task_device_target_status` (`device_row_id`, `target`, `deploy_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='VLS device OTA firmware deployment task';

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000704, '固件下发', 1900000000000000700, 4, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:firmware:deploy', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000704);
