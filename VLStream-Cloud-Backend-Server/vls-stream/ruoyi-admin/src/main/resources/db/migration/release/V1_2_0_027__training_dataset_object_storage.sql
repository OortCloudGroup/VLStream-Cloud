-- New training packages are portable verified objects; historical SSH paths are unchanged.
CREATE TABLE IF NOT EXISTS vls_training_dataset_artifact (
    id CHAR(36) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    dataset_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    annotation_type VARCHAR(64) NOT NULL,
    snapshot_sha256 CHAR(64) NOT NULL,
    dataset_yaml TEXT NOT NULL,
    storage_state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    storage_config VARCHAR(255) NULL,
    storage_bucket VARCHAR(255) NULL,
    object_key VARCHAR(1024) NULL,
    file_size BIGINT NULL,
    sha256 CHAR(64) NULL,
    lease_owner CHAR(36) NULL,
    lease_until DATETIME NULL,
    retry_after DATETIME NULL,
    attempts INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_dataset_version (tenant_id, dataset_id, version_id),
    INDEX idx_training_dataset_retry (storage_state, lease_until, retry_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Portable immutable training dataset packages';

-- Record datasets materialized on the default compute host. No marker means no SSH cleanup is needed.
CREATE TABLE IF NOT EXISTS vls_training_dataset_remote_usage (
    tenant_id VARCHAR(64) NOT NULL,
    dataset_id BIGINT NOT NULL,
    remote_identity VARCHAR(1024) NULL,
    PRIMARY KEY (tenant_id, dataset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Default compute dataset materialization ownership';

-- Historical directories may still exist after editing clears dataset_path.
INSERT IGNORE INTO vls_training_dataset_remote_usage(tenant_id,dataset_id,remote_identity)
SELECT a.tenant_id,a.id,NULL FROM vls_algorithm_annotation a
WHERE a.tenant_id IS NOT NULL AND a.tenant_id<>''
  AND (a.dataset_path LIKE '/%' OR EXISTS (
      SELECT 1 FROM vls_dataset_version v
      WHERE v.tenant_id=a.tenant_id AND v.annotation_id=a.id AND v.version_name='训练生成快照'
  ));
