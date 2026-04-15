-- 场景治理数据表
DROP TABLE IF EXISTS `scene_governance`;

CREATE TABLE `scene_governance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '场景名称',
  `description` text COMMENT '场景描述/备注',
  `devices` varchar(500) DEFAULT NULL COMMENT '关联设备',
  `rules` varchar(500) DEFAULT NULL COMMENT '治理规则',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态: enabled-启用, disabled-禁用',
  `execute_type` varchar(20) NOT NULL DEFAULT 'weekly' COMMENT '执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月',
  `selected_days` json DEFAULT NULL COMMENT '选择的天数（JSON数组）',
  `interval_num` int(11) NOT NULL DEFAULT 1 COMMENT '间隔数量',
  `algorithm` varchar(200) DEFAULT NULL COMMENT 'AI算法',
  `location` varchar(200) DEFAULT NULL COMMENT '区划地点',
  `cameras` varchar(500) DEFAULT NULL COMMENT '摄像头',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_execute_type` (`execute_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `chk_status` CHECK (`status` IN ('enabled', 'disabled')),
  CONSTRAINT `chk_execute_type` CHECK (`execute_type` IN ('daily', 'alternate', 'weekly', 'monthly')),
  CONSTRAINT `chk_interval_num` CHECK (`interval_num` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景治理表';

-- 插入示例数据
INSERT INTO `scene_governance` (`id`, `name`, `description`, `devices`, `rules`, `status`, `execute_type`, `selected_days`, `interval_num`, `algorithm`, `location`, `cameras`, `created_at`) VALUES
(1, '场景1', '每周：星期一', '-', '-', 'enabled', 'weekly', JSON_ARRAY('monday'), 1, '', '', '', '2024-08-19 10:18:00'),
(2, '告警场景', '每天', '摄像头01', 'AI算法01', 'enabled', 'daily', JSON_ARRAY('monday'), 1, 'AI算法01', '区域A', '摄像头01', '2024-08-19 10:18:00'),
(3, '场景3', '每周：星期三、星期五', '-', '-', 'disabled', 'weekly', JSON_ARRAY('wednesday', 'friday'), 1, '', '', '', '2024-08-19 10:18:00'),
(4, '定时场景', '每月', '-', '-', 'enabled', 'monthly', JSON_ARRAY('monday'), 1, '', '', '', '2024-08-19 10:18:00');

-- 创建相关视图（可选）
CREATE VIEW `v_scene_governance_list` AS
SELECT 
    `id`,
    `name`,
    `description`,
    `devices`,
    `rules`,
    `status`,
    CASE 
        WHEN `status` = 'enabled' THEN '启用'
        WHEN `status` = 'disabled' THEN '禁用'
        ELSE '未知'
    END AS `status_text`,
    `execute_type`,
    CASE 
        WHEN `execute_type` = 'daily' THEN '每天'
        WHEN `execute_type` = 'alternate' THEN '隔天'
        WHEN `execute_type` = 'weekly' THEN '每周'
        WHEN `execute_type` = 'monthly' THEN '每月'
        ELSE '未知'
    END AS `execute_type_text`,
    `selected_days`,
    `interval_num`,
    `algorithm`,
    `location`,
    `cameras`,
    `start_time`,
    `end_time`,
    `created_at`,
    `updated_at`,
    `created_by`,
    `updated_by`
FROM `scene_governance`
WHERE `deleted` = 0
ORDER BY `created_at` DESC;

-- 添加表注释
ALTER TABLE `scene_governance` COMMENT = '场景治理表 - 用于管理AI治理场景的配置信息'; 