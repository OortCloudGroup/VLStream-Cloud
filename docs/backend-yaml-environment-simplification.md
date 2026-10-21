# VLS 后端配置精简与旧预览退役（2026-10-20）

## 范围

保留其他未提交改动；不修改 WVP、线上容器、数据库或服务，不提交推送。已阅读并更新根目录 BUSINESS_PROCESSES.md。
前一轮将 WebRTC provider 改为 `webrtc-streamer`、固件协议大小改为 `167772160` 的两个精简保留，默认值不变。

## 当前入口与旧入口

浏览器实测当前 `/bus/vls-ui/custom/device` 加载 CustomProtocolDevice.vue，GET `/wvp-api/custom/device/list`、
`/wvp-api/custom/device/media/status`、`/wvp-api/device/classification/tree` 均 200；视频广场加载
VideoSquareRefactored.vue，GET `/wvp-api/video/device/list` 为 200。未点击预览、录像或修改设备。

当前自定义播放器通过 wvpRequest 提交 WVP `/custom/device/{id}/preview?lease=true`，并向同一后端续租/释放。
Vite `/wvp-api` 直接代理本地 WVP 9080，没有经过 VLS 旧预览后端。视频广场的自定义来源复用该播放器。
旧 WVP 兼容分支仍查询 WVP 详情并直接播放 HTTP/CameraRTC，不调用 VLS ZLM。
旧 `/device-management` 路由另有设备编辑、摄像机设置和录像策略，故保留这些功能；本次移除它对旧预览 API 的调用，
RTSP/RTMP 播放引导至当前自定义页，不把旧 VLS 行 ID 传给 WVP。原审计仅凭旧方法引用判断当前主链路必用 ZLM 不成立。

## 实际清理

- 删除旧 VLS ZLM Service、Properties 及退役服务测试，删除 VlsDeviceInfoController 的旧预览接口。
- legacy VlsMqttDeviceController 仅删除创建/关闭预览、媒体状态和 PreviewRequest，保留设备查询与固件业务。
- 删除 `vlstream.zlm` 全部 8 项和 VLS release Compose 的三项专有注入；不修改 ZLM/WVP 基础设施或浏览器共享播放器。
- 删除前端 createDevicePreview、旧页面 ZLM 专有播放器状态和分支，保留当前 WVP 预览租约。
- 删除无业务消费的 MQTT topic-prefix 字段/配置；连接凭据、常驻/临时 client ID、QoS、7 个控制 Topic 全部保留。
- 删除无消费的 RTSP refresh-interval-millis YAML 及已注释调度代码；实际 record-sync-interval-millis 调度保留，默认 10000 毫秒，支持外部属性覆盖。修正部署手册原有“旧环境变量已绑定”的错误说明。

非整行注释中的大写环境占位符计数（嵌套分别计数，不含 Maven 过滤符及普通属性引用）：

| 范围 | 任务最初 | 前轮两项精简后 | 本轮清理后 |
| --- | --- | --- | --- |
| 主 application.yml 出现次数 | 147 | 145 | 135 |
| 主应用三 YAML 出现次数 | 176 | 174 | 164 |
| 主应用不同变量名 | 170 | 168 | 158 |
| 三应用全部 7 个源码 YAML 出现次数 | 181 | 179 | 169 |

dev 的 29 次/28 个变量、prod 的 0 次保持；monitor、XXL Job admin 仅盘点，不修改。

## 保留理由与边界

地址/端口、凭据/签名、数据库/Redis/存储、租户模式和默认租户、生产/测试差异、安全兼容开关、容量/计费与调度调优项均保留。
MQTT 由 VlsMqttBusService 处理 AI 事件与模型回执，由 VlsMqttPublishService 发布控制；WVP 设备中心不替代这两类业务。
native-device 默认租户仍用于 WVP 身份解析；legacy 心跳、固件和兼容业务未整体删除。
FFmpeg 录像、事件媒体、WVP 的媒体节点及 ZLM、浏览器 SDP URL/候选配置仍有效，不属于 VLS 自有 ZLM 客户端的清理范围。
既有历史数据库 zlm 字段、迁移和 SDK 协议记录保留，本次无数据库迁移。

未确认固定的 WebRTC health-path、LLM retry 与 RTSP 进程停止等待仍保留。Spring Boot 原生外部配置覆盖能力仍适用。

## 验证

- `mvn -ntp -Pdev -pl ruoyi-vlstream -am '-Dtest=RetiredDevicePreviewTest,VlsMqttProtocolTest,VlsMqttInboundDispatcherTest,DeviceModelServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`：20 项通过、0 失败/错误/跳过。新增 MockMvc 验证四个旧接口 404，保留的 legacy 流查询仍 200。
- `mvn -ntp -Pdev -DskipTests clean package`：13 模块全部成功，清理旧 target 后重新编译打包，避免删除源文件却残留旧 class。
- `npm run build`：最终修改后成功（56.09 秒）。标准 ESLint 无项目配置，不能运行；使用临时显式配置，对两份修改的 Vue/JS 通过 Vue 解析与 no-unreachable 检查，未自动修复其他文件。
- 对真实旧页 handlePlay 函数做隔离执行：RTSP、RTMP、大小写/空白三种输入均先跳转 `/custom/device`，不把旧行 ID 或凭据传入新路由、不执行媒体启动。当前 WVP 播放器的 lease、续租和释放代码未修改。
- JDK 8 / SnakeYAML 1.33：与本轮修改前完整 YAML 树比较，精确扣除 zlm 组及两个无消费键后完全相等；dev 14 文档、prod 7 文档、Compose 1 文档解析通过。Spring Boot 2.7.11 Binder 对真实 MQTT/Firmware/WebRTC Properties 的保留字段绑定通过，退役绑定类在 clean 后 classpath 中不存在。
- 修改后用户本地 Edge 回归：自定义页三项 WVP GET 和视频广场 `/wvp-api/video/device/list` 均 200；目录可见、video 元素为 0，未点击预览/录像或创建新媒体会话。浏览器最终恢复自定义设备页。
- 当前有效源码/部署文件搜索退役类、显式变量与旧前端调用没有残留；记录文档中的历史说明及数据库旧字段保留。diff 空白检查通过。

证据保存在 `%TEMP%/vls-zlm-retirement-20261020/`，不提交含原配置的快照。
旧设备管理页在修改前动态加载曾因本地 Vite 的 hls 依赖缓存 504 失败，因此其跳转验证采用真实函数隔离执行和生产构建，未宣称该旧页浏览器端完整验收。
后端新包未加载到运行 JVM；MockMvc 的 404 结果不是对当前旧进程的接口探测。未进行真实出画、租约创建/释放、MQTT 联调或生产验证；未重启、部署或改库。
