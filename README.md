# VLStream Cloud Backend Service

## Project Introduction

VLStream Cloud is an intelligent video stream management platform built with
Spring Boot, MyBatis-Plus, and a multi-module RuoYi-based backend. It provides
backend APIs for device and stream management, algorithm management, intelligent
analysis, monitoring, and alerting.

The backend also includes APaaS administration capabilities such as user and role
management, fine-grained authorization, and Flowable-based workflow management.

## Technology Stack

- **Java**: JDK 8
- **Backend Framework**: Spring Boot 2.7.11
- **Application Foundation**: RuoYi-Flowable-Plus 0.8.3
- **ORM**: MyBatis-Plus 3.5.3.1
- **Authentication and Authorization**: Sa-Token 1.34.0
- **Workflow Engine**: Flowable 6.8.0
- **Databases**: MySQL by default; SQL scripts are also provided for Oracle,
  PostgreSQL, and SQL Server
- **Cache and Distributed Services**: Redis, Redisson 3.20.1, and Lock4j
- **API Documentation**: Springdoc OpenAPI and Knife4j
- **Build Tool**: Maven 3.6+
- **Containerization**: Docker and Docker Compose

## Project Structure

```text
VLStream-Cloud/
├── VLStream-Cloud-Backend-Server/
│   └── vls-stream/                  # Maven multi-module backend
│       ├── ruoyi-admin/             # Main Spring Boot application and API controllers
│       ├── ruoyi-common/            # Shared models, utilities, and infrastructure
│       ├── ruoyi-framework/         # Web, security, and framework configuration
│       ├── ruoyi-system/            # Users, roles, permissions, and system services
│       ├── ruoyi-vlstream/           # VLStream business domain
│       ├── ruoyi-flowable/           # Workflow and approval services
│       ├── ruoyi-generator/          # Code generation
│       ├── ruoyi-job/                # Scheduled jobs
│       ├── ruoyi-oss/                # Object storage
│       ├── ruoyi-sms/                # SMS integration
│       ├── ruoyi-demo/               # Examples and integration tests
│       ├── ruoyi-extend/             # Monitoring and XXL-Job services
│       ├── deploy/                    # Deployment resources
│       ├── script/                    # Database and Docker scripts
│       └── pom.xml                    # Backend parent Maven project
├── VLStream-Web/                     # Web frontend projects
├── LICENSE
└── README.md                         # Project documentation
```

## Core Function Modules

### 1. Device and Stream Management

- Add, update, delete, and query video devices
- Monitor device status and test device connections
- Organize devices with groups and tags
- Control PTZ devices and retrieve stream information

### 2. Algorithm Management

- Manage algorithm metadata and models
- Manage algorithm training tasks
- Maintain annotation data
- Associate algorithms with devices and analysis tasks

### 3. Intelligent Analysis

- Submit and manage analysis requests
- Query analysis results
- Monitor real-time analysis tasks

### 4. Monitoring and Alerting

- Manage device alerts
- Configure alert rules
- Track alert handling workflows

### 5. Users, Roles, and Permissions

- Manage users and account status
- Assign roles and permissions using an RBAC model
- Apply fine-grained functional and data permissions with Sa-Token
- Support role-based access to administrative and business functions

### 6. Workflow Management

- Define and deploy Flowable processes
- Manage workflow tasks and approvals
- Integrate approval flows with platform business modules

## Quick Start

### Environment Requirements

- JDK 8
- Maven 3.6+
- MySQL 5.7+
- Redis

The backend parent POM references internal Maven repositories. Dependency
resolution may require access to the project network or a compatible mirror in
your Maven `settings.xml`.

### 1. Enter the Backend Directory

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
```

### 2. Initialize the Database

Create a database, then import the provided MySQL schema:

```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
mysql -u root -p vlstream --execute="source script/sql/mysql/mysql_ry_v0.8.X.sql"
```

Equivalent initialization scripts for other supported databases are available
under `script/sql/`.

### 3. Configure the Application

Review the following files and set the database, Redis, and other environment
values required by your deployment:

- `ruoyi-admin/src/main/resources/application.yml`
- `ruoyi-admin/src/main/resources/application-dev.yml`
- `ruoyi-admin/src/main/resources/application-prod.yml`

The Maven profiles are `dev`, `local`, and `prod`; `dev` is active by default.
Because Maven filters profile values into application resources, rerun Maven after
switching profiles.

### 4. Build and Run

Build the complete backend with the development profile:

```powershell
mvn -ntp -Pdev clean package
```

Run the main application from source:

```powershell
mvn -ntp -Pdev -pl ruoyi-admin spring-boot:run
```

Or run the packaged application:

```powershell
java -jar ruoyi-admin/target/apaas-workflowforms.jar
```

The default server port is `8080`. After startup, API documentation is available
at:

- Knife4j: `http://localhost:8080/doc.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## API Interfaces

### Device Management API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/vlsDeviceInfo/page` | Query devices with pagination |
| `GET` | `/vlsDeviceInfo/{id}` | Query a device by ID |
| `POST` | `/vlsDeviceInfo` | Add a device |
| `PUT` | `/vlsDeviceInfo/{id}` | Update a device |
| `DELETE` | `/vlsDeviceInfo/{id}` | Delete a device |
| `GET` | `/vlsDeviceInfo/statistics` | Retrieve device statistics |

Use the generated OpenAPI documentation for the complete and current API list.

### Response Format

Standard API responses use the shared `R<T>` structure:

```json
{
  "code": 200,
  "msg": "Operation successful",
  "data": {}
}
```

## Docker Deployment

Docker Compose resources are provided in the backend directory:

```powershell
docker compose -f script/docker/docker-compose.yml up -d
```

To stop the services:

```powershell
docker compose -f script/docker/docker-compose.yml down
```

Some configured container base images are hosted on an internal registry and may
require access to the project network. Review the compose file and `dockerfile`
before deploying outside that environment.

## Contact Information

- **Project Homepage**: https://vls.oortcloudsmart.com
- **Technical Support**: zhangxuelian@oortcloudsmart.com
