-- New local usage/outbox only. No wallets, credentials, grants or paid resources are seeded.
-- Legacy whole-period intents and tenant-scoped remainders stay intact pending reconciliation.
ALTER TABLE vls_compute_usage ADD COLUMN billing_mode VARCHAR(20) NOT NULL DEFAULT 'WHOLE_PERIOD';
CREATE TABLE vls_compute_billing_session (
    usage_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL, opening_user_id VARCHAR(100) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL, payer_user_id VARCHAR(32) NOT NULL,
    instance_ref VARCHAR(200) NOT NULL, quota_unit VARCHAR(80) NOT NULL,
    accepted_snapshot TEXT NOT NULL,
    metered_until_ms BIGINT NOT NULL, last_running_ms BIGINT NOT NULL, next_sequence BIGINT NOT NULL DEFAULT 0,
    risk_code VARCHAR(64) NULL, stop_requested TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vls_compute_account_remainder (
    payer_user_id VARCHAR(32) NOT NULL, quota_unit VARCHAR(80) NOT NULL,
    numerator_text TEXT NOT NULL, denominator_text TEXT NOT NULL,
    PRIMARY KEY (payer_user_id, quota_unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vls_compute_billing_slice (
    merchant_charge_id VARCHAR(120) NOT NULL PRIMARY KEY,
    usage_id VARCHAR(36) NOT NULL, slice_sequence BIGINT NOT NULL,
    interval_start_ms BIGINT NOT NULL, interval_end_ms BIGINT NOT NULL,
    is_tail TINYINT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(80) NOT NULL, request_json TEXT NOT NULL,
    request_hash CHAR(64) NOT NULL, amount_text VARCHAR(32) NOT NULL,
    remainder_numerator TEXT NOT NULL, remainder_denominator TEXT NOT NULL,
    state VARCHAR(32) NOT NULL,
    charge_id VARCHAR(100) NULL, ledger_entry_id VARCHAR(100) NULL,
    error_code VARCHAR(64) NULL,
    lease_token VARCHAR(36) NULL, lease_until_ms BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_billing_slice_sequence (usage_id, slice_sequence),
    UNIQUE KEY uk_billing_slice_key (idempotency_key),
    UNIQUE KEY uk_billing_slice_end (usage_id, interval_end_ms),
    KEY idx_billing_slice_pending (state, lease_until_ms),
    KEY idx_billing_slice_usage (usage_id, interval_start_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
