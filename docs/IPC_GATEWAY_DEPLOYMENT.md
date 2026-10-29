# IPC管理网关：局域网验收与线上部署

本指南保留现有设备/租户/权限、一次性启动令牌、host-only浏览器Cookie及回环映射隔离。部署工具只生成公开配置，不读取/生成私钥或服务凭据，不修改DNS/信任/防火墙，不重启或部署。部署模板通过不等于真实跨机器验收。

## 先决定名称及TLS

优先使用已有授权组织域名和覆盖**会话所在层级**的受信证书，例如`*.ipc.<组织域名>`；`*.组织域名`不覆盖`session.ipc.<组织域名>`。只有公开证书能读取不代表可以复制其私钥。DNS、证书引用/签发、CA信任和配置加载必须按目标环境授权。

局域网后备名称为`*.ipc.vls.test`，仅在获准的内网DNS发布，不公布私网地址到外部DNS。现有`.ipc.localhost`仅保留原开发机用途；不能将其suffix替换成IP，hosts不支持任意会话通配项。
目标终端必须能经获准的LAN/VPN到达配置的监听地址；不能用开发机本地定向IP请求代替。新叶证书须匹配新名称，私有CA须经目标机器管理员明确批准信任；禁止证书警告继续、关闭TLS校验或分发私钥。

## 生成公开配置（不应用）

从仓库根执行；输出目录必须不存在，工具拒绝覆盖现有配置。LAN示例对应待审批方案，不会产生DNS/证书：

```powershell
python tools/ipc/prepare_gateway_deployment.py --mode lan --suffix ipc.vls.test --listen-address 10.0.0.10 --https-port 8444 --certificate private/lan-session-cert.pem --private-key-reference private/lan-session-key.pem --out output/ipc-lan-plan
```

输出：

- `nginx-ipc-map.conf`：只在nginx `http{}`内include一次的WebSocket映射。
- `nginx-ipc-session.conf`：新会话域名server块，保留Host、Upgrade、HTTPS及回环上游。应与原有default/Agent入口并存，**不替换现有默认server**；原始IP和未知Host仍由原default拒绝。
- `gateway-public.env`：应用公开base与网关suffix，二者必须一致。仅合并这些公开键，不替换完整受保护env或身份材料。发布Compose用`VLSTREAM_TUNNEL_GATEWAY_HOST_SUFFIX`映射到运行时`TUNNEL_GATEWAY_HOST_SUFFIX`；原生网关使用后者。
- `backend-public.yml`：仅公开基址overlay，不切库、不启用其他功能。
- `deployment-plan.json`：精确名称/监听/证书引用及`changesApplied=false`。

运行宿主机nginx时，上游127.0.0.1:8088指宿主机回环；如果nginx在容器中，必须使用原已有经授权的同网络路径，不能误把nginx容器自己的127.0.0.1当作宿主机。发布Compose将网关仅映射宿主机127.0.0.1:8088；rathole映射端口61000–61999留在tunnel-runtime容器内，网关与rathole同容器共享回环。
LAN地址示例`10.0.0.10`仅代表私有地址格式；实际地址必须由目标环境管理员确认。模板生成不会扩大监听。根CA/Noise/Agent身份不随名称切换轮换。

## 配置加载及回退

加载前备份原nginx/base/suffix/证书引用并确认实际8080进程、并行任务窗口；运行`nginx -t`后才允许获准reload。VLS公开base和网关suffix必须协调加载，不能只改URL返回值。旧浏览器Cookie不会迁移到新Host，应各自从平台重开；不导出旧Cookie。
只在批准窗口重载nginx及重启确需加载配置的8080/gateway，不动前端、8094、9080、库、设备归属或生产。保留旧default/Agent入口及证书，避免板端HTTPS地址失效。
回退按原base/suffix/nginx引用成组恢复，严格TLS核验原入口；不抹除设备/会话数据，不覆盖旧私钥，不通过清空Redis或重新注册解决。

## 真正的局域网跨机器验收

获准DNS/TLS配置完成后，在**指定同事自己的电脑**运行以下公开探针，或由父会话通过该电脑受支持浏览器完成同等检查：

```powershell
python tools/ipc/verify_gateway_public.py --url https://readiness-check.ipc.vls.test:8444 --ca-file ca.pem --vantage colleague-lan-pc --expected-address 10.0.0.10 --out ipc-lan-client-result.json
```

`ca.pem`仅为已批准的公开CA文件，工具只供当前进程验证，不安装信任。若使用已受信组织证书可省略参数。探针以正常DNS路径连接、保留SNI/Host、严格TLS，不接受Token/启动路径或Cookie、不跟随跳转；正确匿名结果是网关401而不是摄像头页面。报告客户端计算机/本地socket地址、DNS、TLS、HTTP及UTC，但这仍只是网络/门禁验收。

再由该同事：

1. 用自己的已有合法平台账号进入**实际绑定设备**详情（本轮新Agent实际为07，不能冒称06），核对同一endpoint、新鲜Agent心跳、ONLINE隧道、AVAILABLE Web、APPLIED且配置版本一致。
2. 在原有`vls:tunnel:access`及租户范围内正常点击“打开管理后台”，由平台创建该用户自己的短期会话。不共享他人Cookie、私钥、启动令牌或板端密码；缺权限另按最小范围申请，不在部署工具里授予。
3. 确认无TLS警告，实际摄像头登录页、JS/CSS/绝对路径/重定向、只读API与必要WebSocket正常；板端登录使用其既有合法身份，不向日志记录密码。
4. 另一无Cookie浏览器打开最终`/#/login`地址应401；A的Cookie不能进入B的session Host；不复制真实Cookie进行人为测试。自动化隔离测试使用模拟身份，真实验收按各自独立会话观察。
5. 核对过期、吊销、设备离线、路由版本变化时失败关闭；生产数据上的吊销/断网测试有副作用，先确认目标与维护窗口，不为了测试误断真实用户。WebSocket在Upgrade时续期，帧流量不续期；下节只读重验已在本地实现和回归，必须配套部署后才具备已升级连接的有界关闭能力。

记录实际客户端及UTC、DNS→TCP→TLS→HTTP错误层、目标设备/endpoint、页面结果，敏感字段不入报告。本机健康200、模拟测试、定向Host连接、用户本人已打开，均不能替代同事跨机器通过。

## 线上上线前必须具备

| 范围 | 部署条件 |
|---|---|
| 名称/入口 | 经授权公网或VPN可解析的专用会话通配域名，指向实际公网反代/LB；私网地址不自动发布公网。另配置板端可达的Agent HTTPS入口 |
| TLS | 证书SAN覆盖实际会话层通配域名及Agent入口所需名称，完整链/续期可用，浏览器与板端分别信任；私钥仅受保护挂载，不复制测试CA私钥到线上 |
| 网络 | 浏览器HTTPS443或批准端口；板端Agent HTTPS及Noise TCP2333可达。仅批准来源/目标，8088宿主机仅回环，业务映射池不发布，internal接口不公开 |
| 反代 | 保留Host/SNI、HTTPS及WebSocket Upgrade，匹配base/suffix、合理超时；现有Agent/default拒绝站点单独保留。LB/WAF不要记录`/s/`路径中的启动令牌 |
| 配置 | public base、gateway suffix、nginx域名一致；板端remoteAddress/Noise服务端公钥对应实际上线服务；不是把开发机IP复制过去 |
| 身份 | 三类独立高熵服务身份与Noise密钥经授权安全持久化；只复制获准部署身份，不复用测试Agent令牌、激活码或旧endpoint行 |
| 数据/设备 | 正式Flyway状态正确，WVP权威deviceId/rowID/tenant与VLS端点一致；模式和默认租户正确，原绑定不靠批量改tenant修补 |
| 会话/权限 | 每用户独立合法平台会话、显式访问权限、一次性启动、host-only Secure/HttpOnly Cookie；过期/撤销/跨Host/越租户拒绝。复制最终URL不得成为共享登录 |
| 就绪 | 真实板端注册/新鲜心跳、隧道ONLINE、Web AVAILABLE、路由APPLIED且版本一致；PID、进程健康与APPLIED不能代替端到端页面 |
| 可用性 | 网关内存Cookie映射：多实例须正确会话粘滞或设计共享安全存储；直接横向扩容会使Cookie在另一实例401。重启使现有Cookie失效，应重新从平台打开，不复制内存会话 |
| 故障/验证 | 控制面失联两分钟fail-closed、设备重连/重启、续期及空闲到期；配套部署下述只读验证API与升级连接保护。日志脱敏/轮转、证书到期、健康与路由失败监测及备份回退 |

线上实施尚未授权；这些项目明确完成并取得目标环境授权后才部署。工具不会填入未知真实域名、签发证书或假装线上验收完成。

## 已升级WebSocket的会话重验

VLS新增受**现有网关内部凭据**保护的`POST /vlsTunnel/internal/access-sessions/validate`，使用现有Session-Id请求头。只读复用已有opened/expiry/revoked、租户/设备绑定及feature/desiredState/Agent新鲜心跳/tunnel/Web/路由版本检查，不更新expiresAt、updateTime，不消费启动令牌，不增加用户权限或对公网开放内部接口。

新网关在上游返回101、向浏览器发出升级响应前额外验证一次；对所有HTTP101协议（含WebSocket）应用同一保护，不能改Upgrade名称绕开。
已升级连接按默认5秒**起始到起始**间隔只读重验，每次最多3秒总预算；撤销、后端判定设备不可用、归属/上游改变、认证失败、404旧接口、网络超时等均关闭该会话升级连接。无并发HTTP续期时按当前到期时间定时关闭，不等额外检查间隔；帧和周期检查都不续期。
已有合法HTTP活动可照常延长同一会话；旧检查响应不能覆盖并发续期的新到期时间。只有观察到该续期且旧核验因旧到期时间超时/过期时，才在**同一3秒预算**内重读一次；认证/撤销/设备不可用失败不重试放行。

撤销/不可用状态已被后端确立后，正常调度下默认约8秒内关闭；物理设备掉线此前仍按原Agent心跳超时判定，不把“断电”承诺为8秒内发现。相同会话多个socket一并失效，正常关闭其中一个不撤销其他socket；不同用户会话互不影响。关闭通过取消标准ReverseProxy升级请求、关闭上游与浏览器TCP实现，不伪造成功的WebSocket关闭帧。正常网关停机也主动关闭已升级连接。

**部署顺序：先部署含validate API的VLS后端，再发布新tunnel-runtime网关。** 旧网关不会调用新接口，后端可先兼容上线；新网关对旧后端404会失败关闭，不能先切新网关再等待后端。无需数据库迁移、角色授予、证书变更或新依赖；已有内部Token继续使用，不在该变更中轮换。
网关重启会使原内存Cookie会话失效，用户需从各自合法平台入口重新打开。若回退，优先回退网关、确认无新网关调用后再回退后端；不得通过关闭核验、扩大超时或忽略TLS错误维持旧连接。

## 离线测试

```powershell
python -m unittest discover -s tools/ipc -p "test_*.py" -v
```

在`deploy/release/tunnel-runtime`运行既有Go工具的`go test ./...`。覆盖双用户同设备独立Cookie、跨Host拒绝、启动链接不可复用、A撤销不影响B、B过期失败关闭，以及真实101升级/掩码文本帧回显、升级前后拒绝、到期/离线/路由变化/超时关闭、正常socket关闭与并发HTTP续期竞态、停机清理。Java `TunnelAccessServiceTest`覆盖只读验证无更新、失效状态、归属/就绪拒绝及真实网关凭据边界。全部模拟/本地回环，不使用真实设备凭据、注册或数据库。
