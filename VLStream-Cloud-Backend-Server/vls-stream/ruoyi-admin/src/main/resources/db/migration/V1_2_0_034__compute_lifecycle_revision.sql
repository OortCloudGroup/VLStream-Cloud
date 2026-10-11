-- A monotonic revision rejects old SSH probes even if power changes occur within one second.
ALTER TABLE vls_compute_node ADD COLUMN lifecycle_revision BIGINT NOT NULL DEFAULT 0;
