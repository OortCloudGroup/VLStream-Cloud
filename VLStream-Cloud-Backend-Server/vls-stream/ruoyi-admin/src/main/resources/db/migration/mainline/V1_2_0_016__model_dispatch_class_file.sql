-- Preserve the exact UTF-8 YAML bytes sent with each model deployment.
-- Existing tasks remain nullable and retain their original model-only contract.
ALTER TABLE vls_model_dispatch_task
    ADD COLUMN class_file_name VARCHAR(255) NULL,
    ADD COLUMN class_file_size BIGINT NULL,
    ADD COLUMN class_file_sha256 CHAR(64) NULL,
    ADD COLUMN class_file_content MEDIUMTEXT CHARACTER SET utf8mb4 NULL;
