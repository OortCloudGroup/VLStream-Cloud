CREATE TABLE vls_smart_annotation_task (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    dataset_id BIGINT NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    model_path VARCHAR(1000) NOT NULL,
    task_state VARCHAR(20) NOT NULL,
    round_number INT NOT NULL DEFAULT 1,
    confidence DOUBLE NOT NULL DEFAULT 0.25,
    epochs INT NOT NULL DEFAULT 10,
    review_size INT NOT NULL DEFAULT 50,
    error_message VARCHAR(1000) NULL,
    create_user VARCHAR(64) NULL, create_dept VARCHAR(64) NULL, create_time DATETIME NULL,
    update_user VARCHAR(64) NULL, update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1, is_deleted INT NOT NULL DEFAULT 0,
    INDEX idx_smart_dataset (tenant_id, dataset_id, task_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Object detection smart annotation tasks';

CREATE TABLE vls_smart_annotation_round (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    round_number INT NOT NULL,
    version_id BIGINT NOT NULL,
    round_state VARCHAR(20) NOT NULL,
    work_directory VARCHAR(1000) NULL,
    model_path VARCHAR(1000) NULL,
    model_sha256 VARCHAR(64) NULL,
    predicted_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    create_user VARCHAR(64) NULL, create_dept VARCHAR(64) NULL, create_time DATETIME NULL,
    update_user VARCHAR(64) NULL, update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1, is_deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_smart_round (tenant_id, task_id, round_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Frozen input and model provenance for each annotation round';

CREATE TABLE vls_smart_annotation_candidate (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_width INT NOT NULL,
    image_height INT NOT NULL,
    boxes_json MEDIUMTEXT NOT NULL,
    uncertainty DOUBLE NOT NULL,
    hard_example INT NOT NULL DEFAULT 0,
    review_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_user VARCHAR(64) NULL, create_dept VARCHAR(64) NULL, create_time DATETIME NULL,
    update_user VARCHAR(64) NULL, update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1, is_deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_smart_prediction (tenant_id, round_id, image_id),
    INDEX idx_smart_review (tenant_id, round_id, review_state, hard_example)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Predictions kept separate from confirmed annotation instances';
