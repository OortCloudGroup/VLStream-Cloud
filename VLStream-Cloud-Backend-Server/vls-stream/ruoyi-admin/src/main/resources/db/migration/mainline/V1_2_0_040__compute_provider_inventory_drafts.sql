-- Discovery is metadata only. No credentials, SSH fields, public claims or seeded instances.
CREATE TABLE vls_compute_provider_inventory (
    instance_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    account_scope VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provider_instance_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(100) NULL,
    gpu_spec VARCHAR(100) NULL,
    gpu_amount INT NULL,
    gpu_model VARCHAR(100) NULL,
    power_state VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    payg_price DECIMAL(24,8) NULL,
    origin_price DECIMAL(24,8) NULL,
    synced_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    state_checked_at DATETIME(3) NULL,
    UNIQUE KEY uk_inventory_provider_account_instance (provider,account_scope,provider_instance_id),
    KEY idx_inventory_tenant_account (tenant_id,account_scope,synced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
