-- A management owner is distinct from a public instance's current consumer claim.
-- Legacy rows deliberately retain unknown ownership. Never infer an owner from tenant/visitor.
ALTER TABLE vls_compute_node
    ADD COLUMN management_owner_subject CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD INDEX idx_compute_management_owner (tenant_id,management_owner_subject,resource_scope);

-- Permission catalogue only. Existing wildcard administrators retain their scope.
-- No role/user receives this permission automatically.
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003004,'管理本租户全部算力',1,34,'#','','',1,0,'F','0','0','vls:compute:manage-all','#','admin',NOW(),'',NULL,'含历史归属待确认节点；操作仍需原configure/publish等权限，不授权他人任务数据'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003004 OR perms='vls:compute:manage-all');
