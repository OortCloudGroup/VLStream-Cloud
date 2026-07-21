<div align="center">
  <img src="./VLStream-Web/VLStream-ui/src/assets/img/img.png" alt="VLStream Cloud" width="160">

  <h1>VLStream Cloud</h1>

  <p><strong>AI-Driven Open-Source Video IoT and Intelligent Stream Management Platform</strong></p>

  <p>
    <a href="./README.zh-CN.md">简体中文</a> |
    <strong>English</strong>
  </p>

  <p>
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/stargazers"><img src="https://img.shields.io/github/stars/OortCloudGroup/VLStream-Cloud?style=flat-square" alt="GitHub Stars"></a>
    <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="MIT License"></a>
    <img src="https://img.shields.io/badge/Java-8-orange.svg?style=flat-square" alt="Java 8">
    <img src="https://img.shields.io/badge/Spring%20Boot-2.7.11-6DB33F.svg?style=flat-square" alt="Spring Boot 2.7.11">
    <img src="https://img.shields.io/badge/Vue-3.3-42B883.svg?style=flat-square" alt="Vue 3.3">
  </p>

  <p>
    <a href="#-quick-start">Quick Start</a> •
    <a href="#-key-features">Key Features</a> •
    <a href="#-system-screenshots">System Screenshots</a> •
    <a href="#-application-scenarios">Application Scenarios</a> •
    <a href="#-technology-stack">Technology Stack</a> •
    <a href="#-deployment">Deployment</a> •
    <a href="#-help-and-support">Help</a>
  </p>
</div>

---

## 📖 Project Description

VLStream Cloud is an open-source Video IoT platform for device and stream
management, intelligent video analysis, algorithm lifecycle management,
monitoring, and alerting. It combines a Vue-based management console with a
Spring Boot multi-module backend and provides workflow, permission, scheduling,
object storage, and operational support for enterprise video applications.

> [!IMPORTANT]
> Connect only devices and video streams that you are authorized to access. Make
> sure your deployment and use of intelligent analysis comply with applicable
> privacy, security, and data-protection requirements.

---

## ✨ Key Features

| Feature | Description |
| --- | --- |
| Video Device Management | Device registration, grouping, tagging, health monitoring, connection tests, PTZ control, and stream discovery |
| Multi-Protocol Playback | Web video playback and low-latency streaming capabilities for common Video IoT scenarios |
| Intelligent Analysis | Analysis requests, real-time task monitoring, result management, and event governance |
| Algorithm Lifecycle | Algorithm warehouse, training tasks, annotations, model management, Hi3519DV500 OM conversion, and device deployment |
| Workflow Automation | Flowable-based process definition, deployment, tasks, and approval workflows |
| Enterprise Permissions | Sa-Token authentication, RBAC, data permissions, user management, and role management |
| Platform Services | Scheduled jobs, object storage, SMS integration, monitoring, and XXL-Job support |
| Visual Operations | Vue 3 management console with dashboards, GIS views, reusable CRUD components, and video layouts |

---

### Hi3519DV500 Model Deployment

After an operator selects an algorithm and one or more devices, VLStream locates the latest training task with an OM artifact, creates a device-accessible OM download URL, and calls the hardware `latest-training-model` endpoint once per device. The device-to-algorithm association is updated only after the hardware endpoint returns an HTTP 2xx response.

Configure the deployment integration with these environment variables:

```bash
VLSTREAM_HARDWARE_DISPATCH_URL=http://192.168.88.98:8888/vlsDeviceInfo/latest-training-model
VLSTREAM_MODEL_DOWNLOAD_PUBLIC_BASE_URL=http://192.168.88.31:8080
VLSTREAM_HARDWARE_DISPATCH_TIMEOUT_MILLIS=10000
```

`VLSTREAM_MODEL_DOWNLOAD_PUBLIC_BASE_URL` must point to the backend address reachable from the hardware network, not the browser-facing frontend address. The device download route does not require a platform login token, so expose it only on a trusted device network or through a restricted reverse proxy.

---

## 🖥️ System Screenshots

<table>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/01-active-safety-events.png"><img src="./assets/screenshots/01-active-safety-events.png" alt="Active safety event management" width="100%"></a><br>
      <strong>Active Safety Event Management</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/02-event-feedback-workflow.png"><img src="./assets/screenshots/02-event-feedback-workflow.png" alt="Event feedback and workflow" width="100%"></a><br>
      <strong>Event Feedback &amp; Workflow</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/03-work-order-management.png"><img src="./assets/screenshots/03-work-order-management.png" alt="Work order management" width="100%"></a><br>
      <strong>Work Order Management</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/04-workflow-designer.png"><img src="./assets/screenshots/04-workflow-designer.png" alt="Visual workflow designer" width="100%"></a><br>
      <strong>Visual Workflow Designer</strong>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="./assets/screenshots/05-algorithm-training-management.png"><img src="./assets/screenshots/05-algorithm-training-management.png" alt="Algorithm training management" width="100%"></a><br>
      <strong>Algorithm Training Management</strong>
    </td>
    <td align="center" width="50%">
      <a href="./assets/screenshots/06-algorithm-training-console.png"><img src="./assets/screenshots/06-algorithm-training-console.png" alt="Algorithm training console" width="100%"></a><br>
      <strong>Algorithm Training Console</strong>
    </td>
  </tr>
</table>

> Click any screenshot to view it at full resolution.

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
      <img src="./assets/use-cases/06-gas-station-safety.jpg" alt="Gas station safety supervision" width="100%"><br>
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

## 🧰 Technology Stack

### Backend

| Category | Technology |
| --- | --- |
| Runtime | Java 8 |
| Framework | Spring Boot 2.7.11, RuoYi-Flowable-Plus 0.8.3 |
| Persistence | MyBatis-Plus 3.5.3.1 |
| Authentication | Sa-Token 1.34.0 |
| Workflow | Flowable 6.8.0 |
| Cache and Locking | Redis, Redisson 3.20.1, Lock4j |
| API Documentation | Springdoc OpenAPI, Knife4j |
| Build | Maven 3.6+ |

### Frontend

| Category | Technology |
| --- | --- |
| Framework | Vue 3.3, Vue Router 4 |
| Build Tool | Vite 4.4 |
| UI | Element Plus 2.3, Avue 3.7 |
| State Management | Pinia 2.1 |
| Video | hls.js, xgplayer |
| GIS | Leaflet 1.9 |
| HTTP | Axios 1.4 |

---

## 🗂️ Project Structure

```text
VLStream-Cloud/
├── VLStream-Cloud-Backend-Server/
│   └── vls-stream/                  # Maven multi-module backend
│       ├── ruoyi-admin/             # Main Spring Boot application and APIs
│       ├── ruoyi-common/            # Shared models and utilities
│       ├── ruoyi-framework/         # Web, security, and framework configuration
│       ├── ruoyi-system/            # Users, roles, permissions, and system services
│       ├── ruoyi-vlstream/          # VLStream business domain
│       ├── ruoyi-flowable/          # Workflow and approval services
│       ├── ruoyi-generator/         # Code generation
│       ├── ruoyi-job/               # Scheduled jobs
│       ├── ruoyi-oss/               # Object storage
│       ├── ruoyi-sms/               # SMS integration
│       ├── ruoyi-demo/              # Examples and integration tests
│       ├── ruoyi-extend/            # Monitoring and XXL-Job services
│       ├── deploy/                  # Deployment resources
│       └── script/                  # Database and Docker scripts
├── VLStream-Web/
│   └── VLStream-ui/                 # Vue 3 management console
├── LICENSE
├── README.md                        # English documentation (default)
└── README.zh-CN.md                  # Simplified Chinese documentation
```

---

## 🚀 Quick Start

### Requirements

| Component | Requirement |
| --- | --- |
| Java | JDK 8 |
| Maven | 3.6+ |
| Database | MySQL 5.7+ |
| Cache | Redis |
| Frontend | Node.js and npm |

### 1. Clone the Repository

```powershell
git clone https://github.com/OortCloudGroup/VLStream-Cloud.git
cd VLStream-Cloud
```

### 2. Initialize the Database

```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
mysql -u root -p vlstream --execute="source script/sql/mysql/mysql_ry_v0.8.X.sql"
```

SQL initialization scripts for Oracle, PostgreSQL, and SQL Server are also
available under `script/sql/`.

### 3. Configure and Start the Backend

Review the main configuration and the active profile configuration:

- `ruoyi-admin/src/main/resources/application.yml`
- `ruoyi-admin/src/main/resources/application-dev.yml`
- `ruoyi-admin/src/main/resources/application-prod.yml`

The Maven profiles are `dev`, `local`, and `prod`; `dev` is active by default.

```powershell
mvn -ntp -Pdev clean package
mvn -ntp -Pdev -pl ruoyi-admin spring-boot:run
```

After startup:

- Knife4j: `http://localhost:8080/doc.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

> [!NOTE]
> The backend parent POM references internal Maven repositories. Dependency
> resolution may require access to the project network or a compatible mirror in
> your Maven `settings.xml`.

### 4. Start the Frontend

Open a new terminal from the repository root:

```powershell
cd VLStream-Web/VLStream-ui
npm install
npm run dev
```

Use `npm run build` to create a production frontend bundle.

---

## 🔌 API Preview

### Device Management

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/vlsDeviceInfo/page` | Query devices with pagination |
| `GET` | `/vlsDeviceInfo/{id}` | Query a device by ID |
| `POST` | `/vlsDeviceInfo` | Add a device |
| `PUT` | `/vlsDeviceInfo/{id}` | Update a device |
| `DELETE` | `/vlsDeviceInfo/{id}` | Delete a device |
| `GET` | `/vlsDeviceInfo/statistics` | Retrieve device statistics |

Standard API responses use the shared `R<T>` structure:

```json
{
  "code": 200,
  "msg": "Operation successful",
  "data": {}
}
```

Use the generated OpenAPI documentation for the complete and current API list.

---

## 🐳 Deployment

Docker Compose resources are provided in the backend directory:

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
docker compose -f script/docker/docker-compose.yml up -d
```

Stop the services with:

```powershell
docker compose -f script/docker/docker-compose.yml down
```

> [!TIP]
> Some configured container base images are hosted on an internal registry.
> Review `script/docker/docker-compose.yml` and `dockerfile` before deploying
> outside the project network.

---

## 📚 Documentation

| Resource | Link |
| --- | --- |
| Frontend Guide | [`VLStream-Web/README.md`](./VLStream-Web/README.md) |
| Frontend Guide (Chinese) | [`VLStream-Web/README-cn.md`](./VLStream-Web/README-cn.md) |
| Backend Environment Variables | [`ENVIRONMENT_VARIABLES.md`](./VLStream-Cloud-Backend-Server/vls-stream/ENVIRONMENT_VARIABLES.md) |
| API Documentation | Start the backend and open Knife4j or Swagger UI |

---

## 🤝 Help and Support

- **Project Homepage**: [vls.oortcloudsmart.com](https://vls.oortcloudsmart.com)
- **Issue Tracker**: [GitHub Issues](https://github.com/OortCloudGroup/VLStream-Cloud/issues)
- **Technical Support**: [zhangxuelian@oortcloudsmart.com](mailto:zhangxuelian@oortcloudsmart.com)

Contributions are welcome. You can report bugs, propose features, improve the
documentation, or submit pull requests.

---

## 📄 License

VLStream Cloud is released under the [MIT License](./LICENSE).

---

<div align="center">
  <h3>Thank you for using VLStream Cloud</h3>
  <p>If this project helps you, consider giving it a ⭐ on GitHub.</p>
  <p>
    <a href="https://vls.oortcloudsmart.com">Project Homepage</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud/issues">Issue Tracker</a> •
    <a href="https://github.com/OortCloudGroup/VLStream-Cloud">GitHub Repository</a>
  </p>
  <p>Built with ❤️ by OortCloud</p>
</div>
