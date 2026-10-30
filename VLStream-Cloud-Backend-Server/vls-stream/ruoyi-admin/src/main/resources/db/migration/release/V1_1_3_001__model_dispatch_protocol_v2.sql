-- Add the durable model dispatch state introduced after VLStream Cloud v1.1.2.
-- The guards below also support development databases where the legacy SQL
-- files may already have been executed manually.

CREATE TABLE IF NOT EXISTS `vls_model_dispatch_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` varchar(64) NULL DEFAULT NULL COMMENT '租户ID',
    `request_id` varchar(64) NOT NULL COMMENT '下发任务唯一编号',
    `mqtt_message_id` varchar(64) NULL COMMENT 'V2.2下发消息messageId',
    `device_row_id` bigint NOT NULL COMMENT '设备表主键',
    `device_id` varchar(100) NOT NULL COMMENT '硬件设备编号',
    `algorithm_id` bigint NOT NULL COMMENT '算法ID',
    `training_id` bigint NOT NULL COMMENT '训练任务ID',
    `model_type` varchar(20) NOT NULL COMMENT 'pt/onnx/rknn/int8-rknn/om',
    `remote_path` varchar(1000) NOT NULL COMMENT '训练服务器模型路径',
    `file_name` varchar(255) NOT NULL COMMENT '模型文件名',
    `file_size` bigint NOT NULL COMMENT '模型字节数',
    `sha256` char(64) NOT NULL COMMENT '模型SHA-256',
    `dispatch_status` varchar(32) NOT NULL COMMENT 'CREATED/PUBLISHED/DOWNLOADING/DOWNLOADED/VERIFYING/DEPLOYING/SUCCESS/FAILED',
    `mqtt_topic` varchar(255) NULL DEFAULT NULL COMMENT '下发MQTT主题',
    `download_expires_at` bigint NOT NULL COMMENT '下载链接到期Unix秒',
    `published_at` datetime NULL DEFAULT NULL COMMENT 'MQTT发布时间',
    `download_started_at` datetime NULL DEFAULT NULL COMMENT '下载开始时间',
    `download_completed_at` datetime NULL DEFAULT NULL COMMENT '下载完成时间',
    `deployed_at` datetime NULL DEFAULT NULL COMMENT '硬件部署成功时间',
    `last_reply_at` datetime NULL DEFAULT NULL COMMENT '最近硬件回执时间',
    `failure_reason` varchar(2000) NULL DEFAULT NULL COMMENT '失败原因',
    `reply_payload` text NULL COMMENT '最近硬件回执原文',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
    `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
    `create_dept` varchar(64) NULL DEFAULT NULL COMMENT '创建部门',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_user` bigint NULL DEFAULT NULL COMMENT '修改人',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `status` int NULL DEFAULT 1 COMMENT '记录状态',
    `is_deleted` int NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_dispatch_request_id` (`request_id`),
    UNIQUE KEY `uk_model_dispatch_mqtt_message_id` (`mqtt_message_id`),
    KEY `idx_model_dispatch_device_id` (`device_id`),
    KEY `idx_model_dispatch_training_id` (`training_id`),
    KEY `idx_model_dispatch_status` (`dispatch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型下发任务表';

SET @vls_mqtt_message_id_column_exists = (
    SELECT COUNT(*)
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_model_dispatch_task'
      AND `column_name` = 'mqtt_message_id'
);
SET @vls_add_mqtt_message_id_sql = IF(
    @vls_mqtt_message_id_column_exists = 0,
    'ALTER TABLE `vls_model_dispatch_task` ADD COLUMN `mqtt_message_id` varchar(64) NULL COMMENT ''V2.2下发消息messageId'' AFTER `request_id`',
    'SELECT 1'
);
PREPARE vls_add_mqtt_message_id_statement FROM @vls_add_mqtt_message_id_sql;
EXECUTE vls_add_mqtt_message_id_statement;
DEALLOCATE PREPARE vls_add_mqtt_message_id_statement;

UPDATE `vls_model_dispatch_task`
SET `mqtt_message_id` = `request_id`
WHERE `mqtt_message_id` IS NULL;

ALTER TABLE `vls_model_dispatch_task`
    MODIFY COLUMN `mqtt_message_id` varchar(64) NOT NULL COMMENT 'V2.2下发消息messageId';

SET @vls_mqtt_message_id_index_exists = (
    SELECT COUNT(*)
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'vls_model_dispatch_task'
      AND `index_name` = 'uk_model_dispatch_mqtt_message_id'
);
SET @vls_add_mqtt_message_id_index_sql = IF(
    @vls_mqtt_message_id_index_exists = 0,
    'ALTER TABLE `vls_model_dispatch_task` ADD UNIQUE KEY `uk_model_dispatch_mqtt_message_id` (`mqtt_message_id`)',
    'SELECT 1'
);
PREPARE vls_add_mqtt_message_id_index_statement FROM @vls_add_mqtt_message_id_index_sql;
EXECUTE vls_add_mqtt_message_id_index_statement;
DEALLOCATE PREPARE vls_add_mqtt_message_id_index_statement;
