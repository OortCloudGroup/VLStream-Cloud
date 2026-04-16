# VLStream Cloud 后端服务

## 项目简介

VLStream Cloud 是一个智能视频流管理系统，基于SpringBoot + MyBatis Plus + MySQL技术栈开发，提供设备管理、算法管理、智能分析、监控告警等功能的后端API服务。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **ORM**: MyBatis Plus 3.5.3.1
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **API文档**: Knife4j 3.0.3
- **工具库**: Hutool、Apache Commons Lang3
- **JSON**: FastJSON 2.0.25

## 项目结构

```
VLStream-server/
├── src/
│   ├── main/
│   │   ├── java/com/vlstream/server/
│   │   │   ├── entity/           # 实体类
│   │   │   ├── mapper/           # 数据访问层
│   │   │   ├── service/          # 业务逻辑层
│   │   │   ├── controller/       # 控制器层
│   │   │   ├── config/           # 配置类
│   │   │   ├── common/           # 通用类
│   │   │   └── utils/            # 工具类
│   │   └── resources/
│   │       ├── application.yml   # 应用配置
│   │       └── mapper/           # MyBatis XML映射文件
│   └── test/                     # 测试代码
├── docs/
│   ├── sql/                      # 数据库脚本
│   └── api/                      # API文档
├── pom.xml                       # Maven配置
└── README.md                     # 项目说明
```

## 核心功能模块

### 1. 设备管理
- 设备信息的增删改查
- 设备状态监控和管理
- 设备分组和标签管理
- 设备连接测试

### 2. 算法管理
- 算法信息管理
- 算法训练任务管理
- 算法模型管理
- 算法标注数据管理

### 3. 智能分析
- 分析请求管理
- 分析结果查询
- 实时分析监控

### 4. 监控告警
- 设备告警管理
- 告警规则配置
- 告警处理流程

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装步骤

1. **创建数据库**
```sql
CREATE DATABASE vlstream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **执行SQL脚本**
```bash
mysql -u root -p vlstream < docs/sql/vlstream_schema.sql
```

3. **修改配置**
编辑 `src/main/resources/application.yml`，修改数据库和Redis连接信息

4. **启动应用**
```bash
mvn spring-boot:run
```

5. **访问API文档**
启动成功后，访问：http://localhost:8080/api/doc.html

## API接口

### 设备管理 API

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/device/page | 分页查询设备信息 |
| GET | /api/device/{id} | 根据ID查询设备 |
| POST | /api/device | 新增设备 |
| PUT | /api/device | 更新设备 |
| DELETE | /api/device/{id} | 删除设备 |
| GET | /api/device/statistics | 获取设备统计信息 |

### 响应格式

所有API响应都遵循统一格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704038400000
}
```
## 效果图
<img width="2697" height="1791" alt="image" src="https://github.com/user-attachments/assets/d9ee1369-7ae3-4b8f-8bd8-961de320f9bd" />

<img width="3807" height="1767" alt="image" src="https://github.com/user-attachments/assets/5d98c142-ea95-4b6b-be6a-82aa0a3134db" />

<img width="3797" height="1674" alt="image" src="https://github.com/user-attachments/assets/eb63f8b3-c01c-4964-9516-58b0baac3ff6" />

<img width="3804" height="1566" alt="image" src="https://github.com/user-attachments/assets/f6614a0b-9165-4702-8688-cb983ba52cf8" />




## 联系方式

- **项目主页**: https://vls.oortcloudsmart.com
- **技术支持**: zhangxuelian@oortcloudsmart.com
