-- Smart annotation shares the durable compute queue without impersonating a formal training.
ALTER TABLE vls_smart_annotation_task ADD COLUMN compute_node_id BIGINT NULL;
ALTER TABLE vls_cloud_training_job MODIFY COLUMN training_id BIGINT NULL,
    ADD COLUMN smart_round_id BIGINT NULL,
    ADD INDEX idx_cloud_smart_round (tenant_id, smart_round_id);
