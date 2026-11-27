ALTER TABLE vls_smart_annotation_task
    ADD COLUMN llm_pilot_size INT NULL,
    ADD COLUMN llm_review_percent INT NULL;

ALTER TABLE vls_smart_annotation_candidate
    ADD COLUMN llm_review_kind VARCHAR(16) NULL;
