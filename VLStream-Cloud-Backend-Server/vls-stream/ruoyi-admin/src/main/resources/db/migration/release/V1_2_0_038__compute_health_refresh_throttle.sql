-- One persisted cooldown across a physical server. Consumers cannot supply SSH details.
ALTER TABLE vls_compute_node ADD COLUMN probe_requested_at DATETIME NULL;
