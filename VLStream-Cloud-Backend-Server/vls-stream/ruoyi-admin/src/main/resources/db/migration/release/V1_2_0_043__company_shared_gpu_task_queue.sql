-- Company shared GPU queues are opt-in. Private/paid/existing free instance claims remain exclusive.
-- No server, credential, user, price or claim is inserted or inferred by this migration.
ALTER TABLE vls_compute_node
    ADD COLUMN scheduling_mode VARCHAR(32) NOT NULL DEFAULT 'INSTANCE_EXCLUSIVE';

CREATE INDEX idx_compute_shared_queue_mode ON vls_compute_node (scheduling_mode,resource_scope,enabled);
