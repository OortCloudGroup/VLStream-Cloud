# VLStream-Web

> VLStream Cloud — AI 驱动的开源视频物联网（Video IoT）平台前端

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Vue](https://img.shields.io/badge/Vue-3.3-42b883.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-4.4-646cff.svg)](https://vitejs.dev/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.3-409eff.svg)](https://element-plus.org/)

VLStream-Web 是一个高性能、AI 驱动的开源 Web 视频平台，将传统视频监控升级为智能化的视觉管理。平台融合了低延迟视频流、强大的 AI 分析能力与灵活的可扩展性，为构建智能、高效、安全的视频物联网系统提供了坚实的基础。

本仓库为平台的**前端工程**（位于 [`VLStream-ui/`](./VLStream-ui)），基于 Vue 3 + Vite 构建，对接基于 [SpringBlade](https://bladex.cn/) 微服务框架的后端网关与统一身份认证（SSO）。

---

## 目录

- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [功能模块](#功能模块)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [构建与部署](#构建与部署)
- [架构与认证](#架构与认证)
- [许可证](#许可证)

---

## 核心特性

- 🎥 **低延迟视频流** — 支持 RTSP / HLS / WebRTC 多协议接入，内置视频广场、多画面布局、云台（PTZ）控制、视频回放。
- 🤖 **AI 智能分析** — 算法仓库管理、算法训练 / 标注 / 模型管理、智能分析申请与结果、事件管理与场景治理。
- 🗺️ **GIS 地图联动** — 基于 Leaflet 的地图可视化，设备与摄像头空间分布一目了然。
- 🧩 **AI 算力调度** — 容器实例管理，弹性调度算法推理资源。
- 🔐 **企业级权限** — 对接统一用户平台（SSO），支持用户、角色、菜单、部门、岗位、租户、数据权限、接口权限的完整 RBAC 体系。
- 🌐 **开箱即用** — Element Plus + Avue 表单/表格引擎，CRUD Mixin，大量可复用业务组件。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 框架 | [Vue 3](https://vuejs.org/) (Composition API) |
| 构建工具 | [Vite 4](https://vitejs.dev/) |
| UI 组件库 | [Element Plus](https://element-plus.org/) + [@smallwei/avue](https://avuejs.com/) |
| 状态管理 | [Pinia](https://pinia.vuejs.org/) |
| 路由 | [Vue Router 4](https://router.vuejs.org/) |
| HTTP | [axios](https://axios-http.com/)（封装多实例 + 拦截器） |
| 视频播放 | [hls.js](https://github.com/video-dev/hls.js/)、OPlayer |
| 地图 | [Leaflet](https://leafletjs.com/) |
| 加密 | [sm-crypto](https://github.com/JuneAndGreen/sm-crypto)（国密 SM2/SM4） |
| 其他 | markdown-it、nprogress、vue-i18n |

## 项目结构

```
VLStream-Web/
├── LICENSE                         # MIT 许可证
├── README.md                       # 本文档
└── VLStream-ui/                    # 前端工程根目录
    ├── index.html                  # 应用入口 HTML
    ├── package.json                # 依赖与脚本
    ├── vite.config.js              # Vite 配置（base 路径、开发代理）
    ├── nginx.conf.example          # 生产环境 Nginx 配置示例
    ├── build-prod.bat              # Windows 一键构建脚本
    ├── scripts/                    # 部署 / 重构辅助脚本
    ├── public/                     # 静态资源（OPlayer 等）
    └── src/
        ├── main.js                 # 应用入口
        ├── App.vue                 # 根组件
        ├── router/                 # 路由配置 + 全局守卫（含 SSO 鉴权）
        ├── layout/                 # 全局布局框架
        ├── api/                    # 后端接口封装（按业务模块拆分）
        ├── views/                  # 页面（按业务域分目录，见下文）
        ├── components/             # 全局可复用组件
        ├── utils/                  # 工具库（请求、鉴权、Token 同步等）
        ├── mixins/                 # CRUD 等通用 mixin
        └── assets/                 # 图片、样式等静态资源
```

`src/views/` 业务域划分：

| 目录 | 业务域 |
| --- | --- |
| `VideoAggregation/` | 视频聚合：视频广场、设备管理、视频回放、监控告警、标签管理、摄像机设置、智能分析结果 |
| `DecisionAI/` | 决策 AI：事件管理、场景治理、智能分析申请、设计图、场景编辑、算法配置、任务配置 |
| `AlgorithmWarehouse/` | 算法仓库：算法管理、算法训练平台 |
| `AlgorithmTraining/` | 算法训练：算法标注、算法模型、算法训练、SSH 连接 |
| `AIComputingScheduling/` | AI 算力调度：容器实例 |
| `System/` | 系统管理：用户、角色、菜单、部门、岗位、租户、数据权限、接口权限 |

## 功能模块

- **工作台** — 平台总览首页
- **视频广场 / 设备管理** — 多画面实时视频、设备接入与组织管理
- **视频回放 / 监控告警** — 历史录像检索回放、实时告警处置
- **事件管理 / 场景治理** — AI 事件接警、场景化治理编排
- **智能分析（申请 / 结果）** — 发起智能分析任务、查看分析结果
- **算法管理 / 算法训练平台** — 算法上架发布、训练任务调度
- **算法标注 / 模型 / 训练** — 数据集标注、模型管理、训练作业、SSH 远程连接
- **容器实例** — 算力容器调度与监控
- **系统管理** — 完整的 RBAC 权限与租户体系
- **个人设置 / 开放服务中心**

## 快速开始

### 环境要求

- [Node.js](https://nodejs.org/) **16 及以上**（推荐 18 LTS）
- npm（随 Node 安装）或 pnpm / yarn
- 后端服务：SpringBlade 网关默认监听 `http://127.0.0.1:8080`（可通过环境变量覆盖）

### 安装与本地开发

```bash
# 1. 进入前端工程目录
cd VLStream-ui

# 2. 安装依赖
npm install

# 3. 启动开发服务器（默认端口 3000，监听所有网卡）
npm run dev
```

启动后访问：<http://localhost:3000/>

> 开发模式下，所有后端请求（`/system`、`/blade-auth`、`/vls`、`/api`、`/sso` 等前缀）会通过 Vite 代理转发到后端，默认目标为 `http://127.0.0.1:8080`。

### 其他脚本

```bash
npm run build     # 构建生产版本，产物输出到 dist/
npm run preview   # 本地预览构建产物
npm run lint      # ESLint 代码检查并自动修复
```

Windows 下也可直接运行一键构建脚本：

```bash
# 在 VLStream-ui 目录下
build-prod.bat
```

## 环境配置

项目通过 Vite 环境变量进行配置。在 `VLStream-ui/` 目录下创建 `.env.local`（不会被提交到 Git）按需覆盖：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `VITE_DEV_PROXY_TARGET` | 开发环境后端代理目标 | `http://127.0.0.1:8080` |
| `VITE_SSO_PROXY_TARGET` | 开发环境 SSO 代理目标 | `http://oort.oortcloudsmart.com:21410/bus/apaas-sso` |
| `VITE_API_BASE_URL` | 生产环境接口基址 | `http://oort.oortcloudsmart.com:21410/bus/vls-server` |
| `VITE_BLADE_CLIENT_AUTH_HEADER` | OAuth Client Basic 认证头 | `Basic c2FiZXI6c2FiZXJfc2VjcmV0` |

示例 `.env.local`：

```ini
# 将开发请求代理到本地后端
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
```

## 构建与部署

### 1. 构建生产产物

```bash
cd VLStream-ui
npm run build
```

产物目录：`VLStream-ui/dist/`

> ⚠️ 注意：生产构建的 `base` 路径固定为 `/bus/vls-ui/`（见 `vite.config.js`），需确保部署路径与之匹配。

### 2. 使用 Nginx 部署

将 `dist/` 内的全部文件部署到 Nginx 静态目录（如 `/usr/share/nginx/html/`），并参考 [`VLStream-ui/nginx.conf.example`](./VLStream-ui/nginx.conf.example) 配置站点：

```nginx
server {
    listen 21410;
    server_name your-domain.com;

    # 前端静态资源
    location /bus/vls-ui/ {
        alias /usr/share/nginx/html/;
        try_files $uri $uri/ /bus/vls-ui/index.html;
        # 如需多线程视频处理，启用以下跨域隔离头：
        # add_header Cross-Origin-Embedder-Policy "require-corp";
        # add_header Cross-Origin-Opener-Policy "same-origin";
    }

    # 代理后端网关 API（vls-server）
    location /bus/vls-server/ {
        proxy_pass http://your-backend-gateway:18080/;
        # ... proxy_set_header / CORS 头，详见示例配置
    }

    # 代理统一用户平台 API（apaas-sso）
    location /bus/apaas-sso/ {
        proxy_pass http://your-backend-gateway:18080/;
        # ...
    }
}
```

部署要点：

- 静态站点需以 `/bus/vls-ui/` 为根路径提供服务，并开启 History 模式的 `try_files` 回退到 `index.html`。
- 后端网关与 SSO 网关由 Nginx 反向代理转发，避免前端跨域问题。
- 如需启用 `SharedArrayBuffer`（多线程视频解码），需配置 `COEP` / `COOP` 响应头。

## 架构与认证

### 后端架构

前端为纯前端工程，所有业务能力依赖后端服务：

- **后端框架**：SpringBlade 微服务（`blade-auth` 认证、`blade-system` 系统、`blade-job` 调度、`vls-*` 业务服务等）。
- **统一认证（SSO）**：对接 `apaas-sso` 统一用户平台，支持外部平台登录态透传、URL Token 跳转、本地 Token 缓存多级回退。
- **网关**：生产环境通过 Nginx 反向代理统一暴露于 `/bus/vls-server/`。

### 认证机制

`src/utils/request.js` 中封装了三套 axios 实例（业务请求、图片上传、认证请求），统一处理：

- **请求头注入**：自动写入 `Authorization`、`blade-auth`、`accesstoken`，兼容 SpringBlade 的 JWT 认证。
- **OAuth Client 认证**：登录取 Token 时使用 Basic Client 认证头。
- **Token 多级读取**：URL 参数 → sessionStorage → localStorage。
- **统一错误处理**：识别 SpringBlade `success: false` 业务错误，并按 HTTP 状态码给出中文提示。

`src/utils/` 下还包含完整的跨系统 Token 同步工具（`tokenSync`、`crossSystemTokenSync`、`cookieTokenSync`、`storageEventSync` 等），用于与外部统一用户平台保持登录态一致。

## 许可证

本项目基于 [MIT License](./LICENSE) 开源。

Copyright (c) 2026 VLStream
