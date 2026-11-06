-- Explicit product opt-in. No credentials, instances, prices or role grants are seeded.
ALTER TABLE vls_compute_node
    ADD COLUMN provider_product VARCHAR(30) NULL,
    ADD COLUMN provider_region VARCHAR(100) NULL,
    ADD COLUMN provider_credential_cipher TEXT NULL,
    ADD COLUMN requested_stopped_mode VARCHAR(30) NULL,
    ADD COLUMN actual_stopped_mode VARCHAR(30) NULL,
    ADD COLUMN provider_charge_type VARCHAR(30) NULL,
    ADD COLUMN provider_gpu_amount INT NULL,
    ADD COLUMN provider_locked TINYINT NULL,
    ADD INDEX idx_compute_provider_instance (tenant_id,provider,provider_instance_id);

INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003003,'配置算力厂商接口',1,33,'#','','',1,0,'F','0','0','vls:compute:configure','#','admin',NOW(),'',NULL,'仅资源所有者；凭据加密，总开关默认关闭'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003003 OR perms='vls:compute:configure');
