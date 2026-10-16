CREATE TABLE vls_om_conversion_job (
    id CHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    training_id BIGINT NOT NULL,
    source_job_id CHAR(36) NOT NULL,
    source_pt_path VARCHAR(2048) NOT NULL,
    dataset_id BIGINT NOT NULL,
    dataset_ref VARCHAR(100) NOT NULL,
    job_state VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    message VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_om_pending (job_state, create_time),
    INDEX idx_om_source (tenant_id, training_id, source_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
