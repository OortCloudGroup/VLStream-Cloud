# WVP 五协议前端联合接入

## 范围

VLStream 控制台在“视频汇聚 → 设备管理”下提供 ISUP、RTSP、ONVIF、国标和大华五个独立分组。页面及其接口封装来自 WVP 前端，但业务请求仍由现有 WVP 后端处理。

本次接入不迁移 WVP 协议业务代码、数据库或历史数据，也不会把 WVP 设备写入 VLStream 的设备总览、视频广场或 AI 业务。VLStream 与 WVP 是两个独立设备域，前端分别调用两个服务。WVP 后端仅增加 VLStream Token 兼容校验，不迁移五协议实现。

## 请求和认证

- VLStream 原页面继续使用 `src/utils/request.js`。
- 五协议页面使用 `src/utils/wvpRequest.js`，开发默认请求 `/wvp-api`，生产默认请求 `/bus/wvp-server`。
- WVP 请求携带 `accessToken`、`requestType: app` 和 `X-WVP-Auth-Source: vlstream`。WVP 根据认证来源调用 VLStream 用户信息接口校验 SpringBlade Token；原 WVP 页面不带该标记，继续调用外部平台 SSO 校验。
- WVP Token 读取顺序是 URL 参数 `wvpAccessToken`、sessionStorage、localStorage，最后回退到当前 VLStream Token。通常直接使用当前 VLStream Token；`wvpAccessToken` 仅用于联调时显式覆盖。
- 浏览器端不保存或发送 WVP 服务密钥。任何服务间密钥都必须保存在后端或网关配置中，不能使用 `VITE_` 环境变量。
- WVP 后端仍是权限最终判定方。未同步权限清单时前端展示操作入口，WVP 返回的 401/403 会按真实失败处理。

开发环境在 `.env.local` 中配置：

```ini
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
VITE_WVP_PROXY_TARGET=http://127.0.0.1:9080

```

`VITE_WVP_API_BASE_URL` 可覆盖默认前缀并让浏览器直连 WVP；只有 WVP 已正确配置 CORS 时才建议使用。通常保留为空并使用代理。

生产环境参考 `nginx.conf.example`，把 `/bus/wvp-server/` 的 `proxy_pass` 改成实际 WVP 地址。该代理必须透传 GET、POST、PUT、PATCH、DELETE、OPTIONS 以及 WVP 认证请求头。

WVP 后端通过环境变量配置 VLStream 校验接口：

```ini
# WVP 与 VLStream 同机部署示例
VLSTREAM_VERIFY_TOKEN=http://127.0.0.1:8080/blade-system/user/info
# 可选；留空时五协议设备不写入 WVP dept_id
# VLSTREAM_DEFAULT_DEPT_ID=100

# 经内部网关访问示例；应使用服务端可达地址，不要使用浏览器地址
# VLSTREAM_VERIFY_TOKEN=http://gateway:21410/bus/vls-server/blade-system/user/info
```

`VLSTREAM_VERIFY_TOKEN` 只能指向可信的 VLStream 服务。WVP 仅对携带 `X-WVP-Auth-Source: vlstream` 的请求使用该地址，并使用 `Authorization: Bearer <token>`、`blade-auth`、`AccessToken` 和 `accesstoken` 兼容请求头调用接口。校验失败、未配置地址或服务不可用时请求直接失败，不会降级为匿名访问；不带该标记的原 WVP 请求仍走原有平台 SSO。

VLStream 联邦用户不写入 WVP 的 `sys_user`、`sys_user_role` 或 `sys_role` 表。校验成功后，WVP 只在当前请求上下文中构造临时用户和全量协议数据范围，并允许 `isup:*`、`rtsp:*`、`onvif:*`、`dahua:*`、`wvp:*`、`gb:*` 权限；系统管理、监控、工具和部门树接口不会授予。五协议页面不再请求 WVP 部门树，也不要求填写 WVP `deptId`。

## Windows / Linux 边界

VLStream 前端本身不加载 ISUP 或大华原生 SDK；静态页面可在 Windows 和 Linux 部署，并由现代 Chromium、Edge 或 Firefox 浏览器访问。五协议是否能实际注册设备、收流或控制设备，取决于独立 WVP 服务所在主机的运行条件，不是本次前端迁移的验收条件。

当前 WVP 源码的已知后端限制如下：

- ISUP 同时包含 Windows DLL 和 Linux `.so`，但 `ruoyi-admin/src/main/resources/application.yml` 默认设置 `isup-linux64.enabled=false`，原因是部分运行时存在 OpenSSL 原生崩溃风险。Linux 启用前应由 WVP 部署方单独验证 SDK 与运行时兼容性。
- 大华模块代码包含 Linux 动态库路径逻辑，但当前仓库的 `ruoyi-dahua/libs` 只附带 `win64` DLL，没有 `linux64` `.so`。若 WVP 部署在 Linux 且需要大华原生能力，部署方需从合规渠道补齐匹配架构和版本的厂商 SDK。
- 国标 SIP、ZLMediaKit、媒体端口和公网映射均属于 WVP 服务配置；VLStream 前端只消费 WVP 返回的播放地址。

因此，五协议的后端运行完整度以 WVP 自身部署说明和验收结果为准，不要求 VLStream 后端补装这些 SDK。

## 验证清单

1. `npm run build` 成功。
2. VLStream 原有设备管理、视频广场和 AI 页面仍请求 VLStream 后端。
3. 五协议菜单的列表、增删改查、播放和协议专属操作只请求 WVP 前缀。
4. 未配置 WVP、Token 无效或 WVP 接口失败时，页面显示真实错误，不生成模拟数据或伪造成功结果。
5. 生产环境验证 Jessibuca、H265、FLV/WebRTC 等播放地址可从浏览器访问，并确认 HTTPS 页面没有混合内容问题。
6. 分别使用 WVP 外部平台账号和 VLStream 本地账号验证：前者走原 SSO 校验，后者走 `VLSTREAM_VERIFY_TOKEN`，无 Token 或伪造 Token 均返回未认证。
