# VLStream IoT MQTT 设备接入协议

## 1. 文档定位

本文是 VLStream 平台与摄像头、边缘计算盒等硬件之间的 MQTT 接口总协议，作为硬件、平台后端和测试团队共同维护的接入依据。

当前已实现模型下发和模型部署回执；摄像头事件上报、媒体签名上传、心跳与在线状态属于预留协议，服务端尚未实现订阅处理。硬件开发时必须以“实现状态”列为准，不能仅根据 Topic 定义判断平台已经可用。

| 能力 | 方向 | 实现状态 |
| --- | --- | --- |
| 模型下发通知 | 平台 → 设备 | 已实现 |
| 模型部署进度与结果 | 设备 → 平台 | 已实现 |
| 摄像头识别事件 | 设备 → 平台 | 规划中 |
| 事件业务回执 | 平台 → 设备 | 规划中 |
| MinIO 媒体签名上传 | 双向 + HTTP PUT | 规划中 |
| 设备心跳、在线和离线状态 | 设备 → 平台 | 规划中 |
| 通用设备级命令 Topic | 平台 → 设备 | 规划中 |

### 1.1 单文档维护规则

硬件接入相关内容只在本文维护，包括 MQTT 通用连接、当前模型下发，以及规划中的
事件、媒体上传、设备状态和通用命令。README 只提供部署入口和本文链接，不复制
消息载荷、状态枚举或处理流程。

## 2. 协议原则

1. MQTT 只传输命令、状态、事件元数据和媒体 URL，不传输图片 Base64、视频或其他大文件二进制。
2. 事件图片和视频使用平台签发的短期 MinIO 上传 URL 上传，MQTT 事件只引用最终对象 URL。
3. MQTT QoS 1 只保证消息至少到达一次，不等于业务处理成功；业务成功必须以业务回执为准。
4. 设备必须为消息和事件生成稳定的幂等编号，重试时不得生成新编号。
5. 所有 JSON 使用 UTF-8，所有时间使用 UTC ISO-8601，例如 `2026-07-23T10:00:00Z`。
6. `deviceId` 必须与平台设备表中的 `DeviceInfo.deviceId` 完全一致。
7. 生产环境必须使用设备级凭据、Topic ACL 和 TLS，禁止所有设备共用超级管理员账号。

## 3. MQTT 连接参数

### 3.1 基础要求

| 参数 | 要求 |
| --- | --- |
| MQTT 版本 | MQTT 3.1.1；后续可兼容 MQTT 5 |
| 生产端口 | 推荐 TLS `8883`；测试环境可使用明文 `1883` |
| Client ID | `vlstream-device-{deviceId}`，同一设备必须稳定且唯一 |
| Keep Alive | 推荐 60 秒 |
| Clean Session | `false`，使用持久会话接收离线期间积压的 QoS 1 消息 |
| QoS | 命令、事件和业务回执均使用 QoS 1 |
| Retained | 命令、事件和回执均为 `false`；设备状态可为 `true` |
| 自动重连 | 必须支持，并采用带上限的指数退避 |
| 最大 JSON | 建议不超过 128 KiB |

同一个 Client ID 同时上线会被 Broker 判定为重复连接，通常表现为两端不断互相踢下线。设备更换后如果继续使用原 `deviceId`，应先停用原硬件。

### 3.2 设备身份和 ACL

推荐每台设备分配独立用户名、密码或客户端证书。设备只能操作自身 Topic：

```text
订阅：oortcloud/v1/devices/{deviceId}/commands/+
订阅：oortcloud/v1/devices/{deviceId}/events/ack
订阅：oortcloud/v1/devices/{deviceId}/media/upload/reply

发布：oortcloud/v1/devices/{deviceId}/events
发布：oortcloud/v1/devices/{deviceId}/state
发布：oortcloud/v1/devices/{deviceId}/telemetry
发布：oortcloud/v1/devices/{deviceId}/commands/+/ack
发布：oortcloud/v1/devices/{deviceId}/media/upload/request
```

### 3.3 项目本地 EMQX 5.4

本地开发和联调统一使用以下 Broker：

| 项目 | 值 |
| --- | --- |
| Docker 镜像 | `192.168.88.150:80/opensource/emqx/emqx:5.4` |
| MQTT 协议 | MQTT 3.1.1 |
| 后端连接地址 | `127.0.0.1:1883` |
| 当前硬件连接地址 | `192.168.88.31:1883` |
| Dashboard | `http://192.168.88.31:18083` |
| 认证 | EMQX 内置数据库用户名密码认证 |
| TLS / WS / WSS | 本地测试关闭 |

账号密码由 `script/docker/.env` 注入，不写入本文。部署人员需要执行：

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
Copy-Item script/docker/.env.example script/docker/.env
docker login 192.168.88.150:80
docker compose --env-file script/docker/.env -f script/docker/docker-compose.yml up -d emqx
pwsh -File script/docker/init-emqx.ps1
```

本地后端与硬件测试客户端使用 `.env` 中相同的测试凭据。该共享账号只适用于内网
联调；正式环境必须改为设备独立凭据、Topic ACL 和 TLS。硬件端的 `MQTT_HOST`
不能填写 `127.0.0.1`，因为那会指向硬件自身。当前硬件测试参数为：

```bash
export VLSTREAM_MQTT_HOST=192.168.88.31
export VLSTREAM_MQTT_PORT=1883
export VLSTREAM_MQTT_USERNAME=replace-with-local-test-user
export VLSTREAM_MQTT_PASSWORD=replace-with-local-test-password
```

部署机器防火墙需要允许硬件所在网段访问 TCP `1883`。TCP `18083` 是管理端口，
不属于硬件协议，应该只向管理网段开放。

### 3.4 遗嘱和在线状态（规划中）

设备连接前推荐设置 Last Will：

```text
Topic: oortcloud/v1/devices/{deviceId}/state
QoS: 1
Retained: true
Payload:
{
  "protocolVersion": "1.0",
  "messageId": "ad92097e-9320-4bcf-8856-cb1550f09649",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:00Z",
  "type": "device.state",
  "payload": {
    "online": false,
    "reason": "mqtt_connection_lost"
  }
}
```

连接成功后设备向同一 Topic 发布 `online=true` 的 retained 消息。服务端在线状态订阅尚未实现。

## 4. IoT Topic

### 4.1 历史 Topic

平台已有的相机显示、OSD、音频和时间策略等扁平 Topic 属于历史接口。本文不定义
其载荷，新硬件项目应逐项确认后再接入，不能根据 Topic 名猜测字段：

```text
oortcloud/vlsCameraDisplaySetting
oortcloud/vlsCameraOsdSetting
oortcloud/vlsAudioAnomalyDetectionSetting
oortcloud/vlsAudioDefenseTimeSetting
oortcloud/vlsAudioLinkageModeSetting
oortcloud/vlsTimeStrategy
oortcloud/vlsRecordEventStrategy
```

### 4.2 版本化设备 Topic（规划中）

| Topic | 方向 | 说明 |
| --- | --- | --- |
| `oortcloud/v1/devices/{deviceId}/events` | 设备 → 平台 | 摄像头识别事件 |
| `oortcloud/v1/devices/{deviceId}/events/ack` | 平台 → 设备 | 事件持久化业务回执 |
| `oortcloud/v1/devices/{deviceId}/media/upload/request` | 设备 → 平台 | 请求 MinIO 短期上传 URL |
| `oortcloud/v1/devices/{deviceId}/media/upload/reply` | 平台 → 设备 | 返回上传 URL、对象 URL 和有效期 |
| `oortcloud/v1/devices/{deviceId}/state` | 设备 → 平台 | 在线、离线和运行状态 |
| `oortcloud/v1/devices/{deviceId}/telemetry` | 设备 → 平台 | 温度、CPU、内存、磁盘等遥测 |
| `oortcloud/v1/devices/{deviceId}/commands/{command}` | 平台 → 设备 | 版本化设备命令 |
| `oortcloud/v1/devices/{deviceId}/commands/{command}/ack` | 设备 → 平台 | 命令业务回执 |

`oortcloud` 是默认前缀，可通过平台配置调整。硬件接入时由部署方提供最终前缀，硬件程序不得在多处写死。

`{command}` 必须是不含 `/` 的单层命令名。设备订阅 `commands/+`，不能订阅
`commands/#`，否则设备可能收到自己发布的 `commands/{command}/ack`。

## 5. 公共消息包

版本化 Topic 统一使用以下消息包：

```json
{
  "protocolVersion": "1.0",
  "messageId": "66ef4a7a-87f6-480e-9794-486309ba6bfd",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:00Z",
  "type": "camera.event",
  "payload": {}
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `protocolVersion` | 是 | 当前固定 `1.0` |
| `messageId` | 是 | UUID；同一消息重试时保持不变 |
| `deviceId` | 是 | 平台设备编号，必须和 Topic 中的设备编号一致 |
| `sentAt` | 是 | 设备发送时间，UTC ISO-8601 |
| `type` | 是 | 消息类型 |
| `payload` | 是 | 业务数据对象 |

平台必须同时校验 MQTT 认证身份、Topic 中的 `deviceId` 和 JSON 中的 `deviceId`。三者不一致时拒绝处理并记录安全日志。

## 6. 模型下发（已实现）

当前模型下发是兼容协议，没有使用第 5 节的版本化公共消息包。

### 6.1 业务链路与模型存储

1. 平台选择某个算法最新的、已完成且包含指定格式产物的训练任务。
2. 平台通过 SSH 在训练服务器执行 `stat` 和 `sha256sum`，取得文件名、大小和 SHA-256。
3. 平台为每台设备创建一条 `vls_model_dispatch_task` 记录。
4. 平台通过 MQTT 发布模型任务。
5. 设备核对 `deviceId`，通过消息中的短期签名 URL 下载模型。
6. 平台使用 SFTP 从训练服务器读取文件，并以 HTTP 流式响应设备。
7. 设备校验文件大小和 SHA-256，验证模型后原子切换。
8. 设备通过 MQTT 回传部署进度或结果。

模型任务只有收到设备 `SUCCESS` 回执后才算部署成功。

训练任务表保存模型产物路径，不保存模型二进制。训练程序完成后，会把 PT、ONNX、
RKNN、INT8-RKNN、OM 等产物写入 GPU 训练服务器磁盘。例如训练任务
`2077359187012198403` 的 PT 文件位于：

```text
/data/work/ultralytics_yolov8-main/datasets/runs/detect/train9/weights/1sad.pt
```

应用服务器不会在下发前永久复制整份模型。设备访问签名 URL 后，应用服务器才通过
SFTP 读取并以 HTTP 流转发。因此训练服务器的 `/data/work` 必须使用持久化磁盘或
宿主机挂载，不能只保存在随容器销毁的临时层。

### 6.2 平台部署配置

必须配置：

```bash
VLSTREAM_MODEL_PUBLIC_BASE_URL=https://device-accessible.example.com
VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET=replace-with-a-long-random-secret
```

可选配置：

```bash
VLSTREAM_MODEL_DOWNLOAD_URL_TTL_SECONDS=1800
VLSTREAM_MODEL_DISPATCH_REPLY_TOPIC=oortcloud/modelDispatch/reply/#
VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID=vls-model-dispatch-backend
```

`VLSTREAM_MODEL_PUBLIC_BASE_URL` 必须是现场设备能够主动访问的地址，不能填写训练
服务器本地文件路径。多实例部署时，每个后端实例必须使用不同的
`VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID`。

首次部署先执行：

```text
db/2026-07-23-model-dispatch.sql
```

### 6.3 管理端下发接口

```http
POST /vlsDeviceInfo/{algorithmId}/algorithms?deviceIds=1,2&modelType=om
```

`deviceIds` 是 `vls_device_info.id` 的逗号串。`modelType` 支持：

- `pt`
- `onnx`
- `rknn`
- `int8-rknn`
- `om`

旧接口仍可使用：

```http
GET /vlsDeviceInfo/dispatchAlgorithms?algorithmId=1&deviceIds=1,2&modelType=om
```

### 6.4 MQTT Topic 与下发消息

```text
平台发布：oortcloud/dispatchAlgorithms
设备发布：oortcloud/modelDispatch/reply/{deviceId}
```

设备需要订阅共享下发 Topic，并只能发布自身 `deviceId` 对应的回执 Topic。收到
消息后必须先比较 `deviceId`；不属于本机的任务立即忽略，不得下载模型。

共享 Topic 会让同一 Broker 上的设备看到其他设备的下发元数据。当前通过短期签名
URL 降低风险；后续迁移到设备级 Topic 后，应移除设备对共享 Topic 的权限。

下发载荷：

```json
{
  "requestId": "4e129f8d-32ee-4d61-889b-6adb58197b87",
  "deviceId": "CAMERA-001",
  "algorithmId": 1,
  "trainingId": 2077359187012198403,
  "modelType": "om",
  "modelUrl": "https://device-accessible.example.com/vlsModelDispatch/public/4e129f8d-32ee-4d61-889b-6adb58197b87/download?expires=1784800000&signature=...",
  "fileName": "1sad.om",
  "fileSize": 12345678,
  "sha256": "64位十六进制SHA-256",
  "expiresAt": "2026-07-23T10:00:00Z",
  "replyTopic": "oortcloud/modelDispatch/reply/CAMERA-001"
}
```

处理相同 `requestId` 时必须保持幂等，不能重复切换模型。

### 6.5 HTTP 下载、校验与激活

设备直接 GET `modelUrl`，不携带平台登录令牌。平台验证 HMAC 签名及到期时间后
返回：

```text
Content-Type: application/octet-stream
Content-Length: <文件字节数>
ETag: "<sha256>"
X-Model-SHA256: <sha256>
Cache-Control: private, no-store
```

设备处理顺序：

1. 发布 `RECEIVED`。
2. 检查 `expiresAt`；URL 已过期时发布 `FAILED`。
3. 发布 `DOWNLOADING`，将模型下载到临时文件。
4. 下载完成后发布 `DOWNLOADED`。
5. 校验实际文件大小等于 `fileSize`，实际 SHA-256 等于 `sha256`。
6. 发布 `VERIFYING`，使用硬件推理运行时完成加载或最小自检。
7. 发布 `DEPLOYING`，在同一文件系统内通过原子重命名切换正式模型。
8. 保留上一版本；新模型加载失败时回滚。
9. 新模型正常运行后发布 `SUCCESS`，任何阶段失败均发布 `FAILED`。

模型下载不能直接覆盖当前运行模型。仅完成 HTTP 下载但没有通过校验和运行时自检，
不能回传 `SUCCESS`。

### 6.6 MQTT 回执

设备向下发消息中的 `replyTopic` 发布：

```json
{
  "requestId": "4e129f8d-32ee-4d61-889b-6adb58197b87",
  "deviceId": "CAMERA-001",
  "status": "VERIFYING",
  "message": "SHA-256 verified"
}
```

允许状态：

- `RECEIVED`
- `DOWNLOADING`
- `DOWNLOADED`
- `VERIFYING`
- `DEPLOYING`
- `SUCCESS`
- `FAILED`

失败时 `message` 应提供可诊断原因，但不能包含设备密码、MQTT 密码、签名密钥或
完整签名 URL。

### 6.7 状态查询

```http
GET /vlsModelDispatch/task/{requestId}
GET /vlsModelDispatch/tasks?deviceId=CAMERA-001&status=FAILED&limit=50
```

### 6.8 硬件参考客户端

参考实现：

```text
sdk/examples/model_dispatch_mqtt_client.py
```

连接参数使用第 3 节的 `VLSTREAM_MQTT_HOST`、`VLSTREAM_MQTT_PORT`、
`VLSTREAM_MQTT_USERNAME` 和 `VLSTREAM_MQTT_PASSWORD`。模型业务参数：

```bash
export VLSTREAM_DEVICE_ID=CAMERA-001
export VLSTREAM_MODEL_DIR=/mnt/models
export VLSTREAM_MODEL_ACTIVATE_COMMAND="/usr/local/bin/activate-model {path}"
python3 sdk/examples/model_dispatch_mqtt_client.py
```

真实硬件需要把激活命令替换为对应芯片的模型加载与热切换接口。

### 6.9 模型下发联调验收

1. 非本机 `deviceId` 的任务会被忽略。
2. 相同 `requestId` 重复投递不会重复切换模型。
3. 下载 URL 过期时回传 `FAILED`。
4. 文件大小或 SHA-256 不一致时不覆盖当前模型，并回传 `FAILED`。
5. 新模型运行时加载失败时能够回滚上一版本。
6. 部署过程中按实际阶段回传状态，不能提前回传 `SUCCESS`。
7. 成功后平台任务最终状态为 `SUCCESS`。

## 7. 摄像头识别事件（规划中）

> [!WARNING]
> 本节是已确认的目标协议，但平台服务端当前尚未订阅事件 Topic。硬件联调开始前，必须由平台版本说明确认“事件 MQTT 接收已实现”。

### 7.1 Topic

```text
上报：oortcloud/v1/devices/{deviceId}/events
回执：oortcloud/v1/devices/{deviceId}/events/ack
QoS：1
Retained：false
```

### 7.2 事件载荷

```json
{
  "protocolVersion": "1.0",
  "messageId": "66ef4a7a-87f6-480e-9794-486309ba6bfd",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:02Z",
  "type": "camera.event",
  "payload": {
    "eventId": "CAMERA-001-20260723-000001",
    "eventCode": "NO_HELMET",
    "eventName": "未戴安全帽",
    "description": "检测到人员未佩戴安全帽",
    "occurredAt": "2026-07-23T10:00:00Z",
    "deviceName": "东门摄像机",
    "deviceTag": "园区东门",
    "point": {
      "lng": 114.24779,
      "lat": 22.71991,
      "address": "深圳市福田区松岭路57号",
      "coordSystemType": 2,
      "lngChange": 114.24779,
      "latChange": 22.71991,
      "coordSystemTypeChange": 2
    },
    "algorithm": {
      "algorithmId": "1",
      "modelVersion": "1.2.0",
      "confidence": 0.94
    },
    "media": [
      {
        "mediaId": "cce3634d-e596-47a8-8624-1d543ee64ce8",
        "type": "image",
        "url": "https://minio.example.com/vlstream/events/CAMERA-001/2026/07/23/1.jpg",
        "contentType": "image/jpeg",
        "fileSize": 248231,
        "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
      }
    ],
    "extension": {}
  }
}
```

### 7.3 字段约束

| 字段 | 必填 | 约束 |
| --- | --- | --- |
| `payload.eventId` | 是 | 设备生成的全局稳定事件编号；重试不得改变 |
| `payload.eventCode` | 是 | 稳定机器码，建议大写字母、数字和下划线 |
| `payload.eventName` | 是 | 展示名称，最多 10 个 Unicode 字符，与现有 HTTP 接口一致 |
| `payload.description` | 否 | 事件描述，不得包含无限增长的日志 |
| `payload.occurredAt` | 是 | 实际识别时间，不使用 MQTT 到达时间代替 |
| `payload.deviceName` | 是 | 设备展示名称 |
| `payload.deviceTag` | 否 | 设备分组标签 |
| `payload.point` | 是 | 必须包含 `lng`、`lat`；可包含 `address`、`coordSystemType`、`lngChange`、`latChange`、`coordSystemTypeChange` |
| `payload.algorithm` | 否 | 算法、模型版本和置信度 |
| `payload.media` | 否 | 已上传完成的媒体对象；不得放 Base64 |
| `payload.extension` | 否 | 厂商扩展字段，不能覆盖标准字段 |

未来服务端接收后会映射到现有 HTTP 事件语义：

| MQTT 字段 | 现有 HTTP 字段 |
| --- | --- |
| `deviceId` | `device_id` |
| `payload.deviceName` | `device_name` |
| `payload.deviceTag` | `device_tag` |
| `payload.eventName` | `item` / `name` |
| `payload.description` | `describe` |
| `payload.point` | `point` |
| 图片类型 `media[].url` | `pics[]` |
| 视频类型 `media[].url` | `video[]` |

### 7.4 事件业务回执

平台完成格式校验和数据库持久化后发布：

```json
{
  "protocolVersion": "1.0",
  "messageId": "43ef3c5c-19c4-4949-a16c-e48cda35ff9c",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:03Z",
  "type": "camera.event.ack",
  "payload": {
    "requestMessageId": "66ef4a7a-87f6-480e-9794-486309ba6bfd",
    "eventId": "CAMERA-001-20260723-000001",
    "status": "ACCEPTED",
    "code": "OK",
    "message": "event persisted",
    "platformEventId": "dongmensexiangji-20260723-1",
    "retryable": false
  }
}
```

业务状态：

| 状态 | 设备行为 |
| --- | --- |
| `ACCEPTED` | 删除本地待上报记录 |
| `DUPLICATE` | 视为成功，删除本地待上报记录 |
| `REJECTED` 且 `retryable=false` | 记录错误并停止自动重试 |
| `REJECTED` 且 `retryable=true` | 按退避策略重试 |

设备收到 MQTT PUBACK 但没有收到业务 ACK 时，不能删除本地事件。

### 7.5 离线缓存与重试

设备必须提供断网缓存：

1. 识别事件先写入本地持久化队列，再尝试 MQTT 发布。
2. 媒体文件上传成功后记录对象 URL，再发布事件。
3. QoS 1 发布后等待业务 ACK。
4. 超时后使用相同 `messageId` 和 `eventId` 重试。
5. 推荐退避间隔为 5、15、30、60 秒，之后不超过 5 分钟一次。
6. 本地队列需要容量和保留期限；队列满时优先保留高等级事件。
7. 设备时间必须通过 NTP 校准。

平台未来必须以 `deviceId + eventId` 建立唯一幂等约束。QoS 1 重复投递、设备重启和网络重连都不能生成重复业务事件。

## 8. MinIO 媒体上传（规划中）

硬件不能保存 MinIO Access Key 和 Secret Key。标准流程如下：

1. 设备计算媒体文件大小和 SHA-256。
2. 设备通过 MQTT 请求短期上传 URL。
3. 平台校验设备身份，生成只允许上传单个对象的短期签名 URL。
4. 设备使用 HTTP PUT 直接上传到 MinIO。
5. 设备校验 HTTP 结果后，将平台返回的对象 URL 写入事件 `media`。
6. 设备发布事件并等待业务 ACK。

### 8.1 请求上传 URL

Topic：

```text
oortcloud/v1/devices/{deviceId}/media/upload/request
```

载荷：

```json
{
  "protocolVersion": "1.0",
  "messageId": "814366f7-e5e2-4b1c-a642-6c01ff0f6837",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:00Z",
  "type": "media.upload.request",
  "payload": {
    "mediaId": "cce3634d-e596-47a8-8624-1d543ee64ce8",
    "fileName": "event-000001.jpg",
    "contentType": "image/jpeg",
    "fileSize": 248231,
    "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  }
}
```

### 8.2 返回上传 URL

Topic：

```text
oortcloud/v1/devices/{deviceId}/media/upload/reply
```

载荷：

```json
{
  "protocolVersion": "1.0",
  "messageId": "2877d4c6-bdcc-4fc7-98a7-ef9b63335817",
  "deviceId": "CAMERA-001",
  "sentAt": "2026-07-23T10:00:01Z",
  "type": "media.upload.reply",
  "payload": {
    "requestMessageId": "814366f7-e5e2-4b1c-a642-6c01ff0f6837",
    "mediaId": "cce3634d-e596-47a8-8624-1d543ee64ce8",
    "uploadUrl": "https://minio.example.com/signed-put-url",
    "objectUrl": "https://minio.example.com/vlstream/events/CAMERA-001/2026/07/23/event-000001.jpg",
    "method": "PUT",
    "headers": {
      "Content-Type": "image/jpeg"
    },
    "expiresAt": "2026-07-23T10:10:01Z"
  }
}
```

上传时必须使用平台返回的 HTTP 方法和请求头。URL 过期后重新申请，不得重用其他设备或其他对象的签名 URL。

## 9. 硬件端推荐程序结构

```text
MQTT网络线程
  ├─ 维护连接、重连、订阅和PUBACK
  ├─ 收到消息后放入对应业务队列
  └─ 收到业务ACK后通知本地持久化队列

事件上报线程（规划）
  ├─ 本地持久化事件
  ├─ 请求媒体签名URL
  ├─ HTTP PUT上传媒体
  ├─ MQTT发布事件
  └─ 等待业务ACK并清理本地记录
```

MQTT 回调线程中不能执行媒体上传或其他耗时业务，否则会阻塞心跳和消息处理。
其他业务工作线程由对应子协议定义。

## 10. 错误处理和可观测性

设备日志至少记录：

- MQTT 连接、断开、重连原因。
- Broker 返回码和订阅结果。
- `messageId`、`eventId` 或 `mediaId`，但不记录完整签名 URL。
- 事件本地队列长度、最老事件时间和丢弃数量。
- 媒体上传 HTTP 状态码。
- 业务 ACK 状态和错误码。

平台和设备日志应能通过 `deviceId + messageId` 关联。密码、Token、签名 URL 查询参数和 MinIO 凭据必须脱敏。

## 11. 硬件联调验收

### 11.1 MQTT 通用接入

1. 使用设备唯一 Client ID 建立 MQTT 连接。
2. 正确账号密码能够连接，错误密码会被 Broker 拒绝。
3. 断开并重连后能够恢复订阅。
4. 设备只能发布和订阅 ACL 允许的 Topic。
5. 日志中不出现 MQTT 密码和完整签名 URL。

### 11.2 后续事件上报

1. 断网期间事件进入本地持久化队列。
2. 恢复网络后按发生时间补传。
3. 同一事件重试保持相同 `eventId`。
4. MinIO 中的图片大小和 SHA-256 正确。
5. MQTT 中没有 Base64 图片或视频二进制。
6. 收到 `ACCEPTED` 或 `DUPLICATE` 后才删除本地记录。
7. 重复投递不会在平台生成重复事件。
8. 设备时间误差满足项目要求。

## 12. 版本与变更规则

| 协议版本 | 日期 | 说明 |
| --- | --- | --- |
| `1.0-draft` | 2026-07-23 | 定义 MQTT 通用接入和模型下发，并预留事件、媒体上传与设备状态协议 |

协议字段只允许向后兼容地新增。删除字段、改变字段类型、改变 Topic 或改变状态语义必须升级主版本，并同时提供平台与硬件迁移周期。

每次协议或后端实现状态发生变化时，需要同时更新：

1. 本文第 1 节实现状态。
2. 对应 Topic、消息字段和状态。
3. 对应 JSON Schema 或示例。
4. 硬件参考客户端。
5. README 中指向本文的入口；README 不复制协议正文。
