-- Restore the built-in default member role after an accidental delete. This
-- migration is idempotent for all observed states: row logically deleted, row
-- physically absent, or healthy row. It reuses the existing role_id so any
-- sys_user_role bindings remain valid; only the tenant-scoped vls_member row
-- and its six business menu bindings are touched.

SET @vls_member_tenant = COALESCE(
    (SELECT NULLIF(TRIM(tenant_id), '') FROM sys_role WHERE role_id = 1),
    '000000'
);

-- Prefer the existing tenant-scoped vls_member row, including a logically
-- deleted one. If absent, reuse the historical 1900000000000004001 id when it
-- is free, otherwise take the next free fixed id.
SET @vls_member_role_id = (
    SELECT role_id FROM sys_role
    WHERE tenant_id = @vls_member_tenant AND role_key = 'vls_member' AND role_id <> 1
    LIMIT 1
);
SET @vls_member_role_id = COALESCE(@vls_member_role_id,
    (SELECT CASE
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004001) THEN 1900000000000004001
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004002) THEN 1900000000000004002
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004003) THEN 1900000000000004003
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004004) THEN 1900000000000004004
        ELSE 1900000000000004005
     END)
);

-- Restore an existing (possibly logically deleted) row to the canonical
-- initialization values from V1_2_0_062. create_by/create_time are preserved.
UPDATE sys_role
SET role_name = '普通用户',
    role_sort = 10,
    data_scope = '1',
    menu_check_strictly = 1,
    dept_check_strictly = 1,
    status = '0',
    del_flag = '0',
    update_by = 'vls-member-default',
    update_time = NOW()
WHERE role_id = @vls_member_role_id
  AND tenant_id = @vls_member_tenant
  AND role_key = 'vls_member'
  AND role_id <> 1;

-- Insert only when the tenant has no vls_member row at all.
INSERT INTO sys_role
    (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT @vls_member_role_id, @vls_member_tenant, '普通用户', 'vls_member', 10, '1', 1,
       1, '0', '0', 'vls-member-default', NOW(),
       '后续新增用户默认角色；保留业务操作，不授予系统目录、租户设置或公司资源管理权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE tenant_id = @vls_member_tenant AND role_key = 'vls_member'
);

-- Add only the original six business menu bindings that are missing. Existing
-- extra bindings or grants of other roles are not deleted or changed.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT @vls_member_role_id, @vls_member_tenant, candidate.menu_id
FROM (
    SELECT MIN(menu_id) AS menu_id
    FROM sys_menu
    WHERE tenant_id = @vls_member_tenant
      AND perms IN (
          'active-safety:list', 'active-safety:events:list',
          'active-safety:workorder:my', 'active-safety:workorder:pending',
          'active-safety:workorder:completed', 'active-safety:workorder:claimable'
      )
    GROUP BY perms
) candidate
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = @vls_member_role_id AND rm.menu_id = candidate.menu_id
);
