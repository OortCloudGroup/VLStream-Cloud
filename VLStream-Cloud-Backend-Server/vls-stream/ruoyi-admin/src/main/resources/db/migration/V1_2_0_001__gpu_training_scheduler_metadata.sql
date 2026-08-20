-- This migration is deliberately idempotent because v1.2.0 deployments may
-- have applied the scheduler schema manually before Flyway tracked it.
SET @schema_name = DATABASE();

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'training_task_id'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN training_task_id BIGINT NULL COMMENT ''关联算法训练任务ID'' AFTER logs_path');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'server_id'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN server_id BIGINT NULL COMMENT ''GPU服务器ID'' AFTER training_task_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'server_ip'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN server_ip VARCHAR(128) NULL COMMENT ''GPU服务器地址'' AFTER server_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'gpu_index'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN gpu_index INT NULL COMMENT ''GPU序号'' AFTER server_ip');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'gpu_uuid'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN gpu_uuid VARCHAR(128) NULL COMMENT ''GPU UUID'' AFTER gpu_index');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'queue_time'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN queue_time DATETIME NULL COMMENT ''排队时间'' AFTER gpu_uuid');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND column_name = 'error_message'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD COLUMN error_message VARCHAR(1000) NULL COMMENT ''运行错误信息'' AFTER queue_time');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND index_name = 'idx_container_training_task'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD INDEX idx_container_training_task (training_task_id)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'vls_container_instance' AND index_name = 'idx_container_queue'),
    'SELECT 1',
    'ALTER TABLE vls_container_instance ADD INDEX idx_container_queue (instance_type, instance_status, queue_time)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
