# VLS 平台 IPC 远程管理服务端实施计划

## 1. 目标与安全边界

目标是在“设备管理 → VLStream 协议 → 设备详情”中安全打开该 IPC 本机原厂 Web 后台。

首期采用 IPC 本机 Agent + rathole Client。公网浏览器只能访问 HTTPS 网关；rathole 的业务映射端口只监听与网关同一网络命名空间内的 `127.0.0.1`。VLS 校验平台用户、租户、权限、设备绑定和短期会话，IPC 继续校验自身账号密码。

明确不做：

- 不把映射高位端口直接开放公网；
- 不仅凭未经校验的设备 `tenantId` 或当前操作者决定归属；
- 不在 Spring MVC 内实现任意目标的通用反向代理；
- 不保存 IPC Web 用户名和密码；
- 不把 WVP 行 ID 与设备业务 `deviceId` 混用。

## 2. 组件和数据所有权

| 组件 | 职责 |
| --- | --- |
| WVP | VLStream 设备和视频能力权威目录 |
| VLS Tunnel 控制面 | 端点绑定、激活、Agent 状态、配置版本、访问会话 |
| Rathole 控制器 | 拉取期望路由、原子生成配置、校验并热重载/重启、回报状态 |
| HTTPS 网关 | 校验短期访问令牌、动态选择回环上游、代理 HTTP/WebSocket |
| IPC Agent | 管理设备本机 rathole Client 和本地 Web 健康状态 |

## 3. 迭代计划

### 迭代一：控制面数据与凭据

状态：已完成源码，开发数据库已执行 Flyway 迁移。

- Flyway 新增端点、一次性激活和短期访问会话三张表；
- 当前租户签发激活码，设备不能自报租户；
- 激活码和 Agent Token 只存 SHA-256；
- rathole Service Token 由部署密钥、端点 ID 和配置版本 HMAC 派生；
- 新增 `vls:tunnel:manage` 与 `vls:tunnel:access` 权限；
- 默认关闭功能；多租户激活必须与 WVP 的设备归属一致。

### 迭代二：Agent 生命周期 API

状态：已完成源码、定向单元测试和模拟 Agent 运行接口验证。

- `POST /vlsTunnel/agent/register`；
- `GET /vlsTunnel/agent/config`；
- `POST /vlsTunnel/agent/heartbeat`；
- 注册重试限定同一 Agent 实例；
- Agent、隧道、本地 Web 与 WVP 设备在线状态彼此独立；
- 停用、吊销和配置版本变更。

### 迭代三：路由与访问会话契约

状态：已完成源码、定向单元测试和真实后端/sidecar 运行联调。

- 控制器读取期望路由并回报应用结果；
- 只有相同配置版本的 `APPLIED` 路由才能创建访问会话；
- 用户会话要求 Agent 心跳新鲜、隧道在线、本地 Web 可用；
- 访问会话默认空闲 10 小时（36000 秒）过期；入口只能兑换一次，每次 HTTP 请求校验后刷新后端到期时间和 Cookie；
- 网关使用独立内部 Token 解析短期访问令牌；
- 网关只获得回环地址和内部端口，不获得租户业务数据。

### 迭代四：Rathole 控制器与 HTTPS 网关

状态：已完成首版源码、容器构建、本地健康检查和 Noise 隧道代理验证，未部署公网。

- 独立最小权限 sidecar，以 UID 10001 运行；
- 固定 rathole 版本、SHA-256、Noise 私钥和配置目录；
- 原子写配置、受控重启和控制面失联两分钟失败关闭；
- wildcard 子域优先，保证 IPC 绝对路径、Cookie 和多标签会话隔离；
- 支持 HTTP、重定向、Cookie、WebSocket Upgrade 和响应头安全改写；
- 默认关闭访问日志中的会话子域/令牌；
- 健康检查、路由状态回报和短期启动令牌一次性兑换；指标和生产日志轮转待部署补充。

### 迭代五：前端与权限

状态：已完成源码、生产构建和运行中后端的浏览器入口验证。

- VLStream 设备详情展示 Agent、隧道、本地 Web 和路由状态；
- 有权限且可用时显示“打开管理后台”；
- 同步打开空白窗口后再请求会话，避免浏览器拦截异步弹窗；
- 错误区分未激活、Agent 离线、路由未生效和本地 Web 不可用；
- 管理入口支持签发激活码、停用和吊销，不在页面持久保存激活码。

### 迭代六：部署和端到端验证

状态：本地模拟链路已完成；正式域名/TLS 和真实板端待联调。

- 配置独立随机的服务签名、控制器和网关内部 Token；
- rathole 业务端口范围不得发布到宿主机公网；
- 应用 Flyway 后检查索引、租户隔离和重复激活；
- 真实 IPC 验证注册、重启、断网恢复、凭据轮换和吊销；
- 浏览器验证登录、CSS/JS、绝对路径、重定向、Cookie 和 WebSocket；
- 验证跨租户/越权接口拒绝、访问会话到期、直接端口扫描失败；
- 24 小时稳定性和资源占用测试。

## 4. 当前实现文件

- 数据库：`V1_2_0_019__ipc_remote_management_control_plane.sql`
- 配置：`VlsTunnelProperties`、`application.yml`
- 控制器：`VlsTunnelManagementController`、`VlsTunnelAgentController`、
  `VlsTunnelInternalController`
- 服务：`TunnelManagementService`、`TunnelAgentService`、
  `TunnelControlService`、`TunnelAccessService`、`TunnelTokenService`
- 测试：`TunnelTokenServiceTest`、`TunnelAgentServiceTest`、
  `TunnelAccessServiceTest`
- Sidecar：`deploy/release/tunnel-runtime`，包含 Go 控制器、HTTP/WebSocket
  网关、固定 rathole v0.5.0 下载校验和非 root 容器；
- 前端：`src/api/ipcTunnel.js` 与 `VlstreamMqttDevice.vue` 设备详情入口；
- 部署：Compose `tunnel` profile、示例环境变量和 wildcard TLS nginx 示例。

## 5. 当前验证

- `tools/check-database-migrations.ps1`：通过；
- Java 版本：根 POM `java.version=1.8`，使用 Corretto 8；
- 2026-09-22：VLS 24 项定向测试、WVP 29 项定向测试通过，覆盖访问续期、过期/吊销/跨租户拒绝、
  显式 UTC 序列化、默认租户、初始化凭据、联邦租户和 WVP 归属校验；
- Maven reactor 完整测试通过，其中 `ruoyi-vlstream` 200 项通过、4 项环境依赖测试跳过；
- Go sidecar 8 项测试、Docker 镜像构建、UID 10001、只读根文件系统、
  `cap_drop=ALL` 和容器内健康检查：通过；
- 2026-09-22：Go 测试通过，模拟时钟验证第 9 小时请求续到第 19 小时、超过原始期限仍可访问、
  连续空闲 10 小时后拒绝、续期失败不放行；两套 Compose 配置解析通过。VLS/WVP/网关尚未更新运行版本；
- 2026-09-22：MySQL 8.4 隔离实例验证 WVP `V1_2_9` 重复执行、历史空值保留、A/B 查询隔离、
  默认租户兼容及大小写精确匹配；初始化工具输出与 Java 校验器的独立测试向量一致；
- 前端生产构建：提高 Node 构建内存到 4 GiB 后通过，保留项目既有 Sass、
  eval、动态导入和大 chunk 警告；
- 两份 Compose 的 `tunnel` profile 解析：通过；网关端口只绑定宿主机回环，
  rathole 控制端口公开，内部业务端口未发布；
- 开发数据库已实际执行 `V1_2_0_019`，最新版后端和 sidecar 已启动；
- 模拟 IPC Agent 完成注册、7 位小数秒 UTC 心跳、Noise 控制通道和本地 nginx 转发；
- 浏览器设备详情显示“远程管理可用”，短期入口返回 303 和会话 Cookie，后续代理返回
  nginx 页面，一次性入口令牌复用返回 401；
- 联调后已吊销模拟端点并清理模拟容器，活动路由为 0，sidecar 保留运行；
- 登录 SM2 密文 `04` 前缀碰撞已兼容；内部会话令牌改为请求头传输，敏感接口请求参数不写通用日志；
- 尚未配置正式 wildcard DNS/TLS、持久化部署密钥、连接真实 IPC Agent，原厂页面登录、
  静态资源、WebSocket、重启/断网恢复、密钥轮换和 24 小时稳定性仍待板端与部署环境验证。

## 6. 多租户与默认租户

2026-09-22 用户确认“设备初始化时配置租户”，并保留“无租户上报归默认租户”的现有规则。
已在 WVP Lite 增加 `tenant_id`、Flyway `V1_2_9`、经认证的用户租户上下文以及设备列表/操作/分类关联校验。
未上报的新设备归默认租户，已有设备漏报不改归属；显式租户通过初始化绑定凭据校验后自动入库，
普通心跳不能转移设备。已归属默认租户的设备若需转移，仍需另行受控迁移流程。

初始化工具 `apaas-wvp-server/tools/create-device-tenant-binding.mjs` 使用平台独立密钥生成设备专属凭据，
主密钥不下发板端。协议字段见 `docs/VLS-Protocol.md` 第 2.2.1 节。VLS 读取 WVP 权威归属并核对当前用户，
不再以“多租户模式”本身为由拒绝激活。两项目默认设备租户必须一致。

升级前需应用 WVP 迁移、配置默认租户和初始化签名密钥；MQTT 每设备认证/Topic ACL、真实板端新增字段
及 A/B 两租户浏览器联调仍需部署环境验证。分类结构名称及其他协议的数据范围不在本次 VLStream 改造范围。

## 7. 激活交付和访问期限说明

当前“生成激活码”只在页面显示一次，没有 MQTT 自动下发功能。测试阶段需把该设备的激活码
通过设备端本地配置或调试入口交给 Agent；具体入口由板端实现。注册成功后删除激活码，保留 Agent Token，
普通重启无需再次激活。自动下发可复用已鉴权、按设备隔离的控制通道，但需要双方补充下发协议与回执，
目前不应将其描述为已实现。

访问会话的空闲期限默认和上限均为 10 小时，配置项为 `VLSTREAM_TUNNEL_ACCESS_SESSION_TTL_SECONDS=36000`。
例如 09:00 打开后在 18:00 刷新，会延到次日 04:00；每次 HTTP 请求经后端检查后更新数据库期限和 Cookie，
包括页面后台轮询。WebSocket 在建立时续期，后续帧不单独触发续期。过期/吊销/设备不可用时续期失败并停止放行。
该规则不延长 IPC 自身登录 Cookie、不改变激活码的默认 10 分钟有效期；网关重启后仍需从平台重新打开。

浏览器实际检查：2026-09-22 本地设备详情的“设备管理后台”显示“功能未启用”，因此前端隐藏“生成激活码”。
不是独立菜单或列表行按钮；完成运行环境的功能开关及部署凭据配置后，才显示在该详情区域。
