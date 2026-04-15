-- 模拟用户数据插入脚本
-- 用于测试统一用户中心登录功能

-- 清理现有测试数据（可选，如果要重新初始化）
-- DELETE FROM `login_logs` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
-- DELETE FROM `user_permissions` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
-- DELETE FROM `user_roles` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
-- DELETE FROM `local_users` WHERE user_id IN ('admin-user-001', 'user-business-001', 'test-user-001');
-- DELETE FROM `tenant_info` WHERE tenant_id IN ('00072c89-bfde-4200-b955-535e3ad0f518', 'test-tenant-001', 'test-tenant-002');

-- 插入测试租户数据（如果不存在则插入）
INSERT IGNORE INTO `tenant_info` (`tenant_id`, `tenant_name`, `phrase`, `username`, `phone`, `company_name`, `legal_name`, `organization_code`, `status`, `ex_data`, `start_day`, `end_day`) VALUES
('00072c89-bfde-4200-b955-535e3ad0f518', 'VLStream科技有限公司', 'admin', '张经理', '13800138000', 'VLStream科技有限公司', '张三', '91110000000000000X', 1, '{"address": "北京市朝阳区", "area": "朝阳区", "bank": "6212260200000000000", "bankac": "中国工商银行", "remarks": "VLStream主租户"}', '2024-01-01', '2025-12-31'),
('test-tenant-001', '测试企业A', 'company-a', '李经理', '13900139000', '测试企业A有限公司', '李四', '91110000000000001X', 1, '{"address": "上海市浦东新区", "area": "浦东新区", "bank": "6212260200000000001", "bankac": "中国建设银行", "remarks": "测试租户A"}', '2024-01-01', '2025-12-31'),
('test-tenant-002', '测试企业B', 'company-b', '王经理', '13700137000', '测试企业B有限公司', '王五', '91110000000000002X', 1, '{"address": "深圳市南山区", "area": "南山区", "bank": "6212260200000000002", "bankac": "中国农业银行", "remarks": "测试租户B"}', '2024-01-01', '2025-12-31');

-- 插入三个测试用户（如果不存在则插入）
INSERT IGNORE INTO `local_users` (
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

-- 插入用户角色数据（如果不存在则插入）
INSERT IGNORE INTO `user_roles` (`user_id`, `role_uuid`, `role_name`, `service_name`) VALUES
-- 管理员角色
('admin-user-001', 'super-admin-001', '超级管理员', 'vlstream'),
('admin-user-001', 'system-admin-001', '系统管理员', 'vlstream'),

-- 普通用户角色
('user-business-001', 'business-user-001', '业务用户', 'vlstream'),
('user-business-001', 'video-operator-001', '视频操作员', 'vlstream'),

-- 测试用户角色
('test-user-001', 'test-role-001', '测试角色', 'vlstream');

-- 插入用户权限数据（如果不存在则插入）
INSERT IGNORE INTO `user_permissions` (`user_id`, `service_name`, `pauth`, `auth`, `do_action`, `has_permission`) VALUES
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

-- 插入登录日志记录
INSERT INTO `login_logs` (`user_id`, `tenant_id`, `login_id`, `user_name`, `login_time`, `login_ip`, `client`, `user_agent`, `login_result`, `error_message`) VALUES
-- 管理员登录记录
('admin-user-001', '00072c89-bfde-4200-b955-535e3ad0f518', 'admin', '系统管理员', NOW(), '192.168.1.100', 'pcweb', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 1, NULL),
('admin-user-001', '00072c89-bfde-4200-b955-535e3ad0f518', 'admin', '系统管理员', DATE_SUB(NOW(), INTERVAL 1 DAY), '192.168.1.100', 'pcweb', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 1, NULL),

-- 普通用户登录记录
('user-business-001', '00072c89-bfde-4200-b955-535e3ad0f518', 'zhangsan', '张三', DATE_SUB(NOW(), INTERVAL 2 HOUR), '192.168.1.101', 'pcweb', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 1, NULL),
('user-business-001', '00072c89-bfde-4200-b955-535e3ad0f518', 'zhangsan', '张三', DATE_SUB(NOW(), INTERVAL 1 DAY), '192.168.1.101', 'android', 'VLStream-Android/1.0', 1, NULL),

-- 测试用户登录记录  
('test-user-001', 'test-tenant-001', 'testuser', '测试用户', DATE_SUB(NOW(), INTERVAL 1 HOUR), '192.168.1.102', 'pcweb', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 1, NULL),

-- 失败登录记录
('unknown-user', '00072c89-bfde-4200-b955-535e3ad0f518', 'wronguser', '错误用户', DATE_SUB(NOW(), INTERVAL 3 HOUR), '192.168.1.200', 'pcweb', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 0, '用户名或密码错误');

-- 验证插入的数据
SELECT '=== 租户信息 ===' as Info;
SELECT tenant_id, tenant_name, phrase, company_name, status FROM tenant_info;

SELECT '=== 用户信息 ===' as Info;
SELECT user_id, tenant_id, login_id, user_name, status, login_time FROM local_users;

SELECT '=== 用户角色 ===' as Info;
SELECT ur.user_id, lu.user_name, ur.role_name, ur.service_name 
FROM user_roles ur 
JOIN local_users lu ON ur.user_id = lu.user_id;

SELECT '=== 用户权限统计 ===' as Info;
SELECT up.user_id, lu.user_name, up.service_name, COUNT(*) as permission_count
FROM user_permissions up 
JOIN local_users lu ON up.user_id = lu.user_id 
WHERE up.has_permission = 1
GROUP BY up.user_id, lu.user_name, up.service_name;

SELECT '=== 登录日志 ===' as Info;
SELECT user_id, login_id, user_name, login_time, login_ip, client, login_result 
FROM login_logs 
ORDER BY login_time DESC 
LIMIT 10; 