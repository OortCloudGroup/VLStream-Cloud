-- 创建远程服务器配置表
CREATE TABLE IF NOT EXISTS `remote_servers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `server_name` varchar(100) NOT NULL COMMENT '服务器名称',
  `server_ip` varchar(50) NOT NULL COMMENT '服务器IP地址',
  `server_port` int(11) NOT NULL DEFAULT '22' COMMENT 'SSH端口',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码(加密)',
  `conda_env` varchar(50) DEFAULT 'yolo8' COMMENT 'Conda环境名称',
  `work_dir` varchar(255) DEFAULT '/data/work/ultralytics_yolov8-main' COMMENT '工作目录',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(0:禁用 1:启用)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_server_ip` (`server_ip`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='远程服务器配置表';

-- 插入默认服务器配置
INSERT INTO `remote_servers` (`server_name`, `server_ip`, `server_port`, `username`, `password`, `conda_env`, `work_dir`, `status`)
VALUES ('YOLOv8训练服务器', '192.168.88.173', 22, 'oort', 'MTIzNDU2Nzg=', 'yolo8', '/data/work/ultralytics_yolov8-main', 1)
ON DUPLICATE KEY UPDATE
`server_name` = VALUES(`server_name`),
`server_ip` = VALUES(`server_ip`),
`server_port` = VALUES(`server_port`),
`username` = VALUES(`username`),
`password` = VALUES(`password`),
`conda_env` = VALUES(`conda_env`),
`work_dir` = VALUES(`work_dir`),
`status` = VALUES(`status`);