-- Explicit inventory identity only: never infer provider account or seed purchased resources.
CREATE TABLE vls_compute_public_instance (
    instance_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    provider VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    account_scope VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provider_instance_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    public_state VARCHAR(20) NOT NULL DEFAULT 'FREE',
    owner_subject CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    claim_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    generation BIGINT NOT NULL DEFAULT 0,
    workspace_dirty TINYINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_public_provider_account_instance (provider,account_scope,provider_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE vls_compute_node ADD COLUMN public_instance_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD INDEX idx_compute_public_instance (public_instance_key,id);
ALTER TABLE vls_cloud_training_job
    ADD COLUMN public_instance_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN public_claim_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN public_owner_subject CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD INDEX idx_public_claim_jobs (public_instance_key,job_state);
-- Existing PLATFORM rows remain unconfigured/isolated. No owner or cleanup evidence is guessed.
