# 板端事件图片与 MP4 上报

适用：VLStream Cloud 单租户 VM 的主动安全事件。后端相对接口仍为 `/vlsDeviceMedia/public/upload-url`，VM 网关入口为 `https://vlstream.oortcloudsmart.com:2443/bus/vls-server`；不要改用 workup 多租户入口。本文命令供板端接入，开发验收仅使用隔离存储、合成设备和内存库，未向该 VM 发起试写。

## 1. 升级范围与约束

- 更新 **VLS 后端与 VLS 前端**；WVP 的设备身份查询契约未改变，不需要为本需求更新 WVP。
- 数据表已有 `content_type/file_size/sha256` 和主动安全事件 JSON `video` 字段，不需要新增 Flyway 迁移。
- 图片继续支持 `image/jpeg`、`image/png`、`image/webp`；本次视频只支持完整 `video/mp4` 文件。推荐 H.264（可选 AAC 音频）以供浏览器播放；不转码，不承诺所有 MP4 内部编码均能在所有浏览器播放。
- 每条事件支持一张抓拍图片、一个 MP4，或两者一起。两者一起时分别申请上传地址、分别 PUT，然后放到同一 `payload.media` 数组；顺序不限。
- 图片默认上限 10 MiB，MP4 默认上限 100 MiB。后端属性 `vlstream.device-media.max-image-bytes`、`vlstream.device-media.max-video-bytes` 可独立调整，例如启动参数 `--vlstream.device-media.max-video-bytes=104857600`。签名有效期沿用原配置，默认 600 秒。
- 保持原设备入口开关和 WVP 身份/租户校验。未启用入口、未知设备、跨设备/租户媒体、被其他事件占用的媒体均拒绝；本需求不新增认证绕过。

## 2. 申请地址与 PUT

在设备上准备已经写完的 `test.mp4`，对**同一个完整文件**计算长度与真实 SHA-256。不要把“替换为该MP4文件实际的64位SHA256”等提示文字当成摘要。

```bash
API_BASE='https://vlstream.oortcloudsmart.com:2443/bus/vls-server'
DEVICE_ID='YOUR_REGISTERED_DEVICE_ID'
FILE='test.mp4'
SIZE=$(wc -c < "$FILE" | tr -d '[:space:]')
SHA256=$(sha256sum "$FILE" | awk '{print $1}')

jq -n --arg deviceId "$DEVICE_ID" --arg fileName "$FILE" \
  --arg contentType 'video/mp4' --argjson fileSize "$SIZE" --arg sha256 "$SHA256" \
  '{deviceId:$deviceId,fileName:$fileName,contentType:$contentType,fileSize:$fileSize,sha256:$sha256}' \
  > upload-request.json

curl --fail-with-body -sS -X POST "$API_BASE/vlsDeviceMedia/public/upload-url" \
  -H 'Content-Type: application/json' --data-binary @upload-request.json > upload-response.json
jq . upload-response.json
```

先检查业务 `code=200`，保存响应 `data.mediaId`、`data.objectKey`、`data.uploadUrl`、`data.requiredContentType`、`data.expiresAt`；HTTP 200 不替代业务码检查。

```bash
test "$(jq -r '.code' upload-response.json)" = 200 || exit 1
UPLOAD_URL=$(jq -r '.data.uploadUrl' upload-response.json)
CONTENT_TYPE=$(jq -r '.data.requiredContentType' upload-response.json)
curl --fail-with-body -X PUT -H "Content-Type: $CONTENT_TYPE" \
  --data-binary "@$FILE" "$UPLOAD_URL"
```

必须使用完整返回 URL，不能改 Host、协议、端口、bucket、路径或签名查询参数。PUT 只发送文件二进制，不使用 multipart/form-data，不传平台业务 Token，不需要存储 AccessKey/SecretKey。PUT HTTP 2xx 后才上报 MQTT；PUT 成功尚不表示事件接收成功。地址过期或 PUT 失败时重新申请，使用新的媒体 ID/对象键。

需要图片时，对抓拍文件重复上述步骤，`contentType` 使用真实的图片类型；图片与视频不得复用媒体 ID。

## 3. MQTT 绑定事件

沿用设备已经配置的 MQTT Broker、认证与 Topic ACL，QoS 1、Retain=false。Topic 为 `vlstream/v2.2/dev/{deviceId}/bus`，先订阅自己的 Topic 以接收业务回执。

下面用已上传的 MP4 构造**可直接序列化的真实摘要消息**：

```bash
MESSAGE_ID=$(cat /proc/sys/kernel/random/uuid)
EVENT_ID="event-$MESSAGE_ID"
EVENT_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)
MEDIA_ID=$(jq -r '.data.mediaId' upload-response.json)
OBJECT_KEY=$(jq -r '.data.objectKey' upload-response.json)

jq -n --arg messageId "$MESSAGE_ID" --arg eventId "$EVENT_ID" \
  --arg deviceId "$DEVICE_ID" --arg time "$EVENT_TIME" \
  --arg mediaId "$MEDIA_ID" --arg objectKey "$OBJECT_KEY" --arg sha256 "$SHA256" \
  '{protocolVersion:"2.2",messageId:$messageId,deviceId:$deviceId,sentAt:$time,
    msgDir:"dev2platform",mainBizType:"aiBiz",subBizType:"struct",
    payload:{eventId:$eventId,eventType:"person_detected",eventTime:$time,
      media:[{mediaId:$mediaId,objectKey:$objectKey,sha256:$sha256}]},extend:{}}' \
  > event-message.json
```

将该 JSON 交给设备已有 MQTT 发布器。若系统不提供上述 UUID 文件，可用设备自己的 UUID 生成器；不得在不同事件间重复使用 ID。

同时携带图片和视频时，数组形状如下。这里只展示引用占位形状，实际发送应填入两次申请的返回值及各文件真实摘要：

```json
"media": [
  {"mediaId":"图片申请返回的ID","objectKey":"图片申请返回的对象键","sha256":"图片真实64位摘要"},
  {"mediaId":"MP4申请返回的ID","objectKey":"MP4申请返回的对象键","sha256":"MP4真实64位摘要"}
]
```

平台根据媒体登记的 MIME 分类，不相信设备另外填写的 `mediaType` 或文件扩展名。每个对象均验证实际存在、对象 MIME、实际长度、实际 SHA-256；MP4 还检查文件标识、完整容器块、媒体数据和视频轨道，不进入图片解码器。全部校验与事件入库同一事务；任一失败整条事件拒绝，已做的数据库绑定回滚。

启用图片大模型复核的 `struct` 算法仍需抓拍图片，并按原协议带字符串 `algorithmId`。视频仅作事件证据；只有视频而缺少所需图片时明确失败，不绕过复核。图片与视频一起上报，复核使用图片；复核确认/人工确认生成正式事件时也保留视频引用。`faceEvent` 的既有路由保持不变，多租户独立人脸页面的视频呈现不在本单租户主动安全页面的验收范围。

## 4. 回执、预览与重试

只在回执 `payload.code=200` 且 `payload.bizData.status=SUCCESS` 后确认平台已受理；启用复核时 `SUCCESS` 表示进入复核队列，正式事件仍由复核结果决定。超时或 `FAILED` 保留本地记录，重试同一事件使用原 `messageId/eventId`，不要用新的 ID 重复制造事件。不要在成功后再追加第二次同 ID 的视频消息；图片和视频应一次合并上报。

主动安全列表和详情读取原 `video` 字段，将稳定 `vls-media://` 引用重签成短期私有地址，返回 `videoUrl/videoMediaId`；原 HTTP 视频地址继续兼容。原“录制视频”列点击播放；详情可播放和下载。下载按钮经登录 API `GET /vlsDeviceMedia/{mediaId}/download-url` 获取带附件响应头的短期地址。查看仍用 `GET /vlsDeviceMedia/{mediaId}/view-url`，未绑定及其他租户媒体拒绝。地址失效时刷新列表/重开详情，重新获取地址。

MP4 校验成功后，平台从校验过的临时文件保存仅服务端可写的证据副本，预览和下载只签该副本。即使设备在 PUT 地址有效期内再次覆盖原上传对象，也不能改变已绑定的证据内容。存储规划应计入原对象与证据副本（约两份 MP4），校验还需要每个并发任务最多一个文件上限的临时磁盘空间；不会把整个视频读入 Java 堆。

失败会关闭流并清理本次临时文件；沿用原媒体保留规则，不自动物理删除上传原件或未绑定的证据副本。失败或事务回滚的对象不签发给页面，设备可重试；对象保留/归档仍按现有运维策略。删除主动安全事件仍只逻辑删除主事件，原有关系清理不变，图片、视频和媒体登记保留。

## 5. 隔离验证证据

回归夹具为真实 H.264 MP4，64×64、1 秒、957 字节，SHA-256：

`dfd3fe51003014a5902aea9a94521c7bceaa06c42c4308a1a9d06661f63d770c`

该摘要只适用于仓库测试夹具 `ruoyi-vlstream/src/test/resources/event-media.mp4`，不能用于用户的 `test.mp4`。定向测试覆盖请求、MP4与三类图片、大小/MIME/哈希、伪造/截断视频、跨设备/租户、混合引用和事务回滚；可选 MinIO 集成测试只接受 `http://127.0.0.1:<端口>`，凭据固定为隔离专用测试值，不读取应用生产配置。构建与浏览器验证结果见本工作项 `RESULTS.md`，不代表已部署 VM 或真实板端验收。
