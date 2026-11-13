-- Local coordination/reconciliation only; no financial balances or credentials.
ALTER TABLE vls_compute_billing_session ADD COLUMN control_snapshot TEXT NULL;
ALTER TABLE vls_compute_billing_session ADD COLUMN safety_state VARCHAR(32) NOT NULL DEFAULT 'UNARMED';
ALTER TABLE vls_compute_billing_session ADD COLUMN stop_operation_id VARCHAR(36) NULL;
ALTER TABLE vls_compute_billing_session ADD COLUMN physical_stop_observed_ms BIGINT NULL;
ALTER TABLE vls_compute_billing_session ADD COLUMN next_poll_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE vls_compute_billing_slice ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE vls_compute_billing_slice ADD COLUMN next_attempt_ms BIGINT NOT NULL DEFAULT 0;
CREATE TABLE vls_compute_billing_reconciliation (
    merchant_id VARCHAR(64) NOT NULL PRIMARY KEY,
    from_second BIGINT NOT NULL, to_second BIGINT NOT NULL,
    page_cursor VARCHAR(200) NULL,
    state VARCHAR(32) NOT NULL, error_code VARCHAR(64) NULL,
    lease_token VARCHAR(36) NULL, lease_until_ms BIGINT NOT NULL DEFAULT 0,
    next_attempt_ms BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vls_compute_billing_ledger_seen (
    merchant_id VARCHAR(64) NOT NULL, ledger_entry_id VARCHAR(100) NOT NULL,
    entry_hash CHAR(64) NOT NULL, merchant_ref VARCHAR(120) NOT NULL,
    entry_type VARCHAR(20) NOT NULL, quota_delta VARCHAR(32) NOT NULL,
    match_state VARCHAR(32) NOT NULL,
    PRIMARY KEY (merchant_id, ledger_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
