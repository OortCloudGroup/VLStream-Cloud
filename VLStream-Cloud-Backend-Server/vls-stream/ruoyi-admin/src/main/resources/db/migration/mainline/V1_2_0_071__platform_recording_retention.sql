ALTER TABLE vls_platform_record_source
    ADD COLUMN retention_days INT NOT NULL DEFAULT 7 COMMENT '7/30/90 days; 0 means permanent';
