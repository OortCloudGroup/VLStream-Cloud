# VLStream Cloud v1.0.0

首个正式发布版本，包含前端、后端以及完整的 Docker Compose 本地部署方案。

## 部署能力

- 提供公开 GHCR 前端与后端镜像。
- 默认 Compose 内置 MySQL、Redis、MinIO，可执行 `docker compose up -d` 一键启动。
- 提供外部 MySQL / Redis 部署文件，便于接入已有基础设施。
- 发布附件包含全新初始化 SQL、历史数据以及升级 SQL（按 4C 方案原样发布）。

请在生产环境使用前修改默认密码，并先阅读发布包内的 `README.zh-CN.md`。
