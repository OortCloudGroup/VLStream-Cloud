-- Empty application outbox only. No wallet, supplier configuration, grants or credentials are seeded.
CREATE TABLE vls_compute_paid_run (
    run_id VARCHAR(36) NOT NULL PRIMARY KEY,
    node_id BIGINT NOT NULL, instance_key VARCHAR(64) NOT NULL,
    active_instance_key VARCHAR(64) NULL,
    tenant_id VARCHAR(64) NOT NULL, opening_user_id VARCHAR(100) NOT NULL,
    platform_subject VARCHAR(200) NOT NULL, payer_user_id VARCHAR(32) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL, quote_snapshot TEXT NOT NULL,
    reserve_quota VARCHAR(32) NOT NULL, quote_until_ms BIGINT NOT NULL,
    hold_until_ms BIGINT NOT NULL, maximum_run_seconds INT NOT NULL,
    usage_id VARCHAR(36) NULL, hold_intent_id VARCHAR(36) NULL,
    release_intent_id VARCHAR(36) NULL, start_operation_id VARCHAR(36) NOT NULL,
    state VARCHAR(32) NOT NULL, error_code VARCHAR(64) NULL,
    worker_lease_until_ms BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_paid_run_active_instance (active_instance_key),
    UNIQUE KEY uk_paid_run_usage (usage_id),
    UNIQUE KEY uk_paid_run_start_operation (start_operation_id),
    KEY idx_paid_run_actor (tenant_id, opening_user_id, create_time),
    KEY idx_paid_run_pending (state, worker_lease_until_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
