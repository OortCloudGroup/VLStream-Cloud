-- Restore known catalogue entries created with parent 1 when the local
-- single-tenant seed instead uses 900000 for its system directory.
-- This changes no role grants, menu state, or other tenant's catalogue.
UPDATE sys_menu child
JOIN sys_menu local_root
  ON local_root.menu_id = 900000
 AND local_root.tenant_id = '000000'
 AND local_root.parent_id = 0
 AND local_root.menu_type = 'M'
 AND local_root.path = 'system'
LEFT JOIN sys_menu old_root ON old_root.menu_id = 1
SET child.parent_id = local_root.menu_id
WHERE child.parent_id = 1
  AND old_root.menu_id IS NULL
  AND (child.tenant_id IS NULL OR child.tenant_id = '000000')
  AND (
       (child.menu_id = 1900000000000000700 AND child.menu_type = 'C' AND child.perms = 'vls:firmware:list')
    OR (child.menu_id = 1900000000000002000 AND child.menu_type = 'C' AND child.perms = 'system:platformLogo:list')
    OR (child.menu_id = 1900000000000003000 AND child.menu_type = 'F' AND child.perms = 'vls:compute:publish')
    OR (child.menu_id = 1900000000000003001 AND child.menu_type = 'F' AND child.perms = 'vls:compute:power')
    OR (child.menu_id = 1900000000000003002 AND child.menu_type = 'F' AND child.perms = 'vls:compute:observe')
    OR (child.menu_id = 1900000000000003003 AND child.menu_type = 'F' AND child.perms = 'vls:compute:configure')
    OR (child.menu_id = 1900000000000003004 AND child.menu_type = 'F' AND child.perms = 'vls:compute:manage-all')
    OR (child.menu_id = 1900000000000003005 AND child.menu_type = 'F' AND child.perms = 'vls:compute:company-publish')
  );
