# 标签管理 API 文档

## 概述

标签管理API提供了完整的标签管理功能，支持树形结构的标签分类，包括自有标签和公共标签两大类，每个大类下可以创建父级标签和子级标签。

## 数据库设计

### 标签管理表 (tag_management)

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | bigint | 主键ID |
| tag_name | varchar(100) | 标签名称 |
| tag_type | varchar(20) | 标签类型(own-自有, public-公共) |
| parent_id | bigint | 父级标签ID |
| level | int | 层级(0-类型级, 1-父级标签, 2-子级标签) |
| sort_order | int | 排序序号 |
| tag_color | varchar(7) | 标签颜色(十六进制) |
| tag_icon | varchar(100) | 标签图标 |
| description | text | 标签描述 |
| is_active | tinyint(1) | 是否启用 |
| usage_count | int | 使用次数 |
| created_by | varchar(50) | 创建人 |
| updated_by | varchar(50) | 更新人 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| deleted | tinyint(1) | 是否删除 |

### 设备标签关联表 (vls_device_tag_relation)

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | bigint | 主键ID |
| device_id | bigint | 设备ID |
| tag_id | bigint | 标签ID |
| created_by | varchar(50) | 创建人 |
| create_time | datetime | 创建时间 |

## 标签管理 API

### 1. 获取标签树形结构

```http
GET /api/tag-management/tree
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "tagName": "自有标签",
      "tagType": "own",
      "level": 0,
      "tagColor": "#1890ff",
      "children": [
        {
          "id": 3,
          "tagName": "设备类型",
          "tagType": "own",
          "parentId": 1,
          "level": 1,
          "tagColor": "#ff6b6b",
          "children": [
            {
              "id": 7,
              "tagName": "球机监控",
              "tagType": "own",
              "parentId": 3,
              "level": 2,
              "tagColor": "#ff6b6b",
              "usageCount": 5
            }
          ]
        }
      ]
    }
  ]
}
```

### 2. 根据类型获取标签树

```http
GET /api/tag-management/tree/{tagType}
```

**参数:**
- `tagType`: 标签类型 (own-自有, public-公共)

### 3. 创建标签

```http
POST /api/tag-management
```

**请求体:**
```json
{
  "tagName": "新标签",
  "tagType": "own",
  "parentId": 3,
  "tagColor": "#52c41a",
  "tagIcon": "tag",
  "description": "标签描述",
  "createdBy": "admin"
}
```

### 4. 更新标签

```http
PUT /api/tag-management/{id}
```

**请求体:**
```json
{
  "tagName": "更新后的标签名",
  "tagColor": "#faad14",
  "description": "更新后的描述",
  "updatedBy": "admin"
}
```

### 5. 删除标签

```http
DELETE /api/tag-management/{id}
```

**说明:** 删除标签会级联删除其所有子标签和相关的设备关联

### 6. 批量删除标签

```http
DELETE /api/tag-management/batch
```

**请求体:**
```json
[1, 2, 3]
```

### 7. 移动标签

```http
PUT /api/tag-management/{id}/move?targetParentId=5&targetPosition=2
```

**参数:**
- `targetParentId`: 目标父级ID
- `targetPosition`: 目标位置

### 8. 启用/禁用标签

```http
PUT /api/tag-management/{id}/toggle-status?isActive=true
```

**参数:**
- `isActive`: 是否启用

### 9. 获取标签使用统计

```http
GET /api/tag-management/{id}/stats
```

### 10. 检查标签名称是否重复

```http
GET /api/tag-management/check-name?tagName=设备类型&parentId=1&excludeId=3
```

**参数:**
- `tagName`: 标签名称
- `parentId`: 父级ID (可选)
- `excludeId`: 排除的ID (可选，用于编辑时验证)

## 设备标签关联 API

### 1. 获取设备的所有标签

```http
GET /api/device-tag-relation/device/{deviceId}/tags
```

### 2. 获取标签关联的所有设备

```http
GET /api/device-tag-relation/tag/{tagId}/devices
```

### 3. 为设备添加标签

```http
POST /api/device-tag-relation/device/{deviceId}/tag/{tagId}?createdBy=admin
```

### 4. 批量为设备添加标签

```http
POST /api/device-tag-relation/device/{deviceId}/tags?createdBy=admin
```

**请求体:**
```json
[1, 2, 3]
```

### 5. 移除设备标签

```http
DELETE /api/device-tag-relation/device/{deviceId}/tag/{tagId}
```

### 6. 批量移除设备标签

```http
DELETE /api/device-tag-relation/device/{deviceId}/tags
```

**请求体:**
```json
[1, 2, 3]
```

### 7. 更新设备的所有标签

```http
PUT /api/device-tag-relation/device/{deviceId}/tags?createdBy=admin
```

**请求体:**
```json
[1, 2, 3]
```

### 8. 删除设备的所有标签关联

```http
DELETE /api/device-tag-relation/device/{deviceId}/all-tags
```

### 9. 删除标签的所有设备关联

```http
DELETE /api/device-tag-relation/tag/{tagId}/all-devices
```

### 10. 检查设备标签关联是否存在

```http
GET /api/device-tag-relation/check-relation?deviceId=1&tagId=2
```

## 使用示例

### 1. 创建标签层级结构

```bash
# 1. 创建父级标签
curl -X POST "http://localhost:8080/api/tag-management" \
  -H "Content-Type: application/json" \
  -d '{
    "tagName": "设备类型",
    "tagType": "own",
    "parentId": 1,
    "tagColor": "#ff6b6b",
    "description": "设备类型分类标签",
    "createdBy": "admin"
  }'

# 2. 创建子标签
curl -X POST "http://localhost:8080/api/tag-management" \
  -H "Content-Type: application/json" \
  -d '{
    "tagName": "球机监控",
    "tagType": "own",
    "parentId": 3,
    "tagColor": "#ff6b6b",
    "description": "球形摄像机监控设备",
    "createdBy": "admin"
  }'
```

### 2. 为设备分配标签

```bash
# 为设备ID为1的设备添加标签ID为7的标签
curl -X POST "http://localhost:8080/api/device-tag-relation/device/1/tag/7?createdBy=admin"

# 批量为设备添加多个标签
curl -X POST "http://localhost:8080/api/device-tag-relation/device/1/tags?createdBy=admin" \
  -H "Content-Type: application/json" \
  -d '[7, 11, 15]'
```

### 3. 查询设备标签

```bash
# 获取设备的所有标签
curl "http://localhost:8080/api/device-tag-relation/device/1/tags"

# 获取标签关联的所有设备
curl "http://localhost:8080/api/device-tag-relation/tag/7/devices"
```

## 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |

常见错误信息:
- "标签名称已存在" - 同级别下存在相同名称的标签
- "标签不存在" - 指定的标签ID不存在
- "不能删除根级标签分类" - 尝试删除自有标签或公共标签根节点
- "设备标签关联已存在" - 尝试添加已存在的设备标签关联

## 数据初始化

执行以下SQL脚本初始化标签数据:

1. `tag_management_schema.sql` - 创建表结构
2. `tag_sample_data.sql` - 插入示例数据

初始化后将包含完整的标签层级结构，包括设备类型、设备位置、设备状态、业务场景等分类标签。 