<div align="center">
  <img src="./VLStream-Web/VLStream-ui/src/assets/img/img.png" alt="VLStream Cloud" width="160">

  <h1>VLStream Cloud</h1>

  <p><strong>AI 驱动的开源视频物联网与智能流媒体管理平台</strong></p>

  <p>
    <strong>简体中文</strong> |
    <a href="./README.md">English</a>
  </p>

  <p>
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/stargazers"><img src="https://img.shields.io/github/stars/OortCloudGroup/VLStream-Cloud?style=flat-square" alt="GitHub Stars"></a>
    <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="MIT License"></a>
    <img src="https://img.shields.io/badge/Java-8-orange.svg?style=flat-square" alt="Java 8">
    <img src="https://img.shields.io/badge/Spring%20Boot-2.7.11-6DB33F.svg?style=flat-square" alt="Spring Boot 2.7.11">
    <img src="https://img.shields.io/badge/Vue-3.3-42B883.svg?style=flat-square" alt="Vue 3.3">
  </p>

  <p>
    <a href="#-快速开始">快速开始</a> •
    <a href="#-核心特性">核心特性</a> •
    <a href="#-系统截图">系统截图</a> •
    <a href="#-应用场景">应用场景</a> •
    <a href="#-架构与项目结构">架构</a> •
    <a href="#-技术栈">技术栈</a> •
    <a href="#-部署">部署</a> •
    <a href="#-帮助与支持">帮助</a>
  </p>
</div>

---

> [!IMPORTANT]
> **线上环境地址：** [https://vlstream.oortcloudsmart.com:2443/bus/vls-ui/login](https://vlstream.oortcloudsmart.com:2443/bus/vls-ui/login)
> **默认账号：** `admin` / `Codex@123456`
> 这是当前线上环境地址。首次登录后请立即修改默认密码。

---

## 📖 项目介绍

VLStream Cloud 是面向设备与视频流管理、智能视频分析、算法全生命周期、监控和告警场景的开源视频物联网平台。项目由 Vue 管理控制台和 Spring Boot 多模块后端组成，并提供工作流、权限、任务调度、对象存储等企业级视频应用所需的平台能力。

> [!IMPORTANT]
> 请仅接入已获得合法授权的设备和视频流，并确保部署方式以及智能分析功能的使用符合适用的隐私、安全和数据保护要求。

---

## ✨ 核心特性

| 特性 | 说明 |
| --- | --- |
| 视频设备管理 | 设备注册、分组、标签、状态监控、连接测试、云台控制和流地址获取 |
| 多协议播放 | 面向常见视频物联网场景的 Web 视频播放与低延迟流媒体能力 |
| 智能分析 | 分析请求、实时任务监控、结果管理和事件治理 |
| 算法全生命周期 | 算法仓库、训练任务、标注数据、模型管理，以及 Hi3519DV500 OM 模型转换与设备下发 |
| 工作流自动化 | 基于 Flowable 的流程定义、部署、任务和审批 |
| 企业级权限 | Sa-Token 身份认证、RBAC、数据权限、用户和角色管理 |
| 平台服务 | 定时任务、对象存储、短信、系统监控和 XXL-Job 支持 |
| 可视化运营 | Vue 3 管理控制台、数据看板、GIS、通用 CRUD 组件和多画面视频布局 |

---

### 单节点 GPU 训练调度

算法训练支持一台物理 GPU 服务器上的单卡独占队列。训练开始时按需创建 Docker
容器，GPU 忙时任务自动排队，训练结束后删除容器并保留任务记录、日志和模型产物。
部署和环境变量说明见
[单节点 GPU 训练调度](./VLStream-Cloud-Backend-Server/vls-stream/doc/gpu-training-scheduler.md)。

---

### Hi3519DV500 模型下发

平台支持通过 MQTT 向硬件下发训练模型。硬件连接、模型下发、事件上报、媒体上传、
状态回执和联调验收统一以
[VLS 平台与摄像头统一通信协议](./VLStream-Cloud-Backend-Server/vls-stream/doc/VLS-Protocol.md)
为准。

部署时需要配置以下环境变量：

```bash
VLSTREAM_MQTT_HOST=127.0.0.1
VLSTREAM_MQTT_PORT=1883
VLSTREAM_MQTT_USERNAME=vlstream
VLSTREAM_MQTT_PASSWORD=replace-me
VLSTREAM_MODEL_PUBLIC_BASE_URL=https://vlstream.example.com
VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET=replace-with-a-long-random-secret
```

`VLSTREAM_MODEL_PUBLIC_BASE_URL` 必须是硬件设备能够访问的后端地址，不是浏览器访问的前端地址。设备下载入口不要求平台登录令牌，但每个任务都使用短期 HMAC 签名 URL。
`VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET` 由部署方自行生成，只保存在 VLStream 后端，
不下发给摄像头，也不能在不同环境之间复用。PowerShell 可使用以下命令生成 32 字节随机密钥：

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

本地使用 IDEA 启动后端时，在 `VLStream Backend` 运行配置的“环境变量”中加入生成后的
`VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET`。生产环境应通过部署平台的 Secret 或环境变量注入，
不要把实际密钥写入 `.run`、YAML、README 或 Git。

---

## 🖥️ 系统截图

<table>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/01-active-safety-events.png"><img src="./assets/screenshots/01-active-safety-events.png" alt="主动安全事件管理" width="100%"></a><br>
      <strong>主动安全事件管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/02-event-feedback-workflow.png"><img src="./assets/screenshots/02-event-feedback-workflow.png" alt="事件反馈与流程处理" width="100%"></a><br>
      <strong>事件反馈与流程处理</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/03-work-order-management.png"><img src="./assets/screenshots/03-work-order-management.png" alt="工单管理" width="100%"></a><br>
      <strong>工单管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/04-workflow-designer.png"><img src="./assets/screenshots/04-workflow-designer.png" alt="可视化流程设计" width="100%"></a><br>
      <strong>可视化流程设计</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/05-algorithm-training-management.png"><img src="./assets/screenshots/05-algorithm-training-management.png" alt="算法训练管理" width="100%"></a><br>
      <strong>算法训练管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/06-algorithm-training-console.png"><img src="./assets/screenshots/06-algorithm-training-console.png" alt="算法训练控制台" width="100%"></a><br>
      <strong>算法训练控制台</strong>
    </td>
  </tr>
</table>

> 点击任意截图可查看完整分辨率原图。

---

## 🌐 应用场景

<table>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/01-chemical-production-safety.jpg" alt="Chemical production safety" width="100%"><br>
      <strong>化工生产安全</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/02-smart-water-conservancy.jpg" alt="Smart water conservancy" width="100%"><br>
      <strong>智慧水利</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/03-wastewater-treatment.jpg" alt="Wastewater treatment" width="100%"><br>
      <strong>污水处理</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/04-smart-construction-site.jpg" alt="Smart construction site" width="100%"><br>
      <strong>智慧工地</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/05-smart-community.jpg" alt="Smart community" width="100%"><br>
      <strong>智慧社区</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/06-gas-station-safety.jpg" alt="Gas station safety" width="100%"><br>
      <strong>加油站安全监管</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/07-smart-kitchen.jpg" alt="Smart kitchen" width="100%"><br>
      <strong>智慧后厨</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/08-smart-campus.jpg" alt="Smart campus" width="100%"><br>
      <strong>智慧校园</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/09-smart-city-management.jpg" alt="Smart city management" width="100%"><br>
      <strong>智慧城管</strong>
    </td>
  </tr>
</table>

---

## 🧰 技术栈

### 后端

| 分类 | 技术 |
| --- | --- |
| 运行环境 | Java 8 |
| 基础框架 | Spring Boot 2.7.11、RuoYi-Flowable-Plus 0.8.3 |
| 数据访问 | MyBatis-Plus 3.5.3.1 |
| 身份认证 | Sa-Token 1.34.0 |
| 工作流 | Flowable 6.8.0 |
| 缓存与锁 | Redis、Redisson 3.20.1、Lock4j |
| API 文档 | Springdoc OpenAPI、Knife4j |
| 构建工具 | Maven 3.6+ |

### 前端

| 分类 | 技术 |
| --- | --- |
| 基础框架 | Vue 3.3、Vue Router 4 |
| 构建工具 | Vite 4.4 |
| UI 组件 | Element Plus 2.3、Avue 3.7 |
| 状态管理 | Pinia 2.1 |
| 视频播放 | hls.js、xgplayer |
| GIS | Leaflet 1.9 |
| HTTP | Axios 1.4 |

---

## 🏗️ 架构与项目结构

VLStream Cloud 的核心业务架构分为三大类：

- **硬件：** IPC、BOX、NVR，覆盖产线写入、初始化安装与协议接入、平台运营以及设备转让。
- **平台 Server：** VLS 负责设备运营和平台业务；WVP 负责视频设备接入和视频控制；ZLMediaKit 是 WVP 依赖的流媒体服务器；MQTT、MySQL、Redis、MinIO 分别提供消息、持久化、缓存和对象存储能力。
- **客户端：** VLStream-ui 承载平台运营功能，WVP UI 承载视频预览、回放、云台和通道管理。

完整的生命周期时序图和依赖清单见
[核心业务技术架构文档](./architecture/vlstream-core-business-technical-architecture.md)。

```mermaid
sequenceDiagram
    autonumber
    participant P as 产线烧录/配置工具
    participant H as 硬件<br/>IPC / BOX / NVR
    participant C as 客户端<br/>VLStream-ui / WVP UI
    participant V as VLS Server
    participant M as MQTT Broker<br/>EMQX
    participant W as WVP Server
    participant Z as ZLMediaKit
    participant D as MySQL / Redis
    participant O as MinIO / S3

    rect rgb(255, 248, 235)
        Note over P,H: 1. 硬件生产：写入设备身份和平台接入配置
        P->>H: 写入设备 ID、密钥、MQTT 地址和基础配置
        H->>M: 使用预置身份建立连接
        M-->>V: 转发设备身份和上线消息
        V->>D: 保存身份和设备状态
    end

    rect rgb(239, 246, 255)
        Note over C,H: 2. 初始化、安装与视频接入
        C->>V: 初始化或注册设备
        V->>M: 下发初始化和控制配置
        M->>H: MQTT 配置/控制消息
        alt GB28181 / SIP
            H->>W: SIP 注册、心跳和设备目录
            C->>W: 请求预览或回放
            W->>H: SIP INVITE / 回放控制
            H->>Z: RTP 媒体流
        else RTSP / ONVIF
            C->>W: 设备发现、拉流或设备控制
            W->>H: ONVIF / RTSP 请求
            H->>Z: RTSP / RTP 媒体流
        end
        W->>Z: REST API、Hook 和流协同
        Z-->>C: WebRTC / HTTP-FLV / HLS / RTSP 播放流
    end

    rect rgb(240, 253, 244)
        Note over C,H: 3. 平台运营与硬件交互
        C->>V: 设备管理、用户绑定和状态查询
        H->>M: 心跳、事件、状态和模型回执
        M-->>V: 转发硬件消息
        V->>D: 保存业务状态和事件结果
        C->>V: 下发控制或模型任务
        V->>M: 发布控制指令或模型任务
        M->>H: MQTT 指令
        H-->>M: 执行回执
        M-->>V: 转发执行结果
        V->>O: 保存或读取事件媒体和模型文件
    end

    rect rgb(254, 242, 242)
        Note over C,H: 4. 硬件转让：解绑并恢复待绑定状态
        C->>V: 发起解绑或转让
        V->>M: 清理绑定并重置设备
        M->>H: 恢复待绑定状态
        H-->>M: 重置回执
        M-->>V: 转发回执
        V->>D: 清理用户与设备关系
    end
```

### Server 运行依赖

以下版本优先取当前发布 Compose 或项目配置。标记为“未固定”的组件，
正式发布前需要在部署清单中锁定版本。

| 名称 | 用途 | 版本号 | 授权协议 |
| --- | --- | --- | --- |
| VLStream Server（VLS） | 设备注册、用户绑定、事件、模型任务和平台 API | Maven `0.8.3`；Spring Boot `2.7.11`；发布镜像 `1.1.2` | [MIT](./LICENSE) |
| WVP Server | GB28181/SIP、ONVIF、RTSP、预览、回放、云台和视频控制 | `3.8.9`；Spring Boot `2.7.18` | [MIT](https://gitee.com/xiaochemgzi/RuoYi-Wvp/blob/master/LICENSE) |
| ZLMediaKit | RTP 收流、媒体管理、REST/Hook 和播放输出 | WVP/VLStream 仓库中**未固定** | [MIT](https://docs.zlmediakit.com/zh/more/license.html) |
| MQTT Broker / EMQX | 设备消息、心跳、事件、指令和模型回执 | `5.4`；发布 Compose 作为外部服务接入 | [Apache-2.0](https://github.com/emqx/emqx-docker/blob/main/LICENSE) |
| MySQL | 业务数据库 | `8.4.10-oraclelinux9` | [GPLv2 或商业许可](https://dev.mysql.com/doc/refman/8.4/en/what-is-mysql.html) |
| Redis | 缓存、会话、在线状态和运行态数据 | `7.4.9-alpine` | [RSALv2 或 SSPLv1](https://redis.io/legal/licenses/) |
| MinIO / S3 | 事件媒体、模型文件和对象存储 | `RELEASE.2025-09-07T16-13-09Z` | [AGPLv3 或商业许可](https://min.io/compliance) |

前端部署通常还需要 Nginx 或等价网关，用于静态文件和反向代理。WebRTC
Streamer `v0.8.16` 仅用于 VLS 直连 RTSP 转 WebRTC 的可选链路；FFmpeg 是
WVP/ZLMediaKit 的按需拉流和格式转换辅助程序，不是另一套独立流媒体平台。

### 仓库分层

下表中的后端目录均相对于
`VLStream-Cloud-Backend-Server/vls-stream/`。

| 层次 | 主要目录 | 职责 |
| --- | --- | --- |
| 运营客户端 | `VLStream-Web/VLStream-ui/` | 仪表盘、设备与流媒体管理、AI 操作、工作流和系统管理 |
| 设备客户端 | `sdk/` | 摄像机侧 RTSP/WebRTC 推流、AI 推理、事件上报和模型更新 |
| 应用服务 | `ruoyi-admin/`、`ruoyi-vlstream/` | API 入口与 VLStream 领域服务 |
| 平台服务 | `ruoyi-common/`、`ruoyi-framework/`、`ruoyi-system/`、`ruoyi-flowable/`、`ruoyi-job/`、`ruoyi-oss/`、`ruoyi-sms/`、`ruoyi-extend/` | 公共基础设施、认证授权、工作流、任务、存储、消息和监控 |
| 运维与文档 | `deploy/`、`docs/`、后端 `deploy/` 和 `script/` | 容器部署、数据库初始化、迁移支持、协议和运维文档 |

### 顶层目录

```text
VLStream-Cloud/
├── VLStream-Cloud-Backend-Server/
│   └── vls-stream/                  # Java 8 / Spring Boot Maven 多模块工程
│       ├── ruoyi-admin/             # 可执行应用与 REST API
│       ├── ruoyi-vlstream/          # 设备、流媒体、AI、事件和模型
│       ├── ruoyi-system/            # 用户、角色、权限和系统服务
│       ├── ruoyi-framework/         # Web、安全与框架配置
│       ├── ruoyi-flowable/          # 工作流与审批服务
│       ├── ruoyi-common/            # 公共模型、工具和基础组件
│       ├── ruoyi-generator/         # 代码生成
│       ├── ruoyi-job/               # 定时任务
│       ├── ruoyi-oss/               # 对象存储集成
│       ├── ruoyi-sms/               # 短信集成
│       ├── ruoyi-extend/            # 监控与 XXL-Job 服务
│       ├── ruoyi-demo/              # 示例与集成测试
│       ├── deploy/                  # 后端部署资源
│       └── script/                  # 数据库与 Docker 脚本
├── VLStream-Web/
│   └── VLStream-ui/                 # Vue 3 管理控制台
├── sdk/                             # Hi3519DV500 原生摄像机业务 SDK
├── deploy/                          # 仓库级部署资源
├── docs/                            # 仓库级文档
├── assets/                          # 截图和应用场景图片
├── tools/                           # 开发与校验工具
├── LICENSE
├── README.md                        # 英文文档（默认）
└── README.zh-CN.md                  # 简体中文文档
```

### 设备 SDK（`sdk/`）

`sdk/` 是摄像机侧原生组件，不属于 Maven 或 npm 模块。它导出了 Hi3519DV500 板卡上
用于构建 `rtsp_streamer` 可执行文件的业务源码，依赖原始海思 MPP/ACL SDK、交叉编译
工具链和外部 WebRTC Streamer SDK。

| 区域 | 内容 |
| --- | --- |
| 媒体管线 | `src/rtsp_streamer.c`、`rtsp_lib/`：RTSP 输入、帧处理和推流编排 |
| WebRTC 桥接 | `src/webrtc_bridge.c`、`include/webrtc_bridge.h`：WebRTC 生命周期、会话、编码头和关键帧控制 |
| AI 运行时 | `src/ai_bridge.cpp`、`src/ai_acl_adapter.cpp`、`src/ai_runtime_config.cpp`：ACL 推理、OM 模型校验/热切换和运行时配置 |
| 平台集成 | `src/http_reporter.cpp`、`src/model_receiver.cpp`：异步事件/JPEG 上报和 HTTP 模型接收 |
| 配置与示例 | `config/`、`examples/`：板端配置、类别标签和 MQTT 模型下发示例 |
| 依赖与说明 | `third_party/`、`docs/`、`Makefile`：外部声明、移植说明、调试记录和板端构建规则 |

SDK 与服务端构建相互独立：根目录 Maven 和前端命令不会编译它。依赖条件、原始工程路径、
不包含的厂商二进制文件以及板端构建说明，请查看 [SDK 指南](./sdk/README.md)。

---

## 🚀 快速开始

### 环境要求

| 组件 | 要求 |
| --- | --- |
| Java | JDK 8 |
| Maven | 3.6+ |
| 数据库 | MySQL 5.7+ |
| 缓存 | Redis |
| 对象存储 | MinIO 或其他 S3 兼容服务；完整算法标注功能必需 |
| 消息服务 | MQTT Broker；设备控制和模型下发必需 |
| 训练节点 | 支持 SSH/SFTP 的 Linux GPU 服务器；算法训练功能必需 |
| GPT 服务 | 能由 APaaS 网关路由的 `apaas-ai` 服务；AI 文本/图片功能必需 |
| 前端 | Node.js 与 npm |

### WebRTC 实时预览依赖

浏览器不能直接播放 RTSP；摄像头实时预览依赖 WebRTC Streamer 将 RTSP 转换为 WebRTC。
本项目固定使用并已验证的 Docker 镜像为 **`mpromonet/webrtc-streamer:v0.8.16`**。
请保持此精确标签，不要使用未验证的 `latest` 或旧版 Windows 二进制。

本地独立启动示例：

```powershell
docker run -d --name vlstream-webrtc --restart unless-stopped -p 8000:8000 `
  mpromonet/webrtc-streamer:v0.8.16 -H 0.0.0.0:8000 -vvv
```

启动后执行 `curl.exe http://127.0.0.1:8000/api/version`，应返回
`v0.8.16/Linux-x86_64`。后端配置位于
`ruoyi-admin/src/main/resources/application.yml`：

```bash
VLSTREAM_WEBRTC_ENABLED=true
VLSTREAM_WEBRTC_RUNTIME_IMAGE=mpromonet/webrtc-streamer:v0.8.16
VLSTREAM_WEBRTC_INTERNAL_URL=http://127.0.0.1:8000
VLSTREAM_WEBRTC_PUBLIC_URL=/bus/webrtc-streamer-server
```

容器化发布使用 `deploy/release/.env` 中同一个版本的
`WEBRTC_STREAMER_IMAGE=mpromonet/webrtc-streamer:v0.8.16`。`runtime-image` 仅用于后端声明
和状态展示，后端不会自行拉取或启动 Docker 容器。

### 1. 克隆项目

```powershell
git clone https://github.com/OortCloudGroup/VLStream-Cloud.git
cd VLStream-Cloud
```

### 2. 初始化与升级数据库

```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
mysql -u root -p vlstream --execute="source script/sql/mysql/mysql_ry_v0.8.X.sql"
```

`script/sql/` 下还提供了 Oracle、PostgreSQL 和 SQL Server 的初始化脚本。
后续表结构和基础数据变更由 Flyway 在后端启动时自动执行。每次数据库变更都应在
`ruoyi-admin/src/main/resources/db/migration/` 中新增一个不可修改的迁移文件，已经运行过的
迁移不能继续编辑。详细规则见
[`DATABASE_MIGRATIONS.md`](./VLStream-Cloud-Backend-Server/vls-stream/DATABASE_MIGRATIONS.md)。

### 3. 配置并启动后端

请检查主配置文件和当前环境配置：

- `ruoyi-admin/src/main/resources/application.yml`
- `ruoyi-admin/src/main/resources/application-dev.yml`
- `ruoyi-admin/src/main/resources/application-prod.yml`

#### 部署前必做配置

完整功能部署不能直接使用仓库中的测试地址和示例密码。启动前至少完成以下配置：

| 配置项 | 用途 | 配置位置 |
| --- | --- | --- |
| MySQL | 业务数据、训练任务、下发任务 | `application-dev.yml` / `application-prod.yml` |
| Redis | 登录状态、缓存和分布式状态 | `application-dev.yml` / `application-prod.yml` |
| MinIO | 算法标注图片、数据集和普通文件上传 | 数据库表 `sys_oss_config` |
| GPU 训练服务器 | 训练、格式转换、模型产物保存 | `VLSTREAM_SSH_*`、`VLSTREAM_TRAINING_*` |
| MQTT Broker | 设备控制、模型任务发布和硬件回执 | `VLSTREAM_MQTT_*` |
| 模型下载入口 | 现场设备通过 HTTP 拉取模型 | `VLSTREAM_MODEL_*` |
| GPT/AI 服务 | AI 文本生成和文生图 | 前端 APaaS 网关配置及独立 `apaas-ai` 服务 |

推荐通过部署环境注入敏感配置，不要把真实密码、密钥提交到 Git：

```bash
# MySQL
MYSQL_HOST=mysql.example.internal
MYSQL_PORT=3306
MYSQL_DB_NAME=vlstream
MYSQL_USERNAME=vlstream
MYSQL_PASSWORD=replace-me

# Redis
REDIS_HOST=redis.example.internal
REDIS_PORT=6379
REDIS_PASSWORD=replace-me

# GPU 训练服务器；模型产物实际保存在该服务器的 /data/work
VLSTREAM_SSH_HOST=gpu.example.internal
VLSTREAM_SSH_PORT=22
VLSTREAM_SSH_USERNAME=vlstream
VLSTREAM_SSH_PASSWORD=replace-me
VLSTREAM_TRAINING_HOST_DATA_DIR=/data/work
VLSTREAM_TRAINING_WORK_DIR=/data/work/ultralytics_yolov8-main/datasets

# MQTT
VLSTREAM_MQTT_HOST=127.0.0.1
VLSTREAM_MQTT_PORT=1883
VLSTREAM_MQTT_USERNAME=vlstream
VLSTREAM_MQTT_PASSWORD=replace-me
VLSTREAM_MQTT_QOS=1

# 模型下发；PUBLIC_BASE_URL 必须能被现场硬件访问
VLSTREAM_MODEL_PUBLIC_BASE_URL=https://vlstream.example.com
VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET=replace-with-a-long-random-secret
VLSTREAM_MODEL_DOWNLOAD_URL_TTL_SECONDS=1800
VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID=vls-model-dispatch-backend-01

# 硬件事件图片；生产使用独立私有 OSS 配置
VLSTREAM_DEVICE_MEDIA_OSS_CONFIG_KEY=vlstream-events
VLSTREAM_DEVICE_MEDIA_UPLOAD_TTL_SECONDS=600
VLSTREAM_DEVICE_MEDIA_MAX_IMAGE_BYTES=10485760
VLSTREAM_DEVICE_MEDIA_ALLOW_UNAUTHENTICATED=false
```

多实例部署时，每个后端实例的 `VLSTREAM_MODEL_DISPATCH_MQTT_CLIENT_ID` 必须唯一。
VLS-Protocol 2.2 的设备 bus Topic
`vlstream/v2.2/dev/{deviceId}/bus` 是固定协议，不再通过环境变量修改。Topic、ACL 和
硬件行为统一查看
[VLS 平台与摄像头统一通信协议](./VLStream-Cloud-Backend-Server/vls-stream/doc/VLS-Protocol.md)。

#### EMQX 5.4 本地测试环境

项目 Compose 使用内网镜像
`192.168.88.150:80/opensource/emqx/emqx:5.4`，运行单节点 EMQX 5.4：

- Java 后端 MQTT：`127.0.0.1:1883`。
- 当前硬件联调 MQTT：`192.168.88.31:1883`。
- Dashboard：`http://192.168.88.31:18083`。
- 数据目录：`/docker/emqx/data`。
- 日志目录：`/docker/emqx/log`。
- MQTT 3.1.1 用户名密码认证：启用。
- TLS、WebSocket 和 WSS：本地测试环境关闭。

先复制环境变量模板并修改所有占位密码：

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
Copy-Item script/docker/.env.example script/docker/.env
```

`.env` 已被 Git 忽略，不能提交。镜像仓库需要先登录：

```powershell
docker login 192.168.88.150:80
```

如果 Docker 报错 `server gave HTTP response to HTTPS client`，说明该仓库使用
HTTP，需要在部署机器的 Docker daemon 中加入：

```json
{
  "insecure-registries": ["192.168.88.150:80"]
}
```

修改 Docker daemon 配置后必须重启 Docker。然后启动 EMQX，并初始化本地 MQTT
用户：

```powershell
docker compose --env-file script/docker/.env -f script/docker/docker-compose.yml up -d emqx
pwsh -File script/docker/init-emqx.ps1
```

`init-emqx.ps1` 会从 `.env` 读取 `VLSTREAM_MQTT_USERNAME` 和
`VLSTREAM_MQTT_PASSWORD`，通过 EMQX 5.4 API 幂等创建或更新本地测试用户，不会把
密码写进 Git。EMQX 5.4 不支持较新版本的认证用户启动文件，因此不能使用 5.7
以后的 `bootstrap_file` 配置替代此步骤。

本地从源码启动 Java 后端时，也必须把 `.env` 中相同的 MQTT 用户名和密码设置为
进程环境变量。`application.yml` 默认连接 `127.0.0.1:1883`，但不再提供默认密码。

后端和 EMQX 在同一台服务器时使用 `127.0.0.1:1883`；硬件设备不能使用
`127.0.0.1`，当前联调地址固定为 `192.168.88.31:1883`。部署机器防火墙需要允许
硬件网段访问 TCP `1883`；Dashboard 的 TCP `18083` 只应向管理网段开放。

#### MinIO 与算法标注

算法标注上传通过 RuoYi OSS 服务读取数据库表 `sys_oss_config`，不是读取 `application.yml` 中的固定 MinIO 账号。首次启动后，在对象存储配置中启用一条 `config_key=minio` 的记录，并填写：

- `access_key`：MinIO Access Key。
- `secret_key`：MinIO Secret Key。
- `bucket_name`：模型数据和标注图片使用的 Bucket。
- `endpoint`：例如 `minio.example.internal:9000`，不要误填控制台端口。
- `domain`：浏览器或训练服务器访问对象时使用的外部域名，可按部署方式留空。
- `is_https`：外部访问是否使用 HTTPS。
- `access_policy`：按现场安全要求选择；若使用私有桶，必须确认下载链路能够取得有效签名 URL。
- `status`：必须为启用状态。

MinIO 的数据目录必须挂载到持久化磁盘。后端、浏览器和 GPU 训练服务器都需要能够解析并访问最终生成的对象 URL，否则会出现“上传成功但标注页或训练任务无法读取图片”。

硬件事件图片复用同一套 MinIO 服务，但生产环境推荐新增
`config_key=vlstream-events` 的私有 OSS 配置并使用独立 Bucket。硬件只接收单对象、
短时有效的预签名 PUT 地址，不得持有 MinIO AccessKey/SecretKey。本地 `dev` 配置默认
允许局域网无认证申请上传地址；生产必须设置
`VLSTREAM_DEVICE_MEDIA_ALLOW_UNAUTHENTICATED=false`，在设备 HMAC 认证上线前该接口保持关闭。
部署前必须执行 `db/2026-07-29-vls-device-event-media.sql`。

#### GPT/AI 服务器

当前 VLStream 前端不会直接调用 OpenAI，也不在 Java 后端保存 GPT API Key。AI 功能调用以下网关路由：

```text
{APaaS网关前缀}/apaas-ai/api/v1/text_completion
{APaaS网关前缀}/apaas-ai/api/v1/text_img
```

因此需要同时完成两层配置：

1. 在 VLStream 前端配置能够访问 `apaas-ai` 的 APaaS 网关。
2. 在独立的 `apaas-ai` 服务中配置实际大模型的 Base URL、API Key、模型名称和超时时间；该服务不在本仓库中。

`apaas-ai` 至少需要确认以下服务端参数，具体环境变量名称以该服务自身的部署包为准：

- 大模型提供方的 Base URL。
- API Key 或内部认证凭据。
- 文本生成使用的模型名称。
- 文生图使用的模型名称或服务地址。
- 请求超时、最大响应长度以及失败重试策略。
- 服务器到大模型提供方的 DNS、代理和防火墙出口。

开发环境示例：

```bash
VITE_APAAS_PROXY_TARGET=http://apaas-gateway.example.internal:21410
```

生产构建示例：

```bash
VITE_APAAS_GATEWAY_PREFIX=https://gateway.example.com/bus
```

配置完成后，应直接验证网关能够访问 `apaas-ai/api/v1/text_completion`。只有网关健康但 `apaas-ai` 未配置模型密钥时，前端 AI 按钮仍会调用失败。

Maven Profile 包括 `dev`、`local` 和 `prod`，默认启用 `dev`。

```powershell
mvn -ntp -Pdev clean package
mvn -ntp -Pdev -pl ruoyi-admin spring-boot:run
```

启动后可以访问：

- Knife4j：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

> [!NOTE]
> 后端父 POM 配置了内部 Maven 仓库。解析依赖时可能需要连接项目网络，或者在 Maven `settings.xml` 中配置可用的镜像。

### 4. 启动前端

从仓库根目录打开新的终端：

```powershell
cd VLStream-Web/VLStream-ui
npm install
npm run dev
```

本地开发时还应在前端环境文件中设置：

```bash
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
VITE_APAAS_PROXY_TARGET=http://apaas-gateway.example.internal:21410
```

使用 `npm run build` 构建生产环境前端资源。

#### 启动后验收

部署完成后至少验证：

1. 后端 `/actuator/health` 正常，能够连接 MySQL 和 Redis。
2. 在文件管理中上传测试图片，并从浏览器打开返回的 MinIO URL。
3. 新建算法标注任务，确认图片可显示、标注结果可保存。
4. 调用 GPT 文本生成功能，确认请求实际到达 `apaas-ai`。
5. 按 `VLS-Protocol.md` 完成 MQTT 和模型下发联调验收。
6. 执行一次训练任务，确认容器调度、日志和 `/data/work` 模型产物均正常。

---

## 🔌 API 示例

### 设备管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/vlsDeviceInfo/page` | 分页查询设备 |
| `GET` | `/vlsDeviceInfo/{id}` | 根据 ID 查询设备 |
| `POST` | `/vlsDeviceInfo` | 新增设备 |
| `PUT` | `/vlsDeviceInfo/{id}` | 更新设备 |
| `DELETE` | `/vlsDeviceInfo/{id}` | 删除设备 |
| `GET` | `/vlsDeviceInfo/statistics` | 获取设备统计信息 |

标准 API 响应使用公共的 `R<T>` 结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

完整、最新的接口列表请以服务启动后生成的 OpenAPI 文档为准。

---

## 🐳 部署

从 [GitHub Releases](https://github.com/OortCloudGroup/VLStream-Cloud/releases)
下载并解压部署包，然后复制环境变量模板并启动：

```powershell
Copy-Item .env.example .env
docker compose up -d
```

停止服务：

```powershell
docker compose down
```

> [!TIP]
> 部署包默认包含 MySQL、Redis、MinIO、WebRTC-streamer、后端和前端，也支持使用已有的
> 外部基础服务。配置和升级方法见[部署指南](./deploy/release/README.zh-CN.md)。

---

## 📚 项目文档

| 资源 | 链接 |
| --- | --- |
| 前端指南 | [`VLStream-Web/README.md`](./VLStream-Web/README.md) |
| 前端中文指南 | [`VLStream-Web/README-cn.md`](./VLStream-Web/README-cn.md) |
| 设备 SDK 指南 | [`sdk/README.md`](./sdk/README.md) |
| 核心业务技术架构 | [`architecture/vlstream-core-business-technical-architecture.md`](./architecture/vlstream-core-business-technical-architecture.md) |
| 后端环境变量 | [`ENVIRONMENT_VARIABLES.md`](./VLStream-Cloud-Backend-Server/vls-stream/ENVIRONMENT_VARIABLES.md) |
| 部署指南 | [`deploy/release/README.zh-CN.md`](./deploy/release/README.zh-CN.md) |
| 数据库迁移 | [`DATABASE_MIGRATIONS.md`](./VLStream-Cloud-Backend-Server/vls-stream/DATABASE_MIGRATIONS.md) |
| VLS 协议规范（中文） | [`VLS-Protocol.docx`](./VLStream-Cloud-Backend-Server/vls-stream/doc/VLS-Protocol.docx) |
| VLS 协议规范（英文） | [`VLS-Protocol-EN.docx`](./VLStream-Cloud-Backend-Server/vls-stream/doc/VLS-Protocol-EN.docx) |
| API 文档 | 启动后端后访问 Knife4j 或 Swagger UI |

---

## 🤝 帮助与支持

- **项目主页**：[vls.oortcloudsmart.com](https://vls.oortcloudsmart.com)
- **问题反馈**：[GitHub Issues](https://github.com/OortCloudGroup/VLStream-Cloud/issues)
- **技术支持**：[zhangxuelian@oortcloudsmart.com](mailto:zhangxuelian@oortcloudsmart.com)

欢迎参与项目贡献，包括报告问题、提出功能建议、改进文档或提交 Pull Request。

---

## 📄 开源许可

VLStream Cloud 基于 [MIT License](./LICENSE) 发布。

---

<div align="center">
  <h3>感谢使用 VLStream Cloud</h3>
  <p>如果本项目对你有帮助，欢迎在 GitHub 上点亮 ⭐。</p>
  <p>
    <a href="https://vls.oortcloudsmart.com">项目主页</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/issues">问题反馈</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud">GitHub 仓库</a>
  </p>
  <p>Built with ❤️ by OortCloud</p>
</div>
