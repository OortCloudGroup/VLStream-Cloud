# VLStream Cloud 部署指南

此目录是每个 GitHub Release 部署包的源模板。一键安装时请下载 Release 压缩包，
因为压缩包中还包含完整的数据库初始化 SQL。

## 快速启动

```powershell
Copy-Item .env.example .env
docker compose up -d
```

- 本地访问：`http://localhost/bus/vls-ui/`
- 公网占位地址：`https://www.example.com/bus/vls-ui/`
- 默认账号：`admin`
- 默认密码：`Codex@123456`

启动前请修改 `.env` 中的所有密码，首次登录后请立即修改系统默认密码。

WVP 是 VLStream 的必选依赖和唯一视频设备中心。请先部署
[VLStream WVP Lite v1.0.11](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.11)，
其公开镜像为 `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.11`。本部署包不重复启动 WVP。
同机部署时，请将 WVP 的 HTTP 主机端口配置为 9080。后端通过
`VLSTREAM_WVP_DEVICE_BASE_URL` 访问 WVP 设备和租户 API。前端代理地址分别由
`WVP_UPSTREAM` 和 `ZLM_UPSTREAM` 配置，默认指向宿主机的 9080 和 8081 端口。
跨主机部署时，后端和前端地址都必须能从各自容器访问。ZLMediaKit 仍由 WVP 管理。
WVP v1.0.11 支持 Cloud AI 所需的跨协议视频源授权，并增加租户范围的事件和 ONVIF 控制。
视频设备共享权限仍需显式配置：请将 `vlstream:device:share:manage` 和
`vlstream:device:share:approve` 分配给目标角色，迁移不会自动授予已有角色。

默认 Compose 会启动 MySQL、Redis、MinIO、WebRTC-streamer、后端和前端。如果使用
已有的 MySQL 与 Redis，请填写外部服务连接变量后执行：

```powershell
docker compose -f compose.external.yaml up -d
```

## 数据库升级

MySQL 仅在数据卷为空时导入 `sql/init/*.sql`。完成首次安装后，每次后端启动都会由
Flyway 自动执行尚未运行的新迁移文件。升级前请备份数据库，已经运行过的迁移文件不能修改。

在 `.env` 中更新镜像版本后执行：

```powershell
docker compose pull
docker compose up -d
```

查看状态和日志：

```powershell
docker compose ps
docker compose logs -f backend frontend
```

## 可选的 IPC 远程管理

跨机器局域网验收、公开配置生成器、无凭据客户端探针与线上检查清单见
[IPC网关部署指南](../../docs/IPC_GATEWAY_DEPLOYMENT.md)。`.ipc.localhost`仅适合开发机，
分享最终页面URL不能转移浏览器会话。生成模板不修改DNS、证书信任或正在运行的服务。

`tunnel-runtime` profile 将固定版本的 rathole Server 与受限的
HTTP/WebSocket 网关放在同一容器中，默认不启用。

启用前必须：

1. 为独立域名配置通配 DNS 和 TLS 证书，例如 `*.ipc.example.com`。
2. 构建运行时并生成 rathole Noise 密钥对：

   ```powershell
   docker build -t vlstream/tunnel-runtime:local .\tunnel-runtime
   docker run --rm --entrypoint /usr/local/bin/rathole vlstream/tunnel-runtime:local --genkey
   ```

3. 分别生成不少于 32 字节的服务签名密钥、控制器 Token 和网关 Token；
   三者不能复用，不能提交到仓库。
4. 配置 `.env` 中全部 `VLSTREAM_TUNNEL_*` 变量，其中网关地址应类似
   `https://{sessionId}.ipc.example.com`。
5. 参考 `VLStream-Web/VLStream-ui/nginx.conf.example` 配置通配域名 TLS，
   保留 Host 和 WebSocket Upgrade 头并转发到宿主机回环端口 8088。
6. 启动：

   ```powershell
   docker compose --profile tunnel up -d --build
   docker compose --profile tunnel ps
   ```

公网只开放 rathole 控制端口（默认 2333）。网关仅发布到
`127.0.0.1`，61000-61999 的设备映射端口只存在于运行时容器内，严禁发布。
控制面连续两分钟不可达时运行时会停止 rathole，按失败关闭处理。

浏览器访问会话默认空闲 10 小时过期；首次打开及后续 HTTP 请求经后端校验后续期 10 小时，并刷新 Cookie。
入口令牌仍只允许兑换一次；已过期、吊销或设备不可用时不续期。仅保持页面打开不会自行续期，页面后台
HTTP 轮询会计入访问；WebSocket 在 Upgrade 请求时续期，后续帧不单独延长会话。
`VLSTREAM_TUNNEL_ACCESS_SESSION_TTL_SECONDS=36000` 可覆盖为 60～36000 秒。
此配置不改变激活码或 IPC 自身登录的有效期；后端与网关需同步更新以支持续期接口。

容器健康不代表真实 IPC 已可访问；仍需验证激活、Agent 心跳、路由
`APPLIED`、浏览器登录及设备重启/断网恢复。
