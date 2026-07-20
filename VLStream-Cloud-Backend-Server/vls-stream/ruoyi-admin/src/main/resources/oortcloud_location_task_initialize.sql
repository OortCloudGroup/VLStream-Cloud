-- Location task module schema for the single-database MySQL deployment.
-- This script is idempotent and never creates or references another schema.

CREATE TABLE IF NOT EXISTS `oort_user_opinion` (
  `id` char(36) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '本地用户ID',
  `content` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '常用语内容',
  `is_open` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0个人 1公开',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_oort_user_opinion_user_id` (`user_id`),
  KEY `idx_oort_user_opinion_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户常用语';

CREATE TABLE IF NOT EXISTS `ap_definition_app_group_config_v2` (
  `id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `app_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用ID',
  `group_uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组ID',
  `uid_path` char(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识路径',
  `data` json DEFAULT NULL COMMENT '数据',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_id_uid` (`app_id`,`group_uid`),
  KEY `idx_ap_definition_app_group_config_v2_group_uid` (`group_uid`),
  KEY `idx_ap_definition_app_group_config_v2_uid_path` (`uid_path`),
  KEY `idx_ap_definition_app_group_config_v2_created_at` (`created_at`),
  KEY `idx_ap_definition_app_group_config_v2_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `ap_definition_table_group_app_v2` (
  `id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `app_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用ID',
  `group_uid` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '分组ID',
  `group_type` tinyint NOT NULL DEFAULT '0' COMMENT '分组类型：1区域，2分组，3标签',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '租户ID',
  `uid_path` char(255) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '分组路径',
  `authorize_at` bigint NOT NULL DEFAULT '0' COMMENT '授权时间戳',
  `data` json DEFAULT NULL COMMENT '数据',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_id_uid` (`app_id`,`group_uid`,`tenant_id`) USING BTREE,
  KEY `idx_ap_definition_table_group_app_v2_tenant_id` (`tenant_id`),
  KEY `idx_ap_definition_table_group_app_v2_authorize_at` (`authorize_at`),
  KEY `idx_ap_definition_table_group_app_v2_created_at` (`created_at`),
  KEY `idx_ap_definition_table_group_app_v2_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用关联分组表';

CREATE TABLE IF NOT EXISTS `oort_definition_app_group` (
  `uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
  `app_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '应用ID',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '租户ID',
  `group_type` tinyint NOT NULL DEFAULT '0' COMMENT '分组类型：1区域，2分组，3标签',
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `puid` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父级唯一标识',
  `data` json DEFAULT NULL COMMENT '配置数据',
  `uid_path` char(255) COLLATE utf8mb4_general_ci GENERATED ALWAYS AS (json_unquote(json_extract(`data`,_utf8mb4'$.uid_path'))) VIRTUAL NOT NULL COMMENT '标识路径',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`uid`),
  KEY `idx_oort_definition_app_group_app_id` (`app_id`),
  KEY `idx_oort_definition_app_group_group_type` (`group_type`),
  KEY `idx_oort_definition_app_group_puid` (`puid`),
  KEY `idx_oort_definition_app_group_created_at` (`created_at`),
  KEY `idx_oort_definition_app_group_updated_at` (`updated_at`),
  KEY `idx_oort_definition_app_group_tenant_id` (`tenant_id`),
  KEY `idx_oort_definition_app_group_uid_path` (`uid_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_definition_app_group_device` (
  `id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
  `table_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '表ID',
  `device_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_id_device_name` (`uid`,`table_id`,`device_id`),
  KEY `idx_oort_definition_app_group_device_table_id` (`table_id`),
  KEY `idx_oort_definition_app_group_device_device_id` (`device_id`),
  KEY `idx_oort_definition_app_group_device_created_at` (`created_at`),
  KEY `idx_oort_definition_app_group_device_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_definition_table_group_v2` (
  `uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '租户ID',
  `group_type` tinyint NOT NULL DEFAULT '0' COMMENT '分组类型：1区域，2分组，3标签',
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `puid` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父级唯一标识',
  `uid_path` char(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识路径',
  `remark` varchar(255) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`uid`),
  KEY `idx_oort_definition_table_group_v2_tenant_id` (`tenant_id`),
  KEY `idx_oort_definition_table_group_v2_group_type` (`group_type`),
  KEY `idx_oort_definition_table_group_v2_puid` (`puid`),
  KEY `idx_oort_definition_table_group_v2_uid_path` (`uid_path`),
  KEY `idx_oort_definition_table_group_v2_created_at` (`created_at`),
  KEY `idx_oort_definition_table_group_v2_updated_at` (`updated_at`),
  KEY `idx_oort_definition_table_group_v2_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_event` (
  `id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `no` int NOT NULL COMMENT '序号',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户ID',
  `uuid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户UUID',
  `data` json NOT NULL COMMENT '数据存放',
  `status` tinyint NOT NULL DEFAULT '2' COMMENT '事件状态：1已完成，2待处理',
  `item` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '类型',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `client` char(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '登录标记',
  `name` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '标题',
  `mod_type` int NOT NULL DEFAULT '1' COMMENT '事件模块：1事件拍传，2主动安全',
  `mod_status` int NOT NULL DEFAULT '0' COMMENT '主动安全原始状态：0待确认，1真实告警，2维保，3误报',
  `device_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '设备ID',
  `device_name` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '设备名称',
  `device_tag` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '设备标签',
  `deleted_at` bigint unsigned NOT NULL DEFAULT '0' COMMENT '删除时间戳',
  `work_order_status` tinyint NOT NULL DEFAULT '0' COMMENT '转工单状态：0未转，1已转',
  `pic_len` int unsigned GENERATED ALWAYS AS (json_length(json_extract(`data`,_utf8mb4'$.pics'))) VIRTUAL NOT NULL COMMENT '图片数量',
  PRIMARY KEY (`id`,`no`),
  KEY `idx_oort_task_event_tenant_id` (`tenant_id`),
  KEY `idx_oort_task_event_uuid` (`uuid`),
  KEY `idx_oort_task_event_status` (`status`),
  KEY `idx_oort_task_event_item` (`item`),
  KEY `idx_oort_task_event_created_at` (`created_at`),
  KEY `idx_oort_task_event_updated_at` (`updated_at`),
  KEY `idx_oort_task_event_client` (`client`),
  KEY `idx_oort_task_event_mod_type` (`mod_type`),
  KEY `idx_oort_task_event_mod_status` (`mod_status`),
  KEY `idx_oort_task_event_device_id` (`device_id`),
  KEY `idx_oort_task_event_deleted_at` (`deleted_at`),
  KEY `idx_oort_task_event_work_order_status` (`work_order_status`),
  KEY `idx_oort_task_event_pic_len` (`pic_len`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_event_back` (
  `id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户ID',
  `task_event_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件主键ID',
  `uuid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户UUID',
  `data` json NOT NULL COMMENT '数据存放',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_oort_task_event_back_tenant_id` (`tenant_id`),
  KEY `idx_oort_task_event_back_t_event_id` (`task_event_id`),
  KEY `idx_oort_task_event_back_uuid` (`uuid`),
  KEY `idx_oort_task_event_back_created_at` (`created_at`),
  KEY `idx_oort_task_event_back_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_event_group` (
  `uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户ID',
  `group_type` tinyint NOT NULL DEFAULT '1' COMMENT '分组类型：1区域，2分组，3标签',
  `mod_type` tinyint NOT NULL DEFAULT '1' COMMENT '事件模块：1事件拍传，2主动安全',
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `puid` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父级唯一标识',
  `uid_path` char(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组UID路径',
  `data` json DEFAULT NULL COMMENT '配置数据',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`uid`),
  KEY `idx_oort_task_event_group_tenant_id` (`tenant_id`),
  KEY `idx_oort_task_event_group_group_type` (`group_type`),
  KEY `idx_oort_task_event_group_mod_type` (`mod_type`),
  KEY `idx_oort_task_event_group_name` (`name`),
  KEY `uid_path` (`uid_path`),
  KEY `idx_oort_task_event_group_created_at` (`created_at`),
  KEY `idx_oort_task_event_group_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_event_item` (
  `uid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
  `item` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `mod_type` tinyint NOT NULL DEFAULT '1' COMMENT '事件模块：1事件拍传，2主动安全',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '租户ID',
  `data` json DEFAULT NULL COMMENT '配置数据',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `item` (`item`,`mod_type`,`tenant_id`),
  KEY `idx_oort_task_event_item_tenant_id` (`tenant_id`),
  KEY `idx_oort_task_event_item_created_at` (`created_at`),
  KEY `idx_oort_task_event_item_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_event_user` (
  `task_event_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件主键ID',
  `tenant_id` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户ID',
  `uuid` char(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户UUID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `devicde_id` char(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '设备ID',
  `describe` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
  `u_type` tinyint NOT NULL DEFAULT '1' COMMENT '执行对象类型：1用户，2车辆',
  PRIMARY KEY (`task_event_id`,`uuid`),
  KEY `idx_oort_task_event_user_updated_at` (`updated_at`),
  KEY `idx_oort_task_event_user_tenant_id` (`tenant_id`),
  KEY `idx_oort_task_event_user_uuid` (`uuid`),
  KEY `idx_oort_task_event_user_created_at` (`created_at`),
  KEY `idx_oort_task_event_user_device_id` (`devicde_id`),
  KEY `idx_oort_task_event_user_u_type` (`u_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `oort_task_setting` (
  `key` varchar(191) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `val` json DEFAULT NULL COMMENT '配置值',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件模块配置表';
