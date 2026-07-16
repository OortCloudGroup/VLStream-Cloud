# 主动安全硬件设备事件上报接口

本文档适用于硬件设备或设备接入网关上报主动安全事件。

## 1. 接入说明

- 协议：HTTP/HTTPS
- 请求方法：`POST`
- 请求格式：`application/json; charset=UTF-8`
- 鉴权：当前兼容旧 Go 服务，无需登录 Token
- 业务租户：当前为单租户部署，`device_tenant_id` 必须传后端环境变量 `VLS_SINGLE_TENANT_ID` 的值，默认值为 `000000`
- 响应判断：接口通常返回 HTTP 200，调用方必须继续判断响应 JSON 中的 `code`
- 直连地址：`http://{host}:{port}/task/v1/...`
- 网关地址：如果后端挂载在 `/bus/apaas-workflowforms`，则使用 `https://{host}/bus/apaas-workflowforms/task/v1/...`

## 2. 单个事件上报

### 2.1 基本信息

```text
POST /task/v1/event_report_camera
Content-Type: application/json
```

### 2.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 否 | 事件标题，最多 10 个字符；为空时响应中的标题使用 `item` |
| `describe` | string | 条件必填 | 事件描述；当 `item` 为空时作为事件类型使用，此时最多 10 个字符 |
| `item` | string | 条件必填 | 事件类型，最多 10 个字符；为空时自动使用 `describe` |
| `point` | object | 是 | 事件坐标对象 |
| `pics` | string[] | 否 | 图片 URL 列表，服务端会去重并移除空字符串 |
| `video` | string[] | 否 | 视频 URL 列表 |
| `device_id` | string | 是 | 设备唯一 ID；用于匹配主动安全设备分组和自动派单配置 |
| `device_name` | string | 是 | 设备名称，也用于生成事件业务 ID 前缀 |
| `device_tag` | string | 否 | 设备标签 |
| `device_tenant_id` | string | 是 | 单租户 ID，应与 `VLS_SINGLE_TENANT_ID` 一致 |

`point` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `lng` | number | 是 | 经度 |
| `lat` | number | 是 | 纬度 |
| `address` | string | 否 | 地址文本 |
| `coord_system_type` | integer | 否 | 原始坐标系：`1` WGS84、`2` GCJ02、`3` BD09 |
| `lng_change` | number | 否 | 已转换的经度 |
| `lat_change` | number | 否 | 已转换的纬度 |
| `coord_system_type_change` | integer | 否 | 转换后的坐标系：`1` WGS84、`2` GCJ02、`3` BD09 |

硬件端应至少保证 `item` 和 `describe` 其中一个非空。为了确保自动转工单流程可执行，事件还必须携带至少一张有效图片。

### 2.3 请求示例

```json
{
  "name": "未戴安全帽",
  "describe": "检测到人员未戴安全帽",
  "item": "未戴安全帽",
  "point": {
    "lng": 114.24779,
    "lat": 22.71991,
    "address": "深圳市福田区松岭路57号",
    "coord_system_type": 1
  },
  "pics": [
    "https://device.example.com/events/20260715/001.jpg"
  ],
  "video": [
    "https://device.example.com/events/20260715/001.mp4"
  ],
  "device_id": "camera-001",
  "device_name": "东门摄像机",
  "device_tag": "园区东门",
  "device_tenant_id": "000000"
}
```

### 2.4 成功响应

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": "东门摄像机-20260715-1",
    "tenant_id": "000000",
    "uuid": "camera-001",
    "name": "未戴安全帽",
    "describe": "检测到人员未戴安全帽",
    "point": {
      "lng": 114.24779,
      "lat": 22.71991,
      "address": "深圳市福田区松岭路57号",
      "coord_system_type": 1
    },
    "pics": [
      "https://device.example.com/events/20260715/001.jpg"
    ],
    "send_pics": [],
    "video": [
      "https://device.example.com/events/20260715/001.mp4"
    ],
    "finish_at": "2026-07-15 14:30:00",
    "status": 2,
    "item": "未戴安全帽",
    "client": "camera",
    "uuids": [],
    "mod_type": 2,
    "mod_status": 0,
    "device_id": "camera-001",
    "device_name": "东门摄像机",
    "device_tag": "园区东门",
    "work_order_status": 0,
    "created_at": "2026-07-15 14:30:00",
    "updated_at": "2026-07-15 14:30:00",
    "pic_len": 0
  }
}
```

事件保存成功后，自动转工单在后台异步执行，因此本次响应中的 `work_order_status` 可能仍为 `0`。后台匹配到已开启的自动派单配置并成功创建工单后，该字段更新为 `1`。

### 2.5 curl 示例

```bash
curl -X POST "http://127.0.0.1:8080/task/v1/event_report_camera" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"未戴安全帽",
    "describe":"未戴安全帽",
    "item":"未戴安全帽",
    "point":{"lng":114.24779,"lat":22.71991,"coord_system_type":1},
    "pics":["https://device.example.com/events/001.jpg"],
    "video":[],
    "device_id":"camera-001",
    "device_name":"东门摄像机",
    "device_tag":"园区东门",
    "device_tenant_id":"000000"
  }'
```

## 3. 批量事件上报

### 3.1 基本信息

```text
POST /task/v1/event_report_cameras
Content-Type: application/json
```

### 3.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `event_report` | object[] | 是 | 非空事件数组；数组中每项的字段与单个上报完全一致 |

### 3.3 请求示例

```json
{
  "event_report": [
    {
      "name": "未戴安全帽",
      "describe": "未戴安全帽",
      "item": "未戴安全帽",
      "point": {
        "lng": 114.24779,
        "lat": 22.71991,
        "coord_system_type": 1
      },
      "pics": ["https://device.example.com/events/001.jpg"],
      "video": [],
      "device_id": "camera-001",
      "device_name": "东门摄像机",
      "device_tag": "园区东门",
      "device_tenant_id": "000000"
    },
    {
      "name": "人员闯入",
      "describe": "人员闯入",
      "item": "人员闯入",
      "point": {
        "lng": 114.24801,
        "lat": 22.72011,
        "coord_system_type": 1
      },
      "pics": ["https://device.example.com/events/002.jpg"],
      "video": ["https://device.example.com/events/002.mp4"],
      "device_id": "camera-002",
      "device_name": "仓库摄像机",
      "device_tag": "仓库",
      "device_tenant_id": "000000"
    }
  ]
}
```

### 3.4 成功响应

```json
{
  "code": 200,
  "msg": "成功"
}
```

批量接口会先校验全部事件。任意一项格式错误时，整批不写入；全部校验通过后逐条保存。为兼容旧 Go 接口，单条数据库写入失败不会出现在成功响应中，也没有逐条处理结果。因此调用方如需要严格确认每条写入结果，推荐使用单个上报接口。

## 4. 业务错误码

| `code` | 含义 | 常见原因 |
| --- | --- | --- |
| `200` | 成功 | 事件已保存；自动转工单可能仍在异步执行 |
| `4101` | 参数错误 | `point` 缺失、设备字段缺失、事件标题或类型超过 10 个字符、批量数组为空或格式错误 |
| `5003` | 数据库操作失败 | 数据库不可用或写入失败；单个接口会返回，批量接口的单项写入失败为兼容旧接口会跳过 |
| `500` | 服务器内部错误 | 未预期的服务异常 |

错误响应示例：

```json
{
  "code": 4101,
  "msg": "参数错误 device_id、device_name和device_tenant_id不能为空"
}
```

## 5. 自动转工单规则

设备事件成功写入后，Java 服务直接调用本地工单服务，不再通过旧 Go 服务或外部 HTTP 工单接口。

自动转工单同时满足以下条件才执行：

1. 事件至少包含一张有效图片；
2. 主动安全设置中存在与事件类型、设备所属分组/上级分组或全局区域/分组相匹配的配置；
3. 匹配配置的“自动派单”已开启；
4. 已选择有效的流程模型和应用；
5. 本地工单流程启动成功。

关闭“自动派单”时只创建事件，不自动转工单；事件仍可在管理页面手动转工单。

## 6. Swagger 地址与故障说明

- Swagger UI：`http://{host}:{port}/swagger-ui/index.html`
- OpenAPI JSON：`http://{host}:{port}/v3/api-docs`

OpenAPI JSON 的根节点应包含：

```json
{
  "openapi": "3.0.1"
}
```

如果 Swagger UI 报 `The provided definition does not specify a valid version field`，先直接访问 `/v3/api-docs`。若返回的是 `{"code":500,...}` 而不是带 `openapi` 字段的文档，说明运行中的 Java 进程仍使用热更新后的不一致 class。请用 JDK 8 完整构建并重启后端进程，不要只依赖 IDE HotSwap。
