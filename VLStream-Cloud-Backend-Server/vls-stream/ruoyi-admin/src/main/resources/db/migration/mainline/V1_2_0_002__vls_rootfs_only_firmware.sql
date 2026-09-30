-- VLS devices report payload.version as the current RootFS version.
-- Preserve the legacy aggregate firmware_version for existing list APIs while
-- moving previous application-version data into the RootFS field.
UPDATE `vls_mqtt_device`
SET `rootfs_version` = COALESCE(NULLIF(`rootfs_version`, ''),
        NULLIF(`application_version`, ''), NULLIF(`firmware_version`, '')),
    `firmware_version` = COALESCE(NULLIF(`rootfs_version`, ''),
        NULLIF(`application_version`, ''), NULLIF(`firmware_version`, '')),
    `application_version` = NULL
WHERE `application_version` IS NOT NULL
   OR `rootfs_version` IS NOT NULL
   OR `firmware_version` IS NOT NULL;

-- Convert legacy application packages when the same model/version does not
-- already have a RootFS record. Duplicate historical rows remain untouched
-- and are hidden by the RootFS-only repository query.
UPDATE `vls_device_firmware` AS `legacy`
LEFT JOIN `vls_device_firmware` AS `rootfs`
    ON `rootfs`.`tenant_id` = `legacy`.`tenant_id`
   AND `rootfs`.`camera_model` = `legacy`.`camera_model`
   AND `rootfs`.`firmware_version` = `legacy`.`firmware_version`
   AND `rootfs`.`target` = 'rootfs'
SET `legacy`.`target` = 'rootfs'
WHERE `legacy`.`target` = 'application'
  AND `rootfs`.`id` IS NULL;
