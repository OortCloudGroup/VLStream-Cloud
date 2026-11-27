CREATE TABLE IF NOT EXISTS vls_hub_publication (
 tenant_id VARCHAR(64) NOT NULL,
 resource_kind VARCHAR(16) NOT NULL,
 resource_id BIGINT NOT NULL,
 featured TINYINT(1) NOT NULL DEFAULT 0,
 version_hash VARCHAR(64) NULL,
 metadata_hash VARCHAR(64) NULL,
 remote_uid VARCHAR(128) NULL,
 published_commit VARCHAR(64) NULL,
 publish_state VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 publish_key VARCHAR(128) NULL,
 error_message VARCHAR(256) NULL,
 updated_by VARCHAR(128) NULL,
 published_at DATETIME NULL,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (tenant_id,resource_kind,resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE vls_model_dispatch_task MODIFY COLUMN training_id BIGINT NULL COMMENT 'Training task; null for independently verified Hub artifact';
