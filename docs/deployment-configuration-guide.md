# VLStream Cloud 独立部署配置手册

本文面向从公开仓库自行部署 VLStream Cloud 的用户，说明哪些配置必须替换为部署者自己的资源，哪些仅在启用对应功能时配置，以及哪些属于当前单后端模式不需要的旧平台集成。

本文以当前架构为准：

- 后端为 Spring Boot 2.7.11、Java 8 的 RuoYi/workflows 单后端。
- 用户、角色和权限使用本地 RuoYi 数据库及 Sa-Token。
- 前端保留 `/blade-auth/token`、`/blade-auth/logout` 和 `/blade-system/user/info` 路由外观。
- 不要求部署 SpringBlade system、外部 SSO 或多租户服务。
- VLS 业务表按单租户运行，默认租户标识为 `000000`。

## 1. 上线前必须更换的内容

以下内容不得直接使用仓库中的示例值部署到公网：

| 类别 | 必须更换的内容 | 配置位置 |
| --- | --- | --- |
| MySQL | 主机、端口、数据库、用户名、密码 | `application-dev.yml`、`application-prod.yml` 或环境变量 |
| Redis | 主机、端口、密码 | `application-dev.yml`、`application-prod.yml` 或环境变量 |
| 本地管理员 | 初始管理员密码及 BCrypt 哈希 | `oortcloud_local_user_system_seed.sql` 或首次启动后用户管理 |
| 登录加密 | SM2 公钥、私钥；前后端必须配对 | 后端 `BLADE_OAUTH2_*`，前端 `VITE_BLADE_AUTH_PUBLIC_KEY` |
| Token 安全 | Sa-Token/JWT 密钥 | `application.yml` 或对应 Spring 环境变量 |
| 对象存储 | Access Key、Secret Key、Bucket、Endpoint、公开域名 | 数据库 `sys_oss_config` |
| SSH/GPU | 训练服务器地址、账号、密码和训练目录 | `VLSTREAM_SSH_*`、`VLSTREAM_TRAINING_*` |
| MQTT | Broker 地址、账号、密码、Topic | `VLSTREAM_MQTT_*` |
| RTSP/录像 | FFmpeg 路径、录像目录、磁盘容量策略 | `VLSTREAM_RTSP_*` |
| 前端 API | 后端公开地址或同域相对路径 | `VITE_API_BASE_URL` |
| Nginx | 域名、监听端口、证书、后端代理地址 | `VLStream-ui/nginx.conf.example` |
| WebRTC | WebSocket 服务地址 | 当前位于 `src/utils/oplayer.js` |
| XXL-Job | 调度中心地址和 Access Token，或关闭该功能 | `application-*.yml` |

仓库中的 IP、域名、密码和密钥仅用于原开发环境或示例。它们不代表公开可用的基础设施。

## 2. Spring 配置和环境变量规则

配置形式：

```yaml
host: ${VLSTREAM_SSH_HOST:192.0.2.10}
```

含义是优先读取环境变量 `VLSTREAM_SSH_HOST`；变量不存在时才使用冒号后的默认值。

推荐部署时不修改受版本控制的 YAML，而是通过容器、systemd、PowerShell 或 CI/CD Secret 注入环境变量。配置优先级通常是：

1. Java 命令行参数，例如 `--server.port=8080`
2. 操作系统或容器环境变量
3. `application-{profile}.yml`
4. `application.yml`

后端默认使用 `dev` Profile。生产环境应显式指定：

```powershell
java -jar ruoyi-admin/target/apaas-workflowforms.jar --spring.profiles.active=prod
```

## 3. 最小必需基础设施

一个能登录并使用基础 CRUD 的部署至少需要：

- JDK 8
- MySQL 5.7+ 或 MySQL 8
- Redis
- 一个可写的对象存储；推荐 MinIO，也支持 S3 兼容服务
- 构建前端所需的 Node.js/npm
- Nginx 或其他静态文件/Web API 反向代理

训练、设备下发和视频功能还需要 SSH/GPU、MQTT、FFmpeg、RTSP/WebRTC 等额外服务。

## 4. 后端基础配置

### 4.1 服务端口

| 环境变量 | 是否必改 | 说明 | 示例 |
| --- | --- | --- | --- |
| `SERVER_PORT` | 否 | 后端监听端口，默认 `8080` | `18080` |
| `SERVER_TEMP_DIR` | 容器部署推荐 | multipart 临时目录，运行用户必须可写 | `/srv/vlstream/temp` |

如果使用 Nginx，外部端口和 Spring Boot 端口可以不同；Nginx 的 `proxy_pass` 必须指向实际后端端口。

### 4.2 MySQL

| 环境变量 | 是否必改 | 说明 |
| --- | --- | --- |
| `MYSQL_HOST` | 是 | MySQL 主机名或容器服务名 |
| `MYSQL_PORT` | 是 | MySQL 端口，通常为 `3306` |
| `MYSQL_DB_NAME` | 是 | VLStream 使用的数据库名 |
| `MYSQL_USERNAME` | 是 | 建议使用专用账号，不使用 root |
| `MYSQL_PASSWORD` | 是 | 使用独立强密码，通过 Secret 注入 |

`application-prod.yml` 已使用上述变量，不再写死 `ry-vue/root/root`。如确实启用只读从库，可另外设置 `MYSQL_SLAVE_HOST`、`MYSQL_SLAVE_PORT`、`MYSQL_SLAVE_DB_NAME`、`MYSQL_SLAVE_USERNAME` 和 `MYSQL_SLAVE_PASSWORD`；未设置时继承主库参数。

初始化示例：

```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'vlstream'@'%' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON vlstream.* TO 'vlstream'@'%';
FLUSH PRIVILEGES;
```

然后执行项目实际需要的 MySQL 初始化脚本。生产部署前应先在空数据库演练，不要把初始化脚本直接应用到已有业务库。

### 4.3 Redis

| 环境变量 | 是否必改 | 说明 |
| --- | --- | --- |
| `REDIS_HOST` | 是 | Redis 主机名或容器服务名 |
| `REDIS_PORT` | 是 | Redis 端口，通常为 `6379` |
| `REDIS_PASSWORD` | 推荐 | Redis 密码；公网部署必须设置 |

Redis 保存 Sa-Token 会话、缓存和分布式状态。Redis 清空或切换实例会导致现有登录 token 失效。

### 4.4 本地管理员账号

当前登录入口是：

- `POST /blade-auth/token`
- `POST /blade-auth/logout`
- `GET /blade-system/user/info`

路由外观兼容 Blade，底层仍是 RuoYi `SysLoginService`、本地 `sys_user` 和 Sa-Token。

部署者必须处理初始管理员密码：

1. 在私有部署流程中生成自己的 BCrypt 密码哈希。
2. 替换 `ruoyi-admin/src/main/resources/oortcloud_local_user_system_seed.sql` 中管理员密码哈希，或者首次登录后立即修改密码。
3. 不要在文档、镜像或 Git 历史中保存管理员明文密码。

### 4.5 SM2 登录密钥

| 后端环境变量 | 说明 |
| --- | --- |
| `BLADE_OAUTH2_PUBLIC_KEY` | 前端加密密码使用的 SM2 公钥 |
| `BLADE_OAUTH2_PRIVATE_KEY` | 后端解密密码使用的 SM2 私钥 |

前端构建变量：

```text
VITE_BLADE_AUTH_PUBLIC_KEY=<与后端匹配的 SM2 公钥>
```

要求：

- 公钥和私钥必须是同一密钥对。
- 私钥只能存在于后端 Secret 中。
- 前端只配置公钥。
- 更换密钥后必须重新构建前端。
- 不要继续使用公开仓库中的默认私钥。

`VITE_BLADE_CLIENT_AUTH_HEADER` 是前端兼容请求头。当前本地认证主要依赖账号密码和 Sa-Token；如果网关或部署环境校验客户端 Basic 凭据，也必须替换默认值。

## 5. 对象存储和文件上传

事件图片、标注文件和业务附件通过 RuoYi OSS 上传，配置主要保存在数据库表 `sys_oss_config`，不是只改 YAML。

至少检查以下字段：

| 字段 | 说明 |
| --- | --- |
| `config_key` | 存储配置名称，例如 `minio` |
| `access_key` | 对象存储账号 |
| `secret_key` | 对象存储密钥 |
| `bucket_name` | Bucket 名称 |
| `endpoint` | S3/MinIO API 地址 |
| `domain` | 浏览器可访问的公开域名或反向代理地址 |
| `is_https` | 是否使用 HTTPS |
| `region` | 云厂商区域，MinIO 通常可留空 |
| `status` | 当前默认存储配置 |
| `access_policy` | public/private/custom |

Docker Compose 示例里的 MinIO 用户名、密码和 Bucket 都必须替换。外部浏览器需要访问文件时，`domain` 不能写容器内部地址。

## 6. VLStream 业务配置

### 6.1 检测功能开关

下面的环境变量决定是否注册对应检测任务，默认值均为 `false`：

| 环境变量 | Spring 属性 | 功能 |
| --- | --- | --- |
| `VLSTREAM_PERSON_DETECTION_ENABLED` | `vlstream.person-detection.enabled` | 人员检测 |
| `VLSTREAM_FACE_DETECTION_ENABLED` | `vlstream.face-detection.enabled` | 人脸检测 |
| `VLSTREAM_OBJECT_DETECTION_ENABLED` | `vlstream.object-detection.enabled` | 目标检测 |
| `VLSTREAM_CLASSIFY_DETECTION_ENABLED` | `vlstream.classify-detection.enabled` | 图像分类 |
| `VLSTREAM_INSTANCE_SEG_DETECTION_ENABLED` | `vlstream.instance-seg-detection.enabled` | 实例分割 |
| `VLSTREAM_OBB_DETECTION_ENABLED` | `vlstream.obb-detection.enabled` | 旋转框检测 |
| `VLSTREAM_POSE_DETECTION_ENABLED` | `vlstream.pose-detection.enabled` | 姿态检测 |
| `VLSTREAM_SEMSEG_DETECTION_ENABLED` | `vlstream.semseg-detection.enabled` | 语义分割 |

部署建议：首次启动全部设为 `false`。确认模型、GPU/CPU、FFmpeg、SSH 和存储都可用后，再逐项开启，避免启动时同时加载大量模型。

### 6.2 SSH 和 GPU 服务器

| 环境变量 | 必要性 | 说明 |
| --- | --- | --- |
| `VLSTREAM_SSH_HOST` | 训练/数据集功能必需 | GPU 或训练服务器地址 |
| `VLSTREAM_SSH_PORT` | 必需 | SSH 端口 |
| `VLSTREAM_SSH_USERNAME` | 必需 | SSH/SFTP 用户 |
| `VLSTREAM_SSH_PASSWORD` | 必需或改密钥认证 | SSH 密码 |

这些配置会用于远程训练、日志读取、数据集 SFTP、标注数据同步和模型文件传输。目标服务器必须允许后端主机访问。

生产建议使用受限账号和 SSH 密钥，不使用 root，也不要把密码写进 YAML。

### 6.3 训练目录

| 环境变量 | 说明 |
| --- | --- |
| `VLSTREAM_TRAINING_CONDA_PROFILE` | 远程服务器上的 Conda 初始化脚本 |
| `VLSTREAM_TRAINING_CONDA_ENV` | 默认 Conda 环境名 |
| `VLSTREAM_TRAINING_WORK_DIR` | 远程训练/数据集工作目录 |
| `VLSTREAM_TRAINING_LOG_DIR` | 训练日志目录 |
| `VLSTREAM_TRAINING_SYNSET_FILE_NAME` | 分类训练标签文件名 |
| `VLSTREAM_TRAINING_RKNN_MODEL_ZOO_PATH` | RKNN `convert.py` 所在的模型转换工程目录 |

这些路径是远程 SSH 服务器上的路径，不是后端服务器本地路径。部署前需要用同一个 SSH 用户验证目录存在且可读写。

### 6.4 MQTT 和设备下发

| 环境变量 | 是否必改 | 说明 |
| --- | --- | --- |
| `VLSTREAM_MQTT_HOST` | 是 | MQTT Broker 地址 |
| `VLSTREAM_MQTT_PORT` | 是 | MQTT 端口 |
| `VLSTREAM_MQTT_USERNAME` | 是 | MQTT 用户名 |
| `VLSTREAM_MQTT_PASSWORD` | 是 | MQTT 密码 |
| `VLSTREAM_MQTT_CLIENT_ID_PREFIX` | 推荐 | 客户端 ID 前缀；多实例部署必须避免冲突 |
| `VLSTREAM_MQTT_QOS` | 按设备协议 | QoS，通常为 `0` 或 `1` |
| `VLSTREAM_MQTT_KEEP_ALIVE_SECONDS` | 否 | 心跳时间 |
| `VLSTREAM_MQTT_CONNECTION_TIMEOUT_SECONDS` | 否 | 连接超时 |

Topic 必须与设备固件或边缘端保持一致：

```text
VLSTREAM_MQTT_TOPIC_PREFIX
VLSTREAM_MQTT_DISPATCH_ALGORITHMS_TOPIC
VLSTREAM_MQTT_TOPIC_CAMERA_DISPLAY
VLSTREAM_MQTT_TOPIC_CAMERA_OSD
VLSTREAM_MQTT_TOPIC_AUDIO_ANOMALY
VLSTREAM_MQTT_TOPIC_AUDIO_DEFENSE
VLSTREAM_MQTT_TOPIC_AUDIO_LINKAGE
VLSTREAM_MQTT_TOPIC_TIME_STRATEGY
VLSTREAM_MQTT_TOPIC_RECORD_EVENT
```

如果只改后端 Topic、不改设备订阅 Topic，接口可能返回成功但设备不会收到指令。验收时应同时检查 Broker 消息和设备日志。

### 6.5 RTSP 录像和 FFmpeg

| 环境变量 | 说明 |
| --- | --- |
| `VLSTREAM_RTSP_RECORDING_ENABLED` | 是否启用后台录像管理器 |
| `VLSTREAM_RTSP_FFMPEG_PATH` | FFmpeg 可执行文件或绝对路径 |
| `VLSTREAM_RTSP_STORAGE_PATH` | 录像根目录 |
| `VLSTREAM_RTSP_SEGMENT_SECONDS` | 单个录像切片秒数 |
| `VLSTREAM_RTSP_PROCESS_STOP_WAIT_SECONDS` | 停止进程等待秒数 |
| `VLSTREAM_RTSP_STALE_FILE_SECONDS` | 文件稳定后再同步的等待秒数 |
| `VLSTREAM_RTSP_REFRESH_INTERVAL_MILLIS` | 已完成录像文件扫描与同步的周期，单位毫秒 |

部署前检查：

- `ffmpeg -version` 能由运行后端的系统用户执行。
- 录像目录存在、可写，且磁盘容量、inode 和清理策略明确。
- 容器部署时已经挂载持久化目录。
- RTSP 地址能从后端服务器网络访问。

`VLSTREAM_RTSP_REFRESH_INTERVAL_MILLIS` 已绑定到代码实际读取的 `vlstream.rtsp-recording.record-sync-interval-millis`。

### 6.6 旧 recording/stream 配置

顶层 `recording.*` 和 `stream.*` 是旧录像/WebRTC/HLS 实现遗留配置。当前迁移后的核心 RTSP 录像读取 `vlstream.rtsp-recording.*`，旧 `/api/webrtc` Controller 也处于排除状态。

部署者可以暂时保留这些配置，但不要把它们作为当前录像功能是否生效的判断依据。

## 7. 工作流和单租户配置

| 环境变量 | 默认建议 | 说明 |
| --- | --- | --- |
| `VLS_SINGLE_TENANT_ID` | `000000` | 当前单租户标识；前后端必须一致 |
| `VLS_APAAS_SYNC_ENABLED` | `false` | 独立部署保持关闭 |
| `VLS_LOCATION_TASK_WORKFLOW_APP_PACKAGE` | 自己的应用包标识 | 工单/事件跳转来源 |
| `VLS_LOCATION_TASK_WORKFLOW_JUMP_PATH` | 自己的前端详情路由 | 工单跳转路径 |
| `VLS_LOCATION_TASK_WORKFLOW_JUMP_PARAMS` | 按前端参数格式 | 工单跳转参数模板 |

不要为独立部署启用 SpringBlade 多租户拦截，也不要迁移源项目 system 用户体系。

## 8. XXL-Job

如果需要分布式定时任务：

- 部署 `ruoyi-xxl-job-admin`。
- 设置 `XXL_JOB_ADDRESSES` 为真实调度中心地址。
- 设置 `XXL_JOB_ACCESS_TOKEN`，不要沿用默认 Token。
- 通过 `XXL_JOB_EXECUTOR_APPNAME` 和 `XXL_JOB_EXECUTOR_PORT` 为执行器设置名称和端口。

如果不部署 XXL-Job，应在对应 Profile 中将：

```yaml
xxl-job:
  enabled: false
```

否则后端虽然可以启动，但日志会持续出现连接调度中心失败。

## 9. 前端配置

前端目录：`VLStream-Web/VLStream-ui`。

### 9.1 构建变量

| 变量 | 是否必改 | 说明 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | 是 | 生产后端 API 地址；同域代理可使用 `/bus/vls-server` |
| `VITE_DEV_PROXY_TARGET` | 开发环境 | Vite `/api` 等代理的后端地址 |
| `VITE_BLADE_AUTH_PUBLIC_KEY` | 是 | 与后端 SM2 私钥匹配的公钥 |
| `VITE_BLADE_CLIENT_AUTH_HEADER` | 按网关 | Blade 客户端 Basic 兼容头 |
| `VITE_SSO_PROXY_TARGET` | 独立部署不需要 | 旧 SSO 代理目标 |
| `VITE_SSO_BASE_URL` | 独立部署不需要 | 旧外部 SSO 地址 |
| `VITE_APAAS_PROXY_TARGET` | 独立部署通常不需要 | 旧 APaaS 网关地址 |
| `VITE_APAAS_API_BASE` | 独立部署通常不需要 | 旧 APaaS API 根地址 |

推荐生产环境文件示例：

```dotenv
VITE_API_BASE_URL=/bus/vls-server
VITE_BLADE_AUTH_PUBLIC_KEY=替换为自己的SM2公钥
VITE_BLADE_CLIENT_AUTH_HEADER=替换为自己的客户端凭据或按网关要求删除
```

Vite 环境变量会在构建时写入静态文件。修改后必须重新运行前端构建，不能只重启 Nginx。

### 9.2 当前仍需手工替换的前端地址

以下位置存在面向原环境的默认地址：

- `src/utils/request.js`：生产 API 默认地址
- `src/utils/oplayer.js`：`CAMERA_RTC_SOCKET_URL`
- `src/api/userCenter.js`：旧 SSO 默认地址
- `vite.config.js`：开发代理默认地址
- `nginx.conf.example`：域名和 `proxy_pass`

其中 `CAMERA_RTC_SOCKET_URL` 当前是代码常量。使用视频聚合/WebRTC 时必须改为自己的 `ws://` 或 `wss://` 服务地址，建议后续改成 `VITE_CAMERA_RTC_SOCKET_URL`。

## 10. Nginx、域名和 HTTPS

复制 `VLStream-ui/nginx.conf.example` 后至少修改：

- `listen`
- `server_name`
- 前端静态目录 `alias` 或 `root`
- `/bus/vls-server/` 的 `proxy_pass`
- HTTPS 证书路径
- WebSocket 的 `Upgrade`/`Connection` 代理头
- 上传大小 `client_max_body_size`
- CORS 白名单

生产环境不建议：

```nginx
add_header Access-Control-Allow-Origin *;
```

应改为自己的前端域名，并优先使用前后端同域代理。公网部署应统一使用 HTTPS/WSS。

## 11. Docker Compose 示例需要修改的内容

`script/docker/docker-compose.yml` 是上游示例，不是开箱即用的当前 VLStream 生产编排。至少需要修改：

- MySQL root 密码和数据库名
- Redis 配置和密码
- MinIO 用户名、密码和持久化目录
- 容器镜像名称和版本
- 宿主机挂载路径
- `network_mode: host`
- Nginx 证书和配置目录
- 后端所有 MySQL/Redis/VLStream 环境变量

建议自行维护不提交真实 Secret 的 `compose.override.yml` 或部署平台 Secret。

## 12. 独立部署不需要的旧平台配置

当前单后端本地登录模式不要求以下服务：

- 外部 APaaS SSO
- SpringBlade system 用户服务
- 多租户用户中心
- Eureka/Zuul 旧微服务网关
- 外部用户、部门、岗位同步

包括 `APAAS_*`、`SSO_*`、`GET_MULTI_TENANT_*`、`GET_SINGLE_TENANT_*` 等变量。独立部署时保持相关同步开关关闭，不要把默认 OortCloud 地址当成公共服务使用。

如果未来确实接入自己的统一认证平台，应单独设计并验证，而不是仅替换 URL 后直接启用。

## 13. 推荐环境变量模板

下面只展示变量名和示例占位符，不包含可用密码：

```dotenv
SERVER_PORT=8080

MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DB_NAME=vlstream
MYSQL_USERNAME=vlstream
MYSQL_PASSWORD=<strong-random-password>

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<strong-random-password>

BLADE_OAUTH2_PUBLIC_KEY=<your-sm2-public-key>
BLADE_OAUTH2_PRIVATE_KEY=<your-sm2-private-key>

VLSTREAM_SSH_HOST=<gpu-or-training-host>
VLSTREAM_SSH_PORT=22
VLSTREAM_SSH_USERNAME=<restricted-user>
VLSTREAM_SSH_PASSWORD=<secret-or-use-key-auth>

# 本地实时检测默认全部关闭；完成模型、视频流、CPU/内存和存储验证后再逐项改为 true
VLSTREAM_PERSON_DETECTION_ENABLED=false
VLSTREAM_FACE_DETECTION_ENABLED=false
VLSTREAM_OBJECT_DETECTION_ENABLED=false
VLSTREAM_CLASSIFY_DETECTION_ENABLED=false
VLSTREAM_INSTANCE_SEG_DETECTION_ENABLED=false
VLSTREAM_OBB_DETECTION_ENABLED=false
VLSTREAM_POSE_DETECTION_ENABLED=false
VLSTREAM_SEMSEG_DETECTION_ENABLED=false

VLSTREAM_TRAINING_CONDA_PROFILE=/opt/conda/etc/profile.d/conda.sh
VLSTREAM_TRAINING_CONDA_ENV=vlstream
VLSTREAM_TRAINING_WORK_DIR=/srv/vlstream/training
VLSTREAM_TRAINING_LOG_DIR=/srv/vlstream/logs
VLSTREAM_TRAINING_SYNSET_FILE_NAME=synset.txt
VLSTREAM_TRAINING_RKNN_MODEL_ZOO_PATH=/srv/vlstream/rknn_model_zoo

VLSTREAM_MQTT_HOST=mqtt
VLSTREAM_MQTT_PORT=1883
VLSTREAM_MQTT_USERNAME=vlstream
VLSTREAM_MQTT_PASSWORD=<strong-random-password>
VLSTREAM_MQTT_CLIENT_ID_PREFIX=vlstream-production
VLSTREAM_MQTT_DISPATCH_ALGORITHMS_TOPIC=vlstream/dispatchAlgorithms

VLSTREAM_RTSP_RECORDING_ENABLED=true
VLSTREAM_RTSP_FFMPEG_PATH=/usr/bin/ffmpeg
VLSTREAM_RTSP_STORAGE_PATH=/srv/vlstream/recordings
VLSTREAM_RTSP_SEGMENT_SECONDS=300
VLSTREAM_RTSP_REFRESH_INTERVAL_MILLIS=10000

VLS_SINGLE_TENANT_ID=000000
VLS_APAAS_SYNC_ENABLED=false

XXL_JOB_ADDRESSES=http://xxl-job-admin:9100/xxl-job-admin
XXL_JOB_ACCESS_TOKEN=<strong-random-token>
XXL_JOB_EXECUTOR_APPNAME=vlstream-executor
XXL_JOB_EXECUTOR_PORT=9101
```

前端 `.env.production`：

```dotenv
VITE_API_BASE_URL=/bus/vls-server
VITE_BLADE_AUTH_PUBLIC_KEY=<your-sm2-public-key>
```

## 14. 部署验收清单

### 基础服务

- [ ] 后端使用正确 Profile 启动。
- [ ] MySQL 和 Redis 不再指向仓库默认 IP。
- [ ] 所有默认密码、Token、SM2 私钥和对象存储密钥均已更换。
- [ ] 数据库账号仅具有目标数据库权限。
- [ ] Redis、MySQL、MQTT、MinIO 未直接暴露到公网。

### 登录

- [ ] `/blade-auth/token` 使用本地 RuoYi 用户返回真实 `accessToken`。
- [ ] `/blade-system/user/info` 能加载用户、角色和权限。
- [ ] `/blade-auth/logout` 后原 token 失效。
- [ ] 前端 SM2 公钥和后端私钥属于同一密钥对。

### 文件与录像

- [ ] OSS 测试上传后，浏览器能访问返回 URL。
- [ ] `ffmpeg -version` 可执行。
- [ ] 录像目录已持久化并有磁盘告警。
- [ ] RTSP 地址从后端网络可访问。

### 训练与设备

- [ ] SSH/SFTP 使用受限账号连接成功。
- [ ] Conda、训练环境和工作目录存在。
- [ ] MQTT 发布后 Broker 和设备端都有可观察记录。
- [ ] Topic 与设备固件配置一致。

### 前端和网关

- [ ] 前端产物中不再包含原项目线上域名/IP。
- [ ] Nginx `server_name`、证书和 `proxy_pass` 已替换。
- [ ] WebRTC WebSocket 地址已替换。
- [ ] HTTPS 页面不再请求 HTTP/WS 混合内容。

## 15. 配置泄漏处理

如果真实密码、私钥或 Token 曾经提交到公开 Git 历史，仅删除当前文件中的值是不够的。必须：

1. 立即在对应服务端轮换凭据。
2. 使旧 Token、Access Key、SSH/MQTT 密码失效。
3. 将新凭据迁移到部署平台 Secret。
4. 必要时清理 Git 历史，但不能用清理历史代替服务端轮换。
