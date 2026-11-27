-- Usage/intents only: no wallet balances, credentials, user mappings, prices or debit API are seeded.
CREATE TABLE vls_compute_usage (
    id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
    server_key VARCHAR(200) NOT NULL, active_server_key VARCHAR(200) NULL,
    payer_account_ref VARCHAR(100) NOT NULL, start_operation_id VARCHAR(36) NOT NULL,
    rate_snapshot TEXT NOT NULL, state VARCHAR(20) NOT NULL,
    start_observed_at DATETIME(3) NULL, billing_start_at DATETIME(3) NULL, start_evidence VARCHAR(30) NULL,
    stop_observed_at DATETIME(3) NULL, billing_end_at DATETIME(3) NULL, stop_evidence VARCHAR(30) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_compute_active_usage (active_server_key), UNIQUE KEY uk_compute_usage_operation (start_operation_id),
    KEY idx_compute_usage_owner (tenant_id,payer_account_ref,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vls_compute_charge (
    charge_key VARCHAR(80) PRIMARY KEY, usage_id VARCHAR(36) NOT NULL, tenant_id VARCHAR(64) NOT NULL,
    payer_account_ref VARCHAR(100) NOT NULL, quota_unit VARCHAR(80) NOT NULL,
    amount_text VARCHAR(200) NOT NULL, unrounded_text VARCHAR(200) NOT NULL, remainder_text VARCHAR(200) NOT NULL,
    state VARCHAR(20) NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_compute_usage_charge (usage_id), KEY idx_compute_charge_pending (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vls_compute_billing_remainder (
    tenant_id VARCHAR(64) NOT NULL, payer_account_ref VARCHAR(100) NOT NULL, quota_unit VARCHAR(80) NOT NULL,
    value_text VARCHAR(200) NOT NULL,
    PRIMARY KEY (tenant_id,payer_account_ref,quota_unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
