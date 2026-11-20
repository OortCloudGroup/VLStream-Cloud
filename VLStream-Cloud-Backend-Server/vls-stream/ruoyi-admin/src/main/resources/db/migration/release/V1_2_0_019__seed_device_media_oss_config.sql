-- Seed the public release's device-media OSS key without shipping credentials.
-- Operators must replace the endpoint and credentials before enabling uploads.
INSERT INTO `sys_oss_config` (
    `oss_config_id`, `tenant_id`, `user_id`, `config_key`, `access_key`, `secret_key`,
    `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`,
    `access_policy`, `status`, `ext1`, `create_by`, `create_time`, `update_by`,
    `update_time`, `remark`
)
SELECT UUID_SHORT(), '000000', NULL, 'minio-device-media', '', '', 'vlstream', '',
       'http://minio:9000', '', 'N', 'us-east-1', '0', '0', NULL, 'release',
       CURRENT_TIMESTAMP, 'release', CURRENT_TIMESTAMP,
       'Release placeholder; configure a device-reachable endpoint and credentials before media upload'
FROM (SELECT 1) AS release_seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_oss_config`
    WHERE `config_key` = 'minio-device-media'
);
