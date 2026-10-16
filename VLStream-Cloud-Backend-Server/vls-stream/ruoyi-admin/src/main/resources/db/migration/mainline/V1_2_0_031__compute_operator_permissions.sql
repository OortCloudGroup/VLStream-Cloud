-- Register permission catalogue entries only. No role receives additional permissions here.
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003000,'发布平台算力',1,30,'#','','',1,0,'F','0','0','vls:compute:publish','#','admin',NOW(),'',NULL,'仅授权平台供给管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003000 OR perms='vls:compute:publish');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003001,'算力启停操作',1,31,'#','','',1,0,'F','0','0','vls:compute:power','#','admin',NOW(),'',NULL,'关闭还须资源所有者身份及全服务器无活动任务'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003001 OR perms='vls:compute:power');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 1900000000000003002,'登记厂商状态观察',1,32,'#','','',1,0,'F','0','0','vls:compute:observe','#','admin',NOW(),'',NULL,'仅登记本租户控制台已确认事实，不执行厂商操作'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1900000000000003002 OR perms='vls:compute:observe');
