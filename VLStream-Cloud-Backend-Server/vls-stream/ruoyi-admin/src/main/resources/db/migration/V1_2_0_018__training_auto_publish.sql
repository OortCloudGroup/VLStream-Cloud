CREATE TABLE vls_training_publication (
    tenant_id VARCHAR(64) NOT NULL,
    training_id BIGINT NOT NULL,
    publication_state VARCHAR(20) NOT NULL,
    model_id BIGINT NULL,
    model_path VARCHAR(2048) NULL,
    attempts INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, training_id),
    INDEX idx_training_publication (publication_state, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
