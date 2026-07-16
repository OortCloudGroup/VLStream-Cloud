# VLStream Cloud v1.0.0 部署说明

## 全新一键部署

要求 Docker Engine 24+ 与 Docker Compose v2。解压发布包后执行：

```bash
docker compose up -d
```

默认会启动 MySQL、Redis、MinIO、后端和前端，并在 MySQL 首次创建数据卷时自动导入 `sql/init`。浏览器访问 `http://服务器地址/bus/vls-ui/`。

默认密码用于保证首次体验可直接启动。生产部署请先执行 `cp .env.example .env`，修改所有 `change-me-*` 项以及默认密码，再运行 Compose。

## 使用已有 MySQL / Redis

复制环境变量模板，填写 `EXTERNAL_*` 配置，并先将 `sql/init` 中脚本导入目标 MySQL：

```bash
cp .env.example .env
docker compose -f compose.external.yaml up -d
```

## 升级数据库

`sql/upgrade` 包含本版本保留的升级脚本。执行前必须备份数据库，并根据当前数据库版本按文件名顺序人工审核后执行；不要把全量初始化脚本导入已有数据库。

## 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose pull
docker compose up -d
docker compose down
```

`docker compose down` 不删除数据卷；如显式添加 `-v` 会永久删除数据库和对象存储数据。
