-- Local permission batch, separate from the 6000D-only VM delta (044).
-- Catalogue only: no existing/new user, role, tenant or menu assignment is changed.
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003005,'公司算力供给发布',1,35,'#','','',1,0,'F','0','0','vls:compute:company-publish','#','admin',NOW(),'',NULL,'内部显式授权；仍须公司管理租户及configure/publish原权限，不授权开机、收费、训练或他人任务'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003005 OR perms='vls:compute:company-publish');
