CREATE TABLE `vls_tunnel_endpoint` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL COMMENT 'Owning tenant established by an authenticated operator',
    `wvp_device_row_id` bigint NOT NULL COMMENT 'Authoritative WVP device row captured at enrollment',
    `device_id` varchar(100) NOT NULL COMMENT 'Authoritative WVP business device ID',
    `device_name` varchar(255) NULL,
    `agent_instance_id` varchar(64) NULL COMMENT 'Stable UUID for one agent installation',
    `agent_token_hash` char(64) NULL COMMENT 'SHA-256 of the current agent bearer token',
    `agent_version` varchar(32) NULL,
    `architecture` varchar(32) NULL,
    `firmware_version` varchar(64) NULL,
    `init_system` varchar(32) NULL,
    `local_web_scheme` varchar(8) NOT NULL DEFAULT 'http',
    `local_web_port` int NOT NULL DEFAULT 80,
    `server_bind_port` int NOT NULL COMMENT 'Loopback-only rathole server port',
    `service_name` varchar(128) NOT NULL,
    `config_generation` bigint NOT NULL DEFAULT 1,
    `desired_state` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/REVOKED',
    `agent_status` varchar(32) NOT NULL DEFAULT 'WAITING_FOR_AGENT',
    `tunnel_status` varchar(32) NOT NULL DEFAULT 'OFFLINE',
    `local_web_status` varchar(32) NOT NULL DEFAULT 'UNKNOWN',
    `route_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPLIED/FAILED',
    `route_applied_generation` bigint NULL,
    `last_heartbeat_at` datetime NULL,
    `last_reported_at` datetime NULL,
    `last_error_code` varchar(64) NULL,
    `last_error_message` varchar(255) NULL,
    `revoked_at` datetime NULL,
    `create_user` varchar(64) NULL,
    `create_dept` varchar(64) NULL,
    `create_time` datetime NOT NULL,
    `update_user` varchar(64) NULL,
    `update_time` datetime NOT NULL,
    `status` int NOT NULL DEFAULT 1,
    `is_deleted` int NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tunnel_endpoint_tenant_device` (`tenant_id`, `device_id`),
    UNIQUE KEY `uk_tunnel_endpoint_agent_token` (`agent_token_hash`),
    UNIQUE KEY `uk_tunnel_endpoint_service` (`service_name`),
    UNIQUE KEY `uk_tunnel_endpoint_bind_port` (`server_bind_port`),
    KEY `idx_tunnel_endpoint_heartbeat` (`desired_state`, `last_heartbeat_at`),
    KEY `idx_tunnel_endpoint_route` (`route_status`, `config_generation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='IPC remote-management tunnel endpoint and agent state';

CREATE TABLE `vls_tunnel_enrollment` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL,
    `endpoint_id` bigint NOT NULL,
    `device_id` varchar(100) NOT NULL,
    `code_hash` char(64) NOT NULL COMMENT 'SHA-256 of a high-entropy one-time enrollment code',
    `expires_at` datetime NOT NULL,
    `consumed_at` datetime NULL,
    `consumed_agent_instance_id` varchar(64) NULL,
    `create_user` varchar(64) NULL,
    `create_time` datetime NOT NULL,
    `update_time` datetime NOT NULL,
    `is_deleted` int NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tunnel_enrollment_code` (`code_hash`),
    KEY `idx_tunnel_enrollment_endpoint` (`tenant_id`, `endpoint_id`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Short-lived IPC tunnel enrollment grants';

CREATE TABLE `vls_tunnel_access_session` (
    `id` bigint NOT NULL COMMENT 'Primary key',
    `tenant_id` varchar(64) NOT NULL,
    `session_id` varchar(64) NOT NULL,
    `endpoint_id` bigint NOT NULL,
    `device_id` varchar(100) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `access_token_hash` char(64) NOT NULL COMMENT 'SHA-256 of the gateway bootstrap token',
    `expires_at` datetime NOT NULL,
    `opened_at` datetime NULL,
    `revoked_at` datetime NULL,
    `create_time` datetime NOT NULL,
    `update_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tunnel_access_session_id` (`session_id`),
    UNIQUE KEY `uk_tunnel_access_token` (`access_token_hash`),
    KEY `idx_tunnel_access_endpoint` (`tenant_id`, `endpoint_id`, `expires_at`),
    KEY `idx_tunnel_access_expiry` (`expires_at`, `revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Short-lived authenticated browser access to IPC management';

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000800, 'VLS设备远程管理', 1, 12, '#', '', '', 1, 0, 'C', '1', '0',
    'vls:tunnel:view', 'monitor', 'admin', NOW(), '', NULL, 'Hidden permission container for IPC remote management'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000800);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000801, '远程管理接入配置', 1900000000000000800, 1, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:tunnel:manage', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000801);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 1900000000000000802, '打开设备管理后台', 1900000000000000800, 2, '#', '', '', 1, 0, 'F', '0', '0',
    'vls:tunnel:access', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1900000000000000802);
