# APaaS工作流表单系统

## 项目概述

这是一个基于若依框架的APaaS（Application Platform as a Service）工作流表单系统，提供了完整的用户管理、角色管理、权限控制和工作流功能。

## 技术栈

- **后端框架**: Spring Boot + MyBatis Plus
- **数据库**: MySQL
- **缓存**: Redis
- **工作流引擎**: Flowable
- **权限认证**: Sa-Token
- **构建工具**: Maven
- **容器化**: Docker

## 项目结构

```
apaas-workflowforms/
├── ruoyi-admin/          # 管理后台模块
├── ruoyi-common/         # 公共模块
├── ruoyi-demo/           # 示例模块
├── ruoyi-extend/         # 扩展模块
│   ├── ruoyi-monitor-admin/    # 监控管理
│   └── ruoyi-xxl-job-admin/     # 任务调度管理
├── ruoyi-flowable/       # 工作流模块
├── ruoyi-framework/      # 框架核心
├── ruoyi-generator/      # 代码生成器
├── ruoyi-job/            # 定时任务
├── ruoyi-oss/            # 对象存储
├── ruoyi-sms/            # 短信服务
├── ruoyi-system/         # 系统管理
├── deploy/               # 部署配置
├── script/               # 脚本文件
└── logs/                 # 日志文件
```

## 核心功能

### 1. 用户管理
- 用户信息的增删改查
- 用户状态管理
- 用户密码重置

### 2. 角色管理
- 角色的增删改查
- 角色权限分配
- 角色状态管理

### 3. 权限控制
- 基于Sa-Token的权限认证
- 细粒度的权限控制
- 数据权限管理

### 4. 工作流管理
- 基于Flowable的工作流引擎
- 流程定义和部署
- 任务管理和审批

## 角色服务功能说明

### SysRoleService 角色服务

#### 主要方法

1. **selectRoleById(String roleId)**
   - **功能**: 通过角色ID查询角色信息
   - **参数**: roleId - 角色ID
   - **返回值**: SysUserRoleView - 角色对象信息
   - **使用场景**: 根据角色ID获取角色的详细信息

2. **selectRoleByCondition(String userId, String roleId)** ⭐ 新增功能
   - **功能**: 通过用户ID和角色ID查询角色信息（支持单独或组合条件）
   - **参数**: 
     - userId - 用户ID（可为null）
     - roleId - 角色ID（可为null）
   - **返回值**: SysUserRoleView - 角色对象信息
   - **使用场景**: 
     - 根据用户ID查询角色：selectRoleByCondition("1001", null)
     - 根据角色ID查询角色：selectRoleByCondition(null, "1")
     - 根据用户ID和角色ID组合查询：selectRoleByCondition("1001", "1")

3. **selectRolesByUserId(String userId)**
   - **功能**: 根据用户ID查询角色列表
   - **参数**: userId - 用户ID
   - **返回值**: List<SysRole> - 角色列表
   - **使用场景**: 获取用户的所有角色信息

4. **selectRolePermissionByUserId(String userId)**
   - **功能**: 根据用户ID查询角色权限
   - **参数**: userId - 用户ID
   - **返回值**: Set<String> - 权限列表
   - **使用场景**: 获取用户的所有权限

5. **selectRoleListByUserId(String userId)**
   - **功能**: 根据用户ID获取角色选择框列表
   - **参数**: userId - 用户ID
   - **返回值**: List<Long> - 选中角色ID列表
   - **使用场景**: 用户角色分配界面

#### 使用示例

```java
@Autowired
private ISysRoleService roleService;

// 通过角色ID查询角色
SysUserRoleView role = roleService.selectRoleById("1");

// 通过用户ID查询角色 ⭐ 新增功能
SysUserRoleView userRole = roleService.selectRoleByCondition("1001", null);

// 通过角色ID查询角色 ⭐ 新增功能
SysUserRoleView roleById = roleService.selectRoleByCondition(null, "1");

// 通过用户ID和角色ID组合查询 ⭐ 新增功能
SysUserRoleView userRoleById = roleService.selectRoleByCondition("1001", "1");

// 获取用户的所有角色
List<SysRole> roles = roleService.selectRolesByUserId("1001");

// 获取用户的所有权限
Set<String> permissions = roleService.selectRolePermissionByUserId("1001");
```

## 部署说明

### 1. 环境要求
- JDK 8+
- MySQL 5.7+
- Redis 3.0+
- Maven 3.6+

### 2. 数据库初始化
```bash
# 执行数据库脚本
mysql -u root -p < script/sql/ry_20210908.sql
```

### 3. 配置文件修改
修改 `ruoyi-admin/src/main/resources/application.yml` 中的数据库和Redis配置

### 4. 启动应用
```bash
# 编译项目
mvn clean package

# 启动应用
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 5. Docker部署
```bash
# 构建镜像
docker build -t apaas-workflowforms .

# 运行容器
docker run -d -p 8080:8080 apaas-workflowforms
```

## 开发指南

### 1. 代码规范
- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一异常处理
- 完善的日志记录

### 2. 数据库设计
- 使用MyBatis Plus进行数据访问
- 支持多租户架构
- 软删除机制

### 3. 权限设计
- 基于RBAC模型
- 支持数据权限控制
- 细粒度的功能权限

## 更新日志

### 2024-01-XX
- ✅ 新增通过用户ID查询角色的功能
- ✅ 完善角色服务接口和实现
- ✅ 更新项目文档

## 联系方式

如有问题或建议，请联系开发团队。

---

**注意**: 本项目基于若依框架开发，感谢若依团队提供的优秀开源框架。
