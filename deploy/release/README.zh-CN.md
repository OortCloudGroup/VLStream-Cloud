# VLStream Cloud v1.2.6 部署指南

此 Release 压缩包包含 VLS 应用、MySQL 初始化结构以及供共享 MySQL 使用的独立 WVP 数据库结构。WVP 与 ZLMediaKit 服务仍由独立 Release 发布和部署。

## 启动 VLStream 前

请先部署 [VLStream WVP Lite v1.0.8](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.8)，公开镜像为 `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.8`。WVP 仍是视频设备中心并拥有 ZLMediaKit；本 VLS Compose 不会启动第二套 WVP 或 ZLMediaKit 服务。

两套服务运行在同一主机时，请在 WVP 配置中将 `WVP_HTTP_PORT` 设为 `9080`，因为 VLStream 使用主机端口 `8080`。VLS `.env` 中的 `ZLMEDIAKIT_SECRET` 必须与 WVP 使用的 `ZLM_SECRET` 完全一致。默认 WVP 与 ZLMediaKit 前后端访问端口分别为 `9080` 和 `8081`；网络结构不同时，调整 `VLSTREAM_WVP_*`、`WVP_UPSTREAM`、`VLSTREAM_ZLM_INTERNAL_URL` 和 `ZLM_UPSTREAM`。

## 全新安装

```powershell
Copy-Item .env.example .env
# 编辑 .env，替换所有示例密码与密钥。
docker compose up -d
```

默认 VLS Compose 会启动 MySQL、Redis、MinIO、WebRTC-streamer、后端和前端。MySQL 空数据卷首次启动时会初始化 VLS 数据库，并创建独立的 `ry-wvp` 数据库。WVP 表结构来自已核对的 v1.0.8 初始化结构；此 Compose 不会启动 WVP 服务。若 WVP 复用这套 MySQL，请在 WVP 的 `compose.external.yaml` 中设置：

- `DB_HOST=host.docker.internal`、`DB_PORT=3306`、`DB_NAME=ry-wvp`；
- `DB_USERNAME=wvp`，`DB_PASSWORD` 使用 VLS `.env` 中的 `WVP_MYSQL_PASSWORD`；
- `DB_MIGRATION_USERNAME=root`，`DB_MIGRATION_PASSWORD` 使用 `MYSQL_ROOT_PASSWORD`；
- `WVP_HTTP_PORT=9080`。

请为 `WVP_MYSQL_PASSWORD` 生成至少 32 位的随机字母数字密码。也可以让 WVP 使用独立 MySQL。首次登录 WVP 后请立即修改默认管理员密码。

已发布镜像默认使用 Spring `prod` profile。请保持 `SPRING_PROFILES_ACTIVE=prod`，以使用公开 Release 对应的 Flyway 迁移历史。
需要通过外部统一消息服务发送流程通知时设置 `UNIFIEDMESSAGINGSEND_URL`；留空表示不配置外部消息推送。

- VLStream 页面：`http://localhost/bus/vls-ui/`
- VLS 默认账号：`admin`
- VLS 默认密码：`Codex@123456`（首次登录后请修改）

## 使用已有 MySQL 和 Redis

在 `.env` 中填写外部数据库和缓存配置后运行：

```powershell
docker compose -f compose.external.yaml up -d
```

此 Compose 不会初始化外部数据库。全新 VLS 数据库需先建库，再导入一次 `sql/init/10-oortcloud-workflowforms-vls.sql`。从公开 v1.2.5 升级时不要重新导入初始化结构；保持 `prod` profile，由 Flyway 应用尚未执行的 Release 迁移。若 WVP 复用外部 MySQL，`ry-wvp` 数据库及用户也必须由数据库管理员或 WVP 部署流程初始化。

## 升级

升级前备份 MySQL。保持 `SPRING_PROFILES_ACTIVE=prod`；生产 profile 使用 `db/migration/release` 中的公开迁移历史，v1.2.6 新增 `V1_2_0_024` 至 `V1_2_0_028`。开发数据库的迁移历史保存在独立的 `db/migration/mainline`，不能在已有数据库上切换迁移目录。

更新 `.env` 中的镜像版本，然后运行：

```powershell
docker compose pull
docker compose up -d
```

## IPC 远程管理

控制面默认启用（`VLSTREAM_TUNNEL_ENABLED=true`），但不会因此启动 tunnel sidecar。`tunnel` Compose profile 仍为可选项，配置好路由与密钥后才可启用；设置 `VLSTREAM_TUNNEL_ENABLED=false` 可关闭控制面。

启用 sidecar 前：

1. 为独立域名配置通配 DNS 和 TLS 证书，例如 `*.ipc.example.com`。
2. 构建运行时并生成 rathole Noise 密钥对：

   ```powershell
   docker build -t vlstream/tunnel-runtime:local .\tunnel-runtime
   docker run --rm --entrypoint /usr/local/bin/rathole vlstream/tunnel-runtime:local --genkey
   ```

3. 分别生成不少于 32 字节的服务签名密钥、控制器 Token 和网关 Token，三者不得复用。
4. 配置 `.env` 中所有 `VLSTREAM_TUNNEL_*` 变量，包括 `VLSTREAM_TUNNEL_GATEWAY_BASE_URL=https://{sessionId}.ipc.example.com`。
5. 参考 `VLStream-Web/VLStream-ui/nginx.conf.example` 配置通配域名 TLS，保留 Host 与 WebSocket Upgrade 头并转发到回环端口 `8088`。
6. 启动可选 profile：

   ```powershell
   docker compose --profile tunnel up -d --build
   docker compose --profile tunnel ps
   ```

公网只开放 rathole 控制端口（默认 `2333`）。网关只绑定 `127.0.0.1`；设备映射端口 `61000–61999` 只在 tunnel 容器内使用，严禁发布。

浏览器访问会话默认在 10 小时无 HTTP 请求后过期。后端验证通过的 HTTP 请求会续期并刷新网关 Cookie；入口令牌仍只允许兑换一次。已吊销、过期或设备不可用时不会续期。WebSocket 只在 Upgrade 请求时续期，后续帧不延长会话。运行时连续两分钟无法刷新期望路由时会停止 rathole。

容器健康不代表真实 IPC 已可访问；仍需在目标环境验证激活、Agent 心跳、路由 `APPLIED`、浏览器登录及设备重连。
