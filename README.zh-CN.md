<div align="center">
  <img src="./VLStream-Web/VLStream-ui/src/assets/img/img.png" alt="VLStream Cloud" width="160">

  <h1>VLStream Cloud</h1>

  <p><strong>AI 驱动的开源视频物联网与智能流媒体管理平台</strong></p>

  <p>
    <strong>简体中文</strong> |
    <a href="./README.md">English</a>
  </p>

  <p>
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/stargazers"><img src="https://img.shields.io/github/stars/OortCloudGroup/VLStream-Cloud?style=flat-square" alt="GitHub Stars"></a>
    <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="MIT License"></a>
    <img src="https://img.shields.io/badge/Java-8-orange.svg?style=flat-square" alt="Java 8">
    <img src="https://img.shields.io/badge/Spring%20Boot-2.7.11-6DB33F.svg?style=flat-square" alt="Spring Boot 2.7.11">
    <img src="https://img.shields.io/badge/Vue-3.3-42B883.svg?style=flat-square" alt="Vue 3.3">
  </p>

  <p>
    <a href="#-快速开始">快速开始</a> •
    <a href="#-核心特性">核心特性</a> •
    <a href="#-系统截图">系统截图</a> •
    <a href="#-application-scenarios">Application Scenarios</a> •
    <a href="#-技术栈">技术栈</a> •
    <a href="#-部署">部署</a> •
    <a href="#-帮助与支持">帮助</a>
  </p>
</div>

---

## 📖 项目介绍

VLStream Cloud 是面向设备与视频流管理、智能视频分析、算法全生命周期、监控和告警场景的开源视频物联网平台。项目由 Vue 管理控制台和 Spring Boot 多模块后端组成，并提供工作流、权限、任务调度、对象存储等企业级视频应用所需的平台能力。

> [!IMPORTANT]
> 请仅接入已获得合法授权的设备和视频流，并确保部署方式以及智能分析功能的使用符合适用的隐私、安全和数据保护要求。

---

## ✨ 核心特性

| 特性 | 说明 |
| --- | --- |
| 视频设备管理 | 设备注册、分组、标签、状态监控、连接测试、云台控制和流地址获取 |
| 多协议播放 | 面向常见视频物联网场景的 Web 视频播放与低延迟流媒体能力 |
| 智能分析 | 分析请求、实时任务监控、结果管理和事件治理 |
| 算法全生命周期 | 算法仓库、训练任务、标注数据、模型管理，以及 Hi3519DV500 OM 模型转换与设备下发 |
| 工作流自动化 | 基于 Flowable 的流程定义、部署、任务和审批 |
| 企业级权限 | Sa-Token 身份认证、RBAC、数据权限、用户和角色管理 |
| 平台服务 | 定时任务、对象存储、短信、系统监控和 XXL-Job 支持 |
| 可视化运营 | Vue 3 管理控制台、数据看板、GIS、通用 CRUD 组件和多画面视频布局 |

---

### Hi3519DV500 模型下发

在算法管理页选择算法和设备后，平台会查找该算法最新且已生成 OM 文件的训练任务，为设备生成 OM 下载地址，并逐台调用硬件服务的 `latest-training-model` 接口。只有硬件接口返回 HTTP 2xx 后，平台才更新设备与算法的关联。

部署时需要配置以下环境变量：

```bash
VLSTREAM_HARDWARE_DISPATCH_URL=http://192.168.88.98:8888/vlsDeviceInfo/latest-training-model
VLSTREAM_MODEL_DOWNLOAD_PUBLIC_BASE_URL=http://192.168.88.31:8080
VLSTREAM_HARDWARE_DISPATCH_TIMEOUT_MILLIS=10000
```

`VLSTREAM_MODEL_DOWNLOAD_PUBLIC_BASE_URL` 必须是硬件设备能够访问的后端地址，不是浏览器访问的前端地址。设备下载入口不要求平台登录令牌，应仅通过受信任的设备网络或受控反向代理开放。

---

## 🖥️ 系统截图

<table>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/01-active-safety-events.png"><img src="./assets/screenshots/01-active-safety-events.png" alt="主动安全事件管理" width="100%"></a><br>
      <strong>主动安全事件管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/02-event-feedback-workflow.png"><img src="./assets/screenshots/02-event-feedback-workflow.png" alt="事件反馈与流程处理" width="100%"></a><br>
      <strong>事件反馈与流程处理</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/03-work-order-management.png"><img src="./assets/screenshots/03-work-order-management.png" alt="工单管理" width="100%"></a><br>
      <strong>工单管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/04-workflow-designer.png"><img src="./assets/screenshots/04-workflow-designer.png" alt="可视化流程设计" width="100%"></a><br>
      <strong>可视化流程设计</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/05-algorithm-training-management.png"><img src="./assets/screenshots/05-algorithm-training-management.png" alt="算法训练管理" width="100%"></a><br>
      <strong>算法训练管理</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/06-algorithm-training-console.png"><img src="./assets/screenshots/06-algorithm-training-console.png" alt="算法训练控制台" width="100%"></a><br>
      <strong>算法训练控制台</strong>
    </td>
  </tr>
</table>

> 点击任意截图可查看完整分辨率原图。

---

## 🌐 Application Scenarios

<table>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/01-chemical-production-safety.jpg" alt="Chemical production safety" width="100%"><br>
      <strong>Chemical Production Safety</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/02-smart-water-conservancy.jpg" alt="Smart water conservancy" width="100%"><br>
      <strong>Smart Water Conservancy</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/03-wastewater-treatment.jpg" alt="Wastewater treatment" width="100%"><br>
      <strong>Wastewater Treatment</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/04-smart-construction-site.jpg" alt="Smart construction site" width="100%"><br>
      <strong>Smart Construction Site</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/05-smart-community.jpg" alt="Smart community" width="100%"><br>
      <strong>Smart Community</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/06-gas-station-safety.jpg" alt="Gas station safety" width="100%"><br>
      <strong>Gas Station Safety</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/use-cases/07-smart-kitchen.jpg" alt="Smart kitchen" width="100%"><br>
      <strong>Smart Kitchen</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/08-smart-campus.jpg" alt="Smart campus" width="100%"><br>
      <strong>Smart Campus</strong>
    </td>
    <td align="center" width="33%">
      <img src="./assets/use-cases/09-smart-city-management.jpg" alt="Smart city management" width="100%"><br>
      <strong>Smart City Management</strong>
    </td>
  </tr>
</table>

---

## 🧰 技术栈

### 后端

| 分类 | 技术 |
| --- | --- |
| 运行环境 | Java 8 |
| 基础框架 | Spring Boot 2.7.11、RuoYi-Flowable-Plus 0.8.3 |
| 数据访问 | MyBatis-Plus 3.5.3.1 |
| 身份认证 | Sa-Token 1.34.0 |
| 工作流 | Flowable 6.8.0 |
| 缓存与锁 | Redis、Redisson 3.20.1、Lock4j |
| API 文档 | Springdoc OpenAPI、Knife4j |
| 构建工具 | Maven 3.6+ |

### 前端

| 分类 | 技术 |
| --- | --- |
| 基础框架 | Vue 3.3、Vue Router 4 |
| 构建工具 | Vite 4.4 |
| UI 组件 | Element Plus 2.3、Avue 3.7 |
| 状态管理 | Pinia 2.1 |
| 视频播放 | hls.js、xgplayer |
| GIS | Leaflet 1.9 |
| HTTP | Axios 1.4 |

---

## 🗂️ 项目结构

```text
VLStream-Cloud/
├── VLStream-Cloud-Backend-Server/
│   └── vls-stream/                  # Maven 多模块后端
│       ├── ruoyi-admin/             # Spring Boot 主应用与 API
│       ├── ruoyi-common/            # 公共模型和工具
│       ├── ruoyi-framework/         # Web、安全与框架配置
│       ├── ruoyi-system/            # 用户、角色、权限和系统服务
│       ├── ruoyi-vlstream/          # VLStream 业务领域
│       ├── ruoyi-flowable/          # 工作流与审批服务
│       ├── ruoyi-generator/         # 代码生成
│       ├── ruoyi-job/               # 定时任务
│       ├── ruoyi-oss/               # 对象存储
│       ├── ruoyi-sms/               # 短信集成
│       ├── ruoyi-demo/              # 示例与集成测试
│       ├── ruoyi-extend/            # 监控与 XXL-Job 服务
│       ├── deploy/                  # 部署资源
│       └── script/                  # 数据库与 Docker 脚本
├── VLStream-Web/
│   └── VLStream-ui/                 # Vue 3 管理控制台
├── LICENSE
├── README.md                        # 英文文档（默认）
└── README.zh-CN.md                  # 简体中文文档
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 要求 |
| --- | --- |
| Java | JDK 8 |
| Maven | 3.6+ |
| 数据库 | MySQL 5.7+ |
| 缓存 | Redis |
| 前端 | Node.js 与 npm |

### 1. 克隆项目

```powershell
git clone https://github.com/OortCloudGroup/VLStream-Cloud.git
cd VLStream-Cloud
```

### 2. 初始化数据库

```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
mysql -u root -p vlstream --execute="source script/sql/mysql/mysql_ry_v0.8.X.sql"
```

`script/sql/` 下还提供了 Oracle、PostgreSQL 和 SQL Server 的初始化脚本。

### 3. 配置并启动后端

请检查主配置文件和当前环境配置：

- `ruoyi-admin/src/main/resources/application.yml`
- `ruoyi-admin/src/main/resources/application-dev.yml`
- `ruoyi-admin/src/main/resources/application-prod.yml`

Maven Profile 包括 `dev`、`local` 和 `prod`，默认启用 `dev`。

```powershell
mvn -ntp -Pdev clean package
mvn -ntp -Pdev -pl ruoyi-admin spring-boot:run
```

启动后可以访问：

- Knife4j：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

> [!NOTE]
> 后端父 POM 配置了内部 Maven 仓库。解析依赖时可能需要连接项目网络，或者在 Maven `settings.xml` 中配置可用的镜像。

### 4. 启动前端

从仓库根目录打开新的终端：

```powershell
cd VLStream-Web/VLStream-ui
npm install
npm run dev
```

使用 `npm run build` 构建生产环境前端资源。

---

## 🔌 API 示例

### 设备管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/vlsDeviceInfo/page` | 分页查询设备 |
| `GET` | `/vlsDeviceInfo/{id}` | 根据 ID 查询设备 |
| `POST` | `/vlsDeviceInfo` | 新增设备 |
| `PUT` | `/vlsDeviceInfo/{id}` | 更新设备 |
| `DELETE` | `/vlsDeviceInfo/{id}` | 删除设备 |
| `GET` | `/vlsDeviceInfo/statistics` | 获取设备统计信息 |

标准 API 响应使用公共的 `R<T>` 结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

完整、最新的接口列表请以服务启动后生成的 OpenAPI 文档为准。

---

## 🐳 部署

后端目录中提供了 Docker Compose 资源：

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
docker compose -f script/docker/docker-compose.yml up -d
```

停止服务：

```powershell
docker compose -f script/docker/docker-compose.yml down
```

> [!TIP]
> 部分容器基础镜像配置在内部镜像仓库中。在项目网络外部署前，请检查 `script/docker/docker-compose.yml` 和 `dockerfile`。

---

## 📚 项目文档

| 资源 | 链接 |
| --- | --- |
| 前端指南 | [`VLStream-Web/README.md`](./VLStream-Web/README.md) |
| 前端中文指南 | [`VLStream-Web/README-cn.md`](./VLStream-Web/README-cn.md) |
| 后端环境变量 | [`ENVIRONMENT_VARIABLES.md`](./VLStream-Cloud-Backend-Server/vls-stream/ENVIRONMENT_VARIABLES.md) |
| API 文档 | 启动后端后访问 Knife4j 或 Swagger UI |

---

## 🤝 帮助与支持

- **项目主页**：[vls.oortcloudsmart.com](https://vls.oortcloudsmart.com)
- **问题反馈**：[GitHub Issues](https://github.com/OortCloudGroup/VLStream-Cloud/issues)
- **技术支持**：[zhangxuelian@oortcloudsmart.com](mailto:zhangxuelian@oortcloudsmart.com)

欢迎参与项目贡献，包括报告问题、提出功能建议、改进文档或提交 Pull Request。

---

## 📄 开源许可

VLStream Cloud 基于 [MIT License](./LICENSE) 发布。

---

<div align="center">
  <h3>感谢使用 VLStream Cloud</h3>
  <p>如果本项目对你有帮助，欢迎在 GitHub 上点亮 ⭐。</p>
  <p>
    <a href="https://vls.oortcloudsmart.com">项目主页</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/issues">问题反馈</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud">GitHub 仓库</a>
  </p>
  <p>Built with ❤️ by OortCloud</p>
</div>
