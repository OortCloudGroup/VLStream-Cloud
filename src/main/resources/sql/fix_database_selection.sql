-- 修复数据库选择问题
-- 确保选择正确的数据库

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 选择数据库
USE vlstream;

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 验证数据库选择
SELECT DATABASE() as current_database;

-- 显示当前数据库中的表
SHOW TABLES; 