# Location Task 环境变量迁移说明

本文只覆盖从 `apaas-location-service` 迁入 Java 的事件、主动安全、自动转工单和相关配置接口。
当前 VLStream 按单租户运行，用户身份由 Java 本地登录、Token 会话和 `sys_user`/`sys_dept` 提供。

## 当前实际需要的变量

| Java 环境变量 | 默认值 | 意义 | 是否必需 |
| --- | --- | --- | --- |
| `MYSQL_HOST` | 由运行配置决定 | 主数据库地址，事件表、配置表和工单表共用此连接 | 是 |
| `MYSQL_PORT` | 由运行配置决定 | 主数据库端口 | 是 |
| `MYSQL_DB_NAME` | `oortcloud_workflowforms_vls`（dev） | 单一主数据库名 | 是 |
| `MYSQL_USERNAME` | 由运行配置决定 | 主数据库用户 | 是 |
| `MYSQL_PASSWORD` | 由运行配置决定 | 主数据库密码 | 是 |
| `REDIS_HOST` | 由运行配置决定 | 本地 Java 登录 Token 会话使用的 Redis | 是 |
| `REDIS_PORT` | 由运行配置决定 | Redis 端口 | 是 |
| `REDIS_PASSWORD` | 空或由运行配置决定 | Redis 密码 | 按部署 |
| `VLS_SINGLE_TENANT_ID` | `000000` | 当前单租户标识；本地用户和业务数据仍保留 tenant_id 字段以兼容表结构 | 是 |
| `VLS_LOCATION_TASK_WORKFLOW_APP_PACKAGE` | `com.oort-event.demo` | 自动工单详情页的默认应用包名 | 否 |
| `VLS_LOCATION_TASK_WORKFLOW_JUMP_PATH` | `/event-detail` | 自动工单详情页的默认应用内路径 | 否 |
| `VLS_LOCATION_TASK_WORKFLOW_JUMP_PARAMS` | `task={"id":"{{event_id}}"}` | 自动工单详情页参数模板，`{{event_id}}` 会替换为事件编号 | 否 |

后三项只是部署级默认值。页面保存的事件类型、区域、分组或标签配置优先于默认值。

## 必须保存在数据库的业务配置

以下字段会因事件类型、区域、分组、标签或操作用户不同而变化，不使用环境变量：

| 字段 | 意义 |
| --- | --- |
| `auto_to_work` | 是否自动转工单 |
| `process_id` | 本地 Flowable 流程定义 ID |
| `app_id` | 本地工单应用 ID |
| `tenant_id` | 当前单租户 ID，保留用于兼容既有表结构 |
| `user_id` | 本地发起工单用户 ID |
| `app_package` | 可选的节点级跳转应用覆盖值 |
| `jump_path` | 可选的节点级跳转路径覆盖值 |
| `jump_params` | 可选的节点级跳转参数覆盖值 |

配置查询顺序为：事件类型配置、设备直接关联节点配置、最近父节点配置、全局区域配置、全局分组配置。

## Go 配置迁移判定

| Go 配置或旧变量 | 判定 | Java 对应方式或原因 |
| --- | --- | --- |
| `OORT_MYSQL_*` | 替代 | 使用主项目 `MYSQL_*`，不再维护位置服务独立数据库或连接池 |
| `OORT_REDIS_*` | 替代 | 使用主项目 `REDIS_*`，不再维护位置服务独立 Redis |
| `OORT_SSO_HOST`、`sso.host` | 废弃 | 兼容接口通过本地 Token 会话解析 `sys_user`，不调用外部 SSO |
| `OORT_LOCATION_USERINFO`、`location.userinfo` | 废弃 | 用户和部门直接查询本地 `sys_user`、`sys_dept` |
| `auth.host` | 废弃 | 使用 Java 本地鉴权拦截器和本地用户会话 |
| `Workflow.Host`（通常映射 `OORT_WORKFLOW_HOST`） | 废弃 | 自动转工单直接调用 `IWfProcessService`，不再发 HTTP 请求 |
| `Workflow.Token`（通常映射 `OORT_WORKFLOW_TOKEN`） | 废弃 | 内部 Java 方法调用不需要服务间 Token |
| `Workflow.AppId` | 改为数据库配置 | 应用由页面选择并按配置保存；不能作为所有事件共用的部署变量 |
| `Workflow.AppPackage` | 保留为可选默认值 | 使用 `VLS_LOCATION_TASK_WORKFLOW_APP_PACKAGE` |
| `Workflow.JumpPath` | 保留为可选默认值 | 使用 `VLS_LOCATION_TASK_WORKFLOW_JUMP_PATH` |
| `Workflow.JumpParams` | 保留为可选默认值 | 使用 `VLS_LOCATION_TASK_WORKFLOW_JUMP_PARAMS` |
| `OORT_ADDR`、`Addr` | 替代 | 使用主项目 `SERVER_PORT` 和 Spring Boot 网络配置 |
| `OORT_LOG_LEVEL`、`Log.Level` | 替代 | 使用 Spring Boot `logging.level.*` 和 Maven profile |
| `Model.Test` | 替代 | 使用 `dev`、`local`、`prod` Spring/Maven profile |
| `Build.Version` | 替代 | 使用 Maven 项目版本和构建信息 |
| `OORT_EUREKA_*`、`Eureka.*` | 不属于本迁移 | 主项目如需注册中心，继续使用自己的 `registration.*`；事件模块不读取它 |
| `XH.*` | 不属于本迁移 | 主动安全事件和本地转工单不读取它 |
| `OORT_EZ_ADDR`、`EZ.Addr` | 不迁移 | 属于旧位置设备列表服务，不被本次 27 个兼容接口使用 |
| `OORT_ONLINETIME`、`OnlineTime` | 不迁移 | 属于设备在线状态计算，不被主动安全事件使用 |
| `OORT_COORDINATE_*`、`Coordinate.*` | 不迁移 | 硬件事件已直接提交 point/address，本流程不做坐标转换 |
| `Sync2GA.*` | 废弃 | 旧跨网坐标及配置同步功能不进入单体 Java 项目 |
| `Wxmini.*` | 不迁移 | 微信登录、公众号回调和旧图片入口不在本次接口范围 |
| `GPSCard.*` | 不迁移 | 工牌 TCP/LBS 功能不在本次接口范围 |
| `FastdfsService.Host` | 不迁移 | 摄像头事件只保存硬件提交的图片/视频 URL，不执行旧 FastDFS 上传 |
| `tianditu.key`、高德地图 Key | 不迁移 | 摄像头事件不调用旧地图逆地理或静态图接口 |
| `OORTCLOUD_FILEVIEW_PORT_*` | 不迁移 | 旧水印图片接口不在本次兼容接口范围 |
| `config.cache` | 不迁移 | Java 配置直接读主库，没有复用 Go 的 Redis 配置缓存 |

“不属于本迁移”表示该能力可能仍被主项目其他模块使用，但主动安全兼容模块不应新增或读取对应变量。
