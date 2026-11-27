CREATE TABLE IF NOT EXISTS vls_compute_execution_lease (
    node_id BIGINT NOT NULL,
    job_ref VARCHAR(112) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    app_scope CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    execution_revision BIGINT NOT NULL,
    acquired TINYINT NOT NULL DEFAULT 0,
    release_requested TINYINT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    retry_after DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (node_id, job_ref),
    KEY idx_compute_execution_release (app_scope, release_requested, retry_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
