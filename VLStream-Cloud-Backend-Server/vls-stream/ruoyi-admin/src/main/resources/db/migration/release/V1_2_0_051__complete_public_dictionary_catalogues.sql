-- Add missing public system/workflow/event dictionary catalogues only.
-- No copied tenant/user ownership, credentials or permission assignments.
-- Preserve every existing type and every existing (dict_type,dict_value).
-- The excluded test-only dictionary cs is intentionally not seeded.

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'系统状态','sys_common_status','0','system',NOW(),'登录状态列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_common_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'系统开关','sys_normal_disable','0','system',NOW(),'系统开关列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_normal_disable');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'通知状态','sys_notice_status','0','system',NOW(),'通知状态列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_notice_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'通知类型','sys_notice_type','0','system',NOW(),'通知类型列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_notice_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'操作类型','sys_oper_type','0','system',NOW(),'操作类型列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_oper_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'菜单状态','sys_show_hide','0','system',NOW(),'菜单状态列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_show_hide');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'用户性别','sys_user_sex','0','system',NOW(),'用户性别列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_user_sex');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'系统是否','sys_yes_no','0','system',NOW(),'系统是否列表'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sys_yes_no');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'主动安全确认筛选','vls_event_alarm_filter','0','system',NOW(),'event_list.mod_status：0全部，1待确认，2已确认；不可当作oort_task_event.mod_status原始值'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_alarm_filter');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'主动安全告警状态','vls_event_alarm_status','0','system',NOW(),'oort_task_event.mod_status原始值，不用于列表确认筛选'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_alarm_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件坐标系','vls_event_coordinate_system','0','system',NOW(),'事件point中的coord_system_type'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_coordinate_system');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'我的事件范围','vls_event_execution_scope','0','system',NOW(),'myevent_list.exec_status'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_execution_scope');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件执行对象类型','vls_event_executor_type','0','system',NOW(),'oort_task_event_user.u_type'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_executor_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件分组维度','vls_event_group_type','0','system',NOW(),'V2分组：1区域，2分组，3标签'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_group_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件模块类型','vls_event_module_type','0','system',NOW(),'oort_task_event.mod_type'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_module_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件图片筛选','vls_event_picture_filter','0','system',NOW(),'event_list.had_pic'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_picture_filter');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件统计口径','vls_event_statistics_type','0','system',NOW(),'event_statistics.stat_type'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_statistics_type');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件处理状态','vls_event_status','0','system',NOW(),'oort_task_event.status：1已完成，2待处理'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'事件转工单状态','vls_event_work_order_status','0','system',NOW(),'oort_task_event.work_order_status'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='vls_event_work_order_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'审批按钮','wf_approval_button','0','system',NOW(),'流程审批按钮'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='wf_approval_button');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'流程状态','wf_process_status','0','system',NOW(),'工作流程状态'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='wf_process_status');

INSERT INTO sys_dict_type (dict_id,tenant_id,user_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,'工单按钮','wk_approval_button','0','system',NOW(),'工单设计按钮'
FROM (SELECT COALESCE(MAX(dict_id),0)+1 next_id FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='wk_approval_button');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'成功','0','sys_common_status','','primary','N','0','system',NOW(),'正常状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_common_status' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'失败','1','sys_common_status','','danger','N','0','system',NOW(),'停用状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_common_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'正常','0','sys_normal_disable','','primary','Y','0','system',NOW(),'正常状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_normal_disable' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'停用','1','sys_normal_disable','','danger','N','0','system',NOW(),'停用状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_normal_disable' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'正常','0','sys_notice_status','','primary','Y','0','system',NOW(),'正常状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_notice_status' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'关闭','1','sys_notice_status','','danger','N','0','system',NOW(),'关闭状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_notice_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'通知','1','sys_notice_type','','warning','Y','0','system',NOW(),'通知'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_notice_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'公告','2','sys_notice_type','','success','N','0','system',NOW(),'公告'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_notice_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'新增','1','sys_oper_type','','info','N','0','system',NOW(),'新增操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'修改','2','sys_oper_type','','info','N','0','system',NOW(),'修改操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'删除','3','sys_oper_type','','danger','N','0','system',NOW(),'删除操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'授权','4','sys_oper_type','','primary','N','0','system',NOW(),'授权操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='4');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,5,'导出','5','sys_oper_type','','warning','N','0','system',NOW(),'导出操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='5');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,6,'导入','6','sys_oper_type','','warning','N','0','system',NOW(),'导入操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='6');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,7,'强退','7','sys_oper_type','','danger','N','0','system',NOW(),'强退操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='7');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,8,'生成代码','8','sys_oper_type','','warning','N','0','system',NOW(),'生成操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='8');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,9,'清空数据','9','sys_oper_type','','danger','N','0','system',NOW(),'清空操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='9');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,99,'其他','0','sys_oper_type','','info','N','0','system',NOW(),'其他操作'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_oper_type' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'显示','0','sys_show_hide','','primary','Y','0','system',NOW(),'显示菜单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_show_hide' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'隐藏','1','sys_show_hide','','danger','N','0','system',NOW(),'隐藏菜单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_show_hide' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'男','0','sys_user_sex','','','Y','0','system',NOW(),'性别男'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_user_sex' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'女','1','sys_user_sex','','','N','0','system',NOW(),'性别女'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_user_sex' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'未知','2','sys_user_sex','','','N','0','system',NOW(),'性别未知'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_user_sex' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'是','Y','sys_yes_no','','primary','Y','0','system',NOW(),'系统默认是'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_yes_no' AND dict_value='Y');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'否','N','sys_yes_no','','danger','N','0','system',NOW(),'系统默认否'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sys_yes_no' AND dict_value='N');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'全部','0','vls_event_alarm_filter','','info','Y','0','system',NOW(),'不限制确认状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_filter' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'待确认','1','vls_event_alarm_filter','','warning','N','0','system',NOW(),'筛选原始状态0'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_filter' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'已确认','2','vls_event_alarm_filter','','success','N','0','system',NOW(),'筛选原始状态1、2、3'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_filter' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'待确认','0','vls_event_alarm_status','','warning','Y','0','system',NOW(),'主动安全告警待确认'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_status' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'真实告警','1','vls_event_alarm_status','','danger','N','0','system',NOW(),'确认为真实告警'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'维保','2','vls_event_alarm_status','','primary','N','0','system',NOW(),'设备维保事件'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_status' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'误报','3','vls_event_alarm_status','','info','N','0','system',NOW(),'确认为误报'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_alarm_status' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'WGS84','1','vls_event_coordinate_system','','primary','Y','0','system',NOW(),'WGS84坐标系'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_coordinate_system' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'GCJ02','2','vls_event_coordinate_system','','success','N','0','system',NOW(),'GCJ02坐标系'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_coordinate_system' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'BD09','3','vls_event_coordinate_system','','warning','N','0','system',NOW(),'BD09坐标系'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_coordinate_system' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'我执行的','1','vls_event_execution_scope','','primary','Y','0','system',NOW(),'当前用户为执行人'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_execution_scope' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'我发布的','2','vls_event_execution_scope','','success','N','0','system',NOW(),'当前用户为发起人'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_execution_scope' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'我反馈过的','3','vls_event_execution_scope','','warning','N','0','system',NOW(),'当前用户反馈过'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_execution_scope' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'与我相关的全部','4','vls_event_execution_scope','','info','N','0','system',NOW(),'执行、发布或反馈过'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_execution_scope' AND dict_value='4');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'用户','1','vls_event_executor_type','','primary','Y','0','system',NOW(),'执行对象为用户'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_executor_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'车辆','2','vls_event_executor_type','','info','N','0','system',NOW(),'执行对象为车辆'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_executor_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'区域','1','vls_event_group_type','','primary','Y','0','system',NOW(),'按区域配置'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_group_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'分组','2','vls_event_group_type','','success','N','0','system',NOW(),'按设备分组配置'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_group_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'标签','3','vls_event_group_type','','warning','N','0','system',NOW(),'按设备标签配置'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_group_type' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'事件拍传','1','vls_event_module_type','','primary','Y','0','system',NOW(),'人工事件拍传'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_module_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'主动安全','2','vls_event_module_type','','danger','N','0','system',NOW(),'硬件主动安全事件'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_module_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'全部','0','vls_event_picture_filter','','info','Y','0','system',NOW(),'不限制图片'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_picture_filter' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'有图片','1','vls_event_picture_filter','','success','N','0','system',NOW(),'图片数量大于0'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_picture_filter' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'无图片','2','vls_event_picture_filter','','warning','N','0','system',NOW(),'图片数量等于0'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_picture_filter' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'按执行人','1','vls_event_statistics_type','','primary','Y','0','system',NOW(),'按执行人统计'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_statistics_type' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'按发起人','2','vls_event_statistics_type','','success','N','0','system',NOW(),'按发起人统计'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_statistics_type' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'已完成','1','vls_event_status','','success','N','0','system',NOW(),'事件已完成'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'待处理','2','vls_event_status','','warning','Y','0','system',NOW(),'事件待处理'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_status' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'未转工单','0','vls_event_work_order_status','','info','Y','0','system',NOW(),'尚未关联工单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_work_order_status' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'已转工单','1','vls_event_work_order_status','','success','N','0','system',NOW(),'已关联工单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='vls_event_work_order_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'通过','0','wf_approval_button',' ',NULL,'Y','0','system',NOW(),'工单审批通过'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_approval_button' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'委派','1','wf_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批委派'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_approval_button' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'转办','2','wf_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批转办'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_approval_button' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'退回','3','wf_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批退回'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_approval_button' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,5,'拒绝','4','wf_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批拒绝'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_approval_button' AND dict_value='4');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'进行中','running','wf_process_status','','primary','N','0','system',NOW(),'进行中状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_process_status' AND dict_value='running');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'已终止','terminated','wf_process_status','','danger','N','0','system',NOW(),'已终止状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_process_status' AND dict_value='terminated');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'已完成','completed','wf_process_status','','success','N','0','system',NOW(),'已完成状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_process_status' AND dict_value='completed');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'已取消','canceled','wf_process_status','','warning','N','0','system',NOW(),'已取消状态'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_process_status' AND dict_value='canceled');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,1,'通过','0','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批通过'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='0');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,2,'委派','1','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批委派'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='1');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,3,'转办','2','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批转办'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='2');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,4,'退回','3','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批退回'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='3');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,5,'拒绝','4','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批拒绝'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='4');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,6,'指派','5','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批指派'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='5');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,7,'接单','6','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批接单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='6');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,8,'退单','7','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批退单'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='7');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,9,'提交回访记录','8','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批提交回访记录'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='8');

INSERT INTO sys_dict_data (dict_code,tenant_id,user_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark)
SELECT ids.next_id,NULL,NULL,10,'验收通过','9','wk_approval_button',' ',NULL,'Y','0','system',NOW(),'流程审批验收通过'
FROM (SELECT COALESCE(MAX(dict_code),0)+1 next_id FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wk_approval_button' AND dict_value='9');
