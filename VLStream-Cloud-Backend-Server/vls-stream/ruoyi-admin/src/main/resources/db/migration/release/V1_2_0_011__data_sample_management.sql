-- Extend the existing annotation project and media records; preserve their IDs.
ALTER TABLE vls_algorithm_annotation
    MODIFY COLUMN annotation_rules TEXT NULL COMMENT 'Annotation guidance, preserving legacy JSON text',
    MODIFY COLUMN remark VARCHAR(1000) NULL,
    ADD COLUMN project_code VARCHAR(64) NULL COMMENT 'Project business code',
    ADD COLUMN project_type VARCHAR(64) NOT NULL DEFAULT 'general' COMMENT 'Project category';

UPDATE vls_algorithm_annotation SET project_code = CONCAT('PRJ-', id) WHERE project_code IS NULL;
ALTER TABLE vls_algorithm_annotation
    ADD UNIQUE KEY uk_annotation_project_code (tenant_id, project_code);

ALTER TABLE vls_annotation_image
    ADD COLUMN media_type VARCHAR(16) NOT NULL DEFAULT 'image',
    ADD COLUMN sample_source VARCHAR(128) NOT NULL DEFAULT 'upload',
    ADD COLUMN sample_tags TEXT NULL COMMENT 'JSON tag names',
    ADD COLUMN quality_status VARCHAR(16) NOT NULL DEFAULT 'pending',
    ADD COLUMN quality_note VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN quality_issues VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN quality_checked_at DATETIME NULL,
    ADD COLUMN quality_reviewed_by VARCHAR(64) NOT NULL DEFAULT '',
    ADD COLUMN media_width INT NOT NULL DEFAULT 0,
    ADD COLUMN media_height INT NOT NULL DEFAULT 0,
    ADD COLUMN focus_score DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN content_sha256 VARCHAR(64) NOT NULL DEFAULT '',
    ADD COLUMN dataset_split VARCHAR(16) NOT NULL DEFAULT 'unassigned',
    ADD INDEX idx_sample_project (tenant_id, annotation_id, is_deleted, quality_status),
    ADD INDEX idx_sample_hash (tenant_id, annotation_id, content_sha256);

-- Legacy image inserts omitted tenant_id. Only infer missing ownership from the owning project.
UPDATE vls_annotation_image i JOIN vls_algorithm_annotation p ON p.id = i.annotation_id
SET i.tenant_id = p.tenant_id
WHERE (i.tenant_id IS NULL OR i.tenant_id = '') AND p.tenant_id IS NOT NULL AND p.tenant_id != '';
UPDATE vls_annotation_label l JOIN vls_algorithm_annotation p ON p.id = l.annotation_id
SET l.tenant_id = p.tenant_id
WHERE (l.tenant_id IS NULL OR l.tenant_id = '') AND p.tenant_id IS NOT NULL AND p.tenant_id != '';
UPDATE vls_annotation_instance a JOIN vls_algorithm_annotation p ON p.id = a.annotation_id
SET a.tenant_id = p.tenant_id
WHERE (a.tenant_id IS NULL OR a.tenant_id = '') AND p.tenant_id IS NOT NULL AND p.tenant_id != '';

CREATE TABLE vls_dataset_version (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    annotation_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    snapshot_json LONGTEXT NOT NULL COMMENT 'Immutable project config, samples, labels and instances',
    sample_count INT NOT NULL,
    train_count INT NOT NULL,
    validation_count INT NOT NULL,
    create_user VARCHAR(64) NULL,
    create_dept VARCHAR(64) NULL,
    create_time DATETIME NULL,
    update_user VARCHAR(64) NULL,
    update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_dataset_version (tenant_id, annotation_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable dataset versions';
