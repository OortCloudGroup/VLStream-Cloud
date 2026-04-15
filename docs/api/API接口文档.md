# VLStream Cloud API 接口文档

## 概述

VLStream Cloud 提供了完整的RESTful API接口，用于管理智能视频流系统的各种功能，包括设备管理、算法管理、智能分析、监控告警等。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **版本**: v1.0.0
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

## 响应格式

所有API响应都遵循统一格式：

```json
{
  "code": 200,                    // 响应码
  "message": "操作成功",           // 响应消息
  "data": {},                     // 响应数据
  "timestamp": 1704038400000      // 时间戳
}
```

## 响应码说明

| 响应码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数有误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 设备管理 API

### 1. 分页查询设备信息

**接口**: `GET /device/page`

**描述**: 分页查询设备信息，支持按设备名称、类型、状态等条件筛选

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页 |
| size | Long | 否 | 10 | 每页大小 |
| deviceName | String | 否 | - | 设备名称 |
| deviceType | String | 否 | - | 设备类型 |
| status | Integer | 否 | - | 设备状态(1-在线,0-离线) |
| tags | String | 否 | - | 设备标签 |

**请求示例**:
```
GET /api/device/page?current=1&size=10&deviceName=海康&status=1
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
        "deviceName": "海康云台",
        "deviceId": "65131984",
        "deviceType": "camera",
        "cameraType": "ptz",
        "brand": "海康威视",
        "model": "DS-2DE4A425IW-DE",
        "ipAddress": "192.168.1.100",
        "port": 554,
        "rtspUrl": "rtsp://192.168.1.100:554/stream1",
        "location": "大门入口",
        "resolution": "4MP",
        "frameRate": 25,
        "status": 1,
        "isEnabled": 1,
        "tags": "重要区域,主入口",
        "description": "大门入口监控摄像头",
        "createdTime": "2024-01-15T10:30:00",
        "updatedTime": "2024-01-15T14:20:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "timestamp": 1704038400000
}
```

### 2. 根据ID查询设备信息

**接口**: `GET /device/{id}`

**描述**: 根据设备ID查询设备详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求示例**:
```
GET /api/device/1
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "deviceName": "海康云台",
    "deviceId": "65131984",
    "deviceType": "camera",
    "cameraType": "ptz",
    "brand": "海康威视",
    "model": "DS-2DE4A425IW-DE",
    "ipAddress": "192.168.1.100",
    "port": 554,
    "username": "admin",
    "password": "encrypted_password",
    "rtspUrl": "rtsp://192.168.1.100:554/stream1",
    "rtmpUrl": "rtmp://192.168.1.100:1935/live/stream1",
    "location": "大门入口",
    "latitude": 39.908823,
    "longitude": 116.397470,
    "resolution": "4MP",
    "frameRate": 25,
    "status": 1,
    "isEnabled": 1,
    "tags": "重要区域,主入口",
    "description": "大门入口监控摄像头",
    "createdTime": "2024-01-15T10:30:00",
    "updatedTime": "2024-01-15T14:20:00"
  },
  "timestamp": 1704038400000
}
```

### 3. 新增设备信息

**接口**: `POST /device`

**描述**: 新增设备信息

**请求体**:
```json
{
  "deviceName": "新增摄像头",
  "deviceId": "CAM001",
  "deviceType": "camera",
  "cameraType": "gun",
  "brand": "海康威视",
  "model": "DS-2CD2347G1",
  "ipAddress": "192.168.1.101",
  "port": 554,
  "username": "admin",
  "password": "password123",
  "rtspUrl": "rtsp://192.168.1.101:554/stream1",
  "location": "停车场入口",
  "latitude": 39.908823,
  "longitude": 116.397470,
  "resolution": "4MP",
  "frameRate": 25,
  "tags": "停车场,入口",
  "description": "停车场入口监控"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "新增成功",
  "data": "新增成功",
  "timestamp": 1704038400000
}
```

### 4. 更新设备信息

**接口**: `PUT /device`

**描述**: 更新设备信息

**请求体**:
```json
{
  "id": 1,
  "deviceName": "更新后的设备名称",
  "deviceId": "65131984",
  "deviceType": "camera",
  "cameraType": "ptz",
  "brand": "海康威视",
  "model": "DS-2DE4A425IW-DE",
  "ipAddress": "192.168.1.100",
  "port": 554,
  "username": "admin",
  "password": "new_password",
  "rtspUrl": "rtsp://192.168.1.100:554/stream1",
  "location": "大门入口(更新)",
  "resolution": "4MP",
  "frameRate": 30,
  "tags": "重要区域,主入口,已更新",
  "description": "更新后的设备描述"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": "更新成功",
  "timestamp": 1704038400000
}
```

### 5. 删除设备信息

**接口**: `DELETE /device/{id}`

**描述**: 删除指定ID的设备信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求示例**:
```
DELETE /api/device/1
```

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": "删除成功",
  "timestamp": 1704038400000
}
```

### 6. 批量删除设备信息

**接口**: `DELETE /device/batch`

**描述**: 批量删除设备信息

**请求体**:
```json
[1, 2, 3, 4, 5]
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": "批量删除成功",
  "timestamp": 1704038400000
}
```

### 7. 更新设备状态

**接口**: `PUT /device/{id}/status/{status}`

**描述**: 更新设备状态

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |
| status | Integer | 是 | 设备状态(1-在线,0-离线) |

**请求示例**:
```
PUT /api/device/1/status/1
```

**响应示例**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": "状态更新成功",
  "timestamp": 1704038400000
}
```

### 8. 测试设备连接

**接口**: `POST /device/{id}/test`

**描述**: 测试设备连接状态

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求示例**:
```
POST /api/device/1/test
```

**响应示例**:
```json
{
  "code": 200,
  "message": "设备连接正常",
  "data": "设备连接正常",
  "timestamp": 1704038400000
}
```

### 9. 获取设备统计信息

**接口**: `GET /device/statistics`

**描述**: 获取设备统计信息

**请求示例**:
```
GET /api/device/statistics
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalCount": 100,
    "onlineCount": 85,
    "offlineCount": 15,
    "disabledCount": 5
  },
  "timestamp": 1704038400000
}
```

## 算法管理 API

### 1. 分页查询算法信息

**接口**: `GET /algorithm/page`

**描述**: 分页查询算法信息

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页 |
| size | Long | 否 | 10 | 每页大小 |
| algorithmName | String | 否 | - | 算法名称 |
| algorithmType | String | 否 | - | 算法类型 |
| status | Integer | 否 | - | 算法状态 |

### 2. 新增算法信息

**接口**: `POST /algorithm`

**描述**: 新增算法信息

### 3. 更新算法信息

**接口**: `PUT /algorithm`

**描述**: 更新算法信息

## 智能分析 API

### 1. 提交分析请求

**接口**: `POST /analysis/request`

**描述**: 提交智能分析请求

### 2. 查询分析结果

**接口**: `GET /analysis/result`

**描述**: 查询智能分析结果

## 监控告警 API

### 1. 分页查询告警信息

**接口**: `GET /alarm/page`

**描述**: 分页查询监控告警信息

### 2. 处理告警

**接口**: `PUT /alarm/{id}/handle`

**描述**: 处理告警信息

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 1001 | 参数错误 |
| 1002 | 缺少必要参数 |
| 2001 | 用户不存在 |
| 2004 | 密码错误 |
| 3001 | 设备不存在 |
| 3002 | 设备离线 |
| 4001 | 算法不存在 |
| 4004 | 算法执行失败 |
| 9001 | 系统内部错误 |
| 9002 | 数据库错误 |

## 示例代码

### JavaScript (Axios)

```javascript
// 分页查询设备
async function getDevices(page = 1, size = 10) {
  try {
    const response = await axios.get('/api/device/page', {
      params: { current: page, size: size }
    });
    return response.data;
  } catch (error) {
    console.error('获取设备列表失败:', error);
  }
}

// 新增设备
async function addDevice(deviceData) {
  try {
    const response = await axios.post('/api/device', deviceData);
    return response.data;
  } catch (error) {
    console.error('新增设备失败:', error);
  }
}
```

### Java (RestTemplate)

```java
// 分页查询设备
public Result<IPage<DeviceInfo>> getDevices(int page, int size) {
    String url = "http://localhost:8080/api/device/page?current=" + page + "&size=" + size;
    return restTemplate.getForObject(url, Result.class);
}

// 新增设备
public Result<String> addDevice(DeviceInfo deviceInfo) {
    String url = "http://localhost:8080/api/device";
    return restTemplate.postForObject(url, deviceInfo, Result.class);
}
```

### Python (Requests)

```python
import requests

# 分页查询设备
def get_devices(page=1, size=10):
    url = "http://localhost:8080/api/device/page"
    params = {"current": page, "size": size}
    response = requests.get(url, params=params)
    return response.json()

# 新增设备
def add_device(device_data):
    url = "http://localhost:8080/api/device"
    response = requests.post(url, json=device_data)
    return response.json()
```

## 注意事项

1. **认证授权**: 生产环境中请确保添加适当的认证授权机制
2. **参数验证**: 所有输入参数都会进行严格验证
3. **错误处理**: 请根据响应码和错误消息进行适当的错误处理
4. **频率限制**: 部分接口可能存在调用频率限制
5. **数据格式**: 时间格式统一使用 ISO 8601 标准
6. **字符编码**: 请确保使用 UTF-8 编码 