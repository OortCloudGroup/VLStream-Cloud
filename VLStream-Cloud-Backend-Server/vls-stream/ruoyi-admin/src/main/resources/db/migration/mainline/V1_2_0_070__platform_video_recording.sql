CREATE TABLE vls_platform_record_source (
    id varchar(36) NOT NULL PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    actor_id varchar(64) NOT NULL,
    protocol varchar(20) NOT NULL,
    device_row_id varchar(64) NOT NULL,
    stream_id varchar(64) NOT NULL DEFAULT '',
    grant_id varchar(36) NOT NULL,
    source_json text NOT NULL,
    manual_enabled tinyint NOT NULL DEFAULT 0,
    plan_json text NULL,
    state varchar(24) NOT NULL DEFAULT 'IDLE',
    error_code varchar(64) NULL,
    lease_owner varchar(36) NULL,
    lease_until datetime NULL,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_source (tenant_id, protocol, device_row_id, stream_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vls_platform_record_segment (
    id varchar(36) NOT NULL PRIMARY KEY,
    source_id varchar(36) NOT NULL,
    start_ms bigint NOT NULL,
    end_ms bigint NOT NULL,
    size_bytes bigint NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_source_time (source_id, start_ms),
    KEY idx_record_expiry (end_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
