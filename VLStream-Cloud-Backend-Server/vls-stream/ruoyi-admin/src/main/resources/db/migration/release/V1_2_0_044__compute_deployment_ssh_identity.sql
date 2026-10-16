-- Explicit deployment-key authentication. Existing credentials and default password behavior are preserved.
ALTER TABLE vls_compute_node
    ADD COLUMN auth_mode VARCHAR(32) NOT NULL DEFAULT 'PASSWORD' AFTER username,
    MODIFY COLUMN password_cipher TEXT NULL;
