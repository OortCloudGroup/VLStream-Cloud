-- Forward-only local journal evolution. Existing request bytes/keys/receipts are preserved.
ALTER TABLE vls_compute_funding_intent ADD COLUMN contract_version VARCHAR(24) NOT NULL DEFAULT 'LEGACY_V2';
ALTER TABLE vls_compute_funding_intent ADD COLUMN hold_balance_version BIGINT NULL;
ALTER TABLE vls_compute_funding_intent DROP INDEX uk_compute_funding_operation;
ALTER TABLE vls_compute_funding_intent
    ADD UNIQUE KEY uk_compute_funding_operation (usage_id, operation_kind, business_reference);

ALTER TABLE vls_compute_billing_session ADD COLUMN funding_required TINYINT NOT NULL DEFAULT 0;
ALTER TABLE vls_compute_billing_session ADD COLUMN funding_hold_intent_id VARCHAR(36) NULL;
ALTER TABLE vls_compute_billing_session ADD COLUMN funding_hold_id VARCHAR(32) NULL;
ALTER TABLE vls_compute_billing_session ADD COLUMN funding_expires_at_ms BIGINT NULL;

CREATE TABLE vls_compute_funding_hold_observation (
    hold_intent_id VARCHAR(36) NOT NULL PRIMARY KEY,
    hold_id VARCHAR(32) NOT NULL,
    hold_quota VARCHAR(20) NOT NULL,
    captured_quota VARCHAR(20) NOT NULL,
    released_quota VARCHAR(20) NOT NULL,
    remaining_quota VARCHAR(20) NOT NULL,
    hold_status VARCHAR(32) NOT NULL,
    balance_version BIGINT NOT NULL,
    expires_at_ms BIGINT NOT NULL,
    detail_json TEXT NOT NULL,
    detail_hash CHAR(64) NOT NULL,
    conflicting_detail_json TEXT NULL,
    query_started_at_ms BIGINT NOT NULL DEFAULT 0,
    observed_at_ms BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
