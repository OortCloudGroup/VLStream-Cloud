-- Preserve publication request identity and immutable request metadata, never authentication tokens.
ALTER TABLE vls_hub_publication
 ADD COLUMN request_remote_user_id VARCHAR(128) NULL,
 ADD COLUMN request_remote_tenant_id VARCHAR(128) NULL,
 ADD COLUMN request_version_hash VARCHAR(64) NULL,
 ADD COLUMN request_metadata_json TEXT NULL;
