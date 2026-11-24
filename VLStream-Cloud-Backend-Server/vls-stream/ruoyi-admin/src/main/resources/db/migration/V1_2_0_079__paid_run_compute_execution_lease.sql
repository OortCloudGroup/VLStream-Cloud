-- Accepted paid runs pin their original Deployment execution coordinator and canonical node.
ALTER TABLE vls_compute_paid_run ADD COLUMN central_lease_node_id BIGINT NULL;
ALTER TABLE vls_compute_paid_run ADD COLUMN central_lease_scope CHAR(64) NULL;
ALTER TABLE vls_compute_paid_run ADD COLUMN central_lease_root_run_id VARCHAR(36) NULL;
ALTER TABLE vls_compute_paid_run ADD COLUMN central_lease_acquired_ms BIGINT NULL;
ALTER TABLE vls_compute_paid_run ADD COLUMN central_lease_release_queued_ms BIGINT NULL;
CREATE INDEX idx_paid_run_central_release ON vls_compute_paid_run
    (central_lease_scope, central_lease_release_queued_ms, state, update_time);
CREATE INDEX idx_paid_run_central_instance ON vls_compute_paid_run
    (instance_key, central_lease_release_queued_ms, central_lease_root_run_id);
