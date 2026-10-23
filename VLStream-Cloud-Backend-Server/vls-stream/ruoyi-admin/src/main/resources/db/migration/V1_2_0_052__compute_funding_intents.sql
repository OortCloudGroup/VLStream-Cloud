-- Local request snapshots only. No external funds, grants, wallets or credentials are changed.
-- No dispatcher consumes this table until hold recovery/capture/release contracts are verified.
CREATE TABLE vls_compute_funding_intent (
    intent_id VARCHAR(36) NOT NULL PRIMARY KEY,
    usage_id VARCHAR(36) NOT NULL,
    operation_kind VARCHAR(12) NOT NULL,
    business_reference VARCHAR(128) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    payer_user_id VARCHAR(32) NOT NULL,
    parent_intent_id VARCHAR(36) NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    request_path VARCHAR(200) NOT NULL,
    request_json TEXT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'CONTRACT_BLOCKED',
    error_code VARCHAR(64) NULL,
    hold_id VARCHAR(32) NULL,
    receipt_json TEXT NULL,
    conflicting_receipt_json TEXT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_compute_funding_operation (usage_id, operation_kind),
    UNIQUE KEY uk_compute_funding_reference (merchant_id, business_reference),
    UNIQUE KEY uk_compute_funding_key (idempotency_key),
    KEY idx_compute_funding_parent (parent_intent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
