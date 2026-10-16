-- Do not infer the opening user from a frontend ID, node owner or payer account.
-- Existing rows remain NULL and cannot settle until reconciled with trusted evidence.
ALTER TABLE vls_compute_usage
    ADD COLUMN opening_user_id VARCHAR(64) NULL COMMENT 'Authenticated VLS opener of the exclusive physical-instance rental' AFTER active_server_key;
