-- 标注标签表
CREATE TABLE IF NOT EXISTS `annotation_label` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `annotation_id` bigint NOT NULL COMMENT '关联的标注项目ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `color` varchar(20) NOT NULL COMMENT '标签颜色(十六进制)',
  `description` text COMMENT '标签描述',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `usage_count` int DEFAULT 0 COMMENT '使用次数统计',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_annotation_label_annotation_id` (`annotation_id`),
  KEY `idx_annotation_label_name` (`name`),
  KEY `idx_annotation_label_deleted` (`deleted`),
  KEY `idx_annotation_label_sort` (`annotation_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注标签表';

-- 添加外键约束（可选）
-- ALTER TABLE `annotation_label` ADD CONSTRAINT `fk_annotation_label_annotation_id` 
-- FOREIGN KEY (`annotation_id`) REFERENCES `algorithm_annotation` (`id`) ON DELETE CASCADE;

-- 插入测试数据
INSERT INTO `annotation_label` (`annotation_id`, `name`, `color`, `description`, `sort_order`, `usage_count`) VALUES
(1, '人员', '#FF5722', '检测图片中的人员', 1, 0),
(1, '车辆', '#2196F3', '检测图片中的车辆', 2, 0),
(1, '异常行为', '#FF9800', '检测异常行为', 3, 0),
(2, '合格品', '#4CAF50', '质量合格的工件', 1, 0),
(2, '不合格品', '#F44336', '质量不合格的工件', 2, 0);
