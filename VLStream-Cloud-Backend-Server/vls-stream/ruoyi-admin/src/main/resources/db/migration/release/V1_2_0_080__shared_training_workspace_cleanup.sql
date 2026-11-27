CREATE TABLE vls_shared_training_workspace_cleanup (
    job_id CHAR(36) NOT NULL,
    execution_environment VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    acknowledgement_json TEXT NOT NULL,
    release_node_id BIGINT NULL,
    release_app_scope CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    retry_count INT UNSIGNED NOT NULL DEFAULT 0,
    retry_after DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (job_id),
    KEY idx_shared_workspace_cleanup_retry (execution_environment, retry_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
