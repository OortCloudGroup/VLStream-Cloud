# 应用包名字段与消息推送 app_package 设计

## 背景

`workorder_app` 和 `wf_app` 需要维护应用包名，用于调用统一消息接口 `msg/v1/send/notice` 时传递 `ex_data.app_package`。当前通知监听器已经支持从流程变量 `app_package` 读取并写入 `ex_data`，但缺少按请求头应用 ID 从应用配置表解析应用包名的能力。

## 已确认需求

1. `workorder_app`、`wf_app` 两张表新增数据库列 `app_package`。
2. Java 实体、接口请求体、接口响应体字段使用 `appPackage`。
3. 新增、修改、详情、列表返回、导出接口需要包含 `appPackage`。
4. 列表接口不支持按 `appPackage` 查询过滤。
5. 已有环境需要增量 SQL，后续实现时放到项目根目录 `codex/`；初始化 SQL 也同步更新。
6. 调用 `msg/v1/send/notice` 时，从当前请求头获取 `appid` 或 `appID`。
7. 使用请求头应用 ID 按 `application_id` 先查 `workorder_app`，查不到再查 `wf_app`。
8. 查到的 `appPackage` 优先写入 `ex_data.app_package`；查不到时沿用原流程变量 `app_package`。

## 数据设计

两张表新增相同字段：

```sql
`app_package` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用包名'
```

初始化脚本 `ruoyi-admin/src/main/resources/oortcloud_workflowforms_initialize.sql` 同步增加该列。已有环境使用根目录 `codex/` 下的增量 SQL 执行：

```sql
ALTER TABLE `workorder_app` ADD COLUMN `app_package` varchar(255) NULL DEFAULT NULL COMMENT '应用包名' AFTER `images`;
ALTER TABLE `wf_app` ADD COLUMN `app_package` varchar(255) NULL DEFAULT NULL COMMENT '应用包名' AFTER `images`;
```

## 接口设计

`/WorkOrder/app` 和 `/wf/app` 的新增、修改请求体支持：

```json
{
  "appPackage": "com.example.app"
}
```

详情、列表和导出返回 `appPackage`。列表查询条件保持现状，不新增 `appPackage` 条件，避免引入未确认的过滤语义。

## 代码设计

实体层：

- `WorkOrderApp` 新增 `appPackage`，使用 `@TableField("app_package")` 明确列映射。
- `WfApp` 新增 `appPackage`，使用 `@TableField("app_package")` 明确列映射。

接口对象：

- `WorkOrderAppBo`、`WfAppBo` 新增 `appPackage`，用于新增和修改。
- `WorkOrderAppVo`、`WfAppVo` 新增 `appPackage`，并添加 `@ExcelProperty("应用包名")`。

Mapper XML：

- `WorkOrderAppMapper.xml` 补充 `appPackage` 到 `app_package` 的 result 映射。
- `WfAppMapper.xml` 补充 `appPackage` 到 `app_package` 的 result 映射。

服务层：

- 在应用服务层增加统一解析方法，输入请求头中的应用 ID，输出应用包名。
- 查询顺序固定为 `workorder_app.application_id` 优先，其次 `wf_app.application_id`。
- 空请求头、查不到记录、记录存在但 `appPackage` 为空时返回 `null`。

## 消息推送设计

涉及调用 `msg/v1/send/notice` 的监听器：

- `ApprovalNotificationListener`
- `MessageNotificationListener`
- `TimeoutNotificationListener`

三处统一执行以下逻辑：

1. 从当前请求读取 `appid`；为空时读取 `appID`。
2. 调用统一解析方法获取表字段 `appPackage`。
3. 构造 `ex_data` 时，优先使用解析结果写入 `app_package`。
4. 如果解析结果为空，再使用流程变量 `app_package`。
5. `jump_path`、`jump_params`、`applabel`、`msg_source` 保持现有行为。

## 错误处理

- 应用包名解析失败不阻断流程流转和消息推送。
- 查询异常记录 warn 日志并返回 `null`，由流程变量兜底。
- 请求头不存在时不报错，保持旧逻辑。

## 测试设计

优先补充 focused JUnit 5 测试：

1. 请求头应用 ID 命中 `workorder_app` 时，返回 `workorder_app.appPackage`。
2. `workorder_app` 查不到、`wf_app` 命中时，返回 `wf_app.appPackage`。
3. 两张表都查不到时，返回 `null`，通知逻辑继续使用流程变量 `app_package`。
4. 请求头同时有不同大小写时，能读取 `appid` 或 `appID`。

编译测试前按根目录 `pom.xml` 的 `<java.version>1.8</java.version>` 使用 JDK 8：`C:\Users\oort\.jdks\corretto-1.8.0_442`。

## 非目标

- 不改动前端页面。
- 不新增 `appPackage` 列表过滤条件。
- 不改变统一消息接口的请求头转发范围。
- 不改变现有流程变量 `jump_path`、`jump_params`、`msg_source` 语义。
