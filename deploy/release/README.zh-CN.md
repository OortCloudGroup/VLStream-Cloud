# VLStream Cloud v1.2.2 部署指南

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

WVP 是 VLStream 的必选依赖和唯一视频设备中心，本部署包不重复打包 WVP。请先下载并启动
[APaaS WVP Server v1.0.3](https://github.com/OortCloudGroup/apaas-wvp-server/releases/tag/v1.0.3)。
两套服务在同一台主机运行时，请在 WVP 的 `.env` 中设置 `WVP_HTTP_PORT=9080`，避免与
VLStream 后端的 8080 端口冲突；WVP 的 `ZLM_SECRET` 必须与 VLStream 的
`ZLMEDIAKIT_SECRET` 保持一致。

`VLSTREAM_WVP_INTERNAL_BASE_URL` 与 `VLSTREAM_WVP_DEVICE_BASE_URL` 是两个后端契约：
前者用于通用 WVP 调用，后者用于通过 `/internal/vlstream/device/{deviceId}` 查询 VLS
设备；`WVP_UPSTREAM` 是浏览器侧 WVP 代理。`VLSTREAM_ZLM_INTERNAL_URL` 与
`ZLM_UPSTREAM` 指向 WVP 启动的 ZLMediaKit。发布包提供的同机默认地址分别为
`host.docker.internal:9080` 和 `host.docker.internal:8081`。

工作流通知需要调用平台消息服务时，请设置 `UNIFIEDMESSAGINGSEND_URL`；未部署该集成时可留空。
即使 XXL-Job 已禁用，`XXL_JOB_ADMIN_ADDRESSES` 也必须保留一个有效 URL。

硬件事件图片上传时，生产环境必须保持
`VLSTREAM_DEVICE_MEDIA_ALLOW_UNAUTHENTICATED=false`，并将
`VLSTREAM_DEVICE_MEDIA_OSS_CONFIG_KEY` 指向已启用的 `sys_oss_config` 记录。该配置的
endpoint 必须可由设备访问，不能只填写容器内部的 `minio:9000`。

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
