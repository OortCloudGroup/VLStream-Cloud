# VLStream Cloud v1.1.0

本版本在 v1.0.0 的一键部署基础上补齐 WebRTC 实时播放服务，并加入 Hi3519DV500 模型导出与部署能力。

## 主要更新

- 新增 WebRTC-streamer v0.8.16 容器、同源反向代理、健康检查及 UDP 媒体端口配置。
- 后端新增 WebRTC 运行时配置与健康状态网关，前端播放入口复用运行时服务地址。
- 支持将模型导出为 Hi3519DV500 SVP ACL OM 格式并下发到目标硬件。
- 模型下载界面支持 OM 格式，数据库新增 `om_model_output_path` 升级脚本。
- 前端切换为本地认证，并改进工作台、视频播放、搜索、表格和标签页布局。
- 补充中英文产品截图与 Hi3519DV500 模型部署说明。

## 部署说明

- 默认 Compose 内置 MySQL、Redis、MinIO、WebRTC-streamer、后端与前端，可执行 `docker compose up -d` 一键启动。
- 从其他计算机播放实时视频时，请设置 `WEBRTC_EXTERNAL_HOST`，并放行服务器 UDP 50000-50010。
- 升级已有数据库前请备份，并审核执行发布包 `sql/upgrade` 中的脚本。

