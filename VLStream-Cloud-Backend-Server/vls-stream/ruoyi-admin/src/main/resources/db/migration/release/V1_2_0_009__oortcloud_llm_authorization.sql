ALTER TABLE `vls_llm_provider`
    ADD COLUMN `platform_user_id` varchar(128) NULL COMMENT 'OortCloud user bound to this tenant' AFTER `enabled`,
    ADD COLUMN `platform_user_name` varchar(200) NULL COMMENT 'OortCloud user display name' AFTER `platform_user_id`,
    ADD COLUMN `authorized_at` datetime NULL COMMENT 'Time this tenant completed OortCloud authorization' AFTER `platform_user_name`;
