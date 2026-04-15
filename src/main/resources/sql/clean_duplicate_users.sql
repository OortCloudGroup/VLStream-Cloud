-- 清理重复的用户数据
-- 删除所有local_users表中的数据
DELETE FROM local_users;

-- 重置自增ID
ALTER TABLE local_users AUTO_INCREMENT = 1;

-- 清理其他相关表
DELETE FROM user_roles;
DELETE FROM user_permissions;
DELETE FROM login_logs;

-- 重置自增ID
ALTER TABLE user_roles AUTO_INCREMENT = 1;
ALTER TABLE user_permissions AUTO_INCREMENT = 1;
ALTER TABLE login_logs AUTO_INCREMENT = 1;

-- 重新插入测试数据
INSERT IGNORE INTO tenant_info (tenant_id, tenant_name, phrase, status, created_at, updated_at) 
VALUES ('00072c89-bfde-4200-b955-535e3ad0f518', '测试租户', 'test_tenant', 1, NOW(), NOW());

-- 插入测试用户数据
INSERT IGNORE INTO local_users (
    user_id, tenant_id, login_id, user_name, status, form, 
    login_time, login_ip, login_type, client, access_token, token_expire_time, 
    created_at, updated_at
) VALUES (
    'admin_user_001', '00072c89-bfde-4200-b955-535e3ad0f518', 'admin', '系统管理员', 1, 4,
    '2024-07-24 10:40:00', '127.0.0.1', 1, 'web', 'admin_token_001', '2025-07-25 10:40:00',
    NOW(), NOW()
);

INSERT IGNORE INTO local_users (
    user_id, tenant_id, login_id, user_name, status, form, 
    login_time, login_ip, login_type, client, access_token, token_expire_time, 
    created_at, updated_at
) VALUES (
    'user_002', '00072c89-bfde-4200-b955-535e3ad0f518', 'user1', '普通用户1', 1, 4,
    '2024-07-24 10:40:00', '127.0.0.1', 1, 'web', 'user_token_002', '2025-07-25 10:40:00',
    NOW(), NOW()
);

INSERT IGNORE INTO local_users (
    user_id, tenant_id, login_id, user_name, status, form, 
    login_time, login_ip, login_type, client, access_token, token_expire_time, 
    created_at, updated_at
) VALUES (
    'user_003', '00072c89-bfde-4200-b955-535e3ad0f518', 'user2', '普通用户2', 1, 4,
    '2024-07-24 10:40:00', '127.0.0.1', 1, 'web', 'user_token_003', '2025-07-25 10:40:00',
    NOW(), NOW()
);

-- 插入用户角色数据
INSERT IGNORE INTO user_roles (user_id, role_id, role_name, status, created_at, updated_at) VALUES
('admin_user_001', 'admin_role', '管理员', 1, NOW(), NOW()),
('user_002', 'user_role', '普通用户', 1, NOW(), NOW()),
('user_003', 'user_role', '普通用户', 1, NOW(), NOW());

-- 插入用户权限数据（使用反引号避免关键字冲突）
INSERT IGNORE INTO user_permissions (user_id, permission_id, `permission_name`, `service_name`, status, created_at, updated_at) VALUES
('admin_user_001', 'all_permissions', '所有权限', 'all_services', 1, NOW(), NOW()),
('user_002', 'read_permission', '读取权限', 'video_service', 1, NOW(), NOW()),
('user_003', 'read_permission', '读取权限', 'video_service', 1, NOW(), NOW());

SELECT '数据清理完成' as result; 