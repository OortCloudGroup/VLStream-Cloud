# VLStream Cloud v1.1.0 部署说明

## 全新一键部署

要求 Docker Engine 24+ 与 Docker Compose v2。解压发布包后执行：

```bash
docker compose up -d
```

默认启动 MySQL、Redis、MinIO、WebRTC-streamer、后端和前端，并在 MySQL 首次创建数据卷时自动导入 `sql/init`。浏览器访问 `http://服务器地址/bus/vls-ui/`。

默认密码仅用于首次体验。生产部署请先执行 `cp .env.example .env`，修改所有 `change-me-*` 项及默认密码。

## WebRTC 视频播放

Compose 使用 WebRTC-streamer v0.8.16，并发布 TCP 8000 与 UDP 50000-50010。前端通过 `/bus/webrtc-streamer-server/` 同源访问信令服务。

如果浏览器不在 Docker 主机上，请在 `.env` 中把 `WEBRTC_EXTERNAL_HOST` 设置为浏览器能够访问的服务器局域网 IP、公网 IP 或域名，并在防火墙放行 UDP 50000-50010。仅本机体验可以保留默认的 `host.docker.internal`。

## 使用已有 MySQL / Redis

填写 `.env` 中的 `EXTERNAL_*` 配置，先将 `sql/init` 脚本导入目标 MySQL，然后执行：

```bash
docker compose -f compose.external.yaml up -d
```

该模式仍会启动 WebRTC-streamer、后端和前端，只复用已有 MySQL 与 Redis。

## 升级数据库

`sql/upgrade` 包含历史升级脚本以及 v1.1.0 新增的 Hi3519DV500 OM 模型路径字段。执行前必须备份数据库，并按文件名顺序人工审核后执行；不要把全量初始化脚本导入已有数据库。

## 常用命令

```bash
docker compose ps
docker compose logs -f backend webrtc-streamer
docker compose pull
docker compose up -d
docker compose down
```

`docker compose down` 不删除数据卷；显式添加 `-v` 会永久删除数据库和对象存储数据。

