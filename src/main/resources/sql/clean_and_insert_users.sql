-- 清理并重新插入模拟用户数据
-- 如果你想完全重新初始化测试数据，请使用此脚本

-- 1. 清理现有测试数据（按外键依赖顺序删除）
DELETE FROM `login_logs` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
DELETE FROM `user_permissions` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
DELETE FROM `user_roles` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
DELETE FROM `local_users` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
DELETE FROM `tenant_info` WHERE tenant_id IN ('00072c89-bfde-4200-b955-535e3ad0f518', 'test-tenant-001', 'test-tenant-002');

-- 2. 重新插入租户数据
INSERT INTO `tenant_info` (`tenant_id`, `tenant_name`, `phrase`, `username`, `phone`, `company_name`, `legal_name`, `organization_code`, `status`, `ex_data`, `start_day`, `end_day`) VALUES
('00072c89-bfde-4200-b955-535e3ad0f518', 'VLStream科技有限公司', 'admin', '张经理', '13800138000', 'VLStream科技有限公司', '张三', '91110000000000000X', 1, '{"address": "北京市朝阳区", "area": "朝阳区", "bank": "6212260200000000000", "bankac": "中国工商银行", "remarks": "VLStream主租户"}', '2024-01-01', '2025-12-31'),
('test-tenant-001', '测试企业A', 'company-a', '李经理', '13900139000', '测试企业A有限公司', '李四', '91110000000000001X', 1, '{"address": "上海市浦东新区", "area": "浦东新区", "bank": "6212260200000000001", "bankac": "中国建设银行", "remarks": "测试租户A"}', '2024-01-01', '2025-12-31'),
('test-tenant-002', '测试企业B', 'company-b', '王经理', '13700137000', '测试企业B有限公司', '王五', '91110000000000002X', 1, '{"address": "深圳市南山区", "area": "南山区", "bank": "6212260200000000002", "bankac": "中国农业银行", "remarks": "测试租户B"}', '2024-01-01', '2025-12-31');

-- 3. 重新插入用户数据
INSERT INTO `local_users` (
    `user_id`, 
    `tenant_id`, 
    `login_id`, 
    `user_name`, 
    `user_name_py`, 
    `user_name_fpy`, 
    `photo`, 
    `status`, 
    `form`, 
    `login_time`, 
    `login_ip`, 
    `login_type`, 
    `client`, 
    `access_token`, 
    `token_expire_time`
) VALUES
-- 1. 系统管理员用户
(
    'admin-user-001', 
    '00072c89-bfde-4200-b955-535e3ad0f518',
    'admin',
    '系统管理员',
    'xitongguanliyuan',
    'xtgly',
    '/assets/avatar/admin.png',
    1,
    1,
    NOW(),
    '192.168.1.100',
    1,
    'pcweb',
    CONCAT('admin_token_', UNIX_TIMESTAMP()),
    DATE_ADD(NOW(), INTERVAL 24 HOUR)
),
-- 2. 普通业务用户
(
    'user-business-001',
    '00072c89-bfde-4200-b955-535e3ad0f518',
    'zhangsan',
    '张三',
    'zhangsan',
    'zs',
    '/assets/avatar/user1.png',
    1,
    4,
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    '192.168.1.101',
    1,
    'pcweb',
    CONCAT('user_token_', UNIX_TIMESTAMP()),
    DATE_ADD(NOW(), INTERVAL 24 HOUR)
),
-- 3. 测试用户
(
    'test-user-001',
    'test-tenant-001',
    'testuser',
    '测试用户',
    'ceshiyonghu',
    'csyh',
    '/assets/avatar/test.png',
    1,
    4,
    DATE_SUB(NOW(), INTERVAL 1 HOUR),
    '192.168.1.102',
    1,
    'pcweb',
    CONCAT('test_token_', UNIX_TIMESTAMP()),
    DATE_ADD(NOW(), INTERVAL 24 HOUR)
);

-- 4. 重新插入角色数据
INSERT INTO `user_roles` (`user_id`, `role_uuid`, `role_name`, `service_name`) VALUES
-- 管理员角色
('admin-user-001', 'super-admin-001', '超级管理员', 'vlstream'),
('admin-user-001', 'system-admin-001', '系统管理员', 'vlstream'),
-- 普通用户角色
('user-business-001', 'business-user-001', '业务用户', 'vlstream'),
('user-business-001', 'video-operator-001', '视频操作员', 'vlstream'),
-- 测试用户角色
('test-user-001', 'test-role-001', '测试角色', 'vlstream');

-- 5. 重新插入权限数据
INSERT INTO `user_permissions` (`user_id`, `service_name`, `pauth`, `auth`, `do_action`, `has_permission`) VALUES
-- 管理员权限（全部权限）
('admin-user-001', 'vlstream', 'system', 'management', 'view', 1),
('admin-user-001', 'vlstream', 'system', 'management', 'create', 1),
('admin-user-001', 'vlstream', 'system', 'management', 'update', 1),
('admin-user-001', 'vlstream', 'system', 'management', 'delete', 1),
('admin-user-001', 'vlstream', 'video', 'management', 'view', 1),
('admin-user-001', 'vlstream', 'video', 'management', 'create', 1),
('admin-user-001', 'vlstream', 'video', 'management', 'update', 1),
('admin-user-001', 'vlstream', 'video', 'management', 'delete', 1),
('admin-user-001', 'vlstream', 'algorithm', 'management', 'view', 1),
('admin-user-001', 'vlstream', 'algorithm', 'management', 'create', 1),
('admin-user-001', 'vlstream', 'algorithm', 'management', 'update', 1),
('admin-user-001', 'vlstream', 'algorithm', 'management', 'delete', 1),
-- 普通用户权限（业务操作权限）
('user-business-001', 'vlstream', 'video', 'management', 'view', 1),
('user-business-001', 'vlstream', 'video', 'management', 'create', 1),
('user-business-001', 'vlstream', 'video', 'management', 'update', 1),
('user-business-001', 'vlstream', 'video', 'playback', 'view', 1),
('user-business-001', 'vlstream', 'algorithm', 'analysis', 'view', 1),
('user-business-001', 'vlstream', 'algorithm', 'analysis', 'create', 1),
('user-business-001', 'vlstream', 'device', 'management', 'view', 1),
('user-business-001', 'vlstream', 'device', 'management', 'update', 1),
-- 测试用户权限（基础查看权限）
('test-user-001', 'vlstream', 'video', 'management', 'view', 1),
('test-user-001', 'vlstream', 'video', 'playback', 'view', 1),
('test-user-001', 'vlstream', 'algorithm', 'analysis', 'view', 1),
('test-user-001', 'vlstream', 'device', 'management', 'view', 1);

-- 验证插入的数据
SELECT '=== 数据插入完成 ===' as Result;
SELECT COUNT(*) as tenant_count FROM tenant_info WHERE tenant_id IN ('00072c89-bfde-4200-b955-535e3ad0f518', 'test-tenant-001', 'test-tenant-002');
SELECT COUNT(*) as user_count FROM local_users WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
SELECT COUNT(*) as role_count FROM user_roles WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
SELECT COUNT(*) as permission_count FROM user_permissions WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001'); 