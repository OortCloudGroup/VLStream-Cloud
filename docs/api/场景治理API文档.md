# 场景治理API接口文档

## 概述

场景治理模块提供完整的CRUD操作和业务功能，支持前端页面的所有功能需求。

## 基础信息

- **基础路径**: `/scene-governance`
- **数据格式**: JSON
- **认证方式**: Token认证

## API接口列表

### 1. 分页查询场景治理信息

**接口地址**: `GET /scene-governance/page`

**请求参数**:
```
current: 当前页，默认1
size: 每页大小，默认10
name: 场景名称（可选）
status: 场景状态（可选，enabled/disabled）
startDate: 开始日期（可选，格式：YYYY-MM-DD）
endDate: 结束日期（可选，格式：YYYY-MM-DD）
```

**请求示例**:
```
GET /scene-governance/page?current=1&size=10&name=场景1&startDate=2024-01-01&endDate=2024-12-31
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "场景1",
        "description": "每周：星期一",
        "devices": "-",
        "rules": "-",
        "status": "enabled",
        "executeType": "weekly",
        "selectedDays": "[\"monday\"]",
        "intervalNum": 1,
        "algorithm": "",
        "location": "",
        "cameras": "",
        "startTime": null,
        "endTime": null,
        "createdAt": "2024-08-19T10:18:00",
        "updatedAt": "2024-08-19T10:18:00",
        "createdBy": null,
        "updatedBy": null
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  },
  "timestamp": 1641234567890
}
```

### 2. 根据ID查询场景详情

**接口地址**: `GET /scene-governance/{id}`

**路径参数**:
- `id`: 场景ID

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "场景1",
    "description": "每周：星期一",
    "devices": "-",
    "rules": "-",
    "status": "enabled",
    "executeType": "weekly",
    "selectedDays": "[\"monday\"]",
    "intervalNum": 1,
    "algorithm": "",
    "location": "",
    "cameras": "",
    "startTime": null,
    "endTime": null,
    "createdAt": "2024-08-19T10:18:00",
    "updatedAt": "2024-08-19T10:18:00",
    "createdBy": null,
    "updatedBy": null
  },
  "timestamp": 1641234567890
}
```

### 3. 新增场景治理

**接口地址**: `POST /scene-governance`

**请求体**:
```json
{
  "name": "新场景",
  "description": "场景描述",
  "executeType": "weekly",
  "selectedDays": "[\"monday\", \"wednesday\"]",
  "intervalNum": 1,
  "algorithm": "AI算法01",
  "location": "区域A",
  "cameras": "摄像头01",
  "startTime": "2024-01-01T08:00:00",
  "endTime": "2024-12-31T18:00:00"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "新增成功",
  "data": null,
  "timestamp": 1641234567890
}
```

### 4. 更新场景治理

**接口地址**: `PUT /scene-governance/{id}`

**路径参数**:
- `id`: 场景ID

**请求体**: 同新增接口

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null,
  "timestamp": 1641234567890
}
```

### 5. 删除场景治理

**接口地址**: `DELETE /scene-governance/{id}`

**路径参数**:
- `id`: 场景ID

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "timestamp": 1641234567890
}
```

### 6. 批量删除场景治理

**接口地址**: `DELETE /scene-governance/batch`

**请求体**:
```json
[1, 2, 3, 4]
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": null,
  "timestamp": 1641234567890
}
```

### 7. 更新场景状态

**接口地址**: `PUT /scene-governance/{id}/status/{status}`

**路径参数**:
- `id`: 场景ID
- `status`: 状态值（enabled/disabled）

**响应示例**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null,
  "timestamp": 1641234567890
}
```

### 8. 批量更新场景状态

**接口地址**: `PUT /scene-governance/status/{status}`

**路径参数**:
- `status`: 状态值（enabled/disabled）

**请求体**:
```json
[1, 2, 3, 4]
```

### 9. 启用场景治理

**接口地址**: `PUT /scene-governance/{id}/enable`

### 10. 禁用场景治理

**接口地址**: `PUT /scene-governance/{id}/disable`

### 11. 批量启用场景治理

**接口地址**: `PUT /scene-governance/batch/enable`

### 12. 批量禁用场景治理

**接口地址**: `PUT /scene-governance/batch/disable`

### 13. 根据状态查询场景列表

**接口地址**: `GET /scene-governance/status/{status}`

**路径参数**:
- `status`: 状态值（enabled/disabled）

### 14. 根据执行类型查询场景列表

**接口地址**: `GET /scene-governance/execute-type/{executeType}`

**路径参数**:
- `executeType`: 执行类型（daily/alternate/weekly/monthly）

### 15. 获取场景治理统计信息

**接口地址**: `GET /scene-governance/statistics`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 10,
    "enabled": 7,
    "disabled": 3,
    "statusStatistics": [
      {"status": "enabled", "count": 7},
      {"status": "disabled", "count": 3}
    ],
    "executeTypeStatistics": [
      {"type": "weekly", "count": 5},
      {"type": "daily", "count": 3},
      {"type": "monthly", "count": 2}
    ]
  },
  "timestamp": 1641234567890
}
```

### 16. 获取所有执行类型列表

**接口地址**: `GET /scene-governance/execute-types`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": ["daily", "alternate", "weekly", "monthly"],
  "timestamp": 1641234567890
}
```

### 17. 执行场景治理

**接口地址**: `POST /scene-governance/{id}/execute`

**响应示例**:
```json
{
  "code": 200,
  "message": "执行成功",
  "data": {
    "success": true,
    "message": "场景治理执行成功",
    "executeTime": "2024-01-01T12:00:00"
  },
  "timestamp": 1641234567890
}
```

### 18. 验证场景治理配置

**接口地址**: `POST /scene-governance/validate`

**请求体**: 场景治理对象

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "valid": true,
    "errors": []
  },
  "timestamp": 1641234567890
}
```

### 19. 导出场景治理信息

**接口地址**: `GET /scene-governance/export`

**请求参数**:
```
sceneIds: 场景ID列表（可选，为空时导出所有）
```

### 20. 批量导入场景治理

**接口地址**: `POST /scene-governance/import`

**请求体**: 场景治理对象数组

**响应示例**:
```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failureCount": 2,
    "successList": ["场景1", "场景2", "..."],
    "failureList": ["场景9: 名称已存在", "场景10: 数据验证失败"]
  },
  "timestamp": 1641234567890
}
```

### 21. 获取场景治理执行历史

**接口地址**: `GET /scene-governance/{id}/execute-history`

### 22. 复制场景治理

**接口地址**: `POST /scene-governance/{id}/copy`

**请求参数**:
- `name`: 新场景名称

### 23. 检查场景名称是否存在

**接口地址**: `GET /scene-governance/check-name`

**请求参数**:
- `name`: 场景名称
- `id`: 场景ID（编辑时排除自己，可选）

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": false,
  "timestamp": 1641234567890
}
```

## 前端集成说明

### 1. 列表页面集成

```javascript
// 分页查询
const getSceneGovernanceList = async (params) => {
  const response = await request.get('/scene-governance/page', { params });
  return response.data;
};

// 搜索功能
const searchSceneGovernances = async (name, status, page = 1, size = 10) => {
  const params = { current: page, size, name, status };
  return await getSceneGovernanceList(params);
};
```

### 2. 新增功能集成

```javascript
// 新增场景
const addSceneGovernance = async (sceneData) => {
  const response = await request.post('/scene-governance', sceneData);
  return response.data;
};

// 前端新增弹窗使用
const handleAdd = async () => {
  const formData = {
    name: form.name,
    description: form.description
  };
  
  const result = await addSceneGovernance(formData);
  if (result.code === 200) {
    ElMessage.success('新增成功');
    // 刷新列表
    await loadList();
  } else {
    ElMessage.error(result.message);
  }
};
```

### 3. 编辑功能集成

```javascript
// 获取场景详情
const getSceneGovernanceDetail = async (id) => {
  const response = await request.get(`/scene-governance/${id}`);
  return response.data;
};

// 更新场景
const updateSceneGovernance = async (id, sceneData) => {
  const response = await request.put(`/scene-governance/${id}`, sceneData);
  return response.data;
};
```

### 4. 删除功能集成

```javascript
// 单个删除
const deleteSceneGovernance = async (id) => {
  const response = await request.delete(`/scene-governance/${id}`);
  return response.data;
};

// 批量删除
const batchDeleteSceneGovernances = async (ids) => {
  const response = await request.delete('/scene-governance/batch', { data: ids });
  return response.data;
};
```

### 5. 状态切换集成

```javascript
// 更新状态
const updateSceneGovernanceStatus = async (id, status) => {
  const response = await request.put(`/scene-governance/${id}/status/${status}`);
  return response.data;
};

// 前端状态切换
const handleStatusChange = async (row) => {
  const newStatus = row.status === 'enabled' ? 'disabled' : 'enabled';
  
  try {
    await updateSceneGovernanceStatus(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(`场景已${newStatus === 'enabled' ? '启用' : '禁用'}`);
  } catch (error) {
    ElMessage.error('状态更新失败');
    // 恢复原状态
    row.status = row.status === 'enabled' ? 'disabled' : 'enabled';
  }
};
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 数据字段说明

### SceneGovernance 对象

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 否 | 主键ID |
| name | String | 是 | 场景名称 |
| description | String | 否 | 场景描述/备注 |
| devices | String | 否 | 关联设备 |
| rules | String | 否 | 治理规则 |
| status | String | 否 | 状态（enabled/disabled） |
| executeType | String | 否 | 执行类型（daily/alternate/weekly/monthly） |
| selectedDays | String | 否 | 选择的天数（JSON数组字符串） |
| intervalNum | Integer | 否 | 间隔数量 |
| algorithm | String | 否 | AI算法 |
| location | String | 否 | 区划地点 |
| cameras | String | 否 | 摄像头 |
| startTime | LocalDateTime | 否 | 开始时间 |
| endTime | LocalDateTime | 否 | 结束时间 |
| createdAt | LocalDateTime | 否 | 创建时间 |
| updatedAt | LocalDateTime | 否 | 更新时间 |
| createdBy | String | 否 | 创建人 |
| updatedBy | String | 否 | 更新人 |

## 注意事项

1. 所有时间字段使用ISO 8601格式：`yyyy-MM-ddTHH:mm:ss`
2. selectedDays字段存储JSON数组字符串，如：`["monday", "wednesday"]`
3. 删除操作为软删除，不会物理删除数据
4. 场景名称在同一系统中必须唯一
5. 状态字段只允许 `enabled` 或 `disabled` 两个值
6. 执行类型字段只允许 `daily`、`alternate`、`weekly`、`monthly` 四个值 