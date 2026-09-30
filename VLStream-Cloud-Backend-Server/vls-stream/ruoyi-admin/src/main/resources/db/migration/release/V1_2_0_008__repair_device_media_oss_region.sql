-- MinIO uses us-east-1 when no explicit server region is configured. An empty
-- client region produces an invalid AWS V4 credential scope in pre-signed URLs.
UPDATE `sys_oss_config`
SET `region` = 'us-east-1',
    `update_time` = CURRENT_TIMESTAMP
WHERE `config_key` = 'minio-device-media'
  AND (`region` IS NULL OR TRIM(`region`) = '');
