-- Trusted workflow ownership is separate from the editable legacy work_order row.
-- Reject pre-existing tables rather than silently adopting an unknown schema.
CREATE TEMPORARY TABLE vls_workflow_ownership_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_workflow_ownership_table_conflict CHECK (violations = 0)
);
INSERT INTO vls_workflow_ownership_preflight (violations)
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
  AND table_name IN ('vls_workflow_ownership', 'vls_workflow_start_request');
DROP TEMPORARY TABLE vls_workflow_ownership_preflight;

CREATE TABLE vls_workflow_ownership (
    tenant_id VARBINARY(400) NOT NULL,
    proc_ins_id VARBINARY(256) NOT NULL,
    business_type VARCHAR(24) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, proc_ins_id),
    CONSTRAINT vls_workflow_ownership_type CHECK (business_type IN ('WORK_ORDER', 'PERIODIC_WORK_ORDER', 'OTHER_WORKFLOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vls_workflow_start_request (
    tenant_id VARBINARY(400) NOT NULL,
    user_id VARBINARY(400) NOT NULL,
    request_id VARBINARY(64) NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    proc_ins_id VARBINARY(256) DEFAULT NULL,
    work_order_id VARBINARY(256) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, user_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- No historical backfill: legacy work_order.proc_ins_id is client-editable.
-- Classifications require separately reviewed and auditable attestation.
