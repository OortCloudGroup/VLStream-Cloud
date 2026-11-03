-- Register the console pages which previously had no menu-permission gate.
-- This catalogue was originally authored as V1_2_0_065 but never executed in
-- any environment, which left a version gap below the applied history. It is
-- renumbered above the highest applied version so a strict upgrade (with
-- out-of-order disabled) applies it in order. The statements remain idempotent:
-- matching catalogue rows and existing grants are never duplicated or
-- overwritten, and each temporary table is discarded before it is rebuilt so a
-- retry in the same session cannot fail on a leftover table.
-- Existing system and active-safety records and sensitive operation permissions are
-- deliberately untouched. The temporary catalogue contains only new basic pages.
-- Menu definitions are shared; each existing active role is granted entries only
-- in its own non-empty tenant. New roles are not automatically granted here.
DROP TEMPORARY TABLE IF EXISTS vls_basic_menu_catalogue;
CREATE TEMPORARY TABLE vls_basic_menu_catalogue (
    menu_id BIGINT NOT NULL PRIMARY KEY,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT NOT NULL,
    depth INT NOT NULL,
    path VARCHAR(200) NOT NULL,
    menu_type CHAR(1) NOT NULL,
    perms VARCHAR(100) NOT NULL,
    order_num INT NOT NULL
);

INSERT INTO vls_basic_menu_catalogue
    (menu_id, menu_name, parent_id, depth, path, menu_type, perms, order_num)
VALUES
    (920000, '视频汇聚', 0, 0, 'video-aggregation', 'M', 'vls:menu:group:video-aggregation', 1),
    (920001, '决策式AI', 0, 0, 'decision-ai', 'M', 'vls:menu:group:decision-ai', 2),
    (920002, '算法仓库', 0, 0, 'algorithm-warehouse', 'M', 'vls:menu:group:algorithm-warehouse', 3),
    (920003, 'AI算力调度', 0, 0, 'ai-computing', 'M', 'vls:menu:group:ai-computing', 4),
    (920004, '设备管理', 920000, 1, 'device-management', 'M', 'vls:menu:group:device-management', 5),
    (920005, '国标协议', 920004, 2, 'gb-protocol', 'M', 'vls:menu:group:gb-protocol', 6),
    (920006, '事件管理', 920001, 1, 'event-management', 'M', 'vls:menu:group:event-management', 7),
    (920007, '端侧智能', 920001, 1, 'edge-intelligence', 'M', 'vls:menu:group:edge-intelligence', 8),
    (920008, '智能编排', 920001, 1, 'cloud-intelligence', 'M', 'vls:menu:group:cloud-intelligence', 9),
    (920009, '算法训练平台', 920002, 1, 'algorithm-training-platform', 'M', 'vls:menu:group:algorithm-training-platform', 10),
    (920100, '视频广场', 920000, 3, '/video-square', 'C', 'vls:menu:video-square', 1),
    (920101, '视频回放', 920000, 3, '/video-playback', 'C', 'vls:menu:video-playback', 2),
    (920102, '智能分析结果', 920000, 3, '/intelligent-analysis-result', 'C', 'vls:menu:intelligent-analysis-result', 3),
    (920103, 'VLStream协议', 920004, 3, '/vlstream/device', 'C', 'vls:menu:vlstream:device', 4),
    (920104, 'ISUP协议', 920004, 3, '/isup/isupDevice', 'C', 'vls:menu:isup:isupDevice', 5),
    (920105, 'EHome协议', 920004, 3, '/ehome/device', 'C', 'vls:menu:ehome:device', 6),
    (920106, 'RTSP协议', 920004, 3, '/rtsp/rtspDevice', 'C', 'vls:menu:rtsp:rtspDevice', 7),
    (920107, 'ONVIF协议', 920004, 3, '/onvif/cameraManage', 'C', 'vls:menu:onvif:cameraManage', 8),
    (920108, '国标设备', 920005, 3, '/gbmanger/device', 'C', 'vls:menu:gbmanger:device', 9),
    (920109, '云端录像', 920005, 3, '/gbmanger/cloudRecord', 'C', 'vls:menu:gbmanger:cloudRecord', 10),
    (920110, '推流列表', 920005, 3, '/gbmanger/streamPush', 'C', 'vls:menu:gbmanger:streamPush', 11),
    (920111, '拉流列表', 920005, 3, '/gbmanger/streamProxy', 'C', 'vls:menu:gbmanger:streamProxy', 12),
    (920112, '国标级联', 920005, 3, '/gbmanger/platform', 'C', 'vls:menu:gbmanger:platform', 13),
    (920113, '录像计划', 920005, 3, '/gbmanger/recordPlan', 'C', 'vls:menu:gbmanger:recordPlan', 14),
    (920114, '行政分组', 920005, 3, '/gbmanger/administrativeGrouping', 'C', 'vls:menu:gbmanger:administrativeGrouping', 15),
    (920115, '通道管理', 920005, 3, '/gbmanger/thoroughfare', 'C', 'vls:menu:gbmanger:thoroughfare', 16),
    (920116, '报警管理', 920005, 3, '/gbmanger/alarm', 'C', 'vls:menu:gbmanger:alarm', 17),
    (920117, '节点管理', 920005, 3, '/gbmanger/node', 'C', 'vls:menu:gbmanger:node', 18),
    (920118, '分屏监控', 920005, 3, '/gbmanger/wvpLive', 'C', 'vls:menu:gbmanger:wvpLive', 19),
    (920119, '自定义', 920004, 3, '/custom/device', 'C', 'vls:menu:custom:device', 20),
    (920120, '大模型复核', 920006, 3, '/llm-review-tasks', 'C', 'vls:menu:llm-review-tasks', 21),
    (920121, '算法配置', 920007, 3, '/algorithm-arrangement', 'C', 'vls:menu:algorithm-arrangement', 22),
    (920122, '场景治理', 920008, 3, '/scene-governance', 'C', 'vls:menu:scene-governance', 23),
    (920123, '任务配置', 920008, 3, '/algorithm-task', 'C', 'vls:menu:algorithm-task', 24),
    (920124, '任务状态', 920008, 3, '/task-status', 'C', 'vls:menu:task-status', 25),
    (920125, '智能分析', 920008, 3, '/intelligent-analysis-request', 'C', 'vls:menu:intelligent-analysis-request', 26),
    (920126, '算法管理', 920002, 3, '/algorithm-management', 'C', 'vls:menu:algorithm-management', 27),
    (920127, '大模型管理', 920002, 3, '/llm-provider-management', 'C', 'vls:menu:llm-provider-management', 28),
    (920128, '数据集管理', 920009, 3, '/data-management', 'C', 'vls:menu:data-management', 29),
    (920129, '智能标注', 920009, 3, '/smart-annotation', 'C', 'vls:menu:smart-annotation', 30),
    (920130, '算法标注', 920009, 3, '/algorithm-standard', 'C', 'vls:menu:algorithm-standard', 31),
    (920131, '算法训练', 920009, 3, '/algorithm-training', 'C', 'vls:menu:algorithm-training', 32),
    (920132, '算法模型', 920009, 3, '/algorithm-model', 'C', 'vls:menu:algorithm-model', 33),
    (920133, '训练任务', 920003, 3, '/container-instances', 'C', 'vls:menu:container-instances', 34),
    (920134, '算力市场', 920003, 3, '/compute-market', 'C', 'vls:menu:compute-market', 35),
    (920135, '算力管理', 920003, 3, '/compute-management', 'C', 'vls:menu:compute-management', 36),
    (920136, '旧版设备管理', 920004, 3, '/device-management', 'C', 'vls:menu:legacy-device-management', 37);

-- Existing event/work-order page keys remain authoritative. The event page
-- and event-settings pages were open to signed-in users subject to their
-- existing data checks. Work-order lists and model management already required
-- independent workflow permissions.
DROP TEMPORARY TABLE IF EXISTS vls_legacy_pages;
CREATE TEMPORARY TABLE vls_legacy_pages (
    page_perm VARCHAR(100) NOT NULL PRIMARY KEY,
    expected_path VARCHAR(100) NOT NULL,
    action_perm VARCHAR(100) DEFAULT NULL
);
INSERT INTO vls_legacy_pages (page_perm, expected_path, action_perm) VALUES
    ('active-safety:events:list', 'events/secure', NULL),
    ('active-safety:workorder:my', 'work-orders/my', 'workflow:workorder:selectPageOwnWorkOrderList'),
    ('active-safety:workorder:pending', 'work-orders/pending', 'workflow:workorder:selectPageTodoWorkOrderList'),
    ('active-safety:workorder:completed', 'work-orders/completed', 'workflow:workorder:selectPageFinishedWorkOrderList'),
    ('active-safety:workorder:claimable', 'work-orders/claimable', 'workflow:workorder:claimWorkOrderList'),
    ('active-safety:settings:secure', 'settings/secure', NULL),
    ('active-safety:settings:workorder', 'settings/work-orders', 'workflow:model:list');

DROP TEMPORARY TABLE IF EXISTS vls_legacy_page_preflight;
CREATE TEMPORARY TABLE vls_legacy_page_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_legacy_page_tree_ambiguous CHECK (violations = 0)
);
INSERT INTO vls_legacy_page_preflight (violations)
SELECT COUNT(*) FROM vls_legacy_pages required
LEFT JOIN sys_menu page ON BINARY page.perms = BINARY required.page_perm
LEFT JOIN sys_menu parent ON parent.menu_id = page.parent_id
WHERE (SELECT COUNT(*) FROM sys_menu candidate WHERE BINARY candidate.perms = BINARY required.page_perm) <> 1
   OR (SELECT COUNT(*) FROM sys_menu candidate WHERE BINARY candidate.perms = BINARY 'active-safety:list') <> 1
   OR NOT ((BINARY page.perms <=> BINARY required.page_perm)
       AND (page.menu_type <=> 'C') AND (page.status <=> '0')
       AND (BINARY page.path <=> BINARY required.expected_path)
       AND (BINARY parent.perms <=> BINARY 'active-safety:list')
       AND (parent.menu_type <=> 'M') AND (parent.status <=> '0') AND (parent.parent_id <=> 0)
       AND (BINARY parent.path <=> BINARY 'active-safety'));
DROP TEMPORARY TABLE vls_legacy_page_preflight;

DROP TEMPORARY TABLE IF EXISTS vls_legacy_action_preflight;
CREATE TEMPORARY TABLE vls_legacy_action_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_legacy_action_or_tenant_ambiguous CHECK (violations = 0)
);
INSERT INTO vls_legacy_action_preflight (violations)
SELECT (SELECT COUNT(*) FROM (
    SELECT perms FROM sys_menu WHERE perms IN (
      'workflow:workorder:selectPageOwnWorkOrderList', 'workflow:workorder:selectPageTodoWorkOrderList',
      'workflow:workorder:selectPageFinishedWorkOrderList', 'workflow:workorder:claimWorkOrderList',
      'workflow:model:list', 'workflow:model:add')
    GROUP BY perms HAVING COUNT(*) > 1
) duplicate_actions)
+ (SELECT COUNT(*) FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
   JOIN sys_menu m ON m.menu_id = rm.menu_id
   WHERE r.status = '0' AND r.del_flag = '0'
     AND r.tenant_id IS NOT NULL AND TRIM(r.tenant_id) <> ''
     AND (rm.tenant_id IS NULL OR NOT (BINARY rm.tenant_id <=> BINARY r.tenant_id))
     AND m.perms IN ('active-safety:list', 'active-safety:events:list', 'active-safety:workorder:my',
       'active-safety:workorder:pending', 'active-safety:workorder:completed', 'active-safety:workorder:claimable',
       'active-safety:settings:secure', 'active-safety:settings:workorder',
       'workflow:workorder:selectPageOwnWorkOrderList', 'workflow:workorder:selectPageTodoWorkOrderList',
       'workflow:workorder:selectPageFinishedWorkOrderList', 'workflow:workorder:claimWorkOrderList',
       'workflow:model:list', 'workflow:model:add'));
DROP TEMPORARY TABLE vls_legacy_action_preflight;

-- Fail before persistent writes if either a reserved ID or a permission key is
-- already owned by a different record. Matching rows are allowed for safe
-- retries; a missing parent is created only from this exact catalogue.
DROP TEMPORARY TABLE IF EXISTS vls_basic_menu_preflight;
CREATE TEMPORARY TABLE vls_basic_menu_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_basic_menu_identity_or_parent_conflict CHECK (violations = 0)
);
INSERT INTO vls_basic_menu_preflight (violations)
SELECT COUNT(*)
FROM vls_basic_menu_catalogue c
JOIN sys_menu m ON m.menu_id = c.menu_id OR BINARY m.perms = BINARY c.perms
WHERE NOT (
    (m.menu_id <=> c.menu_id)
    AND (BINARY m.perms <=> BINARY c.perms)
    AND (m.parent_id <=> c.parent_id)
    AND (BINARY m.path <=> BINARY c.path)
    AND (BINARY m.menu_type <=> BINARY c.menu_type)
    AND (m.tenant_id IS NULL)
    AND (BINARY m.status <=> BINARY '0')
);
DROP TEMPORARY TABLE vls_basic_menu_preflight;

-- An already present catalogue node must not be silently adopted with a
-- missing parent, even when a parent could be inserted by this migration.
DROP TEMPORARY TABLE IF EXISTS vls_basic_parent_preflight;
CREATE TEMPORARY TABLE vls_basic_parent_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_basic_menu_existing_parent_missing CHECK (violations = 0)
);
INSERT INTO vls_basic_parent_preflight (violations)
SELECT COUNT(*)
FROM vls_basic_menu_catalogue c
JOIN sys_menu m ON m.menu_id = c.menu_id
LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
WHERE c.parent_id <> 0 AND p.menu_id IS NULL;
DROP TEMPORARY TABLE vls_basic_parent_preflight;

-- Legacy global roles may predate tenant scoping and have no tenant_id. They
-- cannot safely receive tenant-scoped page grants, so they are left unchanged.
-- Their existence must not block the upgrade: the grant statements below only
-- target active roles with an explicit tenant.

-- 066 may recreate or reactivate the default member role. If that occurs
-- after this migration, its users would miss the formerly open pages. Refuse
-- the upgrade rather than guess whether a missing/disabled role was intentional.
-- An operator must first inspect and explicitly authorize the role state.
DROP TEMPORARY TABLE IF EXISTS vls_default_member_preflight;
CREATE TEMPORARY TABLE vls_default_member_preflight (
    violations INT NOT NULL,
    CONSTRAINT vls_default_member_missing_disabled_or_ambiguous CHECK (violations = 0)
);
INSERT INTO vls_default_member_preflight (violations)
SELECT CASE WHEN
    (SELECT COUNT(*) FROM sys_role a WHERE a.role_id = 1 AND a.tenant_id IS NOT NULL AND TRIM(a.tenant_id) <> '') = 1
    AND (SELECT COUNT(*) FROM sys_role r JOIN sys_role a ON a.role_id = 1
         WHERE r.role_key = 'vls_member' AND r.tenant_id = a.tenant_id AND r.role_id <> 1) = 1
    AND (SELECT COUNT(*) FROM sys_role r JOIN sys_role a ON a.role_id = 1
         WHERE BINARY r.role_key = BINARY 'vls_member' AND BINARY r.tenant_id = BINARY a.tenant_id
           AND r.role_id <> 1 AND r.status = '0' AND r.del_flag = '0') = 1
    THEN 0 ELSE 1 END;
DROP TEMPORARY TABLE vls_default_member_preflight;

-- MySQL cannot reopen one temporary table twice in a query.
DROP TEMPORARY TABLE IF EXISTS vls_basic_menu_parents;
CREATE TEMPORARY TABLE vls_basic_menu_parents AS SELECT * FROM vls_basic_menu_catalogue;

-- Resolve parents from our exact reserved ID and permission, so a collision or a
-- different pre-existing catalogue never causes another branch to be modified.
INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT c.menu_id, NULL, c.menu_name, c.parent_id, c.order_num, c.path, '', '',
       1, 0, c.menu_type, '0', '0', c.perms, '#', 'vls-basic-menu', NOW(), '控制台基础入口'
FROM vls_basic_menu_catalogue c
WHERE c.depth = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id = c.menu_id OR BINARY m.perms = BINARY c.perms);

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT c.menu_id, NULL, c.menu_name, c.parent_id, c.order_num, c.path, '', '',
       1, 0, c.menu_type, '0', '0', c.perms, '#', 'vls-basic-menu', NOW(), '控制台基础入口'
FROM vls_basic_menu_catalogue c
JOIN vls_basic_menu_parents p ON p.menu_id = c.parent_id
JOIN sys_menu parent ON parent.menu_id = p.menu_id AND BINARY parent.perms = BINARY p.perms
    AND BINARY parent.path = BINARY p.path AND parent.menu_type = 'M'
WHERE c.depth = 1
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id = c.menu_id OR BINARY m.perms = BINARY c.perms);

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT c.menu_id, NULL, c.menu_name, c.parent_id, c.order_num, c.path, '', '',
       1, 0, c.menu_type, '0', '0', c.perms, '#', 'vls-basic-menu', NOW(), '控制台基础入口'
FROM vls_basic_menu_catalogue c
JOIN vls_basic_menu_parents p ON p.menu_id = c.parent_id
JOIN sys_menu parent ON parent.menu_id = p.menu_id AND BINARY parent.perms = BINARY p.perms
    AND BINARY parent.path = BINARY p.path AND parent.menu_type = 'M'
WHERE c.depth = 2
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id = c.menu_id OR BINARY m.perms = BINARY c.perms);

INSERT INTO sys_menu
    (menu_id, tenant_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT c.menu_id, NULL, c.menu_name, c.parent_id, c.order_num, c.path, '', '',
       1, 0, c.menu_type, '0', '0', c.perms, '#', 'vls-basic-menu', NOW(), '控制台基础入口'
FROM vls_basic_menu_catalogue c
JOIN vls_basic_menu_parents p ON p.menu_id = c.parent_id
JOIN sys_menu parent ON parent.menu_id = p.menu_id AND BINARY parent.perms = BINARY p.perms
    AND BINARY parent.path = BINARY p.path AND parent.menu_type = 'M'
WHERE c.depth = 3
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id = c.menu_id OR BINARY m.perms = BINARY c.perms);

-- Preserve pre-existing roles' formerly open basic-page access. Never touch
-- pre-existing grants or grant sensitive action permissions. A role with an
-- explicit tenant but inconsistent grant ownership fails the preflight; a
-- legacy role without a tenant is left unchanged.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT r.role_id, r.tenant_id, m.menu_id
FROM sys_role r
JOIN vls_basic_menu_catalogue c
JOIN sys_menu m ON m.menu_id = c.menu_id AND BINARY m.perms = BINARY c.perms
    AND BINARY m.path = BINARY c.path AND BINARY m.menu_type = BINARY c.menu_type AND m.parent_id = c.parent_id
WHERE r.status = '0' AND r.del_flag = '0'
  AND r.tenant_id IS NOT NULL AND TRIM(r.tenant_id) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing
      WHERE existing.role_id = r.role_id AND existing.menu_id = m.menu_id
  );

-- All existing active tenant-scoped roles could reach these pages before this gate.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT r.role_id, r.tenant_id, m.menu_id FROM sys_role r
JOIN sys_menu m ON BINARY m.perms = BINARY 'active-safety:list'
WHERE r.status = '0' AND r.del_flag = '0'
  AND r.tenant_id IS NOT NULL AND TRIM(r.tenant_id) <> ''
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu old_grant WHERE old_grant.role_id = r.role_id AND old_grant.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT r.role_id, r.tenant_id, m.menu_id FROM sys_role r
JOIN vls_legacy_pages required ON required.action_perm IS NULL
JOIN sys_menu m ON BINARY m.perms = BINARY required.page_perm
WHERE r.status = '0' AND r.del_flag = '0'
  AND r.tenant_id IS NOT NULL AND TRIM(r.tenant_id) <> ''
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu old_grant WHERE old_grant.role_id = r.role_id AND old_grant.menu_id = m.menu_id);

-- Preserve each ordinary list only for roles already holding its original
-- action permission. This does not create or grant any action permission.
INSERT INTO sys_role_menu (role_id, tenant_id, menu_id)
SELECT DISTINCT r.role_id, r.tenant_id, page.menu_id
FROM sys_role r JOIN vls_legacy_pages required ON required.action_perm IS NOT NULL
JOIN sys_menu page ON BINARY page.perms = BINARY required.page_perm
JOIN sys_menu action_menu ON action_menu.status = '0' AND
    (BINARY action_menu.perms = BINARY required.action_perm OR
     (BINARY required.page_perm = BINARY 'active-safety:settings:workorder' AND BINARY action_menu.perms = BINARY 'workflow:model:add'))
JOIN sys_role_menu action_grant ON action_grant.role_id = r.role_id AND action_grant.menu_id = action_menu.menu_id
    AND BINARY action_grant.tenant_id = BINARY r.tenant_id
WHERE r.status = '0' AND r.del_flag = '0'
  AND r.tenant_id IS NOT NULL AND TRIM(r.tenant_id) <> ''
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu old_grant WHERE old_grant.role_id = r.role_id AND old_grant.menu_id = page.menu_id);

DROP TEMPORARY TABLE vls_basic_menu_parents;
DROP TEMPORARY TABLE vls_legacy_pages;
DROP TEMPORARY TABLE vls_basic_menu_catalogue;
