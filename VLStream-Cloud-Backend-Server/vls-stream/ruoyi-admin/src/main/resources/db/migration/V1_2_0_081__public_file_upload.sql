CREATE TABLE IF NOT EXISTS vls_public_file (
    file_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    uploader_id VARCHAR(64) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    storage_config_key VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Public file uploads';
