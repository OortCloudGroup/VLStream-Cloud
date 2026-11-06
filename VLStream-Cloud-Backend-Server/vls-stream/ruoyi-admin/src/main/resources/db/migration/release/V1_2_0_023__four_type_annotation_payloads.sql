ALTER TABLE vls_smart_annotation_task
    ADD COLUMN annotation_type VARCHAR(50) NOT NULL DEFAULT 'object_detection';

-- Tasks created before this migration supported detection only; keep their original task type.

ALTER TABLE vls_smart_annotation_candidate MODIFY COLUMN boxes_json LONGTEXT NOT NULL;
ALTER TABLE vls_smart_annotation_candidate ADD COLUMN summary_json MEDIUMTEXT NULL, ADD COLUMN review_error VARCHAR(1000) NULL;
UPDATE vls_smart_annotation_candidate SET summary_json=boxes_json;
ALTER TABLE vls_annotation_instance MODIFY COLUMN annotation_data LONGTEXT NOT NULL COMMENT 'Typed annotation JSON, including lossless pixel masks';

ALTER TABLE vls_smart_annotation_task
    ADD COLUMN bulk_cursor BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN bulk_total INT NOT NULL DEFAULT 0,
    ADD COLUMN bulk_processed INT NOT NULL DEFAULT 0,
    ADD COLUMN bulk_accepted INT NOT NULL DEFAULT 0,
    ADD COLUMN bulk_conflicts INT NOT NULL DEFAULT 0,
    ADD COLUMN bulk_error VARCHAR(1000) NULL;

ALTER TABLE vls_smart_annotation_round
    ADD COLUMN progress_stage VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    ADD COLUMN progress_current INT NOT NULL DEFAULT 0,
    ADD COLUMN progress_total INT NOT NULL DEFAULT 0;
