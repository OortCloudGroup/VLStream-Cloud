-- Runtime state on the original paid order, never a compute supply configuration row.
-- SUBMITTED is committed before vendor HTTP; ambiguous results are queried, not resubmitted.
ALTER TABLE vls_compute_paid_run ADD COLUMN power_start_state VARCHAR(20) NOT NULL DEFAULT 'PREPARED';
ALTER TABLE vls_compute_paid_run ADD COLUMN power_start_attempt_ms BIGINT NULL;
ALTER TABLE vls_compute_paid_run ADD COLUMN power_stop_state VARCHAR(20) NOT NULL DEFAULT 'PREPARED';
ALTER TABLE vls_compute_paid_run ADD COLUMN power_stop_attempt_ms BIGINT NULL;
