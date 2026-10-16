-- Remote paths remain computation inputs; verified objects are the download archive.
CREATE TABLE IF NOT EXISTS vls_model_artifact_storage (
    tenant_id VARCHAR(64) NOT NULL,
    path_hash CHAR(64) NOT NULL,
    remote_path VARCHAR(2048) NOT NULL,
    storage_state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    oss_config_key VARCHAR(255) NULL,
    object_key VARCHAR(1024) NULL,
    file_name VARCHAR(255) NULL,
    file_size BIGINT NULL,
    sha256 CHAR(64) NULL,
    lease_owner VARCHAR(36) NULL,
    retry_after DATETIME NULL,
    attempts INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, path_hash),
    INDEX idx_model_storage_retry (storage_state, retry_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
