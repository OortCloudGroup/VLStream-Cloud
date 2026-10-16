-- Preserve all existing repository IDs and algorithm relationships.
ALTER TABLE vls_algorithm_repository
    ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent category, 0 is root',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sibling display order',
    ADD COLUMN flat_category_ids TEXT NULL COMMENT 'Selected direct child IDs for flat navigation',
    ADD INDEX idx_repository_parent (tenant_id, parent_id, is_deleted);

CREATE TABLE vls_algorithm_catalog_preference (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    setting_key VARCHAR(32) NOT NULL DEFAULT 'catalog',
    view_mode VARCHAR(16) NOT NULL DEFAULT 'flat',
    create_user VARCHAR(64) NULL,
    create_dept VARCHAR(64) NULL,
    create_time DATETIME NULL,
    update_user VARCHAR(64) NULL,
    update_time DATETIME NULL,
    status INT NOT NULL DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_catalog_preference (tenant_id, setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant algorithm catalog display preference';
