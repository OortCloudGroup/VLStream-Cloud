# ZLM 浏览器播放与正式前端构建配置

RTSP、ONVIF、自定义协议仍由 WVP 授权并创建预览租约，ZLM 接收保存的视频源；关闭由 WVP 回收。
前端的 `src/utils/zlmPlaybackUrl.js` 已实现可选 SDP 代理和 `cand_tcp` / `cand_udp` 参数。
这是真实打包源码，RTSP 与视频广场共享播放器都会调用；不是依赖浏览器临时拦截才能工作的补丁。

## 配置项与空值默认行为

| 构建环境变量 | 用途 | 为空时 |
| --- | --- | --- |
| `VITE_ZLM_API_BASE_URL` | 浏览器已有的 HTTP(S) SDP 代理基路径，例如 `/bus/apaas-zlmediakit` | 保留 WVP 返回的信令 URL |
| `VITE_ZLM_RTC_CAND_TCP` | TCP 媒体候选，格式为已验证可达的 `IPv4:外部端口`，转为 `cand_tcp` 查询参数 | 不覆盖 ZLM 的 TCP 候选 |
| `VITE_ZLM_RTC_CAND_UDP` | UDP 媒体候选，同格式，转为 `cand_udp` 查询参数 | 不覆盖 ZLM 的 UDP 候选 |
| `VITE_ZLM_RTC_LAN_HOST` | 沿用原 UDP/TCP 端口，额外添加一个 LAN IP | 不额外添加 LAN 候选；保留已有本机开发兼容逻辑 |

候选覆盖保留请求原 `app`、`stream`、`type`、鉴权参数与 SDP 请求体；仅处理 `/index/api/webrtc`。
TCP/UDP 必须分别匹配真实映射，不能把 Pod 8000 的地址换为宿主后仍沿用8000，也不能把同一NodePort
同时当成TCP和UDP端口。只验证TCP时留空UDP；当前ZLM版本接受显式候选时会替换默认候选集合。
候选支持字面IPv4与1–65535端口，非法值明确拒绝；不要把URL、域名、Token或其他查询参数填入候选项。
代理项只填基路径，不附 `?cand_tcp=...` 或重复 `/index/api/webrtc`。

所有这些 `VITE_` 变量在构建时进入静态JS，修改运行容器环境不会自动改变已经构建的前端。
空值默认继续使用原WVP/ZLM链路；仓库不设置任何通用公司内网IP，不要求VM、本地或开源部署覆盖候选。
`VITE_ZLM_RTC_LAN_HOST` 只添加地址而保留原端口，不能替代本次8000→32742这类外部端口映射配置。

## 普通正式打包：命令不变

在**目标部署自己的构建作业或独立checkout**设置这三个环境变量，再执行原命令：

```powershell
cd VLStream-Web/VLStream-ui
npm ci
npm run build
```

`npm run build` 和 `npm run build:prod` 都使用 Vite production 模式并写入 `dist/`。
`npm run build:test` 使用test模式，不能因名称类似就将其工件直接用于正式部署。

也可仅在目标部署的独立checkout创建不提交的 `.env.production.local`，填写该部署自己的值，
然后运行同一个 `npm run build`。Vite优先级为：调用进程环境 > `.env.production.local` >
`.env.production` > `.env.local` / `.env`；test/dev不读取 `.env.production.local`。
不要把目标专用配置写入共用 `.env.local`，不要把公司配置提交到公共模板，也不要复用同一候选工件部署不同环境。
若构建VM或开源默认包，使用其自己的作业/checkout，候选项保持空值；不会自动采用其他部署的媒体地址。
只有这三个变量即可配置本链路，不要用整份 `.env.example` 覆盖既有API、SSO等环境参数。

本次公司内网正式目标已验证的三项值如下，仅用于能访问该现有NodePort的客户端：

```dotenv
VITE_ZLM_API_BASE_URL=/bus/apaas-zlmediakit
VITE_ZLM_RTC_CAND_TCP=192.168.50.14:32742
VITE_ZLM_RTC_CAND_UDP=
```

这是部署输入示例，未写入共享环境文件或源码默认值。2026-10-21当前用户电脑已通过此TCP路径实际播放
RTSP1920×1080和ONVIF704×480，并确认ICE connected、真实解码帧和关闭释放200。
公网客户端不能据此假定能访问192.168.*。若需公网播放，应由部署方先确认已存在且批准的媒体公网映射，
填写其实际可达IP/端口并实播验收；HTTP网关域名可访问不代表其承载了RTC媒体端口。

## 为什么WVP返回可用URL，ZLM仍返回Pod候选

WVP的 `MediaServerServiceImpl.getStreamInfoByAppAndStream` 已使用所选媒体节点的 `streamIp`、
HTTP/HTTPS端口生成 `rtc` / `rtcs`；`StreamInfo.setRtc` 生成的是 `index/api/webrtc` 信令URL。
浏览器随后直接将offer发给ZLM，answer中的ICE候选由ZLM的 `rtc.externIP`、监听端口或显式
`cand_tcp` / `cand_udp` 决定。改WVP `streamIp` 只改信令/媒体URL主机，并不会自动修正answer中的外部RTC端口。

本次正式节点polaris的配置文件 `rtc.externIP` 为空、UDP/TCP监听8000，ZLM默认选择本地Pod地址
172.20.37.222:8000；既有Kubernetes Service的TCP NodePort为32742、UDP NodePort为30017，targetPort仍8000。
这些是两个网络层次，不应把HTTP代理端口、RTP收流端口与WebRTC浏览器候选端口混用。

后端**可以**按可信配置的媒体节点生成带候选参数的URL，但现有节点模型没有分别表示外部RTC TCP/UDP
端口的配置项；不能仅凭streamIp/sdpIp推断NodePort或公网映射。本次没有修改WVP媒体节点模型、数据库
或自动配置ZLM的逻辑。当前单目标最小修复由上述前端部署配置完成；若未来一套前端接多个不同NAT媒体节点，
应单独实现每节点的显式RTC外部地址/端口契约，并保留验证与隔离，不能将一个全局内网候选应用于所有节点。

## 验证与本次待更新服务

仓库根目录运行：

```powershell
node tools/test-zlm-candidates.mjs
node tools/test-wvp-media-status.mjs
node tools/test-request-identity.mjs
```

候选回归覆盖URL参数/默认兼容/非法配置，以及真实Vite production构建读取production.local配置与test模式隔离；
源码不需要手工修改公司IP。完整前端构建也已按上述进程环境设置验证过编译后的resolver。
发布后仍需同一浏览器观察选中的候选对、解码帧/播放进度与关闭释放；接口200不代替出画验收。

- VLS前端：含状态/响应补丁，并按目标部署配置构建。
- apaas-wvp-server后端：含目录/租约/默认租户补丁，并显式设置业务模式multi；单换JAR不足。
- 当前公司内网实测无需改动/重启ZLM、网关或防火墙；未执行任何线上发布。

实时证据与未发布候选工件见本地 `codex/vls-media-regression-20261021/MEDIA_CANDIDATE_VERIFIED.md`。
