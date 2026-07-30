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
