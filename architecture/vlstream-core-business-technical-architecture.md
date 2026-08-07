# VLStream 核心业务时序图

> 本图按“硬件生命周期 + 平台交互”展开，参与者分为硬件、平台 Server 和客户端 Client 三类。
>
> 视频主链路为 WVP 对接 ZLMediaKit，VLS 负责设备运营、事件、模型和平台业务。

```mermaid
sequenceDiagram
    autonumber

    participant Line as 产线烧录/配置工具
    participant H as IPC / BOX / NVR
    participant C as Client<br/>VLStream-ui / WVP UI
    participant V as VLS Server<br/>设备与平台运营
    participant M as MQTT Broker / EMQX
    participant W as WVP Server<br/>GB28181 / ONVIF / RTSP
    participant Z as ZLMediaKit<br/>RTP / REST / Hook
    participant D as MySQL / Redis
    participant O as MinIO / S3

    rect rgb(255, 248, 235)
        Note over Line,H: 1. 硬件生产：写入设备身份和平台接入配置
        Line->>H: 写入设备 ID、设备密钥
        Line->>H: 写入 MQTT 地址、凭证和基础配置
        H-->>Line: 烧录与配置校验完成
        H->>M: 使用预置配置建立 MQTT 连接
        M-->>V: 转发设备上线/身份消息
        V->>D: 保存设备身份和接入状态
        D-->>V: 保存成功
    end

    rect rgb(239, 246, 255)
        Note over C,H: 2. 硬件初始化、安装与使用：注册设备并启用视频协议
        C->>V: 初始化设备、注册设备或获取接入配置
        V->>D: 校验设备身份、租户和设备状态
        D-->>V: 返回设备配置
        V->>M: 下发初始化参数、订阅和控制配置
        M->>H: MQTT 初始化/控制消息
        H-->>M: 初始化回执

        alt GB28181 / SIP
            H->>W: SIP 注册、心跳和设备目录
            C->>W: 请求实时预览或录像回放
            W->>H: SIP INVITE / 点播 / 回放控制
            H->>Z: RTP 媒体流
            W->>Z: REST API、Hook 和 RTP 协调
            Z-->>C: WebRTC / HTTP-FLV / HLS / RTSP 播放流
        else RTSP / ONVIF
            C->>W: 设备发现、拉流或设备控制
            W->>H: ONVIF / RTSP 请求
            H->>Z: RTSP / RTP 媒体流
            W->>Z: REST API、Hook 和流状态同步
            Z-->>C: WebRTC / HTTP-FLV / HLS / RTSP 播放流
        end
    end

    rect rgb(240, 253, 244)
        Note over C,H: 3. 硬件运营接入：硬件消息进入平台，平台功能完成业务处理

        C->>V: 设备管理、用户绑定、在线状态查询
        V->>D: 查询设备、租户和用户关系
        D-->>V: 返回设备运营状态
        V-->>C: 返回设备列表、绑定状态和在线状态

        H->>M: 心跳、事件、运行状态和模型回执
        M-->>V: 转发设备消息
        V->>D: 记录状态、事件和业务处理结果
        V-->>C: 推送或返回事件、告警和任务状态

        C->>V: 下发设备控制或模型任务
        V->>M: 发布控制指令或模型任务
        M->>H: MQTT 控制/模型消息
        H-->>M: 执行回执
        M-->>V: 转发执行结果
        V-->>C: 返回控制或模型任务状态

        opt 事件图片或其他媒体
            H->>V: 请求事件媒体上传地址
            V->>O: 生成预签名 PUT 地址
            O-->>V: 返回预签名地址
            V-->>H: 返回短时有效上传地址
            H->>O: PUT 事件图片/媒体文件
            H->>M: 上报事件媒体标识和业务信息
            M-->>V: 转发事件媒体消息
            V->>D: 保存事件与媒体对象关联
        end

        opt 模型文件下载
            V-->>H: 下发短期签名模型下载 URL
            H->>O: HTTP/HTTPS 下载模型文件
            O-->>H: 返回模型文件
        end
    end

    rect rgb(254, 242, 242)
        Note over C,H: 4. 硬件转让：平台解绑并恢复待绑定状态
        C->>V: 发起设备解绑/转让
        V->>D: 校验当前用户、租户和设备关系
        D-->>V: 校验通过
        V->>M: 清理绑定关系并下发重置指令
        M->>H: 清除原用户关系、恢复待绑定状态
        H-->>M: 解绑/重置执行回执
        M-->>V: 转发解绑回执
        V->>D: 清理用户绑定和设备运营关系
        V-->>C: 返回解绑/转让完成
    end
```

## Server 依赖清单

> 统计口径：以当前项目的 `deploy/release/compose.yaml`、后端 Compose 配置以及 WVP 配置为准。版本号优先采用已经写入镜像或项目配置的版本；“未固定”表示代码依赖该服务能力，但当前仓库没有锁定具体部署版本。

### 核心业务链路

| 名称 | 用途 | 版本号 | 授权协议 |
| --- | --- | --- | --- |
| VLStream Server（VLS） | 当前项目业务后端：设备注册、用户绑定、设备运营、事件处理、模型任务和平台 API | 源码 Maven `1.1.3`；Spring Boot `2.7.11`；Java `8`；发布镜像默认 `1.1.3` | [MIT](../LICENSE)（项目自有代码；第三方依赖另行遵循其协议） |
| WVP Server | 独立视频业务后端：GB28181/SIP、ONVIF、RTSP、设备目录、实时预览、回放、云台控制，并负责协调 ZLMediaKit | 项目 `3.8.9`；Spring Boot `2.7.18`；Java `8` | [MIT](https://gitee.com/xiaochemgzi/RuoYi-Wvp/blob/master/LICENSE)（项目自有代码；第三方依赖另行遵循其协议） |
| ZLMediaKit | WVP 依赖的流媒体服务器：RTP 收流、媒体流管理、REST/Hook、WebRTC/HTTP-FLV/HLS/RTSP 等播放输出 | WVP 配置中未固定；部署时应显式锁定镜像或源码 Tag | [MIT](https://docs.zlmediakit.com/zh/more/license.html)（自有代码；第三方依赖另行遵循其协议） |
| MQTT Broker / EMQX | 硬件与 VLS 之间的消息通道：连接、心跳、事件上报、控制下发、模型任务和执行回执 | `5.4`（现有后端 Compose 固定；发布 Compose 作为外部服务接入） | [Apache-2.0](https://github.com/emqx/emqx-docker/blob/main/LICENSE)（EMQX `5.9.0+` 已变更为 BSL，不适用于当前 `5.4`） |
| MySQL | 业务主数据库：设备、租户、用户绑定、事件、任务、系统配置和审计数据 | `8.4.10-oraclelinux9`（当前发布 Compose；旧脚本为 `8.0.31`） | [GPLv2 或 Oracle 商业许可](https://dev.mysql.com/doc/refman/8.4/en/what-is-mysql.html) |
| Redis | 缓存、登录会话、在线状态、临时数据及 WVP 的运行态缓存 | `7.4.9-alpine`（当前发布 Compose；旧脚本为 `6.2.7`） | [RSALv2 或 SSPLv1](https://redis.io/legal/licenses/) |
| MinIO / S3 Object Storage | 事件图片/媒体、模型文件及其他对象的上传、下载和预签名地址 | `RELEASE.2025-09-07T16-13-09Z`（当前发布 Compose；旧脚本为 `RELEASE.2023-03-24T21-41-23Z`） | [AGPLv3 或商业许可](https://min.io/compliance) |

### 部署支撑和按场景启用

| 名称 | 用途 | 版本号 | 授权协议 |
| --- | --- | --- | --- |
| 前端静态文件/网关 Server（通常为 Nginx） | 托管 VLStream 前端静态资源，反向代理后端 API、WebSocket 及 WebRTC 路径 | 当前发布前端镜像为 `vlstream-frontend:1.1.3`，内部 Web Server 版本未在仓库公开；旧 Compose 固定 `nginx:1.22.1` | [2-clause BSD-like](https://nginx.org/en/docs/faq/license_copyright.html) |
| WebRTC Streamer | VLS 直连 RTSP 到 WebRTC 的可选播放链路；不是 WVP 的核心流媒体服务器 | `v0.8.16`（当前发布 Compose） | [Unlicense](https://github.com/mpromonet/webrtc-streamer/blob/v0.8.16/UNLICENSE)；其内含 WebRTC、civetweb、live555 等依赖还需分别遵循对应协议 |
| FFmpeg | WVP/ZLMediaKit 的按需拉流、转码、截图或格式转换辅助程序；不是独立的媒体平台 | 未固定，由部署环境提供 | [LGPLv2.1+；启用 GPL 部件时为 GPLv2+](https://ffmpeg.org/legal.html) |
| 外部 AI/训练推理服务 | 算法训练、模型转换、推理或模型产物生成；当前时序图只画了模型任务下发和对象存储，不把该服务作为核心视频链路 | 未在本仓库固定 | 外部服务协议，待具体部署确认 |

### 依赖结论

- **核心运行依赖为 7 类**：VLS、WVP、ZLMediaKit、MQTT/EMQX、MySQL、Redis、MinIO。
- **前端部署**通常还需要 Nginx 或等价的静态文件/网关 Server。
- **当前发布 Compose 还显式包含 WebRTC Streamer**，但它只服务 VLS 的直连 WebRTC 播放场景；如果统一走 ZLMediaKit 的 WebRTC 输出，可将它作为可选项。
- **WVP 的核心媒体服务器就是 ZLMediaKit**。当前 WVP 代码中存在 FFmpeg 拉流接口，但没有发现另一套必须独立部署的通用流媒体服务器。
- **版本统一建议**：正式部署前补齐 ZLMediaKit、FFmpeg、EMQX 和前端容器内部 Web Server 的明确 Tag，并将 WVP 与 VLS 的镜像版本写入同一份发布清单。

## 参与者职责

- 硬件：IPC、BOX、NVR，负责设备身份、协议接入、心跳、事件、控制执行和模型回执。
- VLS Server：负责设备注册、用户绑定、设备运营、事件业务、模型任务和平台数据管理。
- MQTT Broker：负责硬件与 VLS 之间的消息传递，不承载设备管理业务规则。
- WVP Server：负责 GB28181、ONVIF、RTSP 等视频设备接入、点播、回放和视频控制。
- ZLMediaKit：负责 RTP 接收、媒体流管理和 WebRTC/HTTP-FLV/HLS/RTSP 等播放输出。
- Client：VLStream 前端承载平台运营功能，WVP 前端承载视频预览、回放、云台和通道管理。
