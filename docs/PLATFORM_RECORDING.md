# 平台录像

平台录像由 VLS 后端独立接收并保存视频，WVP 提供设备权限、视频源和原生协议开流。CameraRTC 使用已有的服务器信令接收器，不依赖浏览器或训练 GPU。视频回放和设备管理操作同一套任务，视频广场窗口仅展示录制状态。

## 依赖清单

Java 8 后端负责权限、在线检查、任务调度和录像索引；Python 子进程负责视频接收、编码和 MP4 分段。当前 CameraRTC、ONVIF、RTSP 等平台录像来源共用此进程，因此这些依赖不是仅供 CameraRTC 使用。它们是启用平台手动／计划录像的前提，不需要训练 GPU，也不复用 OM 模型转换环境。

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| Python | `3.10+` | 执行后端启动的录像接收进程 |
| PyAV（pip 包名 `av`） | `17.1.0` | 视频解码、H.264 编码和 MP4 封装 |
| aiortc | `1.15.0` | CameraRTC / WebRTC 服务端媒体接收 |
| aiohttp | `3.14.5` | 接收器异步 HTTP / WebSocket 通信 |
| 可写持久化目录 | 默认 `./data/platform-recordings` | 保存录像和提取的运行脚本；默认预留 1 GiB 空间 |

Python 包版本以 [requirements.txt](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/recording/requirements.txt) 为准，由 pip 同时安装其传递依赖。在项目根目录执行以下 Linux 安装示例，目标环境须已提供 Python 3.10+ 和 venv 模块：

```sh
python3 -m venv /opt/recording-venv
/opt/recording-venv/bin/python -m pip install -r VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/recording/requirements.txt
/opt/recording-venv/bin/python -c "import av, aiortc, aiohttp; print('recording dependencies OK')"
```

Docker 部署必须在 **VLS backend 镜像内**安装上述环境，不能只安装在宿主机或其他容器中。为录像目录挂载持久卷，并确保运行 Java 的用户可创建目录、写入文件和执行 Python。仅升级 JAR 不会自动安装这些依赖；后续重建或更换 backend 镜像时也必须保留它们。

## 运行配置

以下是 Spring 配置项，也可以用环境变量设置。例如在现有 Compose 的 `services.backend` 中合并以下内容（路径须与镜像及持久卷对应）：

```yaml
environment:
  PLATFORM_RECORDING_PYTHON: /opt/recording-venv/bin/python
  PLATFORM_RECORDING_DIRECTORY: /app/data/platform-recordings
```

`PLATFORM_RECORDING_PYTHON` 对应 `platform.recording.python`，`PLATFORM_RECORDING_DIRECTORY` 对应 `platform.recording.directory`。`/app/data/platform-recordings` 应位于持久卷内；更新现有部署时保留其完整 Compose 配置链和原有环境变量。

部署后使用具备回放权限的登录身份访问 `GET /platform-recordings/runtime`，确认返回 `data.ready=true`。该检查验证依赖和存储环境；实际取流仍需通过目标设备短时录像、生成片段和回放验证。

| Spring 配置 | 默认值 | 含义 |
| --- | --- | --- |
| `platform.recording.python` | `python` | Python 可执行文件，建议指定虚拟环境绝对路径 |
| `platform.recording.directory` | `./data/platform-recordings` | 后端独占的持久化目录，不与旧录像目录混用 |
| `platform.recording.reserve-bytes` | `1073741824` | 磁盘预留空间，低于此值暂停新增录制 |
| `platform.recording.max-workers` | `8` | 本实例接收任务上限，范围 1～8 |
| `platform.recording.poll-ms` | `5000` | 任务调度周期，不建议增大到超过接收器保活期限 |
| `platform.recording.cleanup-ms` | `3600000` | 按通道保留期限清理过期数据的周期 |

Flyway `V1_2_0_070__platform_video_recording.sql` 创建录像来源与片段索引表。不能以手动 SQL 代替迁移，也不能通过前端传入文件路径。源凭据复用已有受控授权，数据库加密保存，接收进程通过标准输入取得凭据；启动参数、片段元数据与日志不包含媒体地址或信令凭据。

录像目录中的 `.runtime` 是从当前应用资源提取的接收器，UUID 子目录保存各来源的片段。运行主机须能访问 WVP、设备视频源与 CameraRTC 信令／ICE 服务。生产环境应使用单个录像执行实例和持久卷；多个应用实例不能依赖各自不共享的本地目录提供同一片段。

## 保留时间与录制状态

每个设备通道可选择 7 天、30 天、90 天或永久，默认 7 天；数据库 `retention_days=0` 表示永久。设置由同租户具备设备和回放管理权限的成员共享。变更同时适用于已有和后续录像，缩短期限会在下一次清理删除超期片段，已删除文件不会恢复。永久录像不做自动过期清理，存储不足仍暂停录制。Flyway `V1_2_0_071__platform_recording_retention.sql` 为已有来源补上默认 7 天。

视频广场不恢复录像按钮，仅显示真实录制状态：设备任一已授权来源通道正在收帧、状态为 RECORDING 且租约有效时显示绿框和右上角“录像中”。连接中、异常重试及查询失败不显示。只读状态接口校验租户和当前设备访问权限，不返回文件、凭据或录制配置。

兼容尚未提供轻量状态接口的旧后端：仅该接口返回 404 时，前端回退读取原有录像来源状态，仍执行旧接口的回放菜单、设备和租户权限校验；无权限或其他请求失败不回退、不显示录制标记。每分钟重新探测新接口，后端升级后自动切回轻量查询。

协议支持取决于 WVP 返回的可用视频源，而非摄像机说明书中的协议名称。VLStream/CameraRTC、ONVIF、RTSP、自定义网络媒体流以及 GB28181、ISUP、EHome、大华已有来源解析或原生开流适配；必须同时满足设备授权、通道可用、后台网络可达、SDK/媒体服务和编解码能力。已实机验证的来源不能代表所有型号或协议都已验收。不支持的来源不能通过统一录像入口自动获得取流能力。

## 任务与权限

视频回放页在设备树旁展示可搜索、分页的设备列表；点击“播放”打开回放弹窗。弹窗按北京时间选择年、月、日，左侧选择已保存片段，右侧播放，并提供倍速、时间轴和当前片段下载。未来日期不可选，设备离线仍允许查阅历史录像。片段封面从真实录像提取，首批最多预取三个片段生成封面；尚未生成或提取失败时显示播放占位图标，不影响播放。

同租户、协议、设备行 ID、通道对应唯一来源。具有回放页权限且当前能访问该设备的成员共享任务、计划与录像。历史文件读取仍要经过 VLS 身份与租户校验和 WVP 当前设备目录授权；设备离线不等于撤销目录权限，跨租户共享预览不产生录像授权。

新增或重新开启录制由当前用户显式委托后台取流。后台延续该授权执行并定期核对来源；后续接管配置可刷新授权。手动和计划需求分开保存，合并为同一路接收；切换页面不改变后台任务。周期计划以北京时间执行，跨午夜时使用开始日的星期。

VLStream 取流类型以 WVP 返回的通道能力为准；CameraRTC 信令地址不是设备在线的证据。手动开始及后台每轮核验都要求 WVP 当前在线状态明确为真，离线或未知时不发起或继续收流，保留录制需求等待恢复。离线设备仍可查看已保存录像、保存计划或停止任务。CameraRTC 信令配置必须匹配服务端协议（TLS 入口使用 `wss`），收到视频帧后才能显示录制中。

接收器对队列设置上限，CPU 繁忙时丢弃积压帧而不无限占用内存。MP4 采用 H.264 视频轨，当前不保存音频。约一分钟完成一个片段，已完成的片段通过元数据侧文件恢复索引；录制过程中尚未封装的片段不出现在回放列表。停止时尝试封装尾段，进程被强杀时未完成片段可能丢失，不能标记成完整录像。

## 异常处理

- `STARTING` 表示准备或建立媒体连接；收到真实帧后才进入 `RECORDING`。
- `RETRYING` 表示来源、收帧、空间或运行环境异常，原手动／计划需求保留，后台重试。
- `WAITING` 表示录像接收任务已达到本实例上限。
- `UNKNOWN` 表示数据库租约失效，不能据此宣称仍在录制。
- 空间不足不会提前删除保留期内或永久录像；增加容量或等待过期清理后，后台重新尝试。
- 过期清理仅覆盖本模块受控文件及对应索引，不能用于清理旧 ZLM、NVR 或智能事件媒体。

本模块不自动合并旧录像；自定义设备的旧版录像入口保留以管理既有任务，避免隐藏仍在运行的旧计划。设备端历史录像和智能告警短片不属于本次平台连续录像目录。
