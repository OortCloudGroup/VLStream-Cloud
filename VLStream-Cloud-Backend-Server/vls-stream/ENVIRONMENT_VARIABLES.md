# 环境变量配置说明文档

本文档详细说明了 `application.yml` 和 `application-dev.yml` 配置文件中使用的所有环境变量，供运维人员部署时参考。

## 一、基础应用配置

### 1.1 应用基本信息

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `ruoyi-flowable-plus.version` | 应用版本号 | 无默认值 | 是 | 字符串 | `1.0.0` |
| `SERVER_PORT` | 应用服务端口 | `8080` | 否 | 1-65535 | `8080` |

### 1.2 日志配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `@logging.level@` | 日志级别 | 由Maven Profile决定 | 否 | `trace/debug/info/warn/error` | `info` |

## 二、数据库配置

### 2.1 MySQL主库配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `MYSQL_HOST` | MySQL数据库主机地址 | `192.168.60.77` | 是 | IP地址或域名 | `192.168.60.77` |
| `MYSQL_PORT` | MySQL数据库端口 | `32443` | 是 | 1-65535 | `3306` |
| `MYSQL_DB_NAME` | 数据库名称 | `oortcloud_workflowforms` | 是 | 数据库名 | `workflow_db` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` | 是 | 用户名 | `workflow_user` |
| `MYSQL_PASSWORD` | 数据库密码 | `mysql@pass` | 是 | 密码字符串 | `secure_password123` |

### 2.2 Redis配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `REDIS_HOST` | Redis服务器地址 | `192.168.60.76` | 是 | IP地址或域名 | `192.168.1.100` |
| `REDIS_PORT` | Redis端口 | `32576` | 是 | 1-65535 | `6379` |
| `REDIS_PASSWORD` | Redis密码 | `redis@pass` | 否 | 密码字符串 | `redis_password` |

## 三、第三方服务集成

### 3.1 XXL-JOB配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `XXL_JOB_ADDRESSES` | XXL-JOB调度中心地址 | `http://127.0.0.1:9100` | 是 | HTTP/HTTPS URL | `http://job-server:9100` |

### 3.2 平台集成配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `LOGINCODEURL` | 平台登录验证码获取地址 | `http://183.62.103.20:21410/bus/apaas-sso/sso/v1/getLoginCode` | 是 | HTTP/HTTPS URL | `http://sso.example.com/getLoginCode` |
| `USERTENANTSURL` | 获取用户租户信息地址 | `http://183.62.103.20:21410/bus/apaas-sso/sso/v1/getUserTenants` | 是 | HTTP/HTTPS URL | `http://sso.example.com/getUserTenants` |
| `LOGINURL` | 平台登录地址 | `http://183.62.103.20:21410/bus/apaas-sso/sso/v1/login` | 是 | HTTP/HTTPS URL | `http://sso.example.com/login` |
| `REPORTDATASCOPEURL` | 上报数据权限地址 | `http://183.62.103.20:21410/bus/apaas-auth/auth/v1/uplist` | 是 | HTTP/HTTPS URL | `http://auth.example.com/uplist` |
| `VERIFYDATASCOPEURL` | 校验数据权限地址 | `http://183.62.103.20:21410/bus/apaas-auth/auth/v2/verifyauth` | 是 | HTTP/HTTPS URL | `http://auth.example.com/verifyauth` |
| `USERURL` | 统一用户服务地址 | `http://183.62.103.20:21410/bus/apaas-user/` | 是 | HTTP/HTTPS URL | `http://user.example.com/` |
| `AUTHURL` | 统一权限服务地址 | `http://183.62.103.20:21410/bus/apaas-auth/` | 是 | HTTP/HTTPS URL | `http://auth.example.com/` |

### 3.3 Token认证配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `SSO_TENANT_TYPE` | 租户类型(single/multi) | `multi` | 是 | `single`/`multi` | `multi` |
| `GET_MULTI_TENANT_ADMIN_USER_URL` | 获取多租户用户信息地址 | `http://183.62.103.20:21410/bus/apaas-user/user/v1/userInfo` | 是 | HTTP/HTTPS URL | `http://user.example.com/userInfo` |
| `SSO_SINGLE_TENANT_VERIFY_TOKEN` | 单租户token校验地址 | `http://oort.oortcloudsmart.com:21310/oort/oortcloud-sso/sso/v1/verifyToken` | 是 | HTTP/HTTPS URL | `http://sso.example.com/verifyToken` |
| `GET_SINGLE_TENANT_ADMIN_USER_URL` | 获取单租户用户信息地址 | `http://oort.oortcloudsmart.com:21310/oort/oortcloud-sso/sso/v1/getUserInfo` | 是 | HTTP/HTTPS URL | `http://sso.example.com/getUserInfo` |
| `SINGLE_TENANT_ID` | 单租户ID | `0e391fd7-1033-4f09-88c0-187582fee462` | 是 | UUID格式 | `123e4567-e89b-12d3-a456-426614174000` |

## 四、微服务注册发现

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `OORT_EUREKA_HOST` | Eureka注册中心地址 | `http://192.168.88.17:8761/ga/xh-registry-center` | 是 | HTTP/HTTPS URL | `http://eureka.example.com:8761/eureka` |
| `OORT_EUREKA_FLOWABLESERVER` | Flowable服务应用名 | `oort-flowable` | 是 | 应用名称 | `workflow-service` |
| `OORT_EUREKA_MYIP` | 本服务IP地址 | `127.0.0.1` | 是 | IP地址 | `192.168.1.100` |
| `OORT_EUREKA_MYPORT` | 本服务端口 | `${server.port}` | 是 | 端口号 | `8080` |
| `OORT_XH_SERVICEID` | 服务ID | `zuul` | 是 | 服务标识 | `gateway` |
| `OORT_XH_SECRETKEY` | 服务密钥 | `zuul` | 是 | 密钥字符串 | `secret_key` |
| `OORT_XH_REQUESTTYPE` | 请求类型 | `zuul` | 是 | 类型标识 | `gateway` |
| `OORT_XH_SWITCH` | SparkBus开关 | `0` | 是 | `0`(关闭)/`1`(开启) | `1` |

## 五、数据同步配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `SYNCHRONISE_DEPT_SYNURL` | 部门同步地址 | `http://192.168.60.76:32610/oort/oortcloud-cloud-classroom/sync/v1/dept` | 是 | HTTP/HTTPS URL | `http://sync.example.com/dept` |
| `SYNCHRONISE_USER_SYNURL` | 用户同步地址 | `http://192.168.60.76:32610/oort/oortcloud-cloud-classroom/sync/v1/user` | 是 | HTTP/HTTPS URL | `http://sync.example.com/user` |
| `SYNCHRONISE_JOB_SYNURL` | 岗位同步地址 | `http://192.168.60.76:32610/oort/oortcloud-cloud-classroom/sync/v1/job` | 是 | HTTP/HTTPS URL | `http://sync.example.com/job` |

## 六、HTTP服务配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `APAAS_SSO` | SSO服务地址 | `http://127.0.0.1:32620/bus/apaas-sso` | 是 | HTTP/HTTPS URL | `http://sso.example.com` |
| `APAAS_WORKFLOWFORMS` | 工作流表单服务地址 | `http://183.62.103.20:21410/bus/apaas-workflowforms` | 是 | HTTP/HTTPS URL | `http://workflow.example.com` |
| `APAAS_GETTENANTADMIN` | 获取租户管理员地址 | `http://127.0.0.1:32620/bus/apaas-user/tenant/v1/getTenantAdmin` | 是 | HTTP/HTTPS URL | `http://user.example.com/getTenantAdmin` |

## 七、业务过滤配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `EXCLUDEDJOBNAME` | 排除的岗位名称 | `科员,警员` | 否 | 岗位名称列表(逗号分隔) | `实习生,临时工` |
| `EXCLUDEDUDID` | 排除的部门UDID | `273ed9a4-0e77-4169-af46-dd2da2aa1fcc` | 否 | 部门UUID | `123e4567-e89b-12d3-a456-426614174000` |

## 八、外部服务配置

### 8.1 密信推送服务

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `SL_URL2` | 密信推送服务地址 | `http://20.72.1.127/ga/akeyme-push-service` | 是 | HTTP/HTTPS URL | `http://push.example.com/service` |
| `SERVICE_ID` | 服务ID | `8c4ae40cf21142449a0f28deeb3679a8` | 是 | 服务标识符 | `service_001` |
| `SECRET_KEY` | 服务密钥 | `5ee850dc5e091fdc422ef017ef527270` | 是 | 密钥字符串 | `secret_key_123` |
| `REQUEST_TYPE` | 请求类型 | `service` | 是 | 类型标识 | `push_service` |

### 8.2 统一消息发送服务

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `UNIFIEDMESSAGINGSEND_URL` | 统一消息发送服务地址 | `http://183.62.103.20:21410/bus/apaas-unified-msg/` | 是 | HTTP/HTTPS URL | `http://msg.example.com/` |

### 8.3 TX回调配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `TX_CALLBACK_ADDRESS` | TX回调地址 | `http://20.72.1.41:8080/jk/RegionPort/areaNode` | 是 | HTTP/HTTPS URL | `http://callback.example.com/api/tx` |

### 8.4 Hi3519DV500 模型下发

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `VLSTREAM_HARDWARE_DISPATCH_URL` | 通知硬件下载并切换模型的接口地址 | `http://192.168.88.98:8888/vlsDeviceInfo/latest-training-model` | 否 | HTTP/HTTPS URL | `http://hardware-service:8888/vlsDeviceInfo/latest-training-model` |
| `VLSTREAM_MODEL_DOWNLOAD_PUBLIC_BASE_URL` | 硬件设备可访问的后端模型下载根地址 | `http://192.168.88.31:8080` | 否 | HTTP/HTTPS URL，不含末尾业务路径 | `http://vlstream-backend:8080` |
| `VLSTREAM_HARDWARE_DISPATCH_TIMEOUT_MILLIS` | 单台设备硬件接口请求超时时间 | `10000` | 否 | 大于等于 1000 的毫秒数 | `15000` |

> 模型下载根地址必须能从设备网络访问。OM 下载接口无需平台登录令牌，请仅在受信任网络中开放，或通过反向代理增加来源限制。

## 九、Swagger文档配置

| 变量名 | 含义 | 默认值 | 是否必填 | 取值范围 | 配置示例 |
|--------|------|--------|----------|----------|----------|
| `SWAGGER_PRE` | Swagger文档前缀 | 无默认值 | 否 | URL路径前缀 | `/api` |

---
*文档版本：1.0*
*最后更新：2026年*
