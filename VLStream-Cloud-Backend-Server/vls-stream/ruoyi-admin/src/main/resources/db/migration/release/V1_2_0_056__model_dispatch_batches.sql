-- Batch ledger only. Device execution continues to use vls_model_dispatch_task.
CREATE TABLE vls_model_dispatch_batch (
    operation_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id varchar(64) NOT NULL,
    owner_id varchar(64) NOT NULL,
    request_hash char(64) NOT NULL,
    snapshot_json longtext NOT NULL,
    items_json longtext NOT NULL,
    retries_json text NOT NULL,
    create_time datetime NOT NULL,
    update_time datetime NOT NULL,
    PRIMARY KEY (operation_id),
    KEY idx_dispatch_batch_owner (tenant_id, owner_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型部署批次及幂等恢复台账';
