-- Single-node GPU training scheduler metadata.
-- Run once before deploying the matching backend build.
ALTER TABLE `vls_container_instance`
    ADD COLUMN `training_task_id` bigint NULL COMMENT '关联算法训练任务ID' AFTER `logs_path`,
    ADD COLUMN `server_id` bigint NULL COMMENT 'GPU服务器ID' AFTER `training_task_id`,
    ADD COLUMN `server_ip` varchar(128) NULL COMMENT 'GPU服务器地址' AFTER `server_id`,
    ADD COLUMN `gpu_index` int NULL COMMENT 'GPU序号' AFTER `server_ip`,
    ADD COLUMN `gpu_uuid` varchar(128) NULL COMMENT 'GPU UUID' AFTER `gpu_index`,
    ADD COLUMN `queue_time` datetime NULL COMMENT '排队时间' AFTER `gpu_uuid`,
    ADD COLUMN `error_message` varchar(1000) NULL COMMENT '运行错误信息' AFTER `queue_time`,
    ADD INDEX `idx_container_training_task` (`training_task_id`),
    ADD INDEX `idx_container_queue` (`instance_type`, `instance_status`, `queue_time`);
