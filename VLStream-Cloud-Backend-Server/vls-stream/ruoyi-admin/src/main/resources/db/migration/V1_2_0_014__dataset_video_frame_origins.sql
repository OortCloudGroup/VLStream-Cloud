ALTER TABLE vls_dataset_import_job ADD COLUMN progress_message VARCHAR(300) NOT NULL DEFAULT '';

CREATE TABLE vls_dataset_frame_origin (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    dataset_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    video_name VARCHAR(200) NOT NULL,
    sample_id BIGINT NOT NULL,
    timestamp_ms BIGINT NOT NULL,
    requested_ms BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    create_user VARCHAR(64) NULL, create_dept VARCHAR(64) NULL, create_time DATETIME NULL,
    update_user VARCHAR(64) NULL, update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1, is_deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_frame_origin (tenant_id, dataset_id, video_id, sample_id, timestamp_ms),
    INDEX idx_frame_sample (tenant_id, dataset_id, sample_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Video frame provenance retained with historical samples';
