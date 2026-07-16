# WebRTC Streamer 移植总览

## 1. 项目用途

这个仓库本质上是一个 `WebRTC 传输/信令接入 SDK`，不是 AI 算法工程。

它提供的核心能力：

- WebRTC 实时音视频会话
- 信令注册/上线/呼叫
- DataChannel
- 内置 WebServer / WebSocket
- WHIP 实时推流
- 云端文件/实时流上传
- 本地 MP4 读写和远程回放辅助接口

它不提供的核心能力：

- 不负责 Sensor / MIPI / VI / ISP / VPSS
- 不负责你的主编码链路初始化
- 不内置目标检测、人脸识别之类 AI 算法

对你当前 Hi3519DV500 + IMX335 固件，正确定位就是：

`现有摄像头业务链路上的 WebRTC 输出层`

---

## 2. 能力边界结论

从 `include/webrtc_streamer.h`、`include/webrtc-mp4.h`、`example/main.c`、`doc/API*.md` 看，结论是：

1. 它主要是 WebRTC 传输/信令 SDK，不是采集 SDK。
2. 它没有内置 AI 识别算法。
3. 它需要你现有固件提供已经编码好的视频帧，典型是完整一帧 H.264/H.265 Annex-B 裸流。
4. 它适合嵌入现有摄像头业务，而不是强迫你另跑一套独立 demo。

最关键的输入接口就是：

- `webrtc_streamer_input_video_data(...)`
- `webrtc_streamer_input_audio_data(...)`
- `webrtc_streamer_input_audio_data_ex(...)`

这已经明确说明：它吃的是现成编码帧，不是原始图像。

---

## 3. 针对当前固件的最小移植方案

你的现状已经有：

- `VI -> ISP -> VPSS -> VENC`
- `VENC1 H.264 -> RTSP`

所以最小移植路径不是重写链路，而是：

`VENC1 取流线程 -> 同时送 RTSP 和 WebRTC`

即：

```text
IMX335
  -> VI / ISP / VPSS
  -> VENC1 (H.264 720p)
      -> RTSP sender
      -> WebRTC SDK input_video_data
```

当前代码已经按这个方案接入：

- 新增 `webrtc_bridge.c/.h`
- 在 `rtsp_streamer.c` 的 `venc_rtsp_thread()` 中，旁路一份 H.264 帧给 WebRTC SDK

这样做的优点：

- 不改驱动
- 不改内核
- 不新起一套采集链路
- 不破坏你现在已经调通的 RTSP 画面
- WebRTC 和 RTSP 共用同一条编码输出

---

## 4. 该 SDK 中真正需要保留的最小路径

### 必须保留

- `webrtc_streamer_init()`
- `webrtc_streamer_uninit()`
- `webrtc_streamer_input_video_data()`
- `webrtc_streamer_register_configuration_callback_fun()`
- `webrtc_streamer_register_authentication_callback_fun()`
- `webrtc_streamer_register_session_ask_iframe_callback_fun()`
- `webrtc_streamer_register_session_pli_callback_fun()`
- `webrtc_streamer_register_check_videocode_callback_fun()`

### 按需保留

- `webrtc_streamer_webserver_start()/stop()`
- `webrtc_streamer_register_webserver_api_callback_fun()`
- `webrtc_streamer_register_webserver_websocket_messaeg_callback_fun()`
- `webrtc_streamer_whip_publish_realtime_stream()`
- `webrtc_streamer_datachannel_send_message()`

### 当前可以先删掉/先不集成

- MP4 回放线程
- AVI/MP4 文件列表、下载逻辑
- 云文件上传
- Alexa / Matter
- 音频解码/重采样示例
- 示例里复杂的 DataChannel 业务 JSON 处理

---

## 5. 对你当前固件，应该接哪一个输出模块

应该接你现有的 `VENC 输出编码帧`，不是 VPSS，也不是原始 YUV。

当前最合适的接点就是：

- `rtsp_streamer.c`
- `venc_rtsp_thread()`
- `ss_mpi_venc_get_stream()` 之后拼好的整帧 `frame_buf`

这是最小风险接法。

---

## 6. H.264 / H.265 怎么接

### 当前推荐

先接 H.264。

原因：

- 你现有 RTSP 小码流已经是 H.264
- 浏览器和通用 WebRTC 终端对 H.264 兼容性通常最好
- 最适合作为第一阶段跑通视频

### 如果固件只有 H.265

也能接，但要注意：

- SDK 头文件明确支持 `WEBRTC_VIDEO_H265`
- 远端播放器/浏览器是否支持 H.265 要单独验证
- 集成点改成喂 `WEBRTC_VIDEO_H265`
- 最好把 `check_videocode_callback` 返回改成 H.265

如果只想先快速成功，优先保留一条 H.264 WebRTC 输出。

---

## 7. 库组合选择建议

仓库里有四种组合：

- `pcmu/mbedtls`
- `pcmu/openssl`
- `opus/mbedtls`
- `opus/openssl`

对你当前平台，建议优先：

`pcmu + mbedtls`

理由：

1. 你当前目标是先跑视频，音频后置，`pcmu` 依赖面最小
2. `mbedtls` 静态库链路更轻，通常比 OpenSSL 集成负担小
3. 现有 SDK 工具链和 sample 场景更容易先跑通

如果后面要更好的音频质量，再换到 `opus`。

---

## 8. 当前已完成的代码改动

### 新增文件

- `webrtc_bridge.c`
- `webrtc_bridge.h`
- `webrtc_webcam.conf.example`
- `WEBRTC_PORTING.md`

### 修改文件

- `rtsp_streamer.c`
- `Makefile`

当前桥接逻辑：

1. 从 `/mnt/webrtc/webcam.conf` 读取 `initstring/serno/serveraddr`
2. 从 `/mnt/webrtc/webrtc.configuration` 读取 SDK 运行时缓存配置
3. 初始化 WebRTC SDK
4. 注册最小必要回调
5. 如存在 `www/cert/key` 则启动内置 WebServer
6. 在 `venc_rtsp_thread()` 里把 H.264 帧喂给 `webrtc_streamer_input_video_data()`
7. 收到 WebRTC 关键帧请求时，对当前 VENC 通道触发 `ss_mpi_venc_request_idr()`
8. 通过 `Message / DataChannel / Web API / WebSocket` 暴露轻量控制命令
9. 记录会话状态、网络质量、信令错误和发送队列状态，供页面和调试接口查询

---

## 9. 运行时依赖清单

板端至少需要：

- `/mnt/rtsp_streamer`
- `/mnt/webrtc/webcam.conf`

如果要启用内置 WebServer，再加：

- `/mnt/webrtc/www/`
- `/mnt/webrtc/cert.pem`
- `/mnt/webrtc/priv.key`

SDK 运行时缓存会写：

- `/mnt/webrtc/webrtc.configuration`

---

## 10. 当前已补的控制面功能

参考 `example/main.c`，当前桥接层已经补上这些适合板端实时流场景的能力：

- `signaling message callback`
- `datachannel open/message callback`
- `signaling socket error callback`
- `network quality callback`
- `get network info callback`
- `call state / queue / session info 状态缓存`

当前支持的轻量命令入口：

- Web API:
  - `/api/webrtc/status`
  - `/api/webrtc/sessioninfo`
  - `/api/webrtc/command`
- 内置 WebSocket:
  - 文本命令同 `/api/webrtc/command`
- DataChannel:
  - 文本命令同 `/api/webrtc/command`

当前支持的命令：

- `ping`
- `get_status`
- `get_session_info`
- `request_iframe`
- `close_session`
- `event:doorbell`
- `event:pir`
- `event:custom`
- `stream:main`
- `stream:sub`

也支持简单 JSON 形式：

- `{"cmd":"ping"}`
- `{"cmd":"get_status"}`
- `{"cmd":"get_session_info"}`
- `{"cmd":"request_iframe"}`
- `{"cmd":"close_session"}`
- `{"cmd":"event_call","event":"doorbell"}`
- `{"cmd":"switch_stream","stream":"main"}`

---

## 11. 仍然没有移植的功能

这些功能在参考工程里存在，但当前板端版本仍然故意不接：

- 音频采集 / 编码 / `input_audio_data`
- 远程回放 MP4/AVI 线程
- DataChannel 文件列表 / 下载 / 回放业务
- 云文件上传 / 云实时流发布
- WHIP 推流
- Alexa / Matter 定制流程

原因很简单：这些模块都需要引入额外业务线程、存储格式、音频链路或云端协议，已经超出你现在这条“实时摄像头推流固件”的最小闭环范围。

---

## 10. 证书和 WebServer 放置建议

建议板端目录：

```text
/mnt/webrtc/
  webcam.conf
  webrtc.configuration
  cert.pem
  priv.key
  www/
```

其中：

- `webcam.conf`：初始注册参数
- `webrtc.configuration`：SDK 回写缓存，掉电后继续使用
- `cert.pem/priv.key`：内置 HTTPS WebServer
- `www/`：前端页面资源

如果你已经有自己的业务 WebServer，可以完全不启用 SDK 内置 WebServer。

---

## 11. 编译建议

当前代码默认按 `pcmu/mbedtls` 组合链接外部静态库。

构建命令继续用你原有交叉链：

```sh
export PATH="$HOME/Desktop/hi3519dv500+IMX335/gcc-20230609-aarch64-v01c01-linux-musl/aarch64-v01c01-linux-musl-gcc/bin:$PATH"
export LC_ALL=C
export LANG=C
make -C "$HOME/Desktop/hi3519dv500+IMX335/Hi3519DV500_SDK_V1.0.1.0/smp/a55_linux/source/mpp/sample/rtsp_streamer"
```

---

## 12. 常见坑

1. `webcam.conf` 缺失  
   结果：RTSP 正常，WebRTC bridge 自动禁用。

2. `initstring/serno/serveraddr` 不对  
   结果：`webrtc_streamer_init()` 失败。

3. 没有完整 I 帧  
   结果：浏览器端首屏慢或不出图。  
   当前桥接已接入 `request IDR` 回调。

4. 只喂 H.265 给不支持 H.265 的浏览器  
   结果：WebRTC 协商成功但不出画面。  
   第一阶段优先 H.264。

5. WebServer 证书路径不存在  
   结果：内置 WebServer 不启动，但实时 WebRTC 主链路仍可继续。

6. 直接照搬 demo  
   风险：把本地回放、DataChannel 文件业务、云上传全拖进现有固件，复杂度陡增。  
   当前方案已经避开这个坑。

---

## 13. 最小可实施方案

你现在真正该走的版本就是：

1. 保留现有 `rtsp_streamer` 主链路
2. 把 `/mnt/webrtc/webcam.conf` 放到板端
3. 先不启音频
4. 先不启 DataChannel 复杂业务
5. 先不启云上传
6. 先验证 WebRTC 视频是否能正常出图

确认视频稳定后，再决定第二阶段是否加：

- 音频
- DataChannel 控制
- WHIP
- 云上传
