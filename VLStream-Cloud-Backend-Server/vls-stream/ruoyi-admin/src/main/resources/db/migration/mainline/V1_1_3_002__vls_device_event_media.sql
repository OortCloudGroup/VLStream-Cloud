-- Add direct device-event media uploads and MQTT event idempotency.
-- This migration follows V1_1_3_001__model_dispatch_protocol_v2.sql.

CREATE TABLE IF NOT EXISTS `vls_device_media_upload` (
    `id` bigint NOT NULL COMMENT '主键',
    `media_id` varchar(64) NOT NULL COMMENT '媒体UUID',
    `device_id` varchar(100) NOT NULL COMMENT '设备编号',
    `oss_config_key` varchar(64) NOT NULL COMMENT 'sys_oss_config.config_key',
    `object_key` varchar(512) NOT NULL COMMENT '对象存储KEY',
    `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `content_type` varchar(100) NOT NULL COMMENT 'Content-Type',
    `file_size` bigint NOT NULL COMMENT '期望文件字节数',
    `sha256` char(64) NOT NULL COMMENT '期望SHA-256',
    `upload_status` varchar(20) NOT NULL COMMENT 'ISSUED/BOUND',
    `expires_at` datetime NOT NULL COMMENT 'PUT签名地址过期时间',
    `bound_event_message_id` varchar(64) NULL COMMENT '绑定的MQTT事件messageId',
    `create_time` datetime NOT NULL,
    `update_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vls_device_media_id` (`media_id`),
    UNIQUE KEY `uk_vls_device_media_object` (`oss_config_key`, `object_key`),
    KEY `idx_vls_device_media_device` (`device_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='硬件事件媒体预签名上传记录';

SET @column_exists = (
    SELECT COUNT(*) FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `column_name` = 'mqtt_message_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `vls_event_management` ADD COLUMN `mqtt_message_id` varchar(64) NULL COMMENT ''MQTT上报messageId'' AFTER `id`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @column_exists = (
    SELECT COUNT(*) FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `column_name` = 'device_event_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `vls_event_management` ADD COLUMN `device_event_id` varchar(64) NULL COMMENT ''设备侧事件ID'' AFTER `mqtt_message_id`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @column_exists = (
    SELECT COUNT(*) FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `column_name` = 'media_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `vls_event_management` ADD COLUMN `media_id` varchar(64) NULL COMMENT ''事件图片mediaId'' AFTER `device_event_id`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

ALTER TABLE `vls_event_management`
    MODIFY COLUMN `report_img` varchar(1024) NULL COMMENT '对象存储内部引用';

SET @index_exists = (
    SELECT COUNT(*) FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `index_name` = 'uk_vls_event_mqtt_message'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_event_management` ADD UNIQUE KEY `uk_vls_event_mqtt_message` (`mqtt_message_id`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `index_name` = 'uk_vls_event_device_event'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_event_management` ADD UNIQUE KEY `uk_vls_event_device_event` (`report_device`, `device_event_id`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_event_management'
      AND `index_name` = 'idx_vls_event_media_id'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_event_management` ADD KEY `idx_vls_event_media_id` (`media_id`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;
