VLS-Protocol

修订记录

|     |     |
| --- | --- |
| 版本/作者/日期 | 修改内容 |
| 1.0/雷超群/2026-07-24 | 初版  |

目录

[一、协议总览 3](#_Toc236038990)

[1.1 设计目标 3](#_Toc236038991)

[1.2 基础通信约束 3](#_Toc236038992)

[二、基础通用规范 3](#_Toc236038993)

[2.1 唯一总线 Topic 3](#_Toc236038994)

[2.2 全局公共消息头 3](#_Toc236038995)

[2.3 通用统一回执模板 4](#_Toc236038996)

[三、设备类 5](#_Toc236038997)

[3.1 设备全量配置 5](#_Toc236038998)

[3.2 通用设备控制 6](#_Toc236038999)

[3.3 远程抓图 7](#_Toc236039000)

[3.4 设备校时 8](#_Toc236039001)

[3.5 媒体上传交互 9](#_Toc236039002)

[3.6 设备心跳 & 硬件遥测 11](#_Toc236039003)

[3.8 二维码识别上报 13](#_Toc236039004)

[3.10 本地录像查询 13](#_Toc236039005)

[3.11 录像上传任务下发 15](#_Toc236039006)

[3.13 硬件信息查询 15](#_Toc236039007)

[3.14 设备日志分页上报 17](#_Toc236039008)

[四、AI 业务类 18](#_Toc236039009)

[4.1 AI 模型下发部署 18](#_Toc236039010)

[4.2 查询当前加载模型 19](#_Toc236039011)

[4.3 模型手动回滚 20](#_Toc236039012)

[4.4 人脸库人员管理 20](#_Toc236039013)

[4.5 人脸通行 / 陌生人抓拍上报 21](#_Toc236039014)

[4.6 结构化人车 / 车牌 / 非机动车识别 23](#_Toc236039015)

[五、IoT Center 类 24](#_Toc236039016)

[5.1 RSGet 24](#_Toc236039017)

[5.2 RSSave 25](#_Toc236039018)

[5.3 电梯梯控 26](#_Toc236039019)

[5.4 液晶屏广告管理 27](#_Toc236039020)

[5.5 MP3 音频播放配置 28](#_Toc236039021)

[六、全局可靠性 & 安全通用规范 29](#_Toc236039022)

# 一、协议总览

## 1.1 设计目标

- 1.  **通用性**：兼容普通 IPC、球机 PTZ、人脸识别一体机、结构化智能相机、边缘盒全品类设备，统一交互范式；
    2.  **可扩展性**：采用分层消息结构，业务载荷独立隔离，新增 AI 事件、设备配置、媒体上传无需修改外层协议头；
    3.  **便利性**：复用文档现有 HTTP 后台接口字段，设备端一套逻辑对接 HTTP/MQTT 双通道，降低改造成本；
    4.  **可靠性**：完善幂等、重试、回执、离线缓存、心跳机制，适配弱网工业场景；
    5.  **安全合规**：设备独立 ACL、TLS 加密、消息鉴权、敏感字段脱敏。

## 1.2 基础通信约束

| **项** | **规范定义** |
| --- | --- |
| MQTT 版本 | 3.1.1（兼容 5.0） |
| 生产端口 | TLS 8883；测试明文 1883 |
| ClientID | vlstream-{deviceId} 全局唯一 |
| KeepAlive | 60s |
| CleanSession | false（持久会话，离线消息不丢失） |
| QoS 统一 | 所有命令 / 事件 / 回执使用 QoS 1 |
| Retain | 业务消息 false；设备在线状态 true |
| 编码  | 全消息 UTF-8 JSON |
| 时间标准 | UTC ISO-8601 yyyy-MM-ddTHH:mm:ssZ |
| 幂等规则 | messageId UUID 全局唯一，重试不变 |
| 二进制策略 | MQTT 仅传 URL / 元数据，图片 / 模型 / 录像走 HTTP/MinIO 上传，不 Base64 大包 |

# 二、基础通用规范

## 2.1 唯一总线 Topic

vlstream/v2.2/dev/{deviceId}/bus

全业务复用

所有平台下发、设备上报、业务回执统一发布 / 订阅；

ACL 权限：设备仅允许读写自身deviceId 对应的总线 Topic，禁止跨设备访问。

## 2.2 全局公共消息头

{  
"protocolVersion": "2.2",  
"messageId": "UUID-v4 全局唯一",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:30:00Z",  
"msgDir": "platform2dev/dev2platform",  
"mainBizType": "device/aiBiz",  
"subBizType": "细分业务标识",  
"payload": {},  
"extend": {}  
}

### 公共头字段说明

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| protocolVersion | string | 是   | 协议版本 2.2，主版本变更代表不兼容 |
| messageId | string | 是   | UUID v4 幂等 ID，重传 / 重试必须复用同一值 |
| deviceId | string | 是   | 设备全局唯一编号，Topic / 消息体 / 平台设备库三统一 |
| sentAt | string | 是   | UTC 标准时间 yyyy-MM-ddTHH:mm:ssZ |
| msgDir | string | 是   | platform2dev 平台下发； dev2platform 设备上报 |
| mainBizType | string | 是   | 仅二选一： device 设备硬件类 / aiBiz AI 全业务类 |
| subBizType | string | 是   | 细分业务标识，区分同大类下不同功能 |
| payload | object | 是   | 独立业务载荷，不同 subBizType 字段完全隔离 |
| extend | object | 否   | 厂商私有硬件 / 调试扩展，不污染标准字段 |

## 2.3 通用统一回执模板

回执subBizType 与原请求保持一致，通过sourceMsgId 绑定原始消息实现请求匹配

{  
"protocolVersion": "2.2",  
"messageId": " 回 执 独 立 UUID",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:30:01Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "config",  
"payload": {  
"sourceMsgId": "原始请求messageId",  
"code": 200,  
"msg": "业务执行描述",  
"errCode": 0,  
"errDetail": "",  
"bizData": {}  
},  
"extend": {}  
}

### 回执 payload 通用字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| sourceMsgId | string | 是   | 对应下发指令的 messageId，用于请求 - 应答关联 |
| code | int | 是   | HTTP 对齐状态码：200 成功，4xx 参数错误，5xx 设备内部异常 |
| msg | string | 是   | 可读简短执行结果文案 |
| errCode | int | 是   | 自定义业务错误码，0 = 无故障，非零代表细分故障 |
| errDetail | string | 否   | 详细故障堆栈 / 原因，成功留空 |
| bizData | object | 否   | 查询 / 抓拍 / 列表类业务返回数据，纯设置操作留空 |

# 三、设备类

**主要业务类型：**mainBizType=device

## 3.1 设备全量配置

**子业务类型：**subBizType=config

### 业务说明

平台下发 / 查询音视频、网络、PTZ、OSD、GB28181、RTMP、串口、存储、MQTT 全硬件参数，复用 HTTP 接口字段。

### 下发 payload 字段

platform2dev

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| operate | string | 是   | get 查询当前配置 / set 保存新配置 |
| configGroup | string | 是   | basic/net/video/audio/ptz/osd/ai/security/storage/time/rs485/rtmp/gb28181/mqtt/serial/mp3 |
| configData | object | 是   | 分组对应参数，get 时传空对象{} |

平台下发 JSON 示例

{  
"protocolVersion": "2.2",  
"messageId": "cmd-config-00112233-4455-6677-8899-aabbccddeeff",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:32:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "config",  
"payload": {  
"operate": "set",  
"configGroup": "video",  
"configData": {  
"code_stream_type": "主码流",  
"resolution": "1920\*1080",  
"video_coding": "H.265",  
"frame_rate": 25,  
"StreamMode": "VBR",  
"MaxStreams": 2048,  
"EncodeAudio": 1,  
"AudioMode": "G711A",  
"AuidoInputVolume": 70,  
"AudioOutputVolume": 70  
}  
},  
"extend": {}  
}

配置回执 JSON 示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-config-00aabbcc-1122-3344-5566-77889900aabb",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:32:02Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "config",  
"payload": {  
"sourceMsgId": "cmd-config-00112233-4455-6677-8899-aabbccddeeff",  
"code": 200,  
"msg": "视频码流配置保存成功",  
"errCode": 0,  
"errDetail": "",  
"bizData": {}  
},  
"extend": {}  
}

## 3.2 通用设备控制

**子业务类型：**subBizType=ctrl

### 业务说明

下发硬件操作指令：重启、恢复出厂、PTZ、格式化 SD、启停录像、固件升级、参数导入导出。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| ctrlAction | string | 是   | reboot/fully_recovery/recordStart/recordStop/ptzCmd/formatDisk/firmwareUpgrade/exportParam/importParams |
| param | object | 否   | 动作配套参数，PTZ / 格式化必填，重启可空 |
| param.ptzCmd | string | ptz 动作必填 | ptz_up/ptz_down/call_preset/add_preset/delete_preset |
| param.presetId | int | 云台预置位必填 | 预置位编号 1~36 |
| param.speed | int | 云台移动必填 | 移动速度 1~36 |

平台下发 PTZ 指令示例

{  
"protocolVersion": "2.2",  
"messageId": "cmd-ctrl-ptz-00998877-6655-4433-2211-abcdef987654",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:34:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "ctrl",  
"payload": {  
"ctrlAction": "ptzCmd",  
"param": {  
"ptzCmd": "call_preset",  
"presetId": 1,  
"speed": 10  
}  
},  
"extend": {}  
}

控制回执示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-ctrl-ptz-11223344-5566-7788-9900-123456abcdef",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:34:03Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "ctrl",  
"payload": {  
"sourceMsgId": "cmd-ctrl-ptz-00998877-6655-4433-2211-abcdef987654",  
"code": 200,  
"msg": "云台调用预置位1完成",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"ctrlAction": "ptzCmd",  
"presetId": 1  
}  
},  
"extend": {}  
}

## 3.3 远程抓图

**子业务类型：**subBizType=snapshot

### 业务说明

平台下发实时抓拍，设备返回 Base64 图片，硬件媒体操作。

### 下发 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| ctrlAction | string | 是   | 固定SnapShot |
| type | string | 是   | 固定Directly |
| snapChannel | int | 是   | 抓拍通道号 |

下发示例

{  
"protocolVersion": "2.2",  
"messageId": "cmd-snap-1234abcd-5678ef90-112233445566",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:35:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "snapshot",  
"payload": {  
"ctrlAction": "SnapShot",  
"type": "Directly",  
"snapChannel": 1  
},  
"extend": {}  
}

### 抓图回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| result | string | ok/failed |
| snapChannel | int | 抓拍通道 |
| image | string | jpg base64 编码图 |

回执完整示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-snap-9876dcba-4321fe09-667788990011",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:35:01Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "snapshot",  
"payload": {  
"sourceMsgId": "cmd-snap-1234abcd-5678ef90-112233445566",  
"code": 200,  
"msg": "抓拍完成",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"result": "ok",  
"snapChannel": 1,  
"image": "data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAEBAQEBAQEBAQEB..."  
}  
},  
"extend": {}  
}

## 3.4 设备校时

**子业务类型：**subBizType=time

### 业务说明

下发标准时间同步设备硬件时钟。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| time | string | 是   | yyyy-MM-dd HH:mm:ss |

下发示例

{  
"protocolVersion": "2.2",  
"messageId": "cmd-time-22334455-66778899-00112233abcd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:36:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "time",  
"payload": {  
"time": "2026-07-24 09:36:00"  
},  
"extend": {}  
}

校时回执示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-time-33445566-77889900-11223344dcba",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:36:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "time",  
"payload": {  
"sourceMsgId": "cmd-time-22334455-66778899-00112233abcd",  
"code": 200,  
"msg": "设备时间同步完成",  
"errCode": 0,  
"errDetail": "",  
"bizData": {}  
},  
"extend": {}  
}

## 3.5 媒体上传交互

**子业务类型：**subBizType=mediaUpload

### 业务说明

图片使用 HTTP PUT 直传 MinIO，MQTT 只传事件元数据，禁止传 Base64 图片。
硬件不持有 MinIO AccessKey/SecretKey，也不自行拼接对象地址。

### 1）设备通过 HTTP 申请上传地址

联调接口：

`POST /vlsDeviceMedia/public/upload-url`

请求体：

```json
{
  "deviceId": "CAM-20260001",
  "fileName": "capture-001.jpg",
  "contentType": "image/jpeg",
  "fileSize": 102400,
  "sha256": "64位图片SHA-256十六进制字符串"
}
```

平台校验设备存在后生成 `mediaId`、平台控制的 `objectKey` 和短期预签名 PUT 地址：

```json
{
  "code": 200,
  "data": {
    "mediaId": "7e70d34c-f24e-44d6-a067-5dcfc8f85e55",
    "objectKey": "events/CAM-20260001/2026/07/29/7e70d34c-f24e-44d6-a067-5dcfc8f85e55.jpg",
    "uploadUrl": "http://minio-host/bucket/events/...?X-Amz-Signature=...",
    "expiresAt": "2026-07-29T10:10:00Z",
    "requiredContentType": "image/jpeg"
  }
}
```

> 当前 `/public/upload-url` 只用于局域网联调，必须由
> `VLSTREAM_DEVICE_MEDIA_ALLOW_UNAUTHENTICATED=true` 显式开启。生产环境必须关闭，
> 并升级为每台设备独立凭证的 HMAC-SHA256 请求认证。

### 2）设备使用 HTTP PUT 上传图片

设备必须使用响应中的原始 `uploadUrl`，且 `Content-Type` 必须与
`requiredContentType` 完全一致：

```bash
curl -X PUT \
  -H "Content-Type: image/jpeg" \
  --data-binary "@capture-001.jpg" \
  "${uploadUrl}"
```

HTTP 2xx 表示对象存储已接收图片。硬件随后在 `faceEvent` 或 `struct` MQTT
事件中携带同一组 `mediaId/objectKey/sha256`。不需要单独发送 `mediaUpload`
完成消息；平台消费事件时会执行 MinIO HEAD、文件大小和 SHA-256 校验。

### 3）私有图片访问

数据库仅保存 `mediaId` 和内部 `objectKey`，不保存会过期的 MinIO 签名 URL。
已登录的平台用户通过以下接口获取短期私有 GET 地址：

`GET /vlsDeviceMedia/{mediaId}/view-url?seconds=300`

生产建议使用独立的私有 OSS 配置（例如 `config_key=vlstream-events`、独立
`vlstream-events` Bucket），通过 `VLSTREAM_DEVICE_MEDIA_OSS_CONFIG_KEY`
指定；本地联调可以留空并复用当前默认 OSS 配置。

## 3.6 设备心跳 & 硬件遥测

**子业务类型：**subBizType=state

### 业务说明

设备定时上报在线、硬件资源、版本以及平台可拉取的视频源。消息 Retain=true，断网自动下发遗嘱离线消息。平台以首次收到的合法 `deviceBiz/state` 自动登记未知设备；设备只负责提供源流，不直接调用 ZLMediaKit，也不生成浏览器 WebRTC 地址。

### 上报 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| online | bool | 是 | true 在线 /false 离线 |
| reason | string | 是 | normal/mqtt_connection_lost/power_off |
| heartbeatIndex | int | 是 | 心跳计数，重启重置 1 |
| deviceName | string | 是 | 设备名称 |
| deviceSerial | string | 否 | 硬件序列号 |
| version | string | 否 | 固件版本 |
| deviceFaceVer | string | 否 | 人脸库版本（人脸机专属） |
| ipAddr | string | 否 | 局域网 IP |
| mac | string | 否 | MAC 地址 |
| telemetry | object | 否 | 硬件资源 |
| telemetry.cpu | int | 否 | CPU 占用 % |
| telemetry.mem | int | 否 | 内存占用 % |
| telemetry.diskUsed | int | 否 | 磁盘使用率 % |
| telemetry.diskTotalMB | int | 否 | 磁盘总容量，单位 MB；适用于存储容量较小的嵌入式设备 |
| telemetry.temp | int | 否 | 设备温度℃ |
| telemetry.netUpMbps | float | 否 | 上行带宽 |
| telemetry.netDownMbps | float | 否 | 下行带宽 |
| serviceStatus | object | 否 | 后台服务状态 |
| serviceStatus.rtsp | bool | 否 | RTSP 服务 |
| serviceStatus.gb28181 | bool | 否 | GB28181 服务 |
| serviceStatus.aiInfer | bool | 否 | AI 推理服务 |
| streams | array | 是 | 视频源描述对象数组，不是视频数据；每个元素代表一路平台可主动拉取的 RTSP/RTMP 地址，每次上报全部已配置流，无视频源时传 `[]` |
| streams[].channelId | string | 条件必填 | `streams` 非空时必填；同一设备内稳定且唯一 |
| streams[].name | string | 否 | 视频流显示名称 |
| streams[].streamType | string | 否 | `main` 主码流 / `sub` 子码流 / `custom` 自定义码流；默认 `main` |
| streams[].protocol | string | 条件必填 | `streams` 非空时必填；当前支持 `rtsp` 或 `rtmp` |
| streams[].url | string | 条件必填 | `streams` 非空时必填；平台后端可访问的完整拉流地址，可包含认证信息 |
| streams[].default | bool | 否 | 是否为默认预览流；默认 `false`，全设备最多一个 `true` |
| streams[].available | bool | 否 | 当前源流是否可用，默认 `true` |

`streams` 是视频源描述对象数组，不是视频文件、视频帧，也不是要求设备向平台推流。每个数组元素描述一路平台后端可以主动拉取的 RTSP/RTMP 视频源。硬件端按以下规则填写：

1. 每次 `deviceBiz/state` 都上报当前全部已配置视频流，不得只传新增或变化的流。
2. 同一摄像头通道有主码流和子码流时，传两个对象：`channelId` 相同，`streamType` 分别为 `main` 和 `sub`。
3. 已配置但暂时无法拉取的流仍保留在数组中，并传 `available=false`；流被永久删除后才从数组移除。
4. 设备没有视频能力或尚未配置拉流地址时传 `streams: []`。
5. `channelId + streamType` 是一路流的稳定标识，设备重启或拉流地址变化后不得随意改变；`url` 必须能被平台后端访问。

平台收到新心跳后，会把未出现在本次数组中的旧流标记为不可用。`url` 属于敏感信息，只允许通过受控 MQTT/TLS 上报，不得写入日志或返回浏览器。平台仅在用户预览时按需拉取该 URL 并转换为 WebRTC，无人观看后自动释放拉流代理。

`state` 上报和平台回执使用 `mainBizType=deviceBiz`。为兼容已按早期文档实现的设备，平台在过渡期也接收旧值 `device`，并在回执中原样返回上报消息使用的 `mainBizType`。

正常心跳上报完整示例

{  
"protocolVersion": "2.2",  
"messageId": "up-heartbeat-33445566-77889900-1122aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:38:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "deviceBiz",
"subBizType": "state",  
"payload": {  
"online": true,  
"reason": "normal",  
"heartbeatIndex": 120,  
"deviceName": "园区东门人脸机",  
"deviceSerial": "3161w316156d33x966",  
"version": "v1.1.02",  
"deviceFaceVer": "1021_v2",  
"ipAddr": "192.168.1.100",  
"mac": "00:11:22:33:44:55",  
"telemetry": {  
"cpu": 32,  
"mem": 45,  
"diskUsed": 68,  
"diskTotalMB": 136,
"temp": 48,  
"netUpMbps": 8.2,  
"netDownMbps": 12.5  
},  
"serviceStatus": {  
"rtsp": true,  
"gb28181": false,  
"aiInfer": true  
},
"streams": [
{
"channelId": "CH-1",
"name": "东门主码流",
"streamType": "main",
"protocol": "rtsp",
"url": "rtsp://user:password@192.168.1.100:554/Streaming/Channels/101",
"default": true,
"available": true
},
{
"channelId": "CH-1",
"name": "东门子码流",
"streamType": "sub",
"protocol": "rtsp",
"url": "rtsp://user:password@192.168.1.100:554/Streaming/Channels/102",
"default": false,
"available": true
}
]
},  
"extend": {}  
}

平台接收成功回执示例

{
"protocolVersion": "2.2",
"messageId": "ack-heartbeat-33445566-77889900-1122aabbccdd",
"deviceId": "CAM-20260001",
"sentAt": "2026-07-24T09:38:01Z",
"msgDir": "platform2dev",
"mainBizType": "deviceBiz",
"subBizType": "state",
"payload": {
"sourceMsgId": "up-heartbeat-33445566-77889900-1122aabbccdd",
"code": 200,
"msg": "状态已接收",
"errCode": 0,
"errDetail": "",
"bizData": {}
},
"extend": {}
}

离线遗嘱消息示例

{  
"protocolVersion": "2.2",  
"messageId": "up-will-offline-44556677-88990011-2233aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:38:50Z",  
"msgDir": "dev2platform",  
"mainBizType": "deviceBiz",
"subBizType": "state",  
"payload": {  
"online": false,  
"reason": "mqtt_connection_lost",  
"heartbeatIndex": 120,  
"deviceName": "园区东门人脸机",  
"deviceSerial": "3161w316156d33x966",  
"version": "v1.1.02",  
"ipAddr": "192.168.1.100",  
"mac": "00:11:22:33:44:55",  
"telemetry": {},  
"serviceStatus": {},
"streams": []
},  
"extend": {}  
}

## 3.8 二维码识别上报

**子业务类型：**subBizType=qrcode

### 业务说明

摄像头硬件识别二维码主动推送内容。

### 上报 payload 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| operator | string | 固定QRCodePush |
| info.facesluiceId | string | 设备 ID |
| info.time | string | 识别时间 yyyy-MM-dd HH:mm:ss |
| info.QRCodeInfo | string | 二维码原始文本 |

上报完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "up-qrcode-scan-33445566-77889900-1122aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:44:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "qrcode",  
"payload": {  
"operator": "QRCodePush",  
"info": {  
"facesluiceId": "CAM-20260001",  
"time": "2026-07-24 09:44:00",  
"QRCodeInfo": "https://test.company.com/user/10001"  
}  
},  
"extend": {}  
}

## 3.10 本地录像查询

**子业务类型：**subBizType=recordSearch

### 业务说明

平台下发时间段，设备返回 SD 卡录像文件列表。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| startTime | string | 是   | yyyy-MM-dd HH:mm:ss |
| endTime | string | 是   | yyyy-MM-dd HH:mm:ss |

下发示例：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-record-search-44556677-88990011-2233aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:45:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "recordSearch",  
"payload": {  
"startTime": "2026-07-24 08:00:00",  
"endTime": "2026-07-24 09:00:00"  
},  
"extend": {}  
}

### 查询回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| facesluiceId | string | 设备 ID |
| result | string | ok/failed |
| filelist | array | 录像文件数组 |
| filelist.filename | string | 本地文件路径 |
| filelist.type | int | 录像类型 |
| filelist.start_time | long | 开始时间戳 |
| filelist.end_time | long | 结束时间戳 |

回执完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "ack-record-search-55667788-99001122-3344aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:45:01Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "recordSearch",  
"payload": {  
"sourceMsgId": "cmd-record-search-44556677-88990011-2233aabbccdd",  
"code": 200,  
"msg": "录像文件查询完成",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"filelist": \[  
{  
"filename": "/sdcard/20260724/N165617.mp4",  
"type": 1,  
"start_time": 1784966400,  
"end_time": 1784969400  
}  
\],  
"facesluiceId": "CAM-20260001",  
"result": "ok"  
}  
},  
"extend": {}  
}

## 3.11 录像上传任务下发

**子业务类型：**subBizType=recordUpload

### 业务说明

下发录像 HTTP 上传任务，设备二进制 POST 上传文件。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| startTime | string | 是   | 录像起始时间 |
| endTime | string | 是   | 录像结束时间 |
| filename | string | 是   | 本地录像路径 |
| uploadurl | string | 是   | 后端接收 POST 地址 |
| videoId | string | 否   | 自定义录像唯一标识 |

下发示例：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-record-upload-66778899-00112233-4455aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:45:30Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "recordUpload",  
"payload": {  
"startTime": "2026-07-24 08:00:00",  
"endTime": "2026-07-24 09:00:00",  
"filename": "/sdcard/20260724/N165617.mp4",  
"uploadurl": "",  
"videoId": "VID-0001"  
},  
"extend": {}  
}

## 3.13 硬件信息查询

**子业务类型：**subBizType=systemInfo

### 业务说明

查询 SD 卡 / 4G/WiFi/ 固件版本硬件信息。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| queryType | string | 是   | GetSdCardInfo/Get4GStatus/GetWifiStatus/GetDeviceVersion |

下发示例（查询 SD 卡）：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-system-sd-77889900-11223344-5566aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:47:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "systemInfo",  
"payload": {  
"queryType": "GetSdCardInfo"  
},  
"extend": {}  
}

### SD 卡查询回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| sd_info.status | int | 0 无卡 / 1 未格式化 / 3 挂载 / 5 使用中 / 6 满 |
| sd_info.total_size | int | 总容量 MB |
| sd_info.free_size | int | 剩余容量 MB |
| sd_info.record_mode | int | 0 停止 / 1 全天 / 2 报警录像 |
| sd_info.record_filelen | int | 录像分片分钟 |

回执完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "ack-system-sd-88990011-22334455-6677aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:47:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "systemInfo",  
"payload": {  
"sourceMsgId": "cmd-system-sd-77889900-11223344-5566aabbccdd",  
"code": 200,  
"msg": "SD卡信息读取成功",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"sd_info": {  
"status": 3,  
"total_size": 8192,  
"free_size": 1024,  
"record_mode": 2,  
"record_filelen": 5  
}  
}  
},  
"extend": {}  
}

## 3.14 设备日志分页上报

**子业务类型：**subBizType=log

### 业务说明

设备运行、操作审计日志分页推送。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| page | int | 当前页码 |
| limit | int | 单页条数 |
| total | int | 日志总条数 |
| logList | array | 日志明细数组 |
| logList.serial | int | 日志序号 |
| logList.time | string | 日志时间 |
| logList.mainType | string | 日志大类：正常 / 异常 |
| logList.subType | string | 细分故障 / 操作 |
| logList.channel | int | 对应通道 |
| logList.user | string | 操作用户 |
| logList.remoteIp | string | 操作客户端 IP |

上报完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "up-log-page-66778899-00112233-4455aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:46:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "log",  
"payload": {  
"page": 1,  
"limit": 10,  
"total": 120,  
"logList": \[  
{  
"serial": 2,  
"time": "2026/07/24 09:45:00",  
"mainType": "异常",  
"subType": "音频输入异常",  
"channel": 1,  
"user": "admin",  
"remoteIp": "192.168.1.100"  
}  
\]  
},  
"extend": {}  
}

# 四、AI 业务类

**主要业务类型：**mainBizType=aiBiz

## 4.1 AI 模型下发部署

**子业务类型：**subBizType=modelDeploy

### 业务说明

AI 推理模型下载、校验、切换推理实例。

> VLStream Cloud 当前实现说明：通信信封、Topic 和回执严格遵守 V2.2；业务字段按
> 现有训练任务和模型产物落地。当前支持 `pt/onnx/rknn/int8-rknn/om`；平台 ID 是 64 位 Snowflake ID，
> `algorithmId/trainingId` 均按字符串传输，避免 JSON 数字精度丢失；`modelConfig`
> 为预留可选字段、当前不下发。模型文件仍保存在
> GPU 训练服务器，`modelUrl` 是平台生成的短期签名 HTTP 地址，由平台通过 SFTP
> 流式转发文件，不要求模型预先迁移到 MinIO。

### 下发 payload 完整字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| requestId | string | 是   | 模型任务唯一 ID |
| algorithmId | string | 是   | 平台算法 Snowflake ID |
| trainingId | string | 是   | 平台训练任务 Snowflake ID |
| modelType | string | 是   | pt/onnx/rknn/int8-rknn/om，以训练任务实际产物为准 |
| modelUrl | string | 是   | 短期签名 HTTP 下载地址 |
| fileName | string | 是   | 模型文件名 |
| fileSize | number | 是   | 文件字节大小 |
| sha256 | string | 是   | 文件哈希校验码 |
| expiresAt | string | 是   | 下载链接过期 UTC 时间 |
| rollbackEnable | bool | 是   | 当前实现固定为 true，失败自动回滚旧模型 |
| modelConfig | object | 否   | 预留推理参数，当前实现不下发 |
| modelConfig.confThreshold | float | 否   | 置信度阈值 |
| modelConfig.nmsThreshold | float | 否   | NMS 抑制阈值 |

下发完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "77886655-1234-4678-abcd-12345678abcd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:33:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "aiBiz",  
"subBizType": "modelDeploy",  
"payload": {  
"requestId": "MODEL-TASK-20260724-001",  
"algorithmId": "2077000000000001001",  
"trainingId": "2077359187012198403",  
"modelType": "om",  
"modelUrl": "http://192.168.88.31:8080/vlsModelDispatch/public/{requestId}/download?expires={unixSeconds}&signature={hmac}",  
"fileName": "detect.om",  
"fileSize": 12580000,  
"sha256": "3f2c9d11e88a77b665443211abcdef009876543210fedcba1234567890abcdef",  
"expiresAt": "2026-07-25T00:00:00Z",  
"rollbackEnable": true  
},  
"extend": {}  
}

### 模型回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| requestId | string | 对应下发任务 ID |
| status | string | RECEIVED/DOWNLOADING/DOWNLOADED/VERIFYING/DEPLOYING/SUCCESS/FAILED |
| fileSha256 | string | 本地校验哈希 |
| costMs | number | 部署耗时毫秒 |

回执完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "11223344-5566-4788-9900-abcdef123456",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:33:45Z",  
"msgDir": "dev2platform",  
"mainBizType": "aiBiz",  
"subBizType": "modelDeploy",  
"payload": {  
"sourceMsgId": "77886655-1234-4678-abcd-12345678abcd",  
"code": 200,  
"msg": "模型校验通过，部署完成",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"requestId": "MODEL-TASK-20260724-001",  
"status": "SUCCESS",  
"fileSha256": "3f2c9d11e88a77b665443211abcdef00987654321",  
"costMs": 1200  
}  
},  
"extend": {}  
}

## 4.2 查询当前加载模型

**子业务类型：**subBizType=modelQuery

### 业务说明

查询设备当前生效 AI 模型，下发 payload 为空对象{} 下发示例：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-model-query-11112222-33334444-55556666",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T10:00:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "aiBiz",  
"subBizType": "modelQuery",  
"payload": {},  
"extend": {}  
}

### 查询回执 bizData 字段

algorithmId、fileName、sha256、modelType

## 4.3 模型手动回滚

**子业务类型：**subBizType=modelRollback

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| requestId | string | 是   | 回滚任务 ID |

## 4.4 人脸库人员管理

**子业务类型：**subBizType=faceLib

### 业务说明

人脸底库增删改查（新增 / 删除 / 分页查询 / 单条查询 / 清空）。

### 下发 payload 通用字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| libOperate | string | 是   | EditPerson/QueryPerson/QuerySample/SearchPerson/DelPerson/DeleteAllPerson |
| customId | string | 增删查单条必填 | 人员全局唯一 UUID |
| name | string | EditPerson 必填 | 人员姓名 |
| telnum1 | string | 可选  | 联系电话 |
| age | int | 可选  | 年龄  |
| gender | int | 0 男 / 1 女 |     |
| idCard | string | 可选身份证 |     |
| valid_time_type | int | 0 每日 / 1 星期 / 2 日期区间 |     |
| start_time | long | 有效期起始秒 |     |
| expire_time | long | 有效期结束秒 |     |
| department_name | string | 部门  |     |
| personType | int | 0 白 / 1 黑 / 2VIP |     |
| notes | string | 备注  |     |
| op_face_ver | string | 人脸库版本 |     |
| pic | string | 人脸底图 base64 |     |

新增人员下发完整示例

{  
"protocolVersion": "2.2",  
"messageId": "cmd-face-lib-add-77889900-11223344-5566aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:40:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "aiBiz",  
"subBizType": "faceLib",  
"payload": {  
"libOperate": "EditPerson",  
"customId": "713BCEF6393955E0DC8822354D0D61E1",  
"name": " 张 三 ",  
"telnum1": "13800138000",  
"age": 30,  
"gender": 0,  
"idCard": "400400199912120001",  
"valid_time_type": 0,  
"start_time": 0,  
"expire_time": 86399,  
"department_name": "行政部",  
"personType": 0,  
"notes": "园区内部员工",  
"op_face_ver": "1021_v2",  
"pic": "data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."  
},  
"extend": {}  
}

人脸库操作回执示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-face-lib-add-88990011-22334455-6677aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:40:02Z",  
"msgDir": "dev2platform",  
"mainBizType": "aiBiz",  
"subBizType": "faceLib",  
"payload": {  
"sourceMsgId": "cmd-face-lib-add-77889900-11223344-5566aabbccdd",  
"code": 200,  
"msg": "人员新增成功",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"customId": "713BCEF6393955E0DC8822354D0D61E1",  
"personId": 1,  
"result": "ok"  
}  
},  
"extend": {}  
}

## 4.5 人脸通行 / 陌生人抓拍上报

**子业务类型：**subBizType=faceEvent

### 业务说明

人脸 AI 识别结果上报，区分白名单通行 / 陌生人。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| eventId | string | 设备生成的事件唯一 ID，同一事件重试时保持不变 |
| eventType | string | 平台事件类型，例如 face_pass/stranger |
| eventDesc | string | 可选事件描述 |
| eventTime | string | UTC ISO-8601 事件时间 |
| eventLevel | string | low/medium/high/urgent，默认 medium |
| media | array | 已通过 3.5 上传的图片，当前至少一张 |
| media[].mediaId | string | 平台签发的 mediaId |
| media[].objectKey | string | 平台签发的 objectKey |
| media[].sha256 | string | 图片实际 SHA-256 |
| operator | string | entr 白名单 /entr_or_exit 陌生人 |
| info.customId | string | 库内人员 ID，陌生人空 |
| info.personid | int | 设备本地人员编号 |
| info.persionName | string | 姓名  |
| info.facesluiceId | string | 设备 ID |
| info.time | string | 抓拍时间 yyyy-MM-dd HH:mm:ss |
| info.score_dect | int | 比对分数 0~100 |
| info.rect | array | \[x,y,w,h\] 人脸框坐标 |
| info.gender | string | M 男 / F 女 |
| info.glasses | int | 1 无 / 2 普通 / 3 墨镜 |
| info.mask | int | 1 无 / 2 佩戴口罩 |
| info.hat | int | 1 无 / 2 戴帽 |

上报完整示例：

{  
"protocolVersion": "2.2",  
"messageId": "up-face-pass-99001122-33445566-7788aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:41:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "aiBiz",  
"subBizType": "faceEvent",  
"payload": {  
"eventId": "face-event-20260724-0001",  
"eventType": "face_pass",  
"eventDesc": "白名单人员通行",  
"eventTime": "2026-07-24T09:41:00Z",  
"eventLevel": "medium",  
"media": [{  
"mediaId": "7e70d34c-f24e-44d6-a067-5dcfc8f85e55",  
"objectKey": "events/CAM-20260001/2026/07/24/7e70d34c-f24e-44d6-a067-5dcfc8f85e55.jpg",  
"sha256": "64位图片SHA-256十六进制字符串"  
}],  
"operator": "entr",  
"info": {  
"customId": "713BCEF6393955E0DC8822354D0D61E1",  
"personid": 1,  
"persionName": "张三",  
"facesluiceId": "CAM-20260001",  
"time": "2026-07-24 09:41:00",  
"score_dect": 92,  
"rect": \[100, 120, 220, 260\],  
"gender": "M",  
"glasses": 1,  
"mask": 1,  
"hat": 1  
}  
},  
"extend": {}  
}

## 4.6 结构化人车 / 车牌 / 非机动车识别

**子业务类型：**subBizType=struct

### 业务说明

通用目标 AI 跟踪上报，人脸 / 人形 / 机动车 / 非机动车 / 车牌统一结构。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| eventId | string | 设备生成的事件唯一 ID，同一事件重试时保持不变 |
| eventType | string | 平台事件类型，例如 person_detected |
| eventDesc | string | 可选事件描述 |
| eventTime | string | UTC ISO-8601 事件时间 |
| eventLevel | string | low/medium/high/urgent，默认 medium |
| media | array | 已通过 3.5 上传的图片，当前至少一张 |
| media[].mediaId | string | 平台签发的 mediaId |
| media[].objectKey | string | 平台签发的 objectKey |
| media[].sha256 | string | 图片实际 SHA-256 |
| operator | string | 固定struct_attr |
| facesluiceId | string | 设备 ID |
| track_id | int | 目标跟踪唯一 ID |
| type | string | face/human/vehicle/cycle/plate |
| keep_time | int | 画面停留毫秒 |
| left/top/right/bottom | int | 目标像素框 |
| worth | float | 识别置信度 0~1 |
| datetime | string | 抓拍时间 |
| bind | array | 绑定目标 track_id（人脸绑定人形） |
| info | object | 目标细分属性 |
| mac | string | 设备 MAC |
| ipaddr | string | 设备 IP |

人形结构化完整上报示例

{  
"protocolVersion": "2.2",  
"messageId": "up-struct-human-00112233-44556677-8899aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:42:00Z",  
"msgDir": "dev2platform",  
"mainBizType": "aiBiz",  
"subBizType": "struct",  
"payload": {  
"eventId": "struct-event-20260724-0001",  
"eventType": "person_detected",  
"eventDesc": "检测到人员",  
"eventTime": "2026-07-24T09:42:00Z",  
"eventLevel": "medium",  
"media": [{  
"mediaId": "b91aec19-fb17-4328-b390-1d9ab6ef6c16",  
"objectKey": "events/CAM-20260001/2026/07/24/b91aec19-fb17-4328-b390-1d9ab6ef6c16.jpg",  
"sha256": "64位图片SHA-256十六进制字符串"  
}],  
"operator": "struct_attr",  
"facesluiceId": "CAM-20260001",  
"track_id": 4,  
"type": "human",  
"keep_time": 2022,  
"left": 783,  
"top": 443,  
"right": 1272,  
"bottom": 983,  
"worth": 0.853293,  
"datetime": "2026-07-24 09:42:00",  
"bind": \[\],  
"info": {  
"clothes_color": 4,  
"safety_helmet": 1,  
"action_watch_phone": 1,  
"bags": 1  
},  
"mac": "00:11:22:33:44:55",  
"ipaddr": "192.168.1.100"  
},  
"extend": {}  
}

### 事件业务回执

平台只在完成 MQTT 消息去重、设备校验、MinIO 对象存在性/大小/SHA-256 校验并成功
写入主动安全事件表 `oort_task_event` 后返回 `SUCCESS`。该事件可由平台
`/task/v1/event_list` 查询。硬件在收到成功回执前必须保留本地
事件记录；超时可以使用相同 `messageId/eventId` 重发，平台会幂等返回成功。

```json
{
  "protocolVersion": "2.2",
  "messageId": "平台新生成的回执UUID",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-07-24T09:42:01Z",
  "msgDir": "platform2dev",
  "mainBizType": "aiBiz",
  "subBizType": "struct",
  "payload": {
    "sourceMsgId": "up-struct-human-00112233-44556677-8899aabbccdd",
    "code": 200,
    "msg": "事件已接收并入库",
    "errCode": 0,
    "errDetail": "",
    "bizData": {
      "eventId": "struct-event-20260724-0001",
      "mediaId": "b91aec19-fb17-4328-b390-1d9ab6ef6c16",
      "status": "SUCCESS"
    }
  },
  "extend": {}
}
```

# 五、IoT Center 类

**主要业务类型：**mainBizType=RS

## 5.1 RSGet

**子业务类型：**subBizType=RSGet

### 业务说明

GET 获取RS-485配置

### 请求参数

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
| **名称** | **位置** | **类型** | **必选** | **说明** |
| accessToken | header | string | 是   | none |

返回示例

200 Response

{

"code": 200, "message": "成功", "data": {

"baud_rate": 115200,

"data_bit": 16,

"stop_bit": 1, "check": "无",

"flow_contrl": "无",

"decoder_type": "PWMC-AE", "decoder_addr": "127.0.0.1"

}

}

**返回结果**

|     |     |     |     |
| --- | --- | --- | --- |
| **状态码** | **状态码含义** | **说明** | **数据模型** |
| 200 | [OK](https://tools.ietf.org/html/rfc7231#section-6.3.1) | none | Inline |

**返回数据结构**

状态码 200

|     |     |     |     |     |     |
| --- | --- | --- | --- | --- | --- |
| **名称** | **类型** | **必选** | **约束** | **中文名** | **说明** |
| » code | integer | true | none |     | none |
| » message | string | true | none |     | none |
| » data | object | true | none |     | none |
| »» baud_rate | integer | true | none | 波特率 | none |
| »» data_bit | integer | true | none | 数据位 | none |
| »» stop_bit | integer | true | none | 停止位 | none |
| »» check | string | true | none | 校验位 | none |
| »» flow_contrl | string | true | none | 流控  | none |
| »» decoder_type | string | true | none | 解码器类型 | none |
| »» decoder_addr | string | true | none | 解码器地址 | none |

## 5.2 RSSave

**子业务类型：**subBizType=RSSave

### 业务说明

POST 保存RS-485配置

### 请求参数

{

"baud_rate": 115200,

"data_bit": 16,

"stop_bit": 1, "check": "无",

"flow_contrl": "无",

"decoder_type": "PWMC-AE", "decoder_addr": "127.0.0.1"

}

**请求参数**

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
| **名称** | **位置** | **类型** | **必选** | **说明** |
| accessToken | header | string | 是   | none |
| body | body | object | 否   | none |

返回示例

200 Response

{

"code": 200,

"message": "成功"

}

**返回结果**

|     |     |     |     |
| --- | --- | --- | --- |
| **状态码** | **状态码含义** | **说明** | **数据模型** |
| 200 | [OK](https://tools.ietf.org/html/rfc7231#section-6.3.1) | none | Inline |

**返回数据结构**

状态码 200

|     |     |     |     |     |     |
| --- | --- | --- | --- | --- | --- |
| **名称** | **类型** | **必选** | **约束** | **中文名** | **说明** |
| » code | integer | true | none |     | none |
| » message | string | true | none |     | none |

## 5.3 电梯梯控

**子业务类型：**subBizType=tkControl

### 业务说明

闸机硬件电梯楼层控制指令下发。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| operator | string | 是   | 固定TKControl |
| info.value | string | 是   | 32 位十六进制梯控指令码 |

下发示例：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-tk-ctrl-11223344-55667788-9900aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:43:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "tkControl",  
"payload": {  
"operator": "TKControl",  
"info": {  
"value": "FF010000000000000000"  
}  
},  
"extend": {}  
}

梯控回执示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-tk-ctrl-22334455-66778899-0011aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:43:01Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "tkControl",  
"payload": {  
"sourceMsgId": "cmd-tk-ctrl-11223344-55667788-9900aabbccdd",  
"code": 200,  
"msg": "梯控指令下发成功",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"result": "ok"  
}  
},  
"extend": {}  

## 5.4 液晶屏广告管理

**子业务类型：**subBizType=ad

### 业务说明

带屏人脸机广告图片增删、轮播时长配置。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| libOperate | string | 是   | EditAD 新增修改 / DelAD 删除 |
| info.adslot | int | 是   | 广告槽位 0~4 |
| info.path | string | EditAD 必填 | 广告图片下载 URL |
| info.polltime | int | EditAD 必填 | 单张轮播时长 (秒) |

下发示例（新增广告）：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-ad-add-44556677-88990011-2233aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:44:30Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "ad",  
"payload": {  
"libOperate": "EditAD",  
"info": {  
"adslot": 0,  
"path": "https://minio.test.com/ad/ad01.jpg",  
"polltime": 10  
}  
},  
"extend": {}  
}

广告操作回执示例

{  
"protocolVersion": "2.2",  
"messageId": "ack-ad-add-55667788-99001122-3344aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:44:31Z",  
"msgDir": "dev2platform",  
"mainBizType": "device",  
"subBizType": "ad",  
"payload": {  
"sourceMsgId": "cmd-ad-add-44556677-88990011-2233aabbccdd",  
"code": 200,  
"msg": "广告配置更新成功",  
"errCode": 0,  
"errDetail": "",  
"bizData": {  
"adslot": 0,  
"result": "ok"  
}  
},  
"extend": {}  
}

## 5.5 MP3 音频播放配置

**子业务类型：**subBizType=mp3Play

### 业务说明

设备本地音频文件、播放模式配置。

### 下发 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| operate | string | 是   | Set 设置 / Get 查询 |
| config.enable | int | 是   | 0 关闭 / 1 启用 |
| config.mode | int | 是   | 0 顺序 / 1 随机 |
| config.mp3file | array | 否   | 音频列表 |
| mp3file.name | string |     | 音频名称 |
| mp3file.select | int |     | 0 不选 / 1 选中播放 |

下发示例：

{  
"protocolVersion": "2.2",  
"messageId": "cmd-mp3-set-77889900-11223344-5566aabbccdd",  
"deviceId": "CAM-20260001",  
"sentAt": "2026-07-24T09:46:00Z",  
"msgDir": "platform2dev",  
"mainBizType": "device",  
"subBizType": "mp3Play",  
"payload": {  
"operate": "Set",  
"config": {  
"enable": 1,  
"mode": 0,  
"mp3file": \[  
{  
"name": "欢迎语音.mp3",  
"select": 1  
},  
{  
"name": "警报提示.mp3",  
"select": 0  
}  
\]  
}  
},  
"extend": {}  
}

# 六、全局可靠性 & 安全通用规范

- - 1.  **幂等去重**：每条消息messageId 全局唯一，平台以deviceId+messageId 建立索引，重复消息直接丢弃；
        2.  **阶梯重试**：未收到对应 ack 自动 5/15/30/60s 退避，最长重试 5 分钟；人脸库批量任务延长至 10

分钟；

- - 1.  **离线缓存**：设备本地持久化 AI 抓拍、人脸库任务、媒体任务，网络恢复按时间顺序补发；
        2.  **二进制约束**：单条 JSON 最大 128KB，模型 / 录像 / 大图禁止 Base64 内嵌，统一 HTTP/MinIO 上传；
        3.  **三要素校验**：MQTT 登录账号、Topic 内 deviceId、消息体 deviceId 三者必须完全一致，不一致丢弃并记录安全日志；
        4.  **传输加密**：生产环境强制 TLS 8883，1883 明文仅内网测试；
        5.  **敏感脱敏**：身份证、密钥、临时上传 URL、人脸原图支持配置脱敏输出；

# 七、VLStream 原生设备当前实现说明

本章记录平台工程实现，不属于硬件必须实现的线协议字段；硬件对接以第二、三、六章为准。

## 7.1 数据与租户边界

- WVP 是 VLStream 设备、心跳、视频流和固件任务的唯一数据源，使用 WVP 自己的
  `wvp_vlstream_device`、`wvp_vlstream_device_stream` 和消息幂等表。
- VLS 继续负责 `aiBiz/struct`、`aiBiz/faceEvent`、模型下发及其回执，不改变硬件已有的
  HTTP 地址、MQTT Topic 或报文字段。
- VLS 在签发事件图片上传地址和消费 AI 事件前，通过
  `GET /internal/vlstream/device/{deviceId}` 向 WVP 校验设备。该只读接口仅供后端服务网络
  调用，不使用用户鉴权或额外共享密钥；硬件端和平台用户不调用。
- 设备只要已登记在 WVP 即可上传和补报事件，不要求当时在线。WVP 不存在该设备时，VLS
  拒绝签发上传地址或返回事件失败回执。
- WVP 设备进入 VLS 业务表时映射到配置的默认租户：单租户使用
  `VLSTREAM_NATIVE_DEVICE_DEFAULT_TENANT_ID`，多租户使用
  `VLSTREAM_NATIVE_DEVICE_MULTI_TENANT_DEFAULT_TENANT_ID`。
- VLS 旧 `vls_mqtt_device` 设备管理、心跳和固件实现仅保留用于回滚，默认由
  `VLSTREAM_NATIVE_DEVICE_LEGACY_ENABLED=false` 禁用，正常部署不得同时启用。

## 7.2 管理与预览接口

设备管理入口为平台“设备管理 / VLStream 协议”菜单，列表、详情、在线状态、流信息和预览
能力由 WVP 提供。VLS 旧 `/vlsMqttDevice/**` 与 `/vlsDeviceFirmware/**` 控制器在正常部署中
不注册；只有显式启用旧实现回滚开关时才恢复。

## 7.3 ZLMediaKit 运行边界

WVP 统一调用 ZLMediaKit 完成拉流、预览、回放和流状态管理。VLS 的 AI 事件与模型业务不直接
管理视频流。WVP 到 ZLMediaKit 的内部 REST 地址、ZLMediaKit 回调 WVP 的地址以及浏览器播放
地址属于三个不同方向，部署时必须分别保证可达，并保证 WVP 使用的 API Secret 与
ZLMediaKit `api.secret` 一致。
