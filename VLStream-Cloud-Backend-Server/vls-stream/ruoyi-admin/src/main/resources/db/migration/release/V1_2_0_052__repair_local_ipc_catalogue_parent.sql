-- Repair only the orphan produced by the local 900000-system seed.
-- Existing deployments with their normal menu 1 are unchanged; no role grants.
UPDATE sys_menu child
JOIN sys_menu local_root ON local_root.menu_id=900000 AND local_root.tenant_id='000000' AND local_root.path='system'
LEFT JOIN sys_menu old_root ON old_root.menu_id=1
SET child.parent_id=local_root.menu_id
WHERE child.menu_id=1900000000000000800 AND child.parent_id=1
  AND old_root.menu_id IS NULL AND (child.tenant_id IS NULL OR child.tenant_id='000000');
