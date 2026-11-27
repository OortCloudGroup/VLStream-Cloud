-- Fill missing business catalogue entries from the existing local bootstrap contract.
-- Never update/enable an existing menu, rewrite a role, or assign an existing user.
-- Match the established bootstrap role's tenant namespace, including a configured private deployment tenant.
SET @vls_member_template_tenant = COALESCE((SELECT NULLIF(TRIM(tenant_id), '') FROM sys_role WHERE role_id = 1), '000000');

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 901000, @vls_member_template_tenant, '主动安全', 0, 91, 'active-safety', NULL, NULL,
       1, 0, 'M', '0', '0', 'active-safety:list', 'shield', 'vls-member-default', NOW(), '原主动安全业务目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'active-safety:list' OR menu_id = 901000);

SET @vls_member_business_root = (SELECT MIN(menu_id) FROM sys_menu WHERE perms = 'active-safety:list');

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT candidate.menu_id, @vls_member_template_tenant, candidate.menu_name, @vls_member_business_root, candidate.order_num,
       candidate.path, candidate.component, NULL, 1, 0, 'C', '0', '0', candidate.perms,
       'list', 'vls-member-default', NOW(), '原主动安全与工单业务入口'
FROM (
    SELECT 901100 AS menu_id, '主动安全事件' AS menu_name, 1 AS order_num,
           'events/secure' AS path, 'active-safety/events/secure' AS component, 'active-safety:events:list' AS perms
    UNION ALL SELECT 901200, '我的工单', 2, 'work-orders/my', 'active-safety/work-orders/my', 'active-safety:workorder:my'
    UNION ALL SELECT 901300, '待办工单', 3, 'work-orders/pending', 'active-safety/work-orders/pending', 'active-safety:workorder:pending'
    UNION ALL SELECT 901400, '已办工单', 4, 'work-orders/completed', 'active-safety/work-orders/completed', 'active-safety:workorder:completed'
    UNION ALL SELECT 901500, '可接工单', 5, 'work-orders/claimable', 'active-safety/work-orders/claimable', 'active-safety:workorder:claimable'
    UNION ALL SELECT 901600, '主动安全设置', 6, 'settings/secure', 'active-safety/settings/secure', 'active-safety:settings:secure'
    UNION ALL SELECT 901700, '工单设置', 7, 'settings/work-orders', 'active-safety/settings/work-orders', 'active-safety:settings:workorder'
) candidate
WHERE @vls_member_business_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.menu_id = candidate.menu_id OR existing.perms = candidate.perms);

-- A new default template only. A pre-existing key/ID is left untouched and validated by the service.
SET @vls_member_seed_new = (
    SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
    FROM sys_role
    WHERE role_id = 1900000000000004001
       OR (tenant_id = @vls_member_template_tenant AND role_key = 'vls_member')
);

INSERT INTO sys_role
    (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 1900000000000004001, @vls_member_template_tenant, '普通用户', 'vls_member', 10, '1', 1,
       1, '0', '0', 'vls-member-default', NOW(),
       '后续新增用户默认角色；保留业务操作，不授予系统目录、租户设置或公司资源管理权限'
WHERE @vls_member_seed_new = 1;

-- Preserve business operations; tenant settings stay with explicitly authorized tenant administrators.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT 1900000000000004001, @vls_member_template_tenant, menu_id
FROM sys_menu
WHERE @vls_member_seed_new = 1
  AND perms IN (
      'active-safety:list', 'active-safety:events:list',
      'active-safety:workorder:my', 'active-safety:workorder:pending',
      'active-safety:workorder:completed', 'active-safety:workorder:claimable'
  );
