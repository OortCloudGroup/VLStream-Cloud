-- 045 creates the company-publish permission with legacy parent 1. Because 061
-- already ran before 045 during the out-of-order catch-up, this child remained
-- an orphan. Attach it to the local system root 900000 when parent 1 is absent.
-- This is the 3005 branch of 061; no role grant or other menu is changed.
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
  AND child.menu_id = 1900000000000003005
  AND child.menu_type = 'F'
  AND child.perms = 'vls:compute:company-publish';
