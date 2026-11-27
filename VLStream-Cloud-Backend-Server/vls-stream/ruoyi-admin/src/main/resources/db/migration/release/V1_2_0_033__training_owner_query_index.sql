-- Existing audit ownership is authoritative. Do not assign historical ownerless tasks to a user.
ALTER TABLE vls_algorithm_training ADD INDEX idx_training_owner_page (tenant_id,create_user,is_deleted,create_time,id);
