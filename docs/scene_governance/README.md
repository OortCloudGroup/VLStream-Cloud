# 场景治理数据表设计说明

## 概述

本文档描述了场景治理功能的数据表结构设计，基于前端页面的字段需求创建了 `scene_governance` 表。

## 表结构说明

### 主表：scene_governance

| 字段名 | 类型 | 默认值 | 是否必填 | 说明 |
|--------|------|--------|----------|------|
| id | bigint(20) | - | 是 | 主键ID，自增 |
| name | varchar(100) | - | 是 | 场景名称 |
| description | text | NULL | 否 | 场景描述/备注 |
| devices | varchar(500) | NULL | 否 | 关联设备 |
| rules | varchar(500) | NULL | 否 | 治理规则 |
| status | varchar(20) | 'enabled' | 是 | 状态：enabled-启用, disabled-禁用 |
| execute_type | varchar(20) | 'weekly' | 是 | 执行类型：daily-每天, alternate-隔天, weekly-每周, monthly-每月 |
| selected_days | json | NULL | 否 | 选择的天数（JSON数组） |
| interval_num | int(11) | 1 | 是 | 间隔数量 |
| algorithm | varchar(200) | NULL | 否 | AI算法 |
| location | varchar(200) | NULL | 否 | 区划地点 |
| cameras | varchar(500) | NULL | 否 | 摄像头 |
| start_time | datetime | NULL | 否 | 开始时间 |
| end_time | datetime | NULL | 否 | 结束时间 |
| created_at | datetime | CURRENT_TIMESTAMP | 是 | 创建时间 |
| updated_at | datetime | CURRENT_TIMESTAMP | 是 | 更新时间 |
| deleted_at | datetime | NULL | 否 | 删除时间（软删除） |
| created_by | varchar(50) | NULL | 否 | 创建人 |
| updated_by | varchar(50) | NULL | 否 | 更新人 |

## 字段映射关系

### 前端列表页字段 → 数据库字段

| 前端字段 | 数据库字段 | 说明 |
|---------|------------|------|
| 序号 | id | 主键ID |
| 场景名称 | name | 场景名称 |
| 场景描述 | description | 场景描述 |
| 关联设备 | devices | 关联设备 |
| 治理规则 | rules | 治理规则 |
| 状态 | status | 状态（enabled/disabled） |
| 创建时间 | created_at | 创建时间 |

### 前端编辑页字段 → 数据库字段

| 前端字段 | 数据库字段 | 说明 |
|---------|------------|------|
| 场景名称 | name | 场景名称 |
| 执行时间 | execute_type | 执行类型 |
| 选择天数 | selected_days | JSON数组存储 |
| 间隔 | interval_num | 间隔数量 |
| AI算法 | algorithm | AI算法 |
| 区划地点 | location | 区划地点 |
| 摄像头 | cameras | 摄像头 |

### 新增弹窗字段 → 数据库字段

| 前端字段 | 数据库字段 | 说明 |
|---------|------------|------|
| 名称 | name | 场景名称 |
| 备注 | description | 场景描述 |

## 索引设计

### 主键索引
- PRIMARY KEY (`id`)

### 常用查询索引
- KEY `idx_name` (`name`) - 按名称查询
- KEY `idx_status` (`status`) - 按状态筛选
- KEY `idx_execute_type` (`execute_type`) - 按执行类型筛选
- KEY `idx_created_at` (`created_at`) - 按创建时间排序
- KEY `idx_deleted_at` (`deleted_at`) - 软删除查询

## 约束条件

### 检查约束
- `chk_status`: 状态只能是 'enabled' 或 'disabled'
- `chk_execute_type`: 执行类型只能是 'daily', 'alternate', 'weekly', 'monthly'
- `chk_interval_num`: 间隔数量必须大于等于1

## JSON字段说明

### selected_days 字段

存储选择的天数，使用JSON数组格式：

```json
// 示例：选择星期一和星期三
["monday", "wednesday"]

// 示例：选择星期五
["friday"]
```

可能的值：
- `sunday` - 星期日
- `monday` - 星期一  
- `tuesday` - 星期二
- `wednesday` - 星期三
- `thursday` - 星期四
- `friday` - 星期五
- `saturday` - 星期六

## 软删除设计

使用 `deleted_at` 字段实现软删除：
- `deleted_at` 为 NULL：记录未删除
- `deleted_at` 不为 NULL：记录已删除

## 视图设计

### v_scene_governance_list 视图

为前端列表页提供便捷的查询视图，包含：
- 所有基础字段
- 状态和执行类型的中文显示
- 自动过滤已删除记录
- 按创建时间倒序排列

## 使用示例

### 查询启用的场景
```sql
SELECT * FROM scene_governance 
WHERE status = 'enabled' AND deleted_at IS NULL;
```

### 查询每周执行的场景
```sql
SELECT * FROM scene_governance 
WHERE execute_type = 'weekly' AND deleted_at IS NULL;
```

### 查询包含特定天数的场景
```sql
SELECT * FROM scene_governance 
WHERE JSON_CONTAINS(selected_days, '"monday"') 
  AND deleted_at IS NULL;
```

### 软删除场景
```sql
UPDATE scene_governance 
SET deleted_at = NOW(), updated_by = '用户名' 
WHERE id = 1;
```

## 注意事项

1. **字符集**: 使用 `utf8mb4` 字符集支持中文和特殊字符
2. **时间处理**: 创建时间和更新时间自动维护
3. **软删除**: 删除操作应更新 `deleted_at` 字段而非物理删除
4. **JSON字段**: 使用MySQL 5.7+的JSON数据类型
5. **索引优化**: 根据实际查询需求可调整索引设计

## 扩展考虑

1. **分表策略**: 如果数据量很大，可考虑按时间分表
2. **缓存策略**: 可为常用查询添加Redis缓存
3. **审计日志**: 可增加操作日志表记录变更历史
4. **权限控制**: 可增加权限相关字段 