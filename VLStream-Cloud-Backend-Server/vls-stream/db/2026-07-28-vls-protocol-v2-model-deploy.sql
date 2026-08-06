-- VLS-Protocol 2.2 modelDeploy: keep the MQTT message id separate from the business task id.
-- Existing legacy tasks use request_id as their historical MQTT correlation id during migration.
ALTER TABLE `vls_model_dispatch_task`
    ADD COLUMN `mqtt_message_id` varchar(64) NULL COMMENT 'V2.2下发消息messageId' AFTER `request_id`;

UPDATE `vls_model_dispatch_task`
SET `mqtt_message_id` = `request_id`
WHERE `mqtt_message_id` IS NULL;

ALTER TABLE `vls_model_dispatch_task`
    MODIFY COLUMN `mqtt_message_id` varchar(64) NOT NULL COMMENT 'V2.2下发消息messageId',
    ADD UNIQUE KEY `uk_model_dispatch_mqtt_message_id` (`mqtt_message_id`);
