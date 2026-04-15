-- 标注实例表
CREATE TABLE IF NOT EXISTS `annotation_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `annotation_id` bigint NOT NULL COMMENT '关联的标注项目ID',
  `label_id` bigint NOT NULL COMMENT '标签ID',
  `image_path` varchar(500) NOT NULL COMMENT '图片路径',
  `image_name` varchar(200) NOT NULL COMMENT '图片名称',
  `annotation_type` varchar(20) NOT NULL COMMENT '标注类型：rect-矩形,circle-圆形,polygon-多边形',
  `annotation_data` text NOT NULL COMMENT '标注坐标数据(JSON格式)',
  `confidence` decimal(5,4) DEFAULT 1.0000 COMMENT '置信度',
  `verified` tinyint(1) DEFAULT 0 COMMENT '是否已验证',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_annotation_instance_annotation_id` (`annotation_id`),
  KEY `idx_annotation_instance_label_id` (`label_id`),
  KEY `idx_annotation_instance_image` (`annotation_id`, `image_name`),
  KEY `idx_annotation_instance_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注实例表';

-- 添加外键约束（可选）
-- ALTER TABLE `annotation_instance` ADD CONSTRAINT `fk_annotation_instance_annotation_id` 
-- FOREIGN KEY (`annotation_id`) REFERENCES `algorithm_annotation` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `annotation_instance` ADD CONSTRAINT `fk_annotation_instance_label_id` 
-- FOREIGN KEY (`label_id`) REFERENCES `annotation_label` (`id`) ON DELETE CASCADE;
