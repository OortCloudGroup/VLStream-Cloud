# VLStream Cloud v1.2.0 部署说明

请下载并解压 GitHub Release 发布包。发布包中包含 Compose 文件、环境变量模板和已清理的
VLStream 初始化 SQL。

## 一键安装

```powershell
Copy-Item .env.example .env
docker compose up -d
```

生产环境启动前，请替换 `.env` 中所有 `change-me` 配置。

- 访问地址：`http://localhost/bus/vls-ui/`
- 默认账号：`admin`
- 默认密码：`Codex@123456`

首次登录后请立即修改应用密码。

默认会启动 MySQL、Redis、MinIO、WebRTC-streamer、ZLMediaKit、VLStream 后端、WVP
后端和前端。MySQL 中使用两个相互独立的数据库：`oortcloud_workflowforms_vls` 和
`ry-wvp`；WVP 复用 Redis，但固定使用第 `10` 号库。

## 设备和媒体端口

设备或浏览器不在 Docker 主机上时，请把以下配置改成 Docker 主机可访问的局域网 IP、
公网 IP 或域名：

```dotenv
SIP_PUBLIC_IP=192.168.1.10
ZLMEDIAKIT_PUBLIC_HOST=192.168.1.10
```

主机防火墙和云安全组需要放行：

- `8116/tcp`、`8116/udp`：GB28181 SIP
- `40000-40300/tcp`、`40000-40300/udp`：ZLMediaKit RTP
- `554/tcp`：RTSP
- `1935/tcp`：RTMP
- `8000/udp`：ZLMediaKit WebRTC
- `50000-50010/udp`：WebRTC-streamer 媒体端口

Linux ISUP 和大华原生 SDK 服务默认关闭，因为公开镜像不包含对应厂商运行库。

## 使用已有 MySQL 和 Redis

启动前请创建数据库并为对应用户授权：

- `oortcloud_workflowforms_vls`
- `ry-wvp`

填写 `.env` 中的 `EXTERNAL_*` 配置后执行：

```powershell
docker compose -f compose.external.yaml up -d
```

## 数据库升级

只有内置 MySQL 数据卷为空时才会导入 `sql/init/*`。之后 VLStream 和 WVP 后端每次启动
都会分别通过 Flyway 自动执行新增 SQL。不要修改或手工重复执行已经可能运行过的迁移；
升级前必须同时备份两个数据库。

更新 `.env` 中的镜像版本后执行：

```powershell
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f backend wvp-backend zlmediakit frontend
```

`docker compose down` 会保留命名数据卷；增加 `-v` 会永久删除数据库、对象存储和服务数据。
