-- Some installations contain both the upstream RuoYi system tree and the
-- local VLS system tree. Duplicate permission keys make revocation ambiguous.
-- Only consolidate the exact known pairs when BOTH catalogues are present.
-- Retain the local IDs, paths and all existing role/tenant associations.
DROP TEMPORARY TABLE IF EXISTS vls_system_menu_pairs;
CREATE TEMPORARY TABLE vls_system_menu_pairs (
    old_id BIGINT NOT NULL PRIMARY KEY,
    keep_id BIGINT NOT NULL,
    permission_key VARCHAR(100) NOT NULL
);
INSERT INTO vls_system_menu_pairs VALUES
    (1, 900000, 'system'),
    (100, 900100, 'system:user:list'),
    (101, 900200, 'system:role:list'),
    (102, 900300, 'system:menu:list'),
    (103, 900400, 'system:dept:list'),
    (104, 900500, 'system:post:list');

DROP TEMPORARY TABLE IF EXISTS vls_system_menu_merge;
CREATE TEMPORARY TABLE vls_system_menu_merge AS
SELECT p.old_id, p.keep_id, p.permission_key
FROM vls_system_menu_pairs p
JOIN sys_menu old_menu ON old_menu.menu_id = p.old_id
JOIN sys_menu keep_menu ON keep_menu.menu_id = p.keep_id;

DROP TEMPORARY TABLE IF EXISTS vls_system_menu_preflight;
CREATE TEMPORARY TABLE vls_system_menu_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_system_menu_merge_conflict CHECK (violations = 0)
);
INSERT INTO vls_system_menu_preflight
SELECT COUNT(*) FROM vls_system_menu_merge p
JOIN sys_menu old_menu ON old_menu.menu_id = p.old_id
JOIN sys_menu keep_menu ON keep_menu.menu_id = p.keep_id
WHERE NOT (
    (BINARY keep_menu.perms <=> BINARY p.permission_key)
    AND (old_menu.status <=> keep_menu.status)
    AND (old_menu.menu_type <=> keep_menu.menu_type)
    AND ((p.old_id = 1 AND old_menu.parent_id = 0 AND keep_menu.parent_id = 0
          AND old_menu.path = 'system' AND keep_menu.path = 'system'
          AND COALESCE(old_menu.perms, '') IN ('', 'system'))
        OR (p.old_id <> 1 AND BINARY old_menu.perms = BINARY p.permission_key
            AND old_menu.parent_id = 1 AND keep_menu.parent_id = 900000))
);
INSERT INTO vls_system_menu_preflight
SELECT COUNT(*) FROM vls_system_menu_merge p
JOIN sys_role_menu old_grant ON old_grant.menu_id = p.old_id
JOIN sys_role_menu keep_grant ON keep_grant.role_id = old_grant.role_id
    AND keep_grant.menu_id = p.keep_id
WHERE NOT (BINARY old_grant.tenant_id <=> BINARY keep_grant.tenant_id);

-- Existing null/historical tenant values are retained, never guessed.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT old_grant.role_id, old_grant.tenant_id, p.keep_id
FROM sys_role_menu old_grant JOIN vls_system_menu_merge p ON p.old_id = old_grant.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu keep_grant
    WHERE keep_grant.role_id = old_grant.role_id AND keep_grant.menu_id = p.keep_id);

UPDATE sys_menu child JOIN vls_system_menu_merge p ON child.parent_id = p.old_id
SET child.parent_id = p.keep_id;
DELETE old_grant FROM sys_role_menu old_grant
JOIN vls_system_menu_merge p ON old_grant.menu_id = p.old_id;
DELETE old_menu FROM sys_menu old_menu
JOIN vls_system_menu_merge p ON old_menu.menu_id = p.old_id;

DROP TEMPORARY TABLE vls_system_menu_preflight;
DROP TEMPORARY TABLE vls_system_menu_merge;
DROP TEMPORARY TABLE vls_system_menu_pairs;
