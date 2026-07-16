-- Align vls_tag_management with the audit fields inherited from BaseEntity.
ALTER TABLE `vls_tag_management`
    ADD COLUMN `create_by` varchar(64) NULL COMMENT '创建者',
    ADD COLUMN `update_by` varchar(64) NULL COMMENT '更新者';

-- Preserve historical audit identifiers stored by the legacy Blade model.
UPDATE `vls_tag_management`
SET `create_by` = COALESCE(`create_by`, CAST(`create_user` AS CHAR)),
    `update_by` = COALESCE(`update_by`, CAST(`update_user` AS CHAR))
WHERE (`create_by` IS NULL AND `create_user` IS NOT NULL)
   OR (`update_by` IS NULL AND `update_user` IS NOT NULL);
