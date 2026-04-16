# VLStream Cloud Backend Service

## Project Introduction

VLStream Cloud is an intelligent video stream management system developed based on SpringBoot + MyBatis Plus + MySQL technology stack, providing backend API services for device management, algorithm management, intelligent analysis, monitoring and alerting functions.

## Technology Stack

- **Framework**: Spring Boot 2.7.18
- **ORM**: MyBatis Plus 3.5.3.1
- **Database**: MySQL 8.0+
- **Cache**: Redis 6.0+
- **API Documentation**: Knife4j 3.0.3
- **Tool Libraries**: Hutool, Apache Commons Lang3
- **JSON**: FastJSON 2.0.25

## Project Structure

```
VLStream-server/
├── src/
│   ├── main/
│   │   ├── java/com/vlstream/server/
│   │   │   ├── entity/           # Entity classes
│   │   │   ├── mapper/           # Data access layer
│   │   │   ├── service/          # Business logic layer
│   │   │   ├── controller/       # Controller layer
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── common/           # Common classes
│   │   │   └── utils/            # Utility classes
│   │   └── resources/
│   │       ├── application.yml   # Application configuration
│   │       └── mapper/           # MyBatis XML mapping files
│   └── test/                     # Test code
├── docs/
│   ├── sql/                      # Database scripts
│   └── api/                      # API documentation
├── pom.xml                       # Maven configuration
└── README.md                     # Project description
```

## Core Function Modules

### 1. Device Management
- Add, delete, modify and query device information
- Device status monitoring and management
- Device grouping and tag management
- Device connection testing

### 2. Algorithm Management
- Algorithm information management
- Algorithm training task management
- Algorithm model management
- Algorithm annotation data management

### 3. Intelligent Analysis
- Analysis request management
- Analysis result query
- Real-time analysis monitoring

### 4. Monitoring and Alerting
- Device alert management
- Alert rule configuration
- Alert processing flow

## Quick Start

### Environment Requirements

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### Installation Steps

1. **Create Database**
```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Execute SQL Script**
```bash
mysql -u root -p vlstream < docs/sql/vlstream_schema.sql
```

3. **Modify Configuration**
Edit `src/main/resources/application.yml` to modify database and Redis connection information

4. **Start Application**
```bash
mvn spring-boot:run
```

5. **Access API Documentation**
After successful startup, visit: http://localhost:8080/api/doc.html

## API Interfaces

### Device Management API

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/device/page | Pagination query device information |
| GET | /api/device/{id} | Query device by ID |
| POST | /api/device | Add new device |
| PUT | /api/device | Update device |
| DELETE | /api/device/{id} | Delete device |
| GET | /api/device/statistics | Get device statistics |

### Response Format

All API responses follow a unified format:

```json
{
  "code": 200,
  "message": "Operation successful",
  "data": {},
  "timestamp": 1704038400000
}
```

## Contact Information

- **Project Homepage**: https://vls.oortcloudsmart.com
- **Technical Support**: zhangxuelian@oortcloudsmart.com