-- 修复字符集排序规则冲突脚本

-- 检查当前数据库的字符集和排序规则
SELECT 'Current database collation:' as info;
SELECT 
    SCHEMA_NAME as database_name,
    DEFAULT_CHARACTER_SET_NAME as charset,
    DEFAULT_COLLATION_NAME as collation
FROM information_schema.SCHEMATA 
WHERE SCHEMA_NAME = DATABASE();

-- 检查现有表的排序规则
SELECT 'Current tables collation:' as info;
SELECT 
    TABLE_NAME,
    TABLE_COLLATION
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('local_users', 'user_roles', 'user_permissions', 'tenant_info', 'login_logs');

-- 删除现有的有问题的表（如果存在）
DROP TABLE IF EXISTS `login_logs`;
DROP TABLE IF EXISTS `user_permissions`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `local_users`;
DROP TABLE IF EXISTS `tenant_info`;

-- 使用统一的排序规则重新创建表（使用 utf8mb4_0900_ai_ci）
-- 本地用户表
CREATE TABLE `local_users` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(64) NOT NULL COMMENT '统一用户中心用户ID',
    `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
    `login_id` varchar(100) NOT NULL COMMENT '登录账号',
    `user_name` varchar(100) NOT NULL COMMENT '用户姓名',
    `user_name_py` varchar(200) DEFAULT NULL COMMENT '姓名拼音',
    `user_name_fpy` varchar(50) DEFAULT NULL COMMENT '姓名首字母',
    `photo` varchar(500) DEFAULT NULL COMMENT '用户头像',
    `status` tinyint DEFAULT 1 COMMENT '用户状态：1正常，2禁用',
    `form` tinyint DEFAULT NULL COMMENT '用户来源：1系统创建，2组织创建，3用户池创建，4注册',
    `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
    `login_type` tinyint DEFAULT NULL COMMENT '用户登录身份：1B/E端用户，2C端用户',
    `client` varchar(20) DEFAULT NULL COMMENT '登录客户端类型',
    `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
    `token_expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_login_id` (`login_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地用户表';

-- 用户角色表
CREATE TABLE `user_roles` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(64) NOT NULL COMMENT '用户ID',
    `role_uuid` varchar(64) NOT NULL COMMENT '角色UUID',
    `role_name` varchar(100) NOT NULL COMMENT '角色名称',
    `service_name` varchar(100) NOT NULL COMMENT '服务名称',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role_service` (`user_id`, `role_uuid`, `service_name`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_uuid` (`role_uuid`),
    KEY `idx_service_name` (`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';

-- 用户权限表
CREATE TABLE `user_permissions` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(64) NOT NULL COMMENT '用户ID',
    `service_name` varchar(100) NOT NULL COMMENT '服务名称',
    `pauth` varchar(100) NOT NULL COMMENT '权限集合',
    `auth` varchar(100) NOT NULL COMMENT '权限分类',
    `do_action` varchar(100) NOT NULL COMMENT '操作权限',
    `has_permission` tinyint DEFAULT 1 COMMENT '是否有权限：1有，0无',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_permission` (`user_id`, `service_name`, `pauth`, `auth`, `do_action`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_service_name` (`service_name`),
    KEY `idx_permission` (`pauth`, `auth`, `do_action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户权限表';

-- 租户信息表
CREATE TABLE `tenant_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(64) NOT NULL COMMENT '租户ID',
    `tenant_name` varchar(200) NOT NULL COMMENT '租户名称',
    `phrase` varchar(100) DEFAULT NULL COMMENT '租户短语',
    `username` varchar(100) DEFAULT NULL COMMENT '联系人姓名',
    `phone` varchar(20) DEFAULT NULL COMMENT '联系人手机号码',
    `company_name` varchar(200) DEFAULT NULL COMMENT '单位名称',
    `legal_name` varchar(100) DEFAULT NULL COMMENT '法人代表',
    `organization_code` varchar(50) DEFAULT NULL COMMENT '组织机构代码',
    `status` tinyint DEFAULT 1 COMMENT '状态：1正常，0禁用，2过期',
    `parent_tenant_id` varchar(64) DEFAULT NULL COMMENT '上级租户ID',
    `company_logo` varchar(500) DEFAULT NULL COMMENT '企业Logo',
    `ex_data` json DEFAULT NULL COMMENT '额外数据',
    `start_day` date DEFAULT NULL COMMENT '开始时间',
    `end_day` date DEFAULT NULL COMMENT '结束时间',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    UNIQUE KEY `uk_phrase` (`phrase`),
    KEY `idx_parent_tenant_id` (`parent_tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户信息表';

-- 登录日志表
CREATE TABLE `login_logs` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(64) NOT NULL COMMENT '用户ID',
    `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
    `login_id` varchar(100) NOT NULL COMMENT '登录账号',
    `user_name` varchar(100) NOT NULL COMMENT '用户姓名',
    `login_time` datetime NOT NULL COMMENT '登录时间',
    `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
    `client` varchar(20) DEFAULT NULL COMMENT '客户端类型',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理信息',
    `login_result` tinyint DEFAULT 1 COMMENT '登录结果：1成功，0失败',
    `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_login_result` (`login_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';

-- 验证表创建结果
SELECT 'Tables created with unified collation!' as result;

-- 再次检查表的排序规则
SELECT 'Updated tables collation:' as info;
SELECT 
    TABLE_NAME,
    TABLE_COLLATION
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('local_users', 'user_roles', 'user_permissions', 'tenant_info', 'login_logs');

-- 显示表结构
SELECT 'Showing table structures:' as info;
SHOW TABLES; 