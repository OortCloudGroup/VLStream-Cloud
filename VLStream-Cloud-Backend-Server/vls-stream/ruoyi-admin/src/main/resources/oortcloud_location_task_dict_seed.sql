-- Location-task numeric contracts used by the Java compatibility API.
-- This script is idempotent and intentionally keeps request-only filters
-- separate from persisted status dictionaries.

INSERT INTO `sys_dict_type`
(`dict_id`, `tenant_id`, `user_id`, `dict_name`, `dict_type`, `status`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT ids.max_id + seed.seq, NULL, NULL, seed.dict_name, seed.dict_type, '0',
       'system', NOW(), '', NULL, seed.remark
FROM (
    SELECT 1 seq, '事件处理状态' dict_name, 'vls_event_status' dict_type,
           'oort_task_event.status：1已完成，2待处理' remark
    UNION ALL SELECT 2, '事件模块类型', 'vls_event_module_type',
           'oort_task_event.mod_type'
    UNION ALL SELECT 3, '主动安全告警状态', 'vls_event_alarm_status',
           'oort_task_event.mod_status原始值，不用于列表确认筛选'
    UNION ALL SELECT 4, '事件转工单状态', 'vls_event_work_order_status',
           'oort_task_event.work_order_status'
    UNION ALL SELECT 5, '事件执行对象类型', 'vls_event_executor_type',
           'oort_task_event_user.u_type'
    UNION ALL SELECT 6, '事件分组维度', 'vls_event_group_type',
           'V2分组：1区域，2分组，3标签'
    UNION ALL SELECT 7, '事件坐标系', 'vls_event_coordinate_system',
           '事件point中的coord_system_type'
    UNION ALL SELECT 8, '主动安全确认筛选', 'vls_event_alarm_filter',
           'event_list.mod_status：0全部，1待确认，2已确认；不可当作oort_task_event.mod_status原始值'
    UNION ALL SELECT 9, '我的事件范围', 'vls_event_execution_scope',
           'myevent_list.exec_status'
    UNION ALL SELECT 10, '事件图片筛选', 'vls_event_picture_filter',
           'event_list.had_pic'
    UNION ALL SELECT 11, '事件统计口径', 'vls_event_statistics_type',
           'event_statistics.stat_type'
) seed
CROSS JOIN (SELECT COALESCE(MAX(`dict_id`), 0) max_id FROM `sys_dict_type`) ids
LEFT JOIN `sys_dict_type` existing ON existing.`dict_type` = seed.dict_type
WHERE existing.`dict_id` IS NULL;

INSERT INTO `sys_dict_data`
(`dict_code`, `tenant_id`, `user_id`, `dict_sort`, `dict_label`, `dict_value`,
 `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT ids.max_id + seed.seq, NULL, NULL, seed.dict_sort, seed.dict_label,
       seed.dict_value, seed.dict_type, '', seed.list_class, seed.is_default,
       '0', 'system', NOW(), '', NULL, seed.remark
FROM (
    SELECT 1 seq, 1 dict_sort, '已完成' dict_label, '1' dict_value,
           'vls_event_status' dict_type, 'success' list_class, 'N' is_default,
           '事件已完成' remark
    UNION ALL SELECT 2, 2, '待处理', '2', 'vls_event_status', 'warning', 'Y', '事件待处理'
    UNION ALL SELECT 3, 1, '事件拍传', '1', 'vls_event_module_type', 'primary', 'Y', '人工事件拍传'
    UNION ALL SELECT 4, 2, '主动安全', '2', 'vls_event_module_type', 'danger', 'N', '硬件主动安全事件'
    UNION ALL SELECT 5, 1, '待确认', '0', 'vls_event_alarm_status', 'warning', 'Y', '主动安全告警待确认'
    UNION ALL SELECT 6, 2, '真实告警', '1', 'vls_event_alarm_status', 'danger', 'N', '确认为真实告警'
    UNION ALL SELECT 7, 3, '维保', '2', 'vls_event_alarm_status', 'primary', 'N', '设备维保事件'
    UNION ALL SELECT 8, 4, '误报', '3', 'vls_event_alarm_status', 'info', 'N', '确认为误报'
    UNION ALL SELECT 9, 1, '未转工单', '0', 'vls_event_work_order_status', 'info', 'Y', '尚未关联工单'
    UNION ALL SELECT 10, 2, '已转工单', '1', 'vls_event_work_order_status', 'success', 'N', '已关联工单'
    UNION ALL SELECT 11, 1, '用户', '1', 'vls_event_executor_type', 'primary', 'Y', '执行对象为用户'
    UNION ALL SELECT 12, 2, '车辆', '2', 'vls_event_executor_type', 'info', 'N', '执行对象为车辆'
    UNION ALL SELECT 13, 1, '区域', '1', 'vls_event_group_type', 'primary', 'Y', '按区域配置'
    UNION ALL SELECT 14, 2, '分组', '2', 'vls_event_group_type', 'success', 'N', '按设备分组配置'
    UNION ALL SELECT 15, 3, '标签', '3', 'vls_event_group_type', 'warning', 'N', '按设备标签配置'
    UNION ALL SELECT 16, 1, 'WGS84', '1', 'vls_event_coordinate_system', 'primary', 'Y', 'WGS84坐标系'
    UNION ALL SELECT 17, 2, 'GCJ02', '2', 'vls_event_coordinate_system', 'success', 'N', 'GCJ02坐标系'
    UNION ALL SELECT 18, 3, 'BD09', '3', 'vls_event_coordinate_system', 'warning', 'N', 'BD09坐标系'
    UNION ALL SELECT 19, 1, '全部', '0', 'vls_event_alarm_filter', 'info', 'Y', '不限制确认状态'
    UNION ALL SELECT 20, 2, '待确认', '1', 'vls_event_alarm_filter', 'warning', 'N', '筛选原始状态0'
    UNION ALL SELECT 21, 3, '已确认', '2', 'vls_event_alarm_filter', 'success', 'N', '筛选原始状态1、2、3'
    UNION ALL SELECT 22, 1, '我执行的', '1', 'vls_event_execution_scope', 'primary', 'Y', '当前用户为执行人'
    UNION ALL SELECT 23, 2, '我发布的', '2', 'vls_event_execution_scope', 'success', 'N', '当前用户为发起人'
    UNION ALL SELECT 24, 3, '我反馈过的', '3', 'vls_event_execution_scope', 'warning', 'N', '当前用户反馈过'
    UNION ALL SELECT 25, 4, '与我相关的全部', '4', 'vls_event_execution_scope', 'info', 'N', '执行、发布或反馈过'
    UNION ALL SELECT 26, 1, '全部', '0', 'vls_event_picture_filter', 'info', 'Y', '不限制图片'
    UNION ALL SELECT 27, 2, '有图片', '1', 'vls_event_picture_filter', 'success', 'N', '图片数量大于0'
    UNION ALL SELECT 28, 3, '无图片', '2', 'vls_event_picture_filter', 'warning', 'N', '图片数量等于0'
    UNION ALL SELECT 29, 1, '按执行人', '1', 'vls_event_statistics_type', 'primary', 'Y', '按执行人统计'
    UNION ALL SELECT 30, 2, '按发起人', '2', 'vls_event_statistics_type', 'success', 'N', '按发起人统计'
) seed
CROSS JOIN (SELECT COALESCE(MAX(`dict_code`), 0) max_id FROM `sys_dict_data`) ids
LEFT JOIN `sys_dict_data` existing
       ON existing.`dict_type` = seed.dict_type
      AND existing.`dict_value` = seed.dict_value
WHERE existing.`dict_code` IS NULL;
