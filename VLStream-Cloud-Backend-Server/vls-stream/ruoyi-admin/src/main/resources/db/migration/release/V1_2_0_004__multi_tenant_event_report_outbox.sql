-- Reliable, tenant-aware delivery of locally persisted device events.

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_device_media_upload'
      AND column_name = 'tenant_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `vls_device_media_upload` ADD COLUMN `tenant_id` varchar(64) NULL COMMENT ''Owning tenant'' AFTER `id`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE `vls_device_media_upload` upload
SET upload.`tenant_id` = COALESCE(
    (
        SELECT MIN(device.`tenant_id`)
        FROM `vls_device_info` device
        -- Existing installations may use different utf8mb4 collations on these
        -- two legacy tables. Normalize only this comparison during backfill.
        WHERE device.`device_id` COLLATE utf8mb4_unicode_ci =
              upload.`device_id` COLLATE utf8mb4_unicode_ci
          AND device.`is_deleted` = 0
          AND device.`tenant_id` IS NOT NULL
          AND device.`tenant_id` <> ''
    ),
    '000000'
)
WHERE upload.`tenant_id` IS NULL OR upload.`tenant_id` = '';

ALTER TABLE `vls_device_media_upload`
    MODIFY COLUMN `tenant_id` varchar(64) NOT NULL COMMENT 'Owning tenant';

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_device_media_upload'
      AND index_name = 'idx_vls_device_media_tenant_device'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_device_media_upload` ADD KEY `idx_vls_device_media_tenant_device` (`tenant_id`, `device_id`, `create_time`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_event_management'
      AND index_name = 'uk_vls_event_mqtt_message'
);
SET @ddl = IF(
    @index_exists > 0,
    'ALTER TABLE `vls_event_management` DROP INDEX `uk_vls_event_mqtt_message`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_event_management'
      AND index_name = 'uk_vls_event_device_event'
);
SET @ddl = IF(
    @index_exists > 0,
    'ALTER TABLE `vls_event_management` DROP INDEX `uk_vls_event_device_event`',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_event_management'
      AND index_name = 'uk_vls_event_tenant_mqtt_message'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_event_management` ADD UNIQUE KEY `uk_vls_event_tenant_mqtt_message` (`tenant_id`, `mqtt_message_id`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vls_event_management'
      AND index_name = 'uk_vls_event_tenant_device_event'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `vls_event_management` ADD UNIQUE KEY `uk_vls_event_tenant_device_event` (`tenant_id`, `report_device`, `device_event_id`)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS `vls_event_report_outbox` (
    `id` bigint NOT NULL COMMENT 'Outbox identifier',
    `tenant_id` varchar(64) NOT NULL COMMENT 'Owning tenant',
    `event_id` bigint NOT NULL COMMENT 'vls_event_management.id',
    `idempotency_key` varchar(128) NOT NULL COMMENT 'Stable third-party request key',
    `payload_json` longtext NOT NULL COMMENT 'Immutable report payload snapshot',
    `status` varchar(20) NOT NULL COMMENT 'PENDING/PROCESSING/RETRY/SUCCESS/DEAD',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Completed delivery attempts',
    `next_retry_time` datetime NOT NULL COMMENT 'Next eligible delivery time',
    `locked_by` varchar(128) NULL COMMENT 'Current worker identifier',
    `locked_at` datetime NULL COMMENT 'Claim time',
    `last_error` varchar(2000) NULL COMMENT 'Last delivery error',
    `reported_at` datetime NULL COMMENT 'Successful delivery time',
    `create_time` datetime NOT NULL,
    `update_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vls_event_report_event` (`tenant_id`, `event_id`),
    UNIQUE KEY `uk_vls_event_report_idempotency` (`idempotency_key`),
    KEY `idx_vls_event_report_claim` (`status`, `next_retry_time`, `locked_at`),
    KEY `idx_vls_event_report_tenant` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reliable third-party event report outbox';
