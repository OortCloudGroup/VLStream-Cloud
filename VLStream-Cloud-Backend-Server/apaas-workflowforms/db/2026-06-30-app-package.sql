-- 应用包名字段增量 SQL
-- 为 workorder_app、wf_app 两张表增加 app_package 列
-- 执行日期：2026-06-30

ALTER TABLE `workorder_app` ADD COLUMN `app_package` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用包名' AFTER `images`;

ALTER TABLE `wf_app` ADD COLUMN `app_package` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用包名' AFTER `images`;
