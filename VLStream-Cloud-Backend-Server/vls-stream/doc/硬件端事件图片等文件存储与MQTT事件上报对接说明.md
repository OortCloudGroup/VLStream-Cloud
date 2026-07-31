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

## 2. 对接流程

### 2.1 申请图片上传地址

设备先计算图片的字节数和 SHA-256，然后调用：

```http
POST /vlsDeviceMedia/public/upload-url
Content-Type: application/json
```

```json
{
  "deviceId": "AETY-00-NJN2-WJUB-00000110",
  "fileName": "capture.jpg",
  "contentType": "image/jpeg",
  "fileSize": 102400,
  "sha256": "图片的64位SHA-256十六进制字符串"
}
```

响应 `data` 中需要保存：

- `mediaId`
- `objectKey`
- `uploadUrl`
- `requiredContentType`

### 2.2 上传图片

使用返回的完整 `uploadUrl` 直接 PUT 图片：

```bash
curl -X PUT \
  -H "Content-Type: image/jpeg" \
  --data-binary "@capture.jpg" \
  "${uploadUrl}"
```

`Content-Type` 必须与 `requiredContentType` 完全一致。HTTP 2xx 表示上传成功。
硬件不需要 MinIO AccessKey/SecretKey，也不要在 MQTT 中传 Base64 图片。

### 2.3 MQTT 上报事件

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

## 4. 当前联调限制

局域网联调暂时允许设备无认证申请上传地址。生产环境会关闭该开关，并增加设备身份认证。
`GET /vlsDeviceMedia/{mediaId}/view-url` 是平台查看私有图片使用的接口，硬件端无需调用。
