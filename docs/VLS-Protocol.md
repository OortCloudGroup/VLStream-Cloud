VLS-Protocol

修订记录

|     |     |
| --- | --- |
| 版本/作者/日期 | 修改内容 |
| 1.0/雷超群/2026-07-24 | 初版  |
| 1.1/雷超群/2026-08-04 | 添加事件上报文件上传模块，调整下发模型 |
| 1.2/雷超群/2026-08-05 | 优化数据格式 |
| 1.3/雷超群/2026-08-12 | 设备心跳增加多码流上报与平台接收回执 |
| 1.3/雷超群/2026-08-17 | 4.8 设备OTA固件升级 |
| 1.4/2026-09-07 | 补充通用设备控制规格 1：能力查询、停止、镜头与高级控制、时效和回执；设备端尚未支持，待开发联调 |

目录

[一、协议总览 2](#_Toc237638629)

[1.1 设计目标 2](#_Toc237638630)

[1.2 基础通信约束 2](#_Toc237638631)

[1.3生命周期时序图 3](#_Toc237638632)

[二、基础通用规范 4](#_Toc237638633)

[2.1 唯一总线 Topic 4](#_Toc237638634)

[2.2 全局公共消息头 4](#_Toc237638635)

[2.3 通用统一回执模板 4](#_Toc237638636)

[三、设备类 5](#_Toc237638637)

[3.1 设备全量配置 5](#_Toc237638638)

[3.2 通用设备控制 7](#_Toc237638639)

[3.3 远程抓图 22](#_Toc237638640)

[3.4 设备校时 23](#_Toc237638641)

[3.5 媒体上传交互 24](#_Toc237638642)

[3.6 设备心跳 & 硬件遥测 26](#_Toc237638643)

[3.8 二维码识别上报 30](#_Toc237638644)

[3.10 本地录像查询 31](#_Toc237638645)

[3.11 录像上传任务下发 32](#_Toc237638646)

[3.13 硬件信息查询 33](#_Toc237638647)

[3.14 设备日志分页上报 34](#_Toc237638648)

[四、AI 业务类 35](#_Toc237638649)

[4.1 AI 模型下发部署 35](#_Toc237638650)

[4.2 查询当前加载模型 37](#_Toc237638651)

[4.3 模型手动回滚 37](#_Toc237638652)

[4.4 人脸库人员管理 37](#_Toc237638653)

[4.5 人脸通行 / 陌生人抓拍上报 39](#_Toc237638654)

[4.6上报文件上传 40](#_Toc237638655)

[4.7 结构化人车 / 车牌 / 非机动车识别 43](#_Toc237638656)

[4.8 设备OTA固件升级 45](#_Toc237638657)

[五、IoT Center 类 48](#_Toc237638658)

[5.1 RSGet 48](#_Toc237638659)

[5.2 RSSave 49](#_Toc237638660)

[5.3 电梯梯控 51](#_Toc237638661)

[5.4 液晶屏广告管理 52](#_Toc237638662)

[5.5 MP3 音频播放配置 53](#_Toc237638663)

[六、全局可靠性 & 安全通用规范 54](#_Toc237638664)

# 一、协议总览

## 1.1 设计目标

- 1.  **通用性**：兼容普通 IPC、球机 PTZ、人脸识别一体机、结构化智能相机、边缘盒全品类设备，统一交互范式；
    2.  **可扩展性**：采用分层消息结构，业务载荷独立隔离，新增 AI 事件、设备配置、媒体上传无需修改外层协议头；
    3.  **便利性**：复用文档现有 HTTP 后台接口字段，设备端一套逻辑对接 HTTP/MQTT 双通道，降低改造成本；
    4.  **可靠性**：完善幂等、重试、回执、离线缓存、心跳机制，适配弱网工业场景；
    5.  **安全合规**：设备独立 ACL、TLS 加密、消息鉴权、敏感字段脱敏。

## 1.2 基础通信约束

| **项** | **规范定义** |
| --- | --- |
| MQTT 版本 | 3.1.1（兼容 5.0） |
| 生产端口 | TLS 8883；测试明文 1883 |
| ClientID | vlstream-{deviceId} 全局唯一 |
| KeepAlive | 60s |
| CleanSession | false（持久会话，离线消息不丢失） |
| QoS 统一 | 所有命令 / 事件 / 回执使用 QoS 1 |
| Retain | 业务消息 false；设备在线状态 true |
| 编码  | 全消息 UTF-8 JSON |
| 时间标准 | UTC ISO-8601 yyyy-MM-ddTHH:mm:ssZ |
| 幂等规则 | messageId UUID 全局唯一，重试不变 |
| 二进制策略 | MQTT 仅传 URL / 元数据，图片 / 模型 / 录像走 HTTP/MinIO 上传，不 Base64 大包 |

## 1.3生命周期时序图  

# 二、基础通用规范

## 2.1 唯一总线 Topic

vlstream/v2.2/dev/{deviceId}/bus

全业务复用

所有平台下发、设备上报、业务回执统一发布 / 订阅；

ACL 权限：设备仅允许读写自身deviceId 对应的总线 Topic，禁止跨设备访问。

## 2.2 全局公共消息头

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"UUID-v4 全局唯一",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:30:00Z",  
    **"msgDir"**:"platform2dev/dev2platform",  
    **"mainBizType"**:"device/aiBiz",  
    **"subBizType"**:"细分业务标识",  
    **"payload"**:{  
<br/>    },  
    **"extend"**:{  
<br/>    }  
}

### 公共头字段说明

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| protocolVersion | string | 是   | 协议版本 2.2，主版本变更代表不兼容 |
| messageId | string | 是   | UUID v4 幂等 ID，重传 / 重试必须复用同一值 |
| deviceId | string | 是   | 设备全局唯一编号，Topic / 消息体 / 平台设备库三统一 |
| sentAt | string | 是   | UTC 标准时间 yyyy-MM-ddTHH:mm:ssZ |
| msgDir | string | 是   | platform2dev 平台下发； dev2platform 设备上报 |
| mainBizType | string | 是   | 仅二选一： device 设备硬件类 / aiBiz AI 全业务类 |
| subBizType | string | 是   | 细分业务标识，区分同大类下不同功能 |
| payload | object | 是   | 独立业务载荷，不同 subBizType 字段完全隔离 |
| extend | object | 否   | 厂商私有硬件 / 调试扩展，不污染标准字段 |

## 2.3 通用统一回执模板

回执subBizType 与原请求保持一致，通过sourceMsgId 绑定原始消息实现请求匹配

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:" 回 执 独 立 UUID",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:30:01Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"config",  
    **"payload"**:{  
        **"sourceMsgId"**:"原始请求messageId",  
        **"code"**:200,  
        **"msg"**:"业务执行描述",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:Object{...}  
    },  
    **"extend"**:Object{...}  
}

### 回执 payload 通用字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| sourceMsgId | string | 是   | 对应下发指令的 messageId，用于请求 - 应答关联 |
| code | int | 是   | HTTP 对齐状态码：200 成功，4xx 参数错误，5xx 设备内部异常 |
| msg | string | 是   | 可读简短执行结果文案 |
| errCode | int | 是   | 自定义业务错误码，0 = 无故障，非零代表细分故障 |
| errDetail | string | 否   | 详细故障堆栈 / 原因，成功留空 |
| bizData | object | 否   | 查询 / 抓拍 / 列表类业务返回数据，纯设置操作留空 |

# 三、设备类

**主要业务类型：**mainBizType=device

## 3.1 设备全量配置

**子业务类型：**subBizType=config

### 业务说明

平台下发 / 查询音视频、网络、PTZ、OSD、GB28181、RTMP、串口、存储、MQTT 全硬件参数，复用 HTTP 接口字段。

### 下发 payload 字段

platform2dev

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| operate | string | 是   | get 查询当前配置 / set 保存新配置 |
| configGroup | string | 是   | basic/net/video/audio/ptz/osd/ai/security/storage/time/rs485/rtmp/gb28181/mqtt/serial/mp3 |
| configData | object | 是   | 分组对应参数，get 时传空对象{} |

平台下发 JSON 示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-config-00112233-4455-6677-8899-aabbccddeeff",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:32:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"config",  
    **"payload"**:{  
        **"operate"**:"set",  
        **"configGroup"**:"video",  
        **"configData"**:{  
            **"code_stream_type"**:"主码流",  
            **"resolution"**:"1920\*1080",  
            **"video_coding"**:"H.265",  
            **"frame_rate"**:25,  
            **"StreamMode"**:"VBR",  
            **"MaxStreams"**:2048,  
            **"EncodeAudio"**:1,  
            **"AudioMode"**:"G711A",  
            **"AuidoInputVolume"**:70,  
            **"AudioOutputVolume"**:70  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

配置回执 JSON 示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-config-00aabbcc-1122-3344-5566-77889900aabb",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:32:02Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"config",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-config-00112233-4455-6677-8899-aabbccddeeff",  
        **"code"**:200,  
        **"msg"**:"视频码流配置保存成功",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
<br/>        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.2 通用设备控制

**子业务类型：**subBizType=ctrl，mainBizType=device。

### 3.2.1 实现状态与适用范围

截至 2026-09-07，设备端尚未支持本节新增的云台、镜头及高级控制功能。本节 controlVersion=1 是供平台和设备端后续开发、联调使用的接口约定，不表示功能已实现或已通过实机验收。以下能力及回执示例均为协议样例，不是当前设备实测数据。

原有 reboot、fully_recovery、recordStart、recordStop、formatDisk、firmwareUpgrade、exportParam、importParams 等动作继续保留，本次不修改其参数和执行语义。原有 ptzCmd、ptz_up、ptz_down、call_preset、add_preset、delete_preset 名称继续使用；新增控制必须经过能力查询，不得向未声明 controlVersion=1 的设备直接试发。

平台提供统一播放界面，按设备和通道的真实能力开放操作。VLStream 设备按本节通过 MQTT 执行控制；其他接入协议由平台适配，不能要求国标、ONVIF、ISUP 或纯视频源直接解析本节 MQTT 消息。设备及通道仍以 WVP 目录为准，指令使用设备业务 deviceId 和设备上报的 channelId，不使用平台数据库行 ID。

方向盘中央按钮固定为“停止云台”，调用 ptz_stop，不能解释为自动旋转、恢复出厂或回到默认预置位。“摄像机管理后台”打开原厂 Web 后台，不进入 VLS 配置页，也不使用视频播放地址代替管理地址。

### 3.2.2 消息封装与控制入口

沿用第 2 章公共消息头、设备总线 Topic、QoS 1 和 sourceMsgId 回执关联规则。请求和回执的 subBizType 均为 ctrl，mainBizType 均为 device，Retain=false。本文档修订版本与协议头 protocolVersion=2.2 分开管理，本次不更改外层版本号。

| payload 字段 | 类型 | 约束 |
| --- | --- | --- |
| ctrlAction | string | 原有通用动作，或下表新增的能力查询、状态查询、预置位列表、巡航组列表；操作设备使用 ptzCmd。 |
| controlVersion | int | 本节新控制规格固定为 1；不改变本节外其他业务消息。 |
| param | object | 根据 ctrlAction 提交参数；能力查询传空对象。 |

| ctrlAction | param | 用途 |
| --- | --- | --- |
| getControlCapabilities | {} | 返回当前设备全部通道的控制能力和原厂管理地址。 |
| getControlState | 对象，含 channelId:string | 查询指定通道运行模式、运动状态和当前请求。 |
| getPresetList | 对象，含 channelId:string | 返回 [{presetId,name}]；仅设备支持预置位时提供。 |
| getCruiseList | 对象，含 channelId:string | 返回 [{cruiseId,name}]；仅设备支持巡航时提供。 |
| ptzCmd | 见后续参数表 | 执行云台、镜头、灯光、菜单、跟踪、定位、巡航及守望操作。 |

查询超时为 5 秒。未收到能力回执、回执版本不支持或缺少通道时，平台记录“能力未知”，禁用该通道控制入口，不把超时解释为支持。设备明确返回不支持时显示“不支持”。播放能力与控制能力分别判断。

### 3.2.3 能力查询与原厂管理地址

getControlCapabilities 成功回执的 payload.bizData 使用下列字段。设备只声明固件已经实现、硬件实际具备且当前可执行的能力；未实现的动作不得列入 supportedCommands。未来固件仅支持部分控制时，可返回部分命令。

| bizData 字段 | 类型 | 约束 |
| --- | --- | --- |
| controlVersion | int | 固定为 1。 |
| capabilityRevision | string | 能力内容变化时更新的版本标识。 |
| firmwareVersion | string | 当前固件版本，供平台区分实现差异。 |
| deviceInstanceId | string | 设备每次启动生成新的 UUID；本次运行周期内稳定，用于拒绝重启前遗留控制。 |
| channels | array | 全量通道能力快照；无控制能力时为空数组。 |
| channels[].channelId | string | 与设备视频源的 channelId 一致；主、辅码流共享同一通道控制。 |
| channels[].supportedCommands | string[] | 取值限于本节 ptzCmd 枚举；未出现即不支持。 |
| channels[].speedMin / speedMax | int | 支持调速时必填，1 ≤ speedMin ≤ speedMax ≤ 36。 |
| channels[].maxHoldMs | int | 支持连续运动时必填，范围 100～1000 毫秒。 |
| channels[].presetIdMin / presetIdMax | int | 支持预置位时必填，范围在 1～36 内。 |
| channels[].menuActions | string[] | 支持 osd_menu 时必填，仅包含设备实际支持的菜单动作。 |
| managementUrl | string 或 null | 原厂 Web 后台完整 HTTP/HTTPS 地址；没有地址时为 null。 |

声明任一云台或镜头运动、跟踪、定位、巡航、守望能力时，必须同时实现 ptz_stop。能力按通道判定，不能由同型号、同协议或“视频已出画”推断。平台在首次打开控制区、设备重连或固件版本变化后重新查询，离线时使缓存失效。旧设备不支持查询时保留视频播放，控制能力维持未知。

managementUrl 只描述原厂后台入口，不携带用户名、密码、平台 Token、一次性鉴权参数或播放器链接。平台验证 HTTP/HTTPS 地址，并结合实际网络判断浏览器是否可达；跨网访问使用已配置的映射地址。缺少或不可达时提示原因，不拼接猜测地址，不自动登录设备。

### 3.2.4 操作请求的公共参数

下表字段位于 ctrlAction=ptzCmd 的 payload.param 中。查询类请求不需要 deviceInstanceId、sequence、expiresAt 或 ptzCmd。

| param 字段 | 类型 | 约束 |
| --- | --- | --- |
| channelId | string | 必填，精确指定设备上报的通道。 |
| deviceInstanceId | string | 必填，与最近一次能力查询结果一致。 |
| ptzCmd | string | 必填，必须在该通道 supportedCommands 中。 |
| sequence | int | 必填，1～9007199254740991；平台按 deviceId、deviceInstanceId、channelId 统一分配递增序号。 |
| expiresAt | string | 必填，UTC ISO-8601；必须晚于 sentAt，且不超过 sentAt 后 1500 毫秒。 |
| speed | int | 连续方向、变倍、聚焦、光圈运动必填；必须在设备声明区间内。停止命令不传 speed。 |
| holdMs | int | 连续运动必填，100～1000 毫秒且不超过 maxHoldMs；表示本次指令最长运动时间。 |
| timeoutMs | int | 预置位调用、3D 定位、镜头初始化及辅助聚焦的执行期限；1000～60000 毫秒，省略时 10000。 |

sequence 由平台统一调度器分配，不能由不同浏览器各自从 1 开始。平台同一通道同一时刻只允许一个操作者发起运动；其他操作者收到占用提示，具有该通道控制权限的停止请求可打断当前操作。设备按本次启动周期保存已接收序号，拒绝乱序的非重复指令；平台重启后应先查询 getControlState.lastSequence，再恢复分配。

deviceInstanceId 不符、通道不存在、能力不支持、时间无效、参数越界或序号陈旧时，设备必须拒绝执行并返回原因。设备启动后未完成校时、已知时钟偏差超过 250 毫秒时，不接受运动指令；停止请求仍应尽力执行，并返回真实停止结果。停止不依赖当前是否正在播放视频。

### 3.2.5 云台与镜头命令

| ptzCmd | 操作 | 专用参数和行为 |
| --- | --- | --- |
| ptz_up / ptz_down | 向上 / 向下 | speed、holdMs；按正常方向视频观察时控制对应方向。 |
| ptz_left / ptz_right | 向左 / 向右 | speed、holdMs。 |
| ptz_stop | 中央停止按钮 | 停止本通道方向、镜头运动及正在执行的跟踪、巡航、预置位或 3D 转动；暂停守望自动触发，需再次明确启用才恢复。 |
| zoom_in / zoom_out | 光学变倍放大 / 缩小 | speed、holdMs；控制物理镜头，不是前端画面缩放。 |
| zoom_stop | 停止变倍 | 只停止变倍轴。 |
| focus_near / focus_far | 聚焦近 / 远 | speed、holdMs；调整清晰度，不改变前端显示倍率。 |
| focus_stop | 停止聚焦 | 只停止聚焦轴。 |
| iris_open / iris_close | 光圈开大 / 缩小 | speed、holdMs；需硬件电动光圈。 |
| iris_stop | 停止光圈 | 只停止光圈轴。 |
| add_preset | 保存预置位 | presetId:int，范围由能力声明；name:string，1～64 字符。 |
| call_preset | 调用预置位 | presetId:int，必须已存在；可选 speed 在设备能力范围内；timeoutMs。 |
| delete_preset | 删除预置位 | presetId:int，必须已存在；不得删除其他编号。 |

界面“光学变倍控制”横条与放大镜一组按钮统一映射 zoom_in/zoom_out，作为同一能力的两个入口；准星一组映射 focus_near/focus_far，文字统一为“聚焦”；光圈一组映射 iris_open/iris_close。不能沿用“调焦/聚集”的模糊名称把变倍和聚焦接反。

停止命令在通道空闲时返回成功并明确 moving=false。若硬件故障或校准过程确实无法中断，必须返回失败及仍在运动的状态，不能仅因已接收命令就声称停止成功。设备不能满足停止要求的动作不得声明可用。

### 3.2.6 高级命令与参数

| ptzCmd | 专用参数 | 行为 |
| --- | --- | --- |
| light_set | enabled:bool | 明确开灯或关灯；重复请求不能切换为相反状态。厂商灯光或辅助开关编号由设备适配，不由界面猜测。 |
| focus_auto | timeoutMs | 执行一次辅助自动聚焦，完成后回报终态。 |
| lens_init | timeoutMs | 执行镜头初始化；是镜头动作，不是重启或恢复出厂。 |
| osd_menu | menuAction:string | open、up、down、left、right、confirm、back、close；必须在 menuActions 中。菜单打开时，菜单方向操作与云台运动分开处理。 |
| manual_track_start | target:object、durationMs:int | 以目标框启动设备侧跟踪；durationMs 为 1000～60000 毫秒，到期自动停止。 |
| manual_track_stop | 无 | 停止跟踪，不删除设备算法或录像配置。 |
| position_3d | target:object、timeoutMs | 对所选画面区域执行一次云台定位及变倍；目标框不是地图地理坐标。 |
| cruise_start | cruiseId:int、durationMs:int | cruiseId 为 getCruiseList 返回的编号；durationMs 为 1000～60000 毫秒，到期自动停止。 |
| cruise_stop | 无 | 停止当前巡航。 |
| guard_set | enabled:bool | false 关闭守望；true 时另传 idleSeconds:int（5～3600）、guardAction（preset 或 cruise）及对应编号。 |

守望 guardAction=preset 时必填 presetId，且设备同时支持 call_preset；guardAction=cruise 时必填 cruiseId 和 durationMs，且设备同时支持 cruise_start。仅在通道无人工操作、无其他运动任务，连续空闲达到 idleSeconds 后触发一次。再次人工操作重置计时；守望动作结束后保持等待，不在无人操作时无限重触发。再次明确启用或下一轮人工操作结束后，开始新的空闲周期。重启、MQTT 断线或 ptz_stop 后守望暂停，平台不得自动恢复，需操作者重新启用。

一键巡航、一键守望必须先有明确的巡航组或预置位设置；尚未配置时引导选择，不能默认使用编号 1。固件暂不提供巡航组配置功能时，可返回原厂后台已配置的巡航组供选择；本版列表接口不承担巡航路线编辑。

跟踪和 3D 定位的 target 固定为 {x,y,width,height}，四项均为整数，使用原始视频有效画面的 0～10000 归一化坐标，左上角为原点，x 向右、y 向下。必须满足 0 ≤ x,y < 10000，1 ≤ width,height ≤ 10000，x+width ≤ 10000，y+height ≤ 10000。平台先去除黑边，逆向还原显示缩放、镜像和旋转，再换算坐标；不允许直接发送网页像素或包含黑边的框。主、辅码流采用相同传感器视场时可共享坐标；裁剪视场不一致时必须完成映射，否则禁用框选。

设备丢失跟踪目标、到达机械限位或无法完成动作时，停止相应运动并回报原因；不能持续运动等待平台猜测。各示例独立，发起任何高级动作前均需能力查询确认。

### 3.2.7 按住运动与停止保障

平台在按下方向或镜头按钮时发送一个新 messageId 和 sequence 的有限时长指令；持续按住时，建议每 200 毫秒发送一次新指令，holdMs 建议为 500 毫秒。设备 maxHoldMs 较小时，平台同步缩短 holdMs 和续发间隔，续发间隔必须小于 holdMs；网络无法满足时使用单次短步控制，不延长设备声明的上限。相同动作的下一条合法指令可以延长运动至该次接收后的 holdMs；松开后必须停止续发并发送对应停止指令。

松开鼠标或触摸、指针取消、切换设备或通道、关闭弹窗、页面失去可操作状态时，平台停止续发并发送停止。中央按钮发送 ptz_stop。设备连续运动的计时器必须独立于平台回执和 MQTT 心跳；最后一条新运动指令的 holdMs 到期后自动停机，因此浏览器断网而设备仍连接 MQTT 时也能停下。

expiresAt 是最晚接收时间，不是允许无限执行的期限。到期的非停止指令一律拒绝；已接收的连续运动按 holdMs 结束，单次异步动作按 timeoutMs 结束，巡航和跟踪按 durationMs 结束。MQTT 断线时立即停止运动和自动模式；不能等待 60 秒 KeepAlive 才停止连续运动。

停止请求优先于排队运动请求执行，并取消本通道尚未执行且序号较小的运动指令。后到的旧运动指令不能覆盖已经生效的停止。有效鉴权且 deviceInstanceId、channelId 正确的停止命令，即使过期、乱序或重复，也应幂等执行停止；它不降低 lastSequence。停止属于保守动作，可能中止较新的运动，平台不得对旧停止无限重试。

运动命令不适用第六章长时间退避重试和离线补发规则。平台不向离线设备发布运动命令，不保存供上线后执行的运动队列。QoS 1 重复投递相同 messageId 时，设备重发该请求最近一次回执，不能重复启动动作、增加计时或延长 holdMs。去重记录至少覆盖本次启动期间最近 5 分钟；过期消息仍按 expiresAt 拒绝。设备重启生成新的 deviceInstanceId，旧消息不能作用于新运行周期。

平台未收到停止回执时，可在 1500 毫秒内以原 messageId、原 sequence 最多补发 2 次，随后显示“停止结果未知”并查询状态，不显示“已停止”。有限时长运动到期、设备断线、机械限位和执行超时均由设备主动结束，并发送可获取的最终状态。

### 3.2.8 回执状态与错误码

回执沿用 payload.sourceMsgId、code、msg、errCode、errDetail、bizData。新增字段放在 bizData 中：controlVersion、deviceInstanceId、channelId、ptzCmd、sequence、status、eventSeq、stopReason 和 state。查询回执按查询结果返回，不需要 ptzCmd 或操作序号。

| 字段或状态 | 约束 |
| --- | --- |
| status=accepted / running | 非终态，code=202、errCode=0；只表示已受理或正在执行。 |
| status=succeeded | 终态，code=200、errCode=0；指令要求的动作已完成。停止请求必须已确认停止。 |
| status=failed | 终态，code 为 4xx/5xx，errCode 非 0；说明拒绝或执行失败原因。 |
| status=canceled | 终态，code=409、errCode=3108；被更新动作或停止打断。 |
| eventSeq | 同一 sourceMsgId 从 1 递增；平台忽略旧序号状态，终态不得回退成执行中。 |
| stopReason | 可选：user、hold_expired、duration_expired、disconnected、limit、timeout、superseded。 |
| state | 当前设备可确认状态；未知字段用 null，不能用 false 代替未知。 |

getControlState 成功结果包括 controlVersion、deviceInstanceId、channelId、lastSequence 和 state。state 至少包含 mode（idle、manual、positioning、tracking、cruise、guard、calibrating 或 unknown）、moving（bool 或 null）、activeRequestIds（string[]）、lightOn（bool 或 null）、guardEnabled（bool 或 null）。supportedCommands 中未提供灯光或守望时，相应状态可为 null。

连续运动启动后可返回 running；holdMs 正常到期后返回 succeeded、stopReason=hold_expired；被新的同向续发请求接替时，旧请求返回 canceled、stopReason=superseded，当前状态仍可 moving=true。平台将同向续发导致的 superseded 视为正常接替，不反复弹出故障提示。停止是独立请求：先终结被中断请求，再针对停止请求返回 succeeded 和 moving=false。自然完成的定位、聚焦、初始化、巡航或跟踪，只有动作实际结束才返回 succeeded。

| code | errCode | 含义 |
| --- | --- | --- |
| 400 | 3101 | 参数缺失、类型错误或超出范围。 |
| 404 | 3102 | 通道、预置位或巡航组不存在。 |
| 501 | 3103 | 控制规格、命令或硬件能力未实现。 |
| 410 | 3104 | 非停止指令已过期。 |
| 409 | 3105 | deviceInstanceId 不匹配或非停止指令序号陈旧。 |
| 409 | 3106 | 通道忙或当前模式不允许该操作。 |
| 503 | 3107 | 设备未校时或时钟不满足实时控制要求。 |
| 409 | 3108 | 请求被停止或更新指令取消。 |
| 504 | 3109 | 动作超时，设备已尝试停止。 |
| 500 | 3110 | 硬件执行失败、目标丢失或无法确认停止。 |
| 409 | 3111 | 到达机械限位，无法继续请求的运动。 |

这些是本节新增的业务错误码，不更改其他业务已有错误码。无法识别本规格的旧固件可能不返回上述回执，平台仍按能力未知处理。MQTT PUBACK、HTTP 200 或消息发布成功只能证明传输/受理情况，不能代替设备终态；状态查询失败或断线时显示结果未知，不伪造成功。

### 3.2.9 JSON 示例

以下示例均使用虚构设备和地址。请求公共头使用标准 UUID；实际发送时必须重新生成 messageId、sentAt、expiresAt，并使用真实能力查询返回的 deviceInstanceId。

**能力查询请求**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000001",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.000Z",
  "msgDir": "platform2dev",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "ctrlAction": "getControlCapabilities",
    "controlVersion": 1,
    "param": {}
  },
  "extend": {}
}
```

**未来固件能力查询成功示例**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000002",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.000Z",
  "msgDir": "dev2platform",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "sourceMsgId": "19272022-b59b-4a74-8e87-000000000001",
    "code": 200,
    "msg": "控制能力查询成功",
    "errCode": 0,
    "errDetail": "",
    "bizData": {
      "controlVersion": 1,
      "capabilityRevision": "ctrl-1-r1",
      "firmwareVersion": "future-control-build",
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "channels": [
        {
          "channelId": "CH-1",
          "supportedCommands": [
            "ptz_up",
            "ptz_down",
            "ptz_left",
            "ptz_right",
            "ptz_stop",
            "zoom_in",
            "zoom_out",
            "zoom_stop"
          ],
          "speedMin": 1,
          "speedMax": 36,
          "maxHoldMs": 500
        }
      ],
      "managementUrl": "http://192.0.2.10/"
    }
  },
  "extend": {}
}
```

**按下向左按钮**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000003",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.000Z",
  "msgDir": "platform2dev",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "ctrlAction": "ptzCmd",
    "controlVersion": 1,
    "param": {
      "channelId": "CH-1",
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "ptzCmd": "ptz_left",
      "sequence": 101,
      "expiresAt": "2026-09-07T08:00:01.000Z",
      "speed": 10,
      "holdMs": 500
    }
  },
  "extend": {}
}
```

**设备开始运动的非终态回执**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000004",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.050Z",
  "msgDir": "dev2platform",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "sourceMsgId": "19272022-b59b-4a74-8e87-000000000003",
    "code": 202,
    "msg": "云台正在向左运动",
    "errCode": 0,
    "errDetail": "",
    "bizData": {
      "controlVersion": 1,
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "channelId": "CH-1",
      "ptzCmd": "ptz_left",
      "sequence": 101,
      "status": "running",
      "eventSeq": 1,
      "state": {
        "mode": "manual",
        "moving": true,
        "activeRequestIds": [
          "19272022-b59b-4a74-8e87-000000000003"
        ],
        "lightOn": null,
        "guardEnabled": false
      }
    }
  },
  "extend": {}
}
```

**松开按钮或点击中央停止**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000005",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.150Z",
  "msgDir": "platform2dev",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "ctrlAction": "ptzCmd",
    "controlVersion": 1,
    "param": {
      "channelId": "CH-1",
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "ptzCmd": "ptz_stop",
      "sequence": 102,
      "expiresAt": "2026-09-07T08:00:01.150Z"
    }
  },
  "extend": {}
}
```

**被停止打断的原运动请求回执**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000006",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.200Z",
  "msgDir": "dev2platform",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "sourceMsgId": "19272022-b59b-4a74-8e87-000000000003",
    "code": 409,
    "msg": "运动已被停止请求中断",
    "errCode": 3108,
    "errDetail": "",
    "bizData": {
      "controlVersion": 1,
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "channelId": "CH-1",
      "ptzCmd": "ptz_left",
      "sequence": 101,
      "status": "canceled",
      "eventSeq": 2,
      "state": {
        "mode": "idle",
        "moving": false,
        "activeRequestIds": [],
        "lightOn": null,
        "guardEnabled": false
      },
      "stopReason": "user"
    }
  },
  "extend": {}
}
```

**停止请求成功回执**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000007",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.210Z",
  "msgDir": "dev2platform",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "sourceMsgId": "19272022-b59b-4a74-8e87-000000000005",
    "code": 200,
    "msg": "本通道已停止",
    "errCode": 0,
    "errDetail": "",
    "bizData": {
      "controlVersion": 1,
      "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
      "channelId": "CH-1",
      "ptzCmd": "ptz_stop",
      "sequence": 102,
      "status": "succeeded",
      "eventSeq": 1,
      "state": {
        "mode": "idle",
        "moving": false,
        "activeRequestIds": [],
        "lightOn": null,
        "guardEnabled": false
      },
      "stopReason": "user"
    }
  },
  "extend": {}
}
```

**尚不支持新控制规格的明确失败回执**

```json
{
  "protocolVersion": "2.2",
  "messageId": "19272022-b59b-4a74-8e87-000000000008",
  "deviceId": "CAM-20260001",
  "sentAt": "2026-09-07T08:00:00.000Z",
  "msgDir": "dev2platform",
  "mainBizType": "device",
  "subBizType": "ctrl",
  "payload": {
    "sourceMsgId": "19272022-b59b-4a74-8e87-000000000001",
    "code": 501,
    "msg": "当前固件尚未实现控制规格 1",
    "errCode": 3103,
    "errDetail": "控制按钮应禁用，视频播放不受影响",
    "bizData": {
      "controlVersion": 1,
      "status": "failed",
      "eventSeq": 1
    }
  },
  "extend": {}
}
```

**镜头与高级动作的 param 示例**

以下对象仅替换 ptzCmd 请求的 payload.param，外层消息格式与前面的向左请求一致。每个例子独立使用，序号仅作演示；发送前更新时效字段并确认设备具备相应能力，巡航组 2 和预置位 3 必须事先存在。

**光学变倍**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "zoom_in",
  "sequence": 103,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "speed": 10,
  "holdMs": 500
}
```

**聚焦**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "focus_near",
  "sequence": 104,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "speed": 8,
  "holdMs": 500
}
```

**光圈**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "iris_open",
  "sequence": 105,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "speed": 8,
  "holdMs": 500
}
```

**灯光**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "light_set",
  "sequence": 106,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "enabled": true
}
```

**辅助聚焦**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "focus_auto",
  "sequence": 107,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "timeoutMs": 10000
}
```

**镜头初始化**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "lens_init",
  "sequence": 108,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "timeoutMs": 30000
}
```

**打开设备菜单**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "osd_menu",
  "sequence": 109,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "menuAction": "open"
}
```

**手动跟踪**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "manual_track_start",
  "sequence": 110,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "target": {
    "x": 2000,
    "y": 2500,
    "width": 3000,
    "height": 4000
  },
  "durationMs": 30000
}
```

**3D 定位**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "position_3d",
  "sequence": 111,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "target": {
    "x": 2000,
    "y": 2500,
    "width": 3000,
    "height": 4000
  },
  "timeoutMs": 10000
}
```

**巡航**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "cruise_start",
  "sequence": 112,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "cruiseId": 2,
  "durationMs": 30000
}
```

**启用预置位守望**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "guard_set",
  "sequence": 113,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "enabled": true,
  "idleSeconds": 60,
  "guardAction": "preset",
  "presetId": 3
}
```

**关闭守望**

```json
{
  "channelId": "CH-1",
  "deviceInstanceId": "9351f7ce-80d6-4f34-bacc-966351e88ad6",
  "ptzCmd": "guard_set",
  "sequence": 114,
  "expiresAt": "2026-09-07T08:00:01.000Z",
  "enabled": false
}
```

### 3.2.10 联调与启用条件

设备端开发完成后，按型号和固件版本记录以下验证结果：能力查询与实际硬件一致；不支持的按钮禁用；指令控制正确通道；方向、变倍、聚焦、光圈和停止行为正确；后台入口打开原厂页面；高级动作使用明确配置和有效坐标；设备真实终态与平台显示一致。

异常用例至少覆盖未知命令、缺字段、越界参数、离线、过期、重复、乱序、设备重启、平台重启后恢复序号、时钟未同步、两人争用、长按后断网、松开及关闭弹窗、镜头或云台限位、设备故障、停止回执丢失，以及终态后收到旧 running 回执。校验连续运动在最后一条有效指令 holdMs 到期后停止，旧消息不能让已停止或已重启的设备重新运动。

完成“平台请求 → 设备执行 → 设备回执 → 页面状态”的实机联调后，才可把对应型号和固件功能标记为已支持。本次仅更新协议文档；平台实现、设备固件、统一弹窗和硬件验收均为后续工作。

## 3.3 远程抓图

**子业务类型：**subBizType=snapshot

### 业务说明

平台下发实时抓拍，设备返回 Base64 图片，硬件媒体操作。

### 下发 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| ctrlAction | string | 是   | 固定SnapShot |
| type | string | 是   | 固定Directly |
| snapChannel | int | 是   | 抓拍通道号 |

下发示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-snap-1234abcd-5678ef90-112233445566",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:35:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"snapshot",  
    **"payload"**:{  
        **"ctrlAction"**:"SnapShot",  
        **"type"**:"Directly",  
        **"snapChannel"**:1  
    },  
    **"extend"**:{  
<br/>    }  
}

### 抓图回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| result | string | ok/failed |
| snapChannel | int | 抓拍通道 |
| image | string | jpg base64 编码图 |

回执完整示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-snap-9876dcba-4321fe09-667788990011",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:35:01Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"snapshot",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-snap-1234abcd-5678ef90-112233445566",  
        **"code"**:200,  
        **"msg"**:"抓拍完成",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"result"**:"ok",  
            **"snapChannel"**:1,  
            **"image"**:"data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAEBAQEBAQEBAQEB..."  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.4 设备校时

**子业务类型：**subBizType=time

### 业务说明

下发标准时间同步设备硬件时钟。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| time | string | 是   | yyyy-MM-dd HH:mm:ss |

下发示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-time-22334455-66778899-00112233abcd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:36:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"time",  
    **"payload"**:{  
        **"time"**:"2026-07-24 09:36:00"  
    },  
    **"extend"**:{  
<br/>    }  
}

校时回执示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-time-33445566-77889900-11223344dcba",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:36:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"time",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-time-22334455-66778899-00112233abcd",  
        **"code"**:200,  
        **"msg"**:"设备时间同步完成",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
<br/>        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.5 媒体上传交互

**子业务类型：**subBizType=mediaUpload

### 业务说明

图片 / 录像走 MinIO HTTP 上传，MQTT 仅传元数据，禁止大 Base64。

### 1）平台获取上传地址

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| mediaId | string | 是   | 媒体唯一 UUID |
| mediaType | string | 是   | image/jpeg / video/mp4 |
| fileSize | number | 是   | 文件字节大小 |
| sha256 | string | 是   | 文件校验哈希 |

下发示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-media-upload-00112233-44556677-8899aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:37:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"mediaUpload",  
    **"payload"**:{  
        **"mediaId"**:"MEDIA-UUID-0001",  
        **"mediaType"**:"image/jpeg",  
        **"fileSize"**:102400,  
        **"sha256"**:"abcdef1234567890abcdef1234567890"  
    },  
    **"extend"**:{  
<br/>    }  
}

### 2）平台返回上传地址回执

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| mediaId | string | 媒体 ID |
| uploadUrl | string | PUT 签名上传地址 |
| objectUrl | string | 永久访问地址 |
| expiresAt | string | URL 过期 UTC 时间 |

回执示例：bizData 字段

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-media-url-11223344-55667788-9900aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:37:01Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"mediaUpload",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-media-upload-00112233-44556677-8899aabbccdd",  
        **"code"**:200,  
        **"msg"**:"上传地址已生成",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"mediaId"**:"MEDIA-UUID-0001",  
            **"uploadUrl"**:"https://minio.test.com/upload/xxx",  
            **"objectUrl"**:"https://minio.test.com/res/xxx",  
            **"expiresAt"**:"2026-07-24T10:37:00Z"  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

### 3）设备上传完成上报

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| mediaId | string | 媒体 ID |
| objectUrl | string | 永久资源地址 |
| fileSize | number | 文件大小 |
| sha256 | string | 文件哈希 |
| uploadCostMs | number | 上传耗时毫秒 |

上报完整示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-media-upload-00112233-44556677-8899aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:37:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"mediaUpload",  
    **"payload"**:{  
        **"mediaId"**:"MEDIA-UUID-0001",  
        **"mediaType"**:"image/jpeg",  
        **"fileSize"**:102400,  
        **"sha256"**:"abcdef1234567890abcdef1234567890"  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.6 设备心跳 & 硬件遥测

**子业务类型：**subBizType=state

### 业务说明

设备定时上报在线状态、硬件资源、版本以及平台可拉取的视频源。消息 Retain=true，断网自动下发遗嘱离线消息。平台以首次收到的合法 deviceBiz/state 自动登记未知设备。设备只负责提供源流，不直接调用 ZLMediaKit，也不生成浏览器 WebRTC 地址。 state 上报使用 mainBizType=deviceBiz；平台在过渡期兼容旧值 device，并在回执中原样返回上报消息使用的 mainBizType。

streams 是视频源描述对象数组，不是视频文件、视频帧，也不是要求设备向平台推流。每个数组元素描述一路平台后端可以主动拉取的 RTSP/RTMP 视频源。硬件端填写规则：  
1\. 每次 deviceBiz/state 都上报当前全部已配置视频流，不得只传新增或变化的流。  
2\. 同一摄像头通道有主码流和子码流时，传两个对象：channelId 相同，streamType 分别为 main 和 sub。  
3\. 已配置但暂时无法拉取的流仍保留在数组中，并传 available=false；流被永久删除后才从数组移除。  
4\. 设备没有视频能力或尚未配置拉流地址时传 streams: \[\]。  
5\. channelId + streamType 是一路流的稳定标识，设备重启或拉流地址变化后不得随意改变；url 必须能被平台后端访问。  
平台收到新心跳后，会把未出现在本次数组中的旧流标记为不可用。url 属于敏感信息，只允许通过受控 MQTT/TLS 上报，不得写入日志。平台仅在用户预览时按需拉流并转换为 WebRTC，无人观看后自动释放。

### 上报 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| online | bool | 是   | true 在线 /false 离线 |
| reason | string | 是   | normal/mqtt_connection_lost/power_off |
| heartbeatIndex | int | 是   | 心跳计数，重启重置 1 |
| deviceName | string | 是   | 设备名称 |
| deviceSerial | string | 否   | 硬件序列号 |
| version | string | 否   | 固件版本 |
| deviceFaceVer | string | 否   | 人脸库版本（人脸机专属） |
| ipAddr | string | 否   | 局域网 IP |
| mac | string | 否   | MAC 地址 |
| telemetry | object | 否   | 硬件资源 |
| telemetry.cpu | int | 否   | CPU 占用 % |
| telemetry.mem | int | 否   | 内存占用 % |
| telemetry.diskUsed | int | 否   | 磁盘使用率 % |
| telemetry.diskTotalMB | int | 否   | 磁盘总容量，单位 MB；适用于存储容量较小的嵌入式设备 |
| telemetry.temp | int | 否   | 设备温度℃ |
| telemetry.netUpMbps | float | 否   | 上行带宽 |
| telemetry.netDownMbps | float | 否   | 下行带宽 |
| serviceStatus | object | 否   | 后台服务状态 |
| serviceStatus.rtsp | bool | 否   | RTSP 服务 |
| serviceStatus.gb28181 | bool | 否   | GB28181 服务 |
| serviceStatus.aiInfer | bool | 否   | AI 推理服务 |
| streams | array | 是   | 视频源描述对象数组，不是视频数据；每个元素代表一路平台可主动拉取的 RTSP/RTMP 地址，每次上报全部已配置流，无视频源时传 \[\] |
| streams\[\].channelId | string | 条件必填 | streams 非空时必填；同一设备内稳定且唯一 |
| streams\[\].name | string | 否   | 视频流显示名称 |
| streams\[\].streamType | string | 否   | main 主码流 / sub 子码流 / custom 自定义码流；默认 main |
| streams\[\].protocol | string | 条件必填 | streams 非空时必填；当前支持 rtsp 或 rtmp |
| streams\[\].url | string | 条件必填 | streams 非空时必填；平台后端可访问的完整拉流地址，可包含认证信息 |
| streams\[\].default | bool | 否   | 是否为默认预览流；默认 false，全设备最多一个 true |
| streams\[\].available | bool | 否   | 当前源流是否可用，默认 true |

正常心跳上报完整示例

{  
**"protocolVersion"**:"2.2",  
    **"messageId"**:"up-heartbeat-33445566-77889900-1122aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:38:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"deviceBiz",  
    **"subBizType"**:"state",  
    **"payload"**:{  
        **"online"**:true,  
        **"reason"**:"normal",  
        **"heartbeatIndex"**:120,  
        **"deviceName"**:"园区东门人脸机",  
        **"deviceSerial"**:"3161w316156d33x966",  
        **"version"**:"v1.1.02",  
        **"deviceFaceVer"**:"1021_v2",  
        **"ipAddr"**:"192.168.1.100",  
        **"mac"**:"00:11:22:33:44:55",  
        **"telemetry"**:{  
            **"cpu"**:32,  
            **"mem"**:45,  
            **"diskUsed"**:68,  
            **"diskTotalMB"**:136,  
            **"temp"**:48,  
            **"netUpMbps"**:8.2,  
            **"netDownMbps"**:12.5  
        },  
        **"serviceStatus"**:{  
            **"rtsp"**:true,  
            **"gb28181"**:false,  
            **"aiInfer"**:true  
        },  
        **"streams"**:\[  
            {  
                **"channelId"**:"CH-1",  
                **"name"**:"东门主码流",  
                **"streamType"**:"main",  
                **"protocol"**:"rtsp",  
                **"url"**:"rtsp://user:password@192.168.1.100:554/Streaming/Channels/101",  
                **"default"**:true,  
                **"available"**:true  
            },  
            {  
                **"channelId"**:"CH-1",  
                **"name"**:"东门子码流",  
                **"streamType"**:"sub",  
                **"protocol"**:"rtsp",  
                **"url"**:"rtsp://user:password@192.168.1.100:554/Streaming/Channels/102",  
                **"default"**:false,  
                **"available"**:true  
            }  
        \]  
    },  
    **"extend"**:{  
<br/>    }  
}

平台接收成功回执示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-heartbeat-33445566-77889900-1122aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:38:01Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"deviceBiz",  
    **"subBizType"**:"state",  
    **"payload"**:{  
        **"sourceMsgId"**:"up-heartbeat-33445566-77889900-1122aabbccdd",  
        **"code"**:200,  
        **"msg"**:"状态已接收",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
<br/>        }  
    },  
    **"extend"**:{  
<br/>    }}

离线遗嘱消息示例

{  
 **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-will-offline-44556677-88990011-2233aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:38:50Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"deviceBiz",  
    **"subBizType"**:"state",  
    **"payload"**:{  
        **"online"**:false,  
        **"reason"**:"mqtt_connection_lost",  
        **"heartbeatIndex"**:120,  
        **"deviceName"**:"园区东门人脸机",  
        **"deviceSerial"**:"3161w316156d33x966",  
        **"version"**:"v1.1.02",  
        **"ipAddr"**:"192.168.1.100",  
        **"mac"**:"00:11:22:33:44:55",  
        **"telemetry"**:{  
<br/>        },  
        **"serviceStatus"**:{  
<br/>        },  
        **"streams"**:\[  
<br/>        \]  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.8 二维码识别上报

**子业务类型：**subBizType=qrcode

### 业务说明

摄像头硬件识别二维码主动推送内容。

### 上报 payload 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| operator | string | 固定QRCodePush |
| info.facesluiceId | string | 设备 ID |
| info.time | string | 识别时间 yyyy-MM-dd HH:mm:ss |
| info.QRCodeInfo | string | 二维码原始文本 |

上报完整示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-qrcode-scan-33445566-77889900-1122aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:44:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"qrcode",  
    **"payload"**:{  
        **"operator"**:"QRCodePush",  
        **"info"**:{  
            **"facesluiceId"**:"CAM-20260001",  
            **"time"**:"2026-07-24 09:44:00",  
            **"QRCodeInfo"**:"https://test.company.com/user/10001"  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.10 本地录像查询

**子业务类型：**subBizType=recordSearch

### 业务说明

平台下发时间段，设备返回 SD 卡录像文件列表。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| startTime | string | 是   | yyyy-MM-dd HH:mm:ss |
| endTime | string | 是   | yyyy-MM-dd HH:mm:ss |

下发示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-record-search-44556677-88990011-2233aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:45:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"recordSearch",  
    **"payload"**:{  
        **"startTime"**:"2026-07-24 08:00:00",  
        **"endTime"**:"2026-07-24 09:00:00"  
    },  
    **"extend"**:{  
<br/>    }  
}

### 查询回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| facesluiceId | string | 设备 ID |
| result | string | ok/failed |
| filelist | array | 录像文件数组 |
| filelist.filename | string | 本地文件路径 |
| filelist.type | int | 录像类型 |
| filelist.start_time | long | 开始时间戳 |
| filelist.end_time | long | 结束时间戳 |

回执完整示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-record-search-55667788-99001122-3344aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:45:01Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"recordSearch",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-record-search-44556677-88990011-2233aabbccdd",  
        **"code"**:200,  
        **"msg"**:"录像文件查询完成",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"filelist"**:\[  
                {  
                    **"filename"**:"/sdcard/20260724/N165617.mp4",  
                    **"type"**:1,  
                    **"start_time"**:1784966400,  
                    **"end_time"**:1784969400  
                }  
            \],  
            **"facesluiceId"**:"CAM-20260001",  
            **"result"**:"ok"  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.11 录像上传任务下发

**子业务类型：**subBizType=recordUpload

### 业务说明

下发录像 HTTP 上传任务，设备二进制 POST 上传文件。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| startTime | string | 是   | 录像起始时间 |
| endTime | string | 是   | 录像结束时间 |
| filename | string | 是   | 本地录像路径 |
| uploadurl | string | 是   | 后端接收 POST 地址 |
| videoId | string | 否   | 自定义录像唯一标识 |

下发示例：

{  
  **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-record-upload-66778899-00112233-4455aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:45:30Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"recordUpload",  
    **"payload"**:{  
        **"startTime"**:"2026-07-24 08:00:00",  
        **"endTime"**:"2026-07-24 09:00:00",  
        **"filename"**:"/sdcard/20260724/N165617.mp4",  
        **"uploadurl"**:"",  
        **"videoId"**:"VID-0001"  
    },  
    **"extend"**:{  
<br/>    }  
}

## 3.13 硬件信息查询

**子业务类型：**subBizType=systemInfo

### 业务说明

查询 SD 卡 / 4G/WiFi/ 固件版本硬件信息。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| queryType | string | 是   | GetSdCardInfo/Get4GStatus/GetWifiStatus/GetDeviceVersion |

下发示例（查询 SD 卡）：

{  
 **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-system-sd-77889900-11223344-5566aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:47:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"systemInfo",  
    **"payload"**:{  
        **"queryType"**:"GetSdCardInfo"  
    },  
    **"extend"**:{  
<br/>    }}

### SD 卡查询回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| sd_info.status | int | 0 无卡 / 1 未格式化 / 3 挂载 / 5 使用中 / 6 满 |
| sd_info.total_size | int | 总容量 MB |
| sd_info.free_size | int | 剩余容量 MB |
| sd_info.record_mode | int | 0 停止 / 1 全天 / 2 报警录像 |
| sd_info.record_filelen | int | 录像分片分钟 |

回执完整示例：

{  
 **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-system-sd-88990011-22334455-6677aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:47:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"systemInfo",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-system-sd-77889900-11223344-5566aabbccdd",  
        **"code"**:200,  
        **"msg"**:"SD卡信息读取成功",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"sd_info"**:{  
                **"status"**:3,  
                **"total_size"**:8192,  
                **"free_size"**:1024,  
                **"record_mode"**:2,  
                **"record_filelen"**:5  
            }  
        }  
    },  
    **"extend"**:{  
<br/>    }}

## 3.14 设备日志分页上报

**子业务类型：**subBizType=log

### 业务说明

设备运行、操作审计日志分页推送。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| page | int | 当前页码 |
| limit | int | 单页条数 |
| total | int | 日志总条数 |
| logList | array | 日志明细数组 |
| logList.serial | int | 日志序号 |
| logList.time | string | 日志时间 |
| logList.mainType | string | 日志大类：正常 / 异常 |
| logList.subType | string | 细分故障 / 操作 |
| logList.channel | int | 对应通道 |
| logList.user | string | 操作用户 |
| logList.remoteIp | string | 操作客户端 IP |

上报完整示例：

{  
  **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-log-page-66778899-00112233-4455aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:46:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"log",  
    **"payload"**:{  
        **"page"**:1,  
        **"limit"**:10,  
        **"total"**:120,  
        **"logList"**:\[  
            {  
                **"serial"**:2,  
                **"time"**:"2026/07/24 09:45:00",  
                **"mainType"**:"异常",  
                **"subType"**:"音频输入异常",  
                **"channel"**:1,  
                **"user"**:"admin",  
                **"remoteIp"**:"192.168.1.100"  
            }  
        \]  
    },  
    **"extend"**:{  
<br/>    }}

# 四、AI 业务类

**主要业务类型：**mainBizType=aiBiz

## 4.1 AI 模型下发部署

**子业务类型：**subBizType=modelDeploy

### 业务说明

AI 推理模型下载、校验、切换推理实例。

### 下发 payload 完整字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| requestId | string | 是   | 模型任务唯一 ID |
| algorithmId | int | 是   | 算法业务编号 |
| trainingId | string | 否   | 训练任务 ID |
| modelType | string | 是   | om/tensorrt |
| modelUrl | string | 是   | 短期签名 HTTP 下载地址 |
| fileName | string | 是   | 模型文件名 |
| fileSize | number | 是   | 文件字节大小 |
| sha256 | string | 是   | 文件哈希校验码 |
| expiresAt | string | 是   | 下载链接过期 UTC 时间 |
| rollbackEnable | bool | 是   | 失败自动回滚旧模型 |
| modelConfig | object | 否   | 推理参数阈值 |
| modelConfig.confThreshold | float | 置信度阈值 |     |
| modelConfig.nmsThreshold | float | NMS 抑制阈值 |     |

下发完整示例：

### 模型回执 bizData 字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| requestId | string | 对应下发任务 ID |
| status | string | RECEIVED/DOWNLOADING/DOWNLOADED/VERIFYING/DEPLOYING/SUCCESS/FAILED |
| fileSha256 | string | 本地校验哈希 |
| costMs | number | 部署耗时毫秒 |

回执完整示例：

{

&nbsp;  **"protocolVersion"**:"2.2",  
    **"messageId"**:"11223344-5566-4788-9900-abcdef123456",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:33:45Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"modelDeploy",  
    **"payload"**:{  
        **"sourceMsgId"**:"77886655-1234-4678-abcd-12345678abcd",  
        **"code"**:200,  
        **"msg"**:"模型校验通过，部署完成",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"requestId"**:"MODEL-TASK-20260724-001",  
            **"status"**:"SUCCESS",  
            **"fileSha256"**:"3f2c9d11e88a77b665443211abcdef00987654321",  
            **"costMs"**:1200  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

## 4.2 查询当前加载模型

**子业务类型：**subBizType=modelQuery

### 业务说明

查询设备当前生效 AI 模型，下发 payload 为空对象{} 下发示例：

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-model-query-11112222-33334444-55556666",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T10:00:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"modelQuery",  
    **"payload"**:{  
<br/>    },  
    **"extend"**:{  
<br/>    }  
}

### 查询回执 bizData 字段

algorithmId、fileName、sha256、modelType

## 4.3 模型手动回滚

**子业务类型：**subBizType=modelRollback

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| requestId | string | 是   | 回滚任务 ID |

## 4.4 人脸库人员管理

**子业务类型：**subBizType=faceLib

### 业务说明

人脸底库增删改查（新增 / 删除 / 分页查询 / 单条查询 / 清空）。

### 下发 payload 通用字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| libOperate | string | 是   | EditPerson/QueryPerson/QuerySample/SearchPerson/DelPerson/DeleteAllPerson |
| customId | string | 增删查单条必填 | 人员全局唯一 UUID |
| name | string | EditPerson 必填 | 人员姓名 |
| telnum1 | string | 可选  | 联系电话 |
| age | int | 可选  | 年龄  |
| gender | int | 0 男 / 1 女 |     |
| idCard | string | 可选身份证 |     |
| valid_time_type | int | 0 每日 / 1 星期 / 2 日期区间 |     |
| start_time | long | 有效期起始秒 |     |
| expire_time | long | 有效期结束秒 |     |
| department_name | string | 部门  |     |
| personType | int | 0 白 / 1 黑 / 2VIP |     |
| notes | string | 备注  |     |
| op_face_ver | string | 人脸库版本 |     |
| pic | string | 人脸底图 base64 |     |

新增人员下发完整示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-face-lib-add-77889900-11223344-5566aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:40:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"faceLib",  
    **"payload"**:{  
        **"libOperate"**:"EditPerson",  
        **"customId"**:"713BCEF6393955E0DC8822354D0D61E1",  
        **"name"**:" 张 三 ",  
        **"telnum1"**:"13800138000",  
        **"age"**:30,  
        **"gender"**:0,  
        **"idCard"**:"400400199912120001",  
        **"valid_time_type"**:0,  
        **"start_time"**:0,  
        **"expire_time"**:86399,  
        **"department_name"**:"行政部",  
        **"personType"**:0,  
        **"notes"**:"园区内部员工",  
        **"op_face_ver"**:"1021_v2",  
        **"pic"**:"data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."  
    },  
    **"extend"**:{  
<br/>    }  
}

人脸库操作回执示例

{  
**"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-face-lib-add-88990011-22334455-6677aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:40:02Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"faceLib",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-face-lib-add-77889900-11223344-5566aabbccdd",  
        **"code"**:200,  
        **"msg"**:"人员新增成功",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"customId"**:"713BCEF6393955E0DC8822354D0D61E1",  
            **"personId"**:1,  
            **"result"**:"ok"  
        }  
    },  
    **"extend"**:{  
<br/>    }}

## 4.5 人脸通行 / 陌生人抓拍上报

**子业务类型：**subBizType=faceEvent

### 业务说明

人脸 AI 识别结果上报，区分白名单通行 / 陌生人。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| operator | string | entr 白名单 /entr_or_exit 陌生人 |
| info.customId | string | 库内人员 ID，陌生人空 |
| info.personid | int | 设备本地人员编号 |
| info.persionName | string | 姓名  |
| info.facesluiceId | string | 设备 ID |
| info.time | string | 抓拍时间 yyyy-MM-dd HH:mm:ss |
| info.pic | string | 现场抓拍图 base64 |
| info.sample_pic | string | 底库人脸图 base64 |
| info.score_dect | int | 比对分数 0~100 |
| info.rect | array | \[x,y,w,h\] 人脸框坐标 |
| info.gender | string | M 男 / F 女 |
| info.glasses | int | 1 无 / 2 普通 / 3 墨镜 |
| info.mask | int | 1 无 / 2 佩戴口罩 |
| info.hat | int | 1 无 / 2 戴帽 |

上报完整示例：

{  
  **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-face-pass-99001122-33445566-7788aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:41:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"faceEvent",  
    **"payload"**:{  
        **"operator"**:"entr",  
        **"info"**:{  
            **"customId"**:"713BCEF6393955E0DC8822354D0D61E1",  
            **"personid"**:1,  
            **"persionName"**:"张三",  
            **"facesluiceId"**:"CAM-20260001",  
            **"time"**:"2026-07-24 09:41:00",  
            **"pic"**:"data:image/jpg;base64,xxx",  
            **"sample_pic"**:"data:image/jpg;base64,xxx",  
            **"score_dect"**:92,  
            **"rect"**:\[  
                100,  
                120,  
                220,  
                260  
            \],  
            **"gender"**:"M",  
            **"glasses"**:1,  
            **"mask"**:1,  
            **"hat"**:1  
        }  
    },  
    **"extend"**:{  
<br/>    }}

## 4.6上报文件上传

#### . 业务说明

硬件端上传事件图片分两步：

1\. 调用 VLS 接口申请一个有时效的上传地址。

2\. 使用返回的 uploadUrl 将图片二进制内容 PUT 到 MinIO。

uploadUrl 由平台动态生成，默认 10 分钟有效。硬件端不需要 MinIO 账号和密码，也不能自行拼接上传地址。

#### 2\. 申请上传地址

**请求地址**

POST /vlsDeviceMedia/public/upload-url

Content-Type: application/json

##### 请求字段

| **字段** | **类型** | **是否必填** | **字段意思** |
| --- | --- | --- | --- |
| deviceId | String | 是   | 平台中已存在的设备编号 |
| fileName | String | 是   | 图片文件名，只传文件名，不传本地完整路径 |
| contentType | String | 是   | 图片类型：image/jpeg、image/png 或 image/webp |
| fileSize | Long | 是   | 图片真实字节数，必须大于 0，默认最大 10 MiB |
| sha256 | String | 是   | 图片内容的 SHA-256，64 位十六进制字符串 |

##### 调用示例

curl --request POST 'http://192.168.88.31:8080/vlsDeviceMedia/public/upload-url' \\

\--header 'Content-Type: application/json' \\

\--data-raw '{

"deviceId": "AETY-00-NJN2-WJUB-00000100",

"fileName": "capture.png",

"contentType": "image/png",

"fileSize": 715443,

"sha256": "fa354d240cf45cecd9383e8f2d4d8cca27333745481222f73561cc9e22c596d2"

}

##### 返回字段

| **字段** | **类型** | **字段意思** |
| --- | --- | --- |
| data.mediaId | String | 图片唯一编号，后续事件上报时使用 |
| data.objectKey | String | 图片在对象存储中的 Key，后续事件上报时使用 |
| data.uploadUrl | String | 本次图片上传的完整临时 PUT 地址 |
| data.expiresAt | String | 上传地址失效时间，UTC ISO-8601 格式 |
| data.requiredContentType | String | PUT 时必须使用的 Content-Type |

##### 返回示例

{

&nbsp;   **"code"**:200,  
    **"success"**:true,  
    **"data"**:{  
        **"mediaId"**:"155e3e5b-da17-42bb-b6c9-86cedeba0f14",  
        **"objectKey"**:"events/AETY-00-NJN2-WJUB-00000100/2026/08/04/155e3e5b-da17-42bb-b6c9-86cedeba0f14.png",  
        **"uploadUrl"**:"http://192.168.88.31:9000/ruoyi/events/...png?X-Amz-Algorithm=AWS4-HMAC-SHA256&...",  
        **"expiresAt"**:"2026-08-04T02:22:10.535Z",  
        **"requiredContentType"**:"image/png"  
    },  
    **"msg"**:"操作成功"

}

#### 3\. 上传图片文件

**请求地址**

使用上一步返回的完整 data.uploadUrl，不是固定地址。

##### 请求要求

| **内容** | **类型** | **是否必填** | **说明** |
| --- | --- | --- | --- |
| HTTP Method | String | 是   | 必须为 PUT |
| Content-Type | Header | 是   | 必须与 requiredContentType 完全一致 |
| Request Body | Binary | 是   | 图片原始二进制内容，不是 JSON、Base64 或文件路径 |

##### 调用示例

curl --request PUT '&lt;将这里替换为完整的 uploadUrl&gt;' \\

\--header 'Content-Type: image/png' \\

\--data-binary '@C:/Users/oort/Pictures/capture.png'

##### 返回示例

上传成功时 MinIO 返回 HTTP 200 OK，响应体通常为空。

如果返回 403 Request has expired，表示 uploadUrl 已过期，需要重新调用第 2 节接口申请新地址。

## 4.7 结构化人车 / 车牌 / 非机动车识别

**子业务类型：**subBizType=struct

### 业务说明

通用目标 AI 跟踪上报，人脸 / 人形 / 机动车 / 非机动车 / 车牌统一结构。

### 上报 payload 完整字段

|     |     |     |
| --- | --- | --- |
| **字段** | **类型** | **释义** |
| operator | string | 固定struct_attr |
| facesluiceId | string | 设备 ID |
| track_id | int | 目标跟踪唯一 ID |
| type | string | face/human/vehicle/cycle/plate |
| keep_time | int | 画面停留毫秒 |
| left/top/right/bottom | int | 目标像素框 |
| worth | float | 识别置信度 0~1 |
| datetime | string | 抓拍时间 |
| bind | array | 绑定目标 track_id（人脸绑定人形） |
| info | object | 目标细分属性 |
| object_image | string | 目标截图 base64 |
| bg_image | string | 背景图 base64 |
| mac | string | 设备 MAC |
| ipaddr | string | 设备 IP |
| algorithmId | string | 算法id |

人形结构化完整上报示例

{  
  **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-struct-human-00112233-44556677-8899aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:42:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"aiBiz",  
    **"subBizType"**:"struct",  
    **"payload"**:{  
        **"operator"**:"struct_attr",  
        **"facesluiceId"**:"CAM-20260001",  
        **"track_id"**:4,  
        **"type"**:"human",  
        **"keep_time"**:2022,  
        **"left"**:783,

&nbsp; **"algorithmId"**:”2077000000000001001”,  
        **"top"**:443,  
        **"right"**:1272,  
        **"bottom"**:983,  
        **"worth"**:0.853293,  
        **"datetime"**:"2026-07-24 09:42:00",  
        **"bind"**:\[  
<br/>        \],  
        **"info"**:{  
            **"clothes_color"**:4,  
            **"safety_helmet"**:1,  
            **"action_watch_phone"**:1,  
            **"bags"**:1  
        },  
        **"object_image"**:"data:image/jpeg;base64,xxx",  
        **"bg_image"**:"data:image/jpeg;base64,xxx",  
        **"mac"**:"00:11:22:33:44:55",  
        **"ipaddr"**:"192.168.1.100"  
    },  
    **"extend"**:{  
<br/>    }}

## 4.8 设备OTA固件升级

**子业务类型：**subBizType= firmwareDeploy

### 业务说明

平台仅对在线设备创建 RootFS OTA 任务。下发前必须按设备上报的 deviceModel 精确匹配固件，并比较 payload.version 与最新 READY 固件。版本采用至少三段的纯数字点分格式，逐段按数值比较，例如 1.0.1.14 高于 1.0.1.9。

OTA 包存放在私有对象存储中。平台先持久化任务，再通过 QoS 1 MQTT 发布携带平台短期下载 URL 的指令。设备必须校验公共信封、deviceId、deviceModel、target、fileSize、sha256 以及包内 manifest。MQTT 发布成功仅表示 PUBLISHED，不代表升级成功。

RootFS 包写入非活动 A/B 槽位，必须启用回滚并在升级后重启。设备只有在新版本通过启动健康检查后才能回执 SUCCESS；随后必须通过 device/state 再次上报实际版本，平台不得仅依据 OTA 回执改写设备当前版本。

### 设备状态上报前置字段

升级判断依赖 device/state 的以下 payload 字段。payload.version 固定表示当前 RootFS 版本。

| 字段  | 类型  | 必填  | 释义  |
| --- | --- | --- | --- |
| deviceModel | string | 是   | 设备型号编码；必须与固件元数据精确一致 |
| version | string | 是   | 当前 RootFS 版本；至少三段纯数字点分格式 |

### 下发公共信封字段

| 字段  | 类型  | 必填  | 释义  |
| --- | --- | --- | --- |
| protocolVersion | string | 是   | 固定为 2.2 |
| messageId | string | 是   | 平台生成的全局唯一指令 ID；用于幂等和回执关联 |
| deviceId | string | 是   | 目标设备 ID；必须与 Topic 中的设备 ID 一致 |
| sentAt | string | 是   | UTC ISO 8601 时间 |
| msgDir | string | 是   | 固定为 platform2dev |
| mainBizType | string | 是   | 固定为 device |
| subBizType | string | 是   | 固定为 firmwareDeploy |
| payload | object | 是   | OTA 业务参数 |
| extend | object | 是   | 扩展对象；无内容时传 {} |

### 下发 payload 字段

| 字段  | 类型  | 必填  | 释义  |
| --- | --- | --- | --- |
| requestId | string | 是   | 平台生成的 OTA 任务 UUID；所有进度回执保持不变 |
| deviceModel | string | 是   | 任务创建时捕获的设备型号 |
| target | string | 是   | 固定为 rootfs |
| version | string | 是   | 目标版本；必须与 OTA manifest 一致 |
| packageUrl | string | 是   | 平台 OTA 下载接口的短期 HTTP(S) 地址；不暴露对象存储签名 |
| urlExpiresAt | string | 是   | 下载地址失效时间，UTC ISO 8601 |
| fileName | string | 是   | OTA 包文件名 |
| fileSize | long | 是   | 文件字节数；不得超过 160 MiB |
| sha256 | string | 是   | 平台上传校验所得的 64 位小写十六进制 SHA-256 |
| rollbackEnable | boolean | 是   | 固定为 true |
| rebootAfter | boolean | 是   | 固定为 true |

### 完整下发示例

{  
    "protocolVersion":"2.2",  
    "messageId":"fw-550e8400-e29b-41d4-a716-446655440000",  
    "deviceId":"AETY-00-NJN2-WJUB-00000100",  
    "sentAt":"2026-08-17T10:30:00Z",  
    "msgDir":"platform2dev",  
    "mainBizType":"device",  
    "subBizType":"firmwareDeploy",  
    "payload":{  
        "requestId":"4f88e9a4-b20d-46cc-a866-d1b6d2d1e55a",  
        "deviceModel":"OORT-6600-2.5",  
        "target":"rootfs",  
        "version":"1.0.1.15",  
        "packageUrl":"http://192.168.88.31:8080/vlsDeviceFirmware/ota/4f88e9a4-b20d-46cc-a866-d1b6d2d1e55a/fw-550e8400-e29b-41d4-a716-446655440000",  
        "urlExpiresAt":"2026-08-17T12:30:00Z",  
        "fileName":"rootfs-1.0.1.15.ota",  
        "fileSize":22020396,  
        "sha256":"aaaaaaaaaaaaaaaaaaaa",  
        "rollbackEnable":true,  
        "rebootAfter":true  
    },  
    "extend":{  
<br/>    }  
}

### 设备回执 payload 字段

| 字段  | 类型  | 必填  | 释义  |
| --- | --- | --- | --- |
| sourceMsgId | string | 是   | 对应下发指令 messageId |
| code | int | 是   | 102 处理中；200 终态成功；400 参数错误；500 设备执行失败 |
| msg | string | 是   | 当前状态或错误摘要 |
| errCode | int | 是   | 业务错误码；无错误为 0 |
| errDetail | string | 是   | 错误详情；无错误传空字符串 |
| bizData | object | 是   | OTA 任务进度详情 |

### bizData 字段

| 字段  | 类型  | 必填  | 释义  |
| --- | --- | --- | --- |
| requestId | string | 是   | 对应下发 payload.requestId |
| status | string | 是   | 见 OTA 状态枚举；状态只能单向推进 |
| deviceModel | string | 是   | 设备实际校验的型号 |
| target | string | 是   | 固定为 rootfs |
| version | string | 是   | 本任务目标版本 |
| fileSha256 | string | SUCCESS 必填 | 设备实际下载文件的 SHA-256 |
| costMs | long | 否   | 从接收任务到当前回执的累计耗时，毫秒 |

### OTA 状态枚举

| 状态  | 终态  | 释义  |
| --- | --- | --- |
| ACCEPTED | 否   | 设备已完成信封和任务参数校验 |
| DOWNLOADING | 否   | 正在通过 packageUrl 下载 OTA 包 |
| VERIFYING | 否   | 正在校验文件 SHA-256 和包内 manifest |
| INSTALLING | 否   | 正在写入非活动 RootFS 槽位 |
| REBOOTING | 否   | 正在重启并执行启动健康检查 |
| SUCCESS | 是   | 升级完成且健康检查通过 |
| FAILED | 是   | 任务失败；msg/errCode/errDetail 必须说明原因 |

### 升级成功回执示例

{  
    **"protocolVersion"**:"2.2",  
    **"messageId"**:"up-fw-resp-1786963200-1",  
    **"deviceId"**:"AETY-00-NJN2-WJUB-00000100",  
    **"sentAt"**:"2026-08-17T10:42:00Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"firmwareDeploy",  
    **"payload"**:{  
        **"sourceMsgId"**:"fw-550e8400-e29b-41d4-a716-446655440000",  
        **"code"**:200,  
        **"msg"**:"固件升级完成并通过启动健康检查",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"requestId"**:"4f88e9a4-b20d-46cc-a866-d1b6d2d1e55a",  
            **"status"**:"SUCCESS",  
            **"deviceModel"**:"OORT-6600-2.5",  
            **"target"**:"rootfs",  
            **"version"**:"1.0.1.15",  
            **"fileSha256"**:"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",  
            **"costMs"**:46412  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

### 一致性与安全要求

平台以 requestId 关联任务，并同时校验 sourceMsgId、deviceId、target、version。SUCCESS 回执还必须校验 fileSha256；任一字段不匹配时不得把任务标记为成功。终态 SUCCESS/FAILED 不允许被后续乱序进度回执覆盖。

同一设备同时只允许一个非终态 RootFS OTA 任务。packageUrl 不要求平台登录令牌，其安全性由不可预测的 requestId 与 messageId 共同保证。平台下载接口必须校验两者与任务记录一致、任务未过期且固件为 READY，并在返回前核对对象大小与 SHA-256；校验通过后才从私有对象存储流式返回 OTA 包。设备应在 urlExpiresAt 前开始下载，不得记录或转发完整 URL。失败后由管理员重新发起新任务，不复用过期地址或旧 requestId。

# 五、IoT Center 类

**主要业务类型：**mainBizType=RS

## 5.1 RSGet

**子业务类型：**subBizType=RSGet

### 业务说明

GET 获取RS-485配置

### 请求参数

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
| **名称** | **位置** | **类型** | **必选** | **说明** |
| accessToken | header | string | 是   | none |

返回示例

200 Response

{  
    **"code"**:200,  
    **"message"**:"成功",  
    **"data"**:{  
        **"baud_rate"**:115200,  
        **"data_bit"**:16,  
        **"stop_bit"**:1,  
        **"check"**:"无",  
        **"flow_contrl"**:"无",  
        **"decoder_type"**:"PWMC-AE",  
        **"decoder_addr"**:"127.0.0.1"  
    }  
}

**返回结果**

|     |     |     |     |
| --- | --- | --- | --- |
| **状态码** | **状态码含义** | **说明** | **数据模型** |
| 200 | [OK](https://tools.ietf.org/html/rfc7231#section-6.3.1) | none | Inline |

**返回数据结构**

状态码 200

|     |     |     |     |     |     |
| --- | --- | --- | --- | --- | --- |
| **名称** | **类型** | **必选** | **约束** | **中文名** | **说明** |
| » code | integer | true | none |     | none |
| » message | string | true | none |     | none |
| » data | object | true | none |     | none |
| »» baud_rate | integer | true | none | 波特率 | none |
| »» data_bit | integer | true | none | 数据位 | none |
| »» stop_bit | integer | true | none | 停止位 | none |
| »» check | string | true | none | 校验位 | none |
| »» flow_contrl | string | true | none | 流控  | none |
| »» decoder_type | string | true | none | 解码器类型 | none |
| »» decoder_addr | string | true | none | 解码器地址 | none |

## 5.2 RSSave

**子业务类型：**subBizType=RSSave

### 业务说明

POST 保存RS-485配置

### 请求参数

{  
    **"baud_rate"**:115200,  
    **"data_bit"**:16,  
    **"stop_bit"**:1,  
    **"check"**:"无",  
    **"flow_contrl"**:"无",  
    **"decoder_type"**:"PWMC-AE",  
    **"decoder_addr"**:"127.0.0.1"  
}

**请求参数**

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
| **名称** | **位置** | **类型** | **必选** | **说明** |
| accessToken | header | string | 是   | none |
| body | body | object | 否   | none |

返回示例

200 Response

{  
    **"code"**:200,  
    **"message"**:"成功"  
}

**返回结果**

|     |     |     |     |
| --- | --- | --- | --- |
| **状态码** | **状态码含义** | **说明** | **数据模型** |
| 200 | [OK](https://tools.ietf.org/html/rfc7231#section-6.3.1) | none | Inline |

**返回数据结构**

状态码 200

|     |     |     |     |     |     |
| --- | --- | --- | --- | --- | --- |
| **名称** | **类型** | **必选** | **约束** | **中文名** | **说明** |
| » code | integer | true | none |     | none |
| » message | string | true | none |     | none |

## 5.3 电梯梯控

**子业务类型：**subBizType=tkControl

### 业务说明

闸机硬件电梯楼层控制指令下发。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| operator | string | 是   | 固定TKControl |
| info.value | string | 是   | 32 位十六进制梯控指令码 |

下发示例：

{  
 **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-tk-ctrl-11223344-55667788-9900aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:43:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"tkControl",  
    **"payload"**:{  
        **"operator"**:"TKControl",  
        **"info"**:{  
            **"value"**:"FF010000000000000000"  
        }  
    },  
    **"extend"**:{  
<br/>    }  
}

梯控回执示例

{  
  **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-tk-ctrl-22334455-66778899-0011aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:43:01Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"tkControl",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-tk-ctrl-11223344-55667788-9900aabbccdd",  
        **"code"**:200,  
        **"msg"**:"梯控指令下发成功",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"result"**:"ok"  
        }  
    },  
    **"extend"**:{  
<br/>    }}

## 5.4 液晶屏广告管理

**子业务类型：**subBizType=ad

### 业务说明

带屏人脸机广告图片增删、轮播时长配置。

### 下发 payload 字段

|     |     |     |     |
| --- | --- | --- | --- |
| **字段** | **类型** | **必填** | **释义** |
| libOperate | string | 是   | EditAD 新增修改 / DelAD 删除 |
| info.adslot | int | 是   | 广告槽位 0~4 |
| info.path | string | EditAD 必填 | 广告图片下载 URL |
| info.polltime | int | EditAD 必填 | 单张轮播时长 (秒) |

下发示例（新增广告）：

{  
 **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-ad-add-44556677-88990011-2233aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:44:30Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"ad",  
    **"payload"**:{  
        **"libOperate"**:"EditAD",  
        **"info"**:{  
            **"adslot"**:0,  
            **"path"**:"https://minio.test.com/ad/ad01.jpg",  
            **"polltime"**:10  
        }  
    },  
    **"extend"**:{  
<br/>    }}

广告操作回执示例

{  
   **"protocolVersion"**:"2.2",  
    **"messageId"**:"ack-ad-add-55667788-99001122-3344aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:44:31Z",  
    **"msgDir"**:"dev2platform",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"ad",  
    **"payload"**:{  
        **"sourceMsgId"**:"cmd-ad-add-44556677-88990011-2233aabbccdd",  
        **"code"**:200,  
        **"msg"**:"广告配置更新成功",  
        **"errCode"**:0,  
        **"errDetail"**:"",  
        **"bizData"**:{  
            **"adslot"**:0,  
            **"result"**:"ok"  
        }  
    },  
    **"extend"**:{  
<br/>    }

}

## 5.5 MP3 音频播放配置

**子业务类型：**subBizType=mp3Play

### 业务说明

设备本地音频文件、播放模式配置。

### 下发 payload 字段

| **字段** | **类型** | **必填** | **释义** |
| --- | --- | --- | --- |
| operate | string | 是   | Set 设置 / Get 查询 |
| config.enable | int | 是   | 0 关闭 / 1 启用 |
| config.mode | int | 是   | 0 顺序 / 1 随机 |
| config.mp3file | array | 否   | 音频列表 |
| mp3file.name | string |     | 音频名称 |
| mp3file.select | int |     | 0 不选 / 1 选中播放 |

下发示例：

{

&nbsp; **"protocolVersion"**:"2.2",  
    **"messageId"**:"cmd-mp3-set-77889900-11223344-5566aabbccdd",  
    **"deviceId"**:"CAM-20260001",  
    **"sentAt"**:"2026-07-24T09:46:00Z",  
    **"msgDir"**:"platform2dev",  
    **"mainBizType"**:"device",  
    **"subBizType"**:"mp3Play",  
    **"payload"**:{  
        **"operate"**:"Set",  
        **"config"**:{  
            **"enable"**:1,  
            **"mode"**:0,  
            **"mp3file"**:\[  
                {  
                    **"name"**:"欢迎语音.mp3",  
                    **"select"**:1  
                },  
                {  
                    **"name"**:"警报提示.mp3",  
                    **"select"**:0  
                }  
            \]  
        }  
    },  
    **"extend"**:{  
<br/>    }

}

# 六、全局可靠性 & 安全通用规范

实时设备控制补充约束：第 3.2 节 controlVersion=1 的运动指令按 expiresAt、holdMs、timeoutMs 或 durationMs 限时执行，不使用本章 5/15/30/60 秒退避重试、离线排队或重连补发。重复请求重发最近回执且不重复动作；停止优先及去重例外按第 3.2.7 节执行。此约束仅适用于新增控制规格，不改变其他业务的重试与缓存规则。

- - 1.  **幂等去重**：每条消息messageId 全局唯一，平台以deviceId+messageId 建立索引，重复消息直接丢弃；
        2.  **阶梯重试**：未收到对应 ack 自动 5/15/30/60s 退避，最长重试 5 分钟；人脸库批量任务延长至 10

分钟；

- - 1.  **离线缓存**：设备本地持久化 AI 抓拍、人脸库任务、媒体任务，网络恢复按时间顺序补发；
        2.  **二进制约束**：单条 JSON 最大 128KB，模型 / 录像 / 大图禁止 Base64 内嵌，统一 HTTP/MinIO 上传；
        3.  **三要素校验**：MQTT 登录账号、Topic 内 deviceId、消息体 deviceId 三者必须完全一致，不一致丢弃并记录安全日志；
        4.  **传输加密**：生产环境强制 TLS 8883，1883 明文仅内网测试；
        5.  **敏感脱敏**：身份证、密钥、临时上传 URL、人脸原图支持配置脱敏输出；