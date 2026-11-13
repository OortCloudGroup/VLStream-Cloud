-- Independent from orchestration scenes, schedules and runs.
CREATE TABLE IF NOT EXISTS vls_cloud_device_owner (
 tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
 actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
 scope varchar(72) COLLATE utf8mb4_bin NOT NULL,
 PRIMARY KEY(tenant_id,actor_id,scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS vls_cloud_device_group (
 id varchar(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
 actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
 scope varchar(72) COLLATE utf8mb4_bin NOT NULL,
 node_id bigint NOT NULL,
 resource_json longtext NOT NULL,
 revision int NOT NULL DEFAULT 1,
 run_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 retry_at bigint NOT NULL DEFAULT 0,
 UNIQUE KEY uk_cloud_device_group(tenant_id,actor_id,scope,node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS vls_cloud_device_binding (
 id varchar(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
 actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
 scope varchar(72) COLLATE utf8mb4_bin NOT NULL,
 group_id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 camera_key varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 algorithm_id varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 camera_json longtext NOT NULL,
 rule_json longtext NOT NULL,
 enabled tinyint NOT NULL DEFAULT 1,
 revision int NOT NULL DEFAULT 1,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_cloud_device_pair(tenant_id,actor_id,scope,camera_key,algorithm_id),
 KEY idx_cloud_device_group(group_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS vls_cloud_device_operation (
 id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tenant_id varchar(64) COLLATE utf8mb4_bin NOT NULL,
 actor_id varchar(128) COLLATE utf8mb4_bin NOT NULL,
 scope varchar(72) COLLATE utf8mb4_bin NOT NULL,
 request_hash varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,actor_id,scope,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS vls_cloud_device_run LIKE vls_cloud_ai_run;
CREATE TABLE IF NOT EXISTS vls_cloud_device_event LIKE vls_cloud_ai_event;

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 920010,'云端智能',920001,9,'cloud-device-intelligence','',1,0,'M','0','0','vls:menu:group:cloud-device-intelligence','云端智能','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=920010 OR perms='vls:menu:group:cloud-device-intelligence');
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 920150,'算法配置',920010,1,'/cloud-algorithm-config','DecisionAI/CloudAlgorithmConfig',1,0,'C','0','0','vls:menu:cloud-algorithm-config','算法配置','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=920150 OR perms='vls:menu:cloud-algorithm-config');
-- Initially grant to roles already allowed to manage cloud scenes, never all roles.
INSERT INTO sys_role_menu(role_id,tenant_id,menu_id)
SELECT DISTINCT r.role_id,r.tenant_id,m.menu_id FROM sys_role r
JOIN sys_role_menu old_grant ON old_grant.role_id=r.role_id AND old_grant.tenant_id=r.tenant_id
JOIN sys_menu old_page ON old_page.menu_id=old_grant.menu_id AND old_page.perms='vls:menu:scene-governance'
JOIN sys_menu m ON m.perms IN ('vls:menu:group:cloud-device-intelligence','vls:menu:cloud-algorithm-config')
WHERE r.status='0' AND r.del_flag='0' AND r.tenant_id IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM sys_role_menu existing WHERE existing.role_id=r.role_id AND existing.menu_id=m.menu_id);
