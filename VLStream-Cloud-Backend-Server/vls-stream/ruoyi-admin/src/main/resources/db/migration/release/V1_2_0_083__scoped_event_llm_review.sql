CREATE TABLE IF NOT EXISTS vls_llm_review_scope (
    tenant_id varchar(64) NOT NULL,
    scope_key varchar(160) NOT NULL,
    PRIMARY KEY (tenant_id, scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

ALTER TABLE vls_llm_review_task MODIFY algorithm_id bigint NULL COMMENT 'Device algorithm when reported',
    ADD COLUMN event_sink varchar(8) NULL COMMENT 'LOCAL or VLS for new tasks';
ALTER TABLE vls_cloud_ai_event ADD COLUMN review_pending tinyint NOT NULL DEFAULT 0
    COMMENT 'Awaiting review before creating formal event';
