-- Repair the default member role for deployments where V1_2_0_045/V1_2_0_046
-- never reached flyway_schema_history (an incomplete release artifact combined
-- with spring.flyway.out-of-order=false). Statements are idempotent and only
-- touch the tenant-scoped vls_member template: no user assignment is rewritten
-- and no administrator role or other menu is enabled or granted.

SET @vls_member_tenant = COALESCE(
    (SELECT NULLIF(TRIM(tenant_id), '') FROM sys_role WHERE role_id = 1),
    '000000'
);

-- 1. Business catalogue: create only when this tenant has no row with the permission.
INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 901000, @vls_member_tenant, '主动安全', 0, 91, 'active-safety', NULL, NULL,
       1, 0, 'M', '0', '0', 'active-safety:list', 'shield', 'vls-member-default', NOW(), '原主动安全业务目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901000)
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu WHERE tenant_id = @vls_member_tenant AND perms = 'active-safety:list'
  );

SET @vls_member_root = (
    SELECT MIN(menu_id) FROM sys_menu
    WHERE tenant_id = @vls_member_tenant AND perms = 'active-safety:list'
);

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT candidate.menu_id, @vls_member_tenant, candidate.menu_name, @vls_member_root, candidate.order_num,
       candidate.path, candidate.component, NULL, 1, 0, 'C', '0', '0', candidate.perms,
       'list', 'vls-member-default', NOW(), '原主动安全与工单业务入口'
FROM (
    SELECT 901100 AS menu_id, '主动安全事件' AS menu_name, 1 AS order_num,
           'events/secure' AS path, 'active-safety/events/secure' AS component, 'active-safety:events:list' AS perms
    UNION ALL SELECT 901200, '我的工单', 2, 'work-orders/my', 'active-safety/work-orders/my', 'active-safety:workorder:my'
    UNION ALL SELECT 901300, '待办工单', 3, 'work-orders/pending', 'active-safety/work-orders/pending', 'active-safety:workorder:pending'
    UNION ALL SELECT 901400, '已办工单', 4, 'work-orders/completed', 'active-safety/work-orders/completed', 'active-safety:workorder:completed'
    UNION ALL SELECT 901500, '可接工单', 5, 'work-orders/claimable', 'active-safety/work-orders/claimable', 'active-safety:workorder:claimable'
) candidate
WHERE @vls_member_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.menu_id = candidate.menu_id)
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = @vls_member_tenant AND existing.perms = candidate.perms
  );

-- 2. Reuse an existing tenant-scoped template, otherwise allocate a free fixed id.
SET @vls_member_role_id = (
    SELECT role_id FROM sys_role
    WHERE tenant_id = @vls_member_tenant AND role_key = 'vls_member' AND role_id <> 1
    LIMIT 1
);
SET @vls_member_role_id = COALESCE(@vls_member_role_id,
    (SELECT CASE
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004001) THEN 1900000000000004001
        WHEN NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1900000000000004002) THEN 1900000000000004002
        ELSE 1900000000000004003
     END)
);

INSERT INTO sys_role
    (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT @vls_member_role_id, @vls_member_tenant, '普通用户', 'vls_member', 10, '1', 1,
       1, '0', '0', 'vls-member-default', NOW(),
       '后续新增用户默认角色；保留业务操作，不授予系统目录、租户设置或公司资源管理权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE tenant_id = @vls_member_tenant AND role_key = 'vls_member'
);

-- A disabled or logically deleted default role cannot accept new members; repair
-- that state only, without renaming the role or changing its tenant.
UPDATE sys_role
SET status = '0', del_flag = '0', update_by = 'vls-member-default', update_time = NOW()
WHERE role_id = @vls_member_role_id
  AND tenant_id = @vls_member_tenant
  AND (status <> '0' OR del_flag <> '0');

-- 3. Keep exactly the original six business menus, one row per permission.
DELETE rm
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id AND r.tenant_id = @vls_member_tenant
LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @vls_member_role_id
  AND (
      rm.tenant_id <> @vls_member_tenant
      OR m.menu_id IS NULL
      OR m.tenant_id <> @vls_member_tenant
      OR m.perms IS NULL
      OR m.perms NOT IN (
          'active-safety:list', 'active-safety:events:list',
          'active-safety:workorder:my', 'active-safety:workorder:pending',
          'active-safety:workorder:completed', 'active-safety:workorder:claimable'
      )
  );

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
