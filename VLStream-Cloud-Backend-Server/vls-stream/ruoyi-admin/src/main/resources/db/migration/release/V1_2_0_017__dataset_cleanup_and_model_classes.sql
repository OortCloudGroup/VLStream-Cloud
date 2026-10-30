CREATE TABLE vls_dataset_cleanup (
    tenant_id VARCHAR(64) NOT NULL,
    annotation_id BIGINT NOT NULL,
    cleanup_state VARCHAR(20) NOT NULL,
    manifest_json LONGTEXT NOT NULL,
    error_message VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, annotation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Model metadata survives deletion of training inputs and is bound to the exact model path.
CREATE TABLE vls_model_class_snapshot (
    tenant_id VARCHAR(64) NOT NULL,
    training_id BIGINT NOT NULL,
    model_path VARCHAR(2048) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, training_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Covers the full conversion chain, including RKNN calibration after ONNX/OM finish.
CREATE TABLE vls_dataset_conversion_guard (
    tenant_id VARCHAR(64) NOT NULL,
    training_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, training_id),
    INDEX idx_conversion_dataset (tenant_id, dataset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
