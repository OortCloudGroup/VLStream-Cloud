ALTER TABLE vls_smart_annotation_task
    ADD COLUMN llm_provider_fingerprint CHAR(64) NULL,
    ADD COLUMN llm_prompt VARCHAR(2000) NULL,
    ADD COLUMN llm_prompt_version INT NULL,
    ADD INDEX idx_smart_llm_queue (mode, task_state, is_deleted, id);

ALTER TABLE vls_smart_annotation_round
    ADD COLUMN llm_failed_count INT NULL;

ALTER TABLE vls_smart_annotation_candidate
    MODIFY COLUMN boxes_json LONGTEXT NOT NULL,
    ADD COLUMN llm_raw_response MEDIUMTEXT NULL,
    ADD COLUMN llm_input_sha256 CHAR(64) NULL,
    ADD COLUMN llm_claim_token CHAR(36) NULL,
    ADD COLUMN llm_claim_until DATETIME(3) NULL;
