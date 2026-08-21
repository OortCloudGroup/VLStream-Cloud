# 硬件端事件图片与 MQTT 事件上报对接说明

本文用于硬件端快速联调事件图片上传与 MQTT 事件上报。完整通信字段和业务协议以
[VLS-Protocol.md](./VLS-Protocol.md) 为准。

## 1. 联调地址

| 服务 | 地址 |
| --- | --- |
| VLStream 后端 | `http://192.168.88.31:8080` |
| MQTT Broker | `192.168.88.31:1883` |
| MQTT Topic | `vlstream/v2.2/dev/{deviceId}/bus` |

MQTT 使用 QoS 1、Retain=false。设备发布事件前，应先订阅自己的 Topic，用于接收平台业务回执。

WVP 是 VLStream 设备、心跳、视频流和固件任务的唯一管理端；VLS 负责事件图片、AI 事件、
模型下发和平台业务入库。硬件仍只使用本文已有的 VLS HTTP 地址和 MQTT 协议，不需要增加
WVP 调用。VLS 收到事件后会在内部向 WVP 校验 `deviceId`，已登记但离线的设备也允许补报事件。

## 2. 对接流程

设备先完成图片上传，再通过 MQTT 上报事件。图片上传的地址、字段和调用示例统一见 [硬件端图片上传接口对接文档](./硬件端图片上传接口对接文档.md)。

### 2.1 MQTT 上报事件

```json
{
  "protocolVersion": "2.2",
  "messageId": "设备生成的消息UUID",
  "deviceId": "AETY-00-NJN2-WJUB-00000110",
  "sentAt": "2026-07-30T10:00:00Z",
  "msgDir": "dev2platform",
  "mainBizType": "aiBiz",
  "subBizType": "struct",
  "payload": {
    "eventId": "设备事件唯一ID",
    "eventType": "person_detected",
    "eventTime": "2026-07-30T10:00:00Z",
    "media": [
      {
        "mediaId": "申请上传地址时返回的mediaId",
        "objectKey": "申请上传地址时返回的objectKey",
        "sha256": "图片的SHA-256"
      }
    ]
  },
  "extend": {}
}
```

人脸事件将 `subBizType` 改为 `faceEvent`；通用结构化识别事件使用 `struct`。

## 3. 回执与重试

平台完成设备校验、图片校验和事件入库后返回：

```json
{
  "msgDir": "platform2dev",
  "subBizType": "struct",
  "payload": {
    "sourceMsgId": "设备原始messageId",
    "code": 200,
    "msg": "事件已接收并入库",
    "bizData": {
      "eventId": "设备事件唯一ID",
      "mediaId": "对应mediaId",
      "status": "SUCCESS"
    }
  }
}
```

- 收到 `SUCCESS`：设备可以删除本地待上报记录。
- 超时或收到 `FAILED`：保留记录并重试。
- 重试必须保持原来的 `messageId` 和 `eventId`，平台会幂等处理。
- 上传地址过期或 PUT 失败：重新申请上传地址。

平台只有在 `mainBizType=aiBiz` 且 `subBizType=struct/faceEvent` 时才按主动安全事件处理。
事件写入活动安全业务表后，可由平台的 `/task/v1/event_list` 接口查询；心跳和其他
`subBizType` 不会进入该事件表。`SUCCESS` 表示平台已经消费、校验并完成业务入库，
不是 MQTT Broker 仅收到消息的确认。

## 4. 当前联调限制

局域网联调暂时允许设备无认证申请上传地址。生产环境会关闭该开关，并增加设备身份认证。
`GET /vlsDeviceMedia/{mediaId}/view-url` 是平台查看私有图片使用的接口，硬件端无需调用。
