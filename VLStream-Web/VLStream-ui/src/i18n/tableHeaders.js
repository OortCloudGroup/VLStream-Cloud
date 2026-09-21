// English is the mandatory fallback for business table headers. Other locales
// override these values through catalog.js as their translations are reviewed.
const tableHeaderTsv = `
报警等级\tAlarm level
报警方式\tAlarm method
报警类型\tAlarm type
报警时间\tAlarm time
备注\tRemarks
编号\tNumber
变化\tChange
变化内容\tChange details
标签 / 类别\tTag / category
标签名称\tTag name
标注\tAnnotation
标注类型\tAnnotation type
标注名称\tAnnotation name
标注配置\tAnnotation configuration
标注状态\tAnnotation status
播放类型\tPlayback type
部门\tDepartment
部门名称\tDepartment name
部门全称\tFull department name
部署量\tDeployments
菜单别名\tMenu alias
菜单名称\tMenu name
参考费用\tEstimated cost
厂家\tManufacturer
场景描述\tScene description
场景名称\tScene name
超时（秒）\tTimeout (seconds)
处理人\tHandler
处理时间\tProcessing time
创建时间\tCreated at
大小\tSize
代理方式\tProxy mode
地址\tAddress
登录地点\tLogin location
登录时间\tLogin time
登录IP\tLogin IP
电话\tPhone
调用次数\tCalls
订阅\tSubscription
订阅信息\tSubscription details
端口\tPort
对象键（点击选择）\tObject key (click to select)
对应模型\tRelated model
对应训练任务名\tRelated training task
发起人\tInitiator
范围名称\tScope name
访问地址\tAccess URL
分析类型\tAnalysis type
分析名称\tAnalysis name
分析时间\tAnalysis time
分析状态\tAnalysis status
岗位\tPosition
岗位编码\tPosition code
岗位分类\tPosition category
岗位名称\tPosition name
告警类型\tAlarm type
告警位置\tAlarm location
更新时间\tUpdated at
工单编号\tWork order number
工单标题\tWork order title
工单来源\tWork order source
工单状态\tWork order status
固件版本号\tFirmware version
关联设备\tRelated devices
规则类型\tRule type
规则名称\tRule name
规则说明\tRule description
国标编码\tGB28181 code
耗时\tDuration
划分\tSplit
基础模型\tBase model
计算配置\tCompute configuration
角色\tRole
角色别名\tRole alias
角色名称\tRole name
接口地址\tAPI URL
接口路径\tAPI path
接收时间\tReceived at
节点名称\tNode name
节点ID\tNode ID
结果\tResult
结束时间\tEnded at
进度\tProgress
进度与结果\tProgress and result
镜像信息\tImage information
开启音频\tEnable audio
开始时间\tStarted at
客户端\tClient
快捷访问\tQuick access
快照\tSnapshot
拉流状态\tPull status
来源\tSource
来源名称\tSource name
类别\tCategory
类型\tType
类型标记\tType marker
类型名称\tType name
联系地址\tContact address
联系电话\tContact phone
联系人\tContact
流程版本\tWorkflow version
流传输模式\tStream transport mode
流地址\tStream URL
流媒体\tMedia server
流应用名\tStream app name
流ID\tStream ID
流id\tStream ID
路由编号\tRoute number
路由地址\tRoute URL
密码\tPassword
名称\tName
模型\tModel
模型大小\tModel size
模型分级\tModel tier
模型来源\tModel source
模型路径\tModel path
模型效果\tModel performance
模型ID\tModel ID
默认节点\tDefault node
昵称\tNickname
排序\tOrder
判断原因\tDecision reason
平台编号\tPlatform number
启用\tEnabled
权限标识\tPermission key
认证方式\tAuthentication method
任务\tTask
任务名称\tTask name
上报设备\tReporting device
上报图片\tReported image
上报位置\tReported location
上传时间\tUploaded at
上级分类\tParent category
设备\tDevice
设备 IP\tDevice IP
设备厂商\tDevice manufacturer
设备国标编号\tDevice GB28181 number
设备事件 ID\tDevice event ID
设备id\tDevice ID
摄像头\tCamera
摄像头名称\tCamera name
摄像头型号\tCamera model
审批结果\tApproval result
审批人\tApprover
审批时间\tApproval time
审批意见\tApproval comments
时长\tDuration
实例名称/ID\tInstance name / ID
视频流路径\tVideo stream path
是否启用\tEnabled
手机\tMobile
数据集\tDataset
数据集编号\tDataset number
数据集路径\tDataset path
数据集名称\tDataset name
数据集说明\tDataset description
算法 ID\tAlgorithm ID
算法类型\tAlgorithm type
算法名称\tAlgorithm name
所属分类\tCategory
提交时间\tSubmitted at
通道编号\tChannel number
通道号\tChannel number
通道类型\tChannel type
通道数\tChannels
头像\tAvatar
推流状态\tPublish status
位置\tLocation
位置信息\tLocation details
文件\tFile
文件 / 目录\tFile / directory
文件大小\tFile size
文件名称\tFile name
物理字段\tPhysical field
下载次数\tDownloads
下载量\tDownloads
协议版本\tProtocol version
信令传输模式\tSignaling transport mode
姓名\tName
性别\tGender
训练\tTraining
训练方式\tTraining method
训练集\tTraining set
训练任务\tTraining task
训练算法\tTraining algorithm
训练状态\tTraining status
验证\tValidation
验证集\tValidation set
样本\tSample
样本 ID\tSample ID
样本数\tSamples
意见/反馈\tComments / feedback
应用名\tApp name
拥有下级\tHas children
用户名\tUsername
邮箱\tEmail
域名地址\tDomain URL
云台类型\tPTZ type
账号\tAccount
真实姓名\tFull name
执行人\tAssignee
执行状态\tExecution status
置信度\tConfidence
抓拍截图\tSnapshot
抓图路径\tSnapshot path
抓图时间\tSnapshot time
准确率\tAccuracy
资源编号\tResource number
字段\tField
子节点数\tChild nodes
租户名称\tTenant name
租户ID\tTenant ID
最后登录\tLast login
最近更新\tLast updated
最近心跳\tLast heartbeat
最近一次录制时间\tLatest recording time
最近注册\tLast registered
IP地址\tIP address
RootFS版本\tRootFS version
SQL列\tSQL column
SDK播放\tSDK playback
修改位置\tEdit location
请输入设备ID\tEnter the device ID
请输入设备的 IP 地址\tEnter the device IP address
请输入设备名称\tEnter the device name
请输入流id\tEnter the stream ID
EasyNTS地址\tEasyNTS URL
请输入EasyNTS地址\tEnter the EasyNTS URL
海康isup不支持获取通道\tHikvision ISUP does not support channel discovery
请输入通道\tEnter the channel
请输入内容\tEnter content
播放视频\tPlay video
播放地址：\tPlayback URL:
聚焦\tFocus
flv播放\tFLV playback
实时视频\tLive video
flv地址\tFLV URL
rtcUrl地址\tRTC URL
wsUrl地址\tWebSocket URL
编码信息\tCodec information
云台控制\tPTZ control
修改地址\tEdit address
EHome 设备\tEHome devices
海康旧版主动接入设备 · 与 ISUP 5.0 分开管理\tLegacy Hikvision active-access devices, managed separately from ISUP 5.0
接入设备\tConnect device
注册服务就绪\tRegistration service ready
注册服务未就绪\tRegistration service unavailable
接入指南\tConnection guide
接入服务未启动\tConnection service is not running
设备列表\tDevice list
搜索名称、设备 ID、序列号或 IP\tSearch name, device ID, serial number, or IP
全部版本\tAll versions
刷新设备列表\tRefresh device list
未识别\tUnrecognized
通道 / 预览\tChannels / preview
设备列表加载失败\tCould not load device list
没有找到匹配的设备\tNo matching devices
等待第一台 EHome 设备接入\tWaiting for the first EHome device
请确认 WVP 服务可用后重试。\tConfirm that WVP is available and try again.
试试其他关键词或筛选条件。\tTry another keyword or filter.
在设备端启用 EHome，并填写上方服务器地址和注册端口。\tEnable EHome on the device and enter the server address and registration port shown above.
查看接入步骤\tView connection steps
清空筛选\tClear filters
ISUP 5.0 设备继续使用左侧 ISUP 协议入口。\tContinue to manage ISUP 5.0 devices from the ISUP entry on the left.
接入 EHome 设备\tConnect an EHome device
让设备主动连接平台\tLet the device connect to the platform
适用于海康 EHome 2.x / 3.x / 4.x，ISUP 5.0 使用独立入口。\tFor Hikvision EHome 2.x, 3.x, and 4.x. Use the separate entry for ISUP 5.0.
开启平台接入\tEnable platform access
进入设备的网络高级配置，选择 ISUP（原 EHome）并启用。\tOpen the device's advanced network settings, select ISUP (formerly EHome), and enable it.
填写服务器参数\tEnter server settings
服务器地址\tServer address
注册端口\tRegistration port
取流端口\tStreaming port
保存并等待上线\tSave and wait for the device
设备注册后会自动进入列表。选择通道和主 / 子码流即可预览。\tAfter registration, the device appears in the list. Select a channel and main or sub stream to preview it.
我知道了\tGot it
通道预览\tChannel preview
设备通道\tDevice channels
主码流\tMain stream
子码流\tSub stream
重新播放\tPlay again
开始预览\tStart preview
选择通道，开始预览\tSelect a channel to start preview
尚未选择通道\tNo channel selected
暂无可用地址\tNo available address
数据来源\tData sources
← 返回数据集\t← Back to datasets
历史数据集\tLegacy dataset
导入图片、视频与标注数据，管理数据来源和数据集版本。\tImport images, videos, and annotations, and manage data sources and dataset versions.
数据集设置\tDataset settings
进入标注\tOpen annotation
导入样本\tImport samples
新建数据集\tNew dataset
数据集名称或编号\tDataset name or number
暂无数据集，点击右上角新建数据集开始导入数据\tNo datasets. Create one in the upper-right corner to start importing data.
管理数据\tManage data
使用流程\tWorkflow
收起流程介绍\tHide workflow guide
展开流程介绍\tShow workflow guide
从原始素材到可用数据集，在数据集中完成数据导入和标注，再划分训练集与验证集，通过版本记录保留每次调整。\tTurn source media into usable datasets: import and annotate data, split training and validation sets, and preserve every change with versions.
创建数据集\tCreate dataset
填写数据集名称、编号和说明，选择标注类型，明确样本用途与标注规则。\tEnter the dataset name, number, and description; select an annotation type and define the sample purpose and annotation rules.
统一管理数据与标注配置\tManage data and annotation settings together
导入数据\tImport data
批量导入图片、视频或 ZIP，记录数据来源，检查文件格式和完整性。\tImport images, videos, or ZIP files in batches, record their source, and check file format and integrity.
形成可检索的原始样本\tCreate searchable source samples
数据标注\tAnnotate data
视频可按时间间隔切图并加入数据集；在标注页面维护类别，为图片添加标注。\tExtract video frames into the dataset at intervals; manage categories and annotate images on the annotation page.
形成带类别与标注的图片样本\tCreate image samples with categories and annotations
数据集与版本\tDatasets and versions
按比例或手动划分训练集和验证集，查看类别分布，保存、对比和回退版本。\tSplit training and validation sets by ratio or manually, inspect category distribution, and save, compare, or restore versions.
查看数据集与版本\tView datasets and versions
在「样本管理」中导入和查看素材，在「数据集划分与版本」中管理数据集版本。\tImport and review media under Sample management, and manage versions under Dataset split and versions.
开始使用：新建数据集，或点击下方数据集的「管理数据」，按流程完成数据准备。\tGet started by creating a dataset or selecting Manage data below, then follow the workflow to prepare the data.
零代码训练介绍\tNo-code training guide
选择已生成的数据集，使用默认配置或自定义训练轮数、批大小和输入尺寸。启动前检查图片、标注及训练/验证集独立性；训练完成后可自动保存到算法模型，实际效果仍需独立样本验证。\tSelect a generated dataset and use the default configuration or customize epochs, batch size, and input size. Before starting, verify images, annotations, and independent training and validation sets. Completed runs can be saved automatically as algorithm models, but results still require validation with independent samples.
1.发起训练\t1. Start training
选择训练方式并发起成功训练参数配置\tChoose a training method and configure the training parameters.
2.选择标注集\t2. Select dataset
选择已生成并独立划分训练集和验证集的数据集\tSelect a generated dataset with independent training and validation sets.
3.评估校验\t3. Evaluate
算法训练完成后，通过评估报告，校验验证性能效果\tAfter training, use the evaluation report to validate model performance.
4.发布为模型\t4. Publish model
效果满足业务需求的任务可发布为模型，进行部署操作\tPublish runs that meet business requirements as models for deployment.
精确度:\tPrecision:
召回率:\tRecall:
完整评估结果\tFull evaluation results
训练完成\tTraining completed
停止\tStopped
功能未启用\tFeature not enabled
过期时间\tExpiration time
激活码只在本次显示。请通过安全渠道交给设备端，设备注册成功后立即失效。\tThe activation code is shown only once. Send it to the device through a secure channel; it expires immediately after registration.
尚未激活\tNot activated
隧道未连接\tTunnel not connected
一次性激活码\tOne-time activation code
已吊销\tRevoked
已停用\tDisabled
暂不可用\tTemporarily unavailable
Agent 离线\tAgent offline
IPC 后台不可用\tIPC administration unavailable
IPC Agent 激活信息\tIPC Agent activation information
2021年04月15日\tApril 15, 2021
白炽灯\tIncandescent
白平衡\tWhite balance
白色\tWhite
白天\tDay
半自动\tSemi-automatic
饱和度\tSaturation
背光\tBacklight
背光补偿\tBacklight compensation
变倍限制\tZoom limit
不透明，不闪烁\tOpaque, not flashing
不透明，闪烁\tOpaque, flashing
垂直镜像\tVertical mirror
大号\tLarge
低照度电子快门\tLow-light electronic shutter
电子防抖\tElectronic image stabilization
定时\tScheduled
对比度\tContrast
对齐方式\tAlignment
防补光过曝\tPrevent fill-light overexposure
高级模式\tAdvanced mode
光圈优先\tAperture priority
黑白自动\tAuto monochrome
黑色\tBlack
红色\tRed
红外灯模式\tIR light mode
恢复默认设置\tRestore defaults
降噪等级\tNoise reduction level
镜头初始化\tLens initialization
镜像\tMirror
居中\tCenter
聚焦模式\tFocus mode
开启\tEnable
快门优先\tShutter priority
宽动态\tWide dynamic range
蓝色\tBlue
亮度\tBrightness
亮度限制\tBrightness limit
灵敏度\tSensitivity
绿色\tGreen
普通模式\tNormal mode
曝光\tExposure
曝光模式\tExposure mode
其他\tOther
强光抑制\tHighlight compensation
请选择场景\tSelect a scene
日光\tDaylight
日夜转换\tDay/night switching
锐度\tSharpness
视频调整\tVideo adjustment
视频制式\tVideo standard
手动\tManual
手动白平衡\tManual white balance
数字降噪\tDigital noise reduction
水平垂直镜像\tHorizontal and vertical mirror
水平镜像\tHorizontal mirror
透明，不闪烁\tTransparent, not flashing
透明，闪烁\tTransparent, flashing
透雾模式\tDefog mode
图像调节\tImage adjustment
图像增强\tImage enhancement
无限远\tInfinity
无限制\tUnlimited
显示设置\tDisplay settings
小号\tSmall
夜间\tNight
一次白平衡\tOne-push white balance
阴天\tCloudy
荧光灯\tFluorescent
右对齐\tAlign right
增益限制\tGain limit
智能\tSmart
中号\tMedium
自动\tAutomatic
自动白平衡\tAutomatic white balance
自适应\tAdaptive
最大快门限制\tMaximum shutter limit
最小聚焦距离\tMinimum focus distance
最小快门限制\tMinimum shutter limit
左对齐\tAlign left
OSD设置\tOSD settings
OSD属性\tOSD properties
OSD颜色\tOSD color
OSD字体\tOSD font
报警输出\tAlarm output
本地模型\tLocal model
布防日期\tArming date
布防时间\tArming time
布防时间段\tArming period
常规\tStandard
常规联动\tStandard linkage
车牌识别\tLicense plate recognition
关闭布防\tDisable arming
检测类型\tDetection type
检测名称\tDetection name
离开区域侦测\tRegion exit detection
联动方式\tLinkage method
录像联动\tRecording linkage
模型超市\tModel marketplace
请输入检测名称\tEnter a detection name
请选择检测类型\tSelect a detection type
区域入侵侦测\tRegion intrusion detection
全天布防\tArm all day
上传\tUpload
声强降降\tDecrease sound threshold
声强增升\tIncrease sound threshold
声音强度阈值\tSound intensity threshold
时间段预览\tPeriod preview
实时音量\tLive volume
添加检测项目\tAdd detection item
添加时间段\tAdd period
物品拿取侦测\tObject removal detection
物品遗留侦测\tUnattended object detection
异常检测\tAnomaly detection
音频输入异常\tAudio input anomaly
邮件\tEmail
越界侦测\tLine crossing detection
至\tto
自定义时间\tCustom time
最多可添加4个时间段\tUp to four periods can be added
半球\tDome
地面\tGround
地图选点\tSelect on map
地下\tUnderground
点击地图任意位置选择坐标，或使用GPS定位到当前位置\tClick the map to select coordinates, or use GPS to locate the current position
高度位置\tHeight position
高空\tElevated
更多信息\tMore information
基本信息\tBasic information
经度: 116.39139 或 116°23′29″E\tLongitude: 116.39139 or 116°23′29″E
经纬度坐标\tCoordinates
枪机\tBullet camera
请输入备注信息\tEnter remarks
请输入视频流路径\tEnter the video stream path
请选择设备标签\tSelect device tags
球机\tPTZ camera
区划选择\tAdministrative area selection
设备标签\tDevice tags
输入设备ID\tEnter the device ID
输入图片路径\tEnter the image path
输入详细地址：省/市/区（县）/街道（村）\tEnter the full address: province/city/district/county/street/village
图片路径\tImage path
纬度: 39.90917 或 39°54′26″N\tLatitude: 39.90917 or 39°54′26″N
详细地址\tFull address
选择区划\tSelect administrative area
选择设备类型\tSelect device type
自动生成名称\tGenerate name automatically
当前布局:\tCurrent layout:
调试信息\tDebug information
二十二画面\t22 views
二十四画面\t24 views
二十五画面\t25 views
放大\tZoom in
复制地址\tCopy URL
切换地图视图\tToggle map view
刷新摄像头位置\tRefresh camera locations
缩小\tZoom out
显示全部摄像头\tShow all cameras
有效设备数量:\tAvailable devices:
原始设备数量:\tTotal source devices:
暂无视频流\tNo video stream
重试连接\tRetry connection
状态: 在线\tStatus: online
最小化\tMinimize
API状态:\tAPI status:
WebRTC状态:\tWebRTC status:
布局模式:\tLayout mode:
调试信息面板\tDebug panel
端口:\tPort:
活动\tActive
活动流:\tActive streams:
加载状态:\tLoading status:
内存使用:\tMemory usage:
设备总数:\tTotal devices:
网络延迟:\tNetwork latency:
系统状态\tSystem status
协议:\tProtocol:
性能监控\tPerformance monitoring
暂无活动流\tNo active streams
帧率:\tFrame rate:
状态:\tStatus:
CPU使用:\tCPU usage:
WebRTC 配置\tWebRTC configuration
2x2模式\t2x2 mode
3x3模式\t3x3 mode
4x4模式\t4x4 mode
5x5模式\t5x5 mode
6x6模式\t6x6 mode
打开视频弹窗\tOpen video dialog
单屏模式\tSingle-view mode
更多布局选项\tMore layout options
离线设备:\tOffline devices:
视频弹窗\tVideo dialog
在线设备:\tOnline devices:
自定义布局\tCustom layout
自定义视频布局\tCustom video layout
总设备:\tTotal devices:
查看文件\tView files
当前目录没有文件\tNo files in this directory
浏览模型和文件目录无需登录，下载、下发时需登录 OortCloud。\tBrowsing models and files does not require sign-in. Downloading or deploying requires OortCloud sign-in.
全部分类\tAll categories
上一级\tParent directory
尚缺少设备兼容格式、类别文件及校验信息\tDevice-compatible format, category file, and verification data are still missing
尚未建立模型文件空间\tModel file storage has not been created
搜索 Model Hub 模型\tSearch Model Hub models
下发\tDeploy
重试\tRetry
重新登录\tSign in again
复制时间设置\tCopy time settings
录制设置\tRecording settings
每天\tDaily
每天录制时间\tDaily recording time
每月\tMonthly
每周\tWeekly
每周录制时间\tWeekly recording time
删除全部\tDelete all
时间策略\tTime policy
事件策略\tEvent policy
不支持的视频格式或地址无效\tUnsupported video format or invalid URL
流地址：\tStream URL:
您的浏览器不支持视频播放\tYour browser does not support video playback
使用VLC播放\tPlay with VLC
无法播放视频\tUnable to play video
RTSP流播放\tRTSP playback
RTSP流需要专用播放器打开\tRTSP streams require a dedicated player
列表\tList
列表视图\tList view
设备统计\tDevice statistics
设置\tSettings
树形\tTree
树形视图\tTree view
搜索设备...\tSearch devices...
模型分类\tModel categories
搜索模型或算法名称\tSearch model or algorithm name
选择算法模型\tSelect algorithm models
重新加载\tReload
自主训练模型列表\tSelf-trained model list
复制流地址\tCopy stream URL
人脸识别类\tFace recognition
人员检测类\tPerson detection
视频分析类\tVideo analytics
已选取模型\tSelected models
暂无选择模型\tNo models selected
扩展布局选项\tExtended layout options
显示离线设备\tShow offline devices
选择布局模式\tSelect layout mode
未知\tUnknown
设备树\tDevice tree
4核\t4 cores
奥尔特云\tOortCloud
奥尔特云 64G*1\tOortCloud 64G × 1
编辑实例\tEdit instance
磁盘配置\tDisk configuration
错误信息\tError details
当前\tCurrent
返回列表\tBack to list
基础镜像\tBase image
计算资源\tCompute resources
监控\tMonitoring
镜像地址\tImage URL
内存\tMemory
内存限制\tMemory limit
平均\tAverage
请输入\tEnter a value
请选择\tSelect
实例名称\tInstance name
实例数量\tNumber of instances
实例ID\tInstance ID
数据盘\tData disk
数据盘：——\tData disk: —
系统盘：40GB\tSystem disk: 40 GB
新增容器实例\tAdd container instance
训练任务ID\tTraining task ID
应用镜像\tApplication image
资源规格：\tResource specification:
资源类型\tResource type
自定义镜像\tCustom image
最大\tMaximum
CPU限制\tCPU limit
CPU型号\tCPU model
GPU限制\tGPU limit
GPU用量\tGPU usage
Intel 4核 | 40G\tIntel 4 cores | 40 GB
编辑用户信息\tEdit user information
查看和管理您的个人信息\tView and manage your personal information
登录类型\tSign-in type
登录历史\tSign-in history
登录账号\tSign-in account
访问令牌\tAccess token
客户端类型\tClient type
令牌过期时间\tToken expiration time
统一用户中心\tUnified user center
系统创建\tCreated by system
用户池创建\tCreated from user pool
用户来源\tUser source
用户姓名\tUser name
组织创建\tCreated by organization
最后登录时间\tLast sign-in time
最后登录IP\tLast sign-in IP
B/E端用户\tBusiness/enterprise user
C端用户\tConsumer user
当前预设:\tCurrent preset:
方向控制\tDirection control
高级控制\tAdvanced control
回中\tCenter
开始扫描\tStart scan
控制模式:\tControl mode:
快速\tFast
连接状态:\tConnection status:
慢速\tSlow
清除预设\tClear preset
扫描模式\tScan mode
设置预设\tSet preset
速度控制\tSpeed control
缩放控制\tZoom control
停止扫描\tStop scan
向上\tUp
向下\tDown
向右\tRight
向左\tLeft
预设位置\tPreset position
正常\tNormal
状态信息\tStatus information
自动巡航\tAuto cruise
PTZ控制\tPTZ control
变倍\tZoom
菜单\tMenu
灯光\tLight
调焦-\tFocus −
调焦+\tFocus +
辅助聚集\tAuxiliary focus
光圈-\tIris −
光圈+\tIris +
光学变倍控制\tOptical zoom control
聚集-\tFocus −
聚集+\tFocus +
开启3D定位\tEnable 3D positioning
开启手动跟踪\tEnable manual tracking
上\tUp
摄像机管理后台\tCamera administration
下\tDown
旋转\tRotate
一键守望\tOne-touch guard
一键巡航\tOne-touch cruise
右\tRight
左\tLeft
八分屏\t8-view layout
八分屏视频播放\t8-view video playback
单画面视频播放\tSingle-view video playback
九分屏\t9-view layout
九分屏视频播放\t9-view video playback
离线：\tOffline:
六分屏\t6-view layout
六分屏视频播放\t6-view video playback
十六分屏\t16-view layout
十六分屏视频播放\t16-view video playback
四分屏视频播放\t4-view video playback
在线：\tOnline:
自定义视频播放\tCustom video playback
总设备\tTotal devices
总设备：\tTotal devices:
12小时制\t12-hour format
24小时制\t24-hour format
保存OSD设置\tSave OSD settings
请输入通道名称\tEnter the channel name
日期格式\tDate format
时间格式\tTime format
通道名称\tChannel name
显示名称\tShow name
显示日期\tShow date
显示星期\tShow weekday
字符叠加\tText overlay
XXXX年XX月XX日\tYYYY-MM-DD
附件可打印者\tUsers allowed to print attachments
附件可拷贝者\tUsers allowed to copy attachments
附件可下载者\tUsers allowed to download attachments
可编辑者\tEditors
可阅读者\tReaders
所有人不可打印\tNobody can print
所有人不可拷贝\tNobody can copy
所有人不可下载\tNobody can download
所有人可打印\tEveryone can print
所有人可拷贝\tEveryone can copy
所有人可下载\tEveryone can download
为空则只有管理员可编辑\tIf empty, only administrators can edit
为空则只有作者和相关人可见\tIf empty, only the author and related users can view
指定人可打印\tSpecified users can print
指定人可拷贝\tSpecified users can copy
指定人可下载\tSpecified users can download
发起人自己\tInitiator
发起人自选\tSelected by initiator
前节点指定\tSpecified by previous node
请选择岗位\tSelect a position
请选择角色\tSelect a role
请选择职位\tSelect a job title
上级\tSupervisor
提交人的岗位：\tSubmitter position:
提交人的职位：\tSubmitter job title:
系统自动拒绝\tAutomatically reject
选择部门\tSelect department
选择人员\tSelect users
指定部门\tSpecified department
指定角色\tSpecified role
指定人员\tSpecified users
触发时机\tTrigger timing
发送网络请求\tSend web request
发送消息\tSend message
监听器类型\tListener type
请输入URL地址\tEnter a URL
请选择触发时机\tSelect trigger timing
在任务被分配给某个办理人之后触发（委派，转办）\tTrigger after a task is assigned to a handler (delegate or transfer)
在任务即将被删除前触发。（通过，拒绝，退回）\tTrigger before a task is removed (approve, reject, or return)
在任务开始时触发\tTrigger when the task starts
在任务完成时触发（通过）\tTrigger when the task completes (approved)
设置通知对象\tSet notification recipients
表单权限设置\tForm permission settings
节点表单\tNode form
请输入节点名称\tEnter a node name
请选择表单\tSelect a form
请在发起人节点设置表单\tSet the form on the initiator node
设置抄送人\tSet CC recipients
设置节点表单\tSet node form
选择抄送人\tSelect CC recipients
允许发起人添加抄送人\tAllow the initiator to add CC recipients
报单时间\tSubmission time
工单类型\tWork order type
工单信息\tWork order information
建单人\tCreator
紧急\tUrgent
紧急程度\tUrgency
所属项目\tProject
严重\tCritical
一般\tNormal
%人员通过\t% of users approve
分钟\tMinutes
需要\tRequired
依次处置（按顺序处置）\tProcess sequentially
依次审批（按顺序审批）\tApprove sequentially
表单权限\tForm permissions
表单设置\tForm settings
节点设置\tNode settings
设置流程表单\tSet workflow form
&nbsp;已选择此通知方式。若此方式发送失败，系统将自动按优先级[逐步降级]尝试，直到通知成功为止\tThis notification method is selected. If delivery fails, the system automatically tries lower-priority methods until successful.
通知节点\tNotification node
通知期限（为0则不生效）\tNotification deadline (0 disables it)
通知说明文字\tNotification description
通知优先级\tNotification priority
固定时长\tFixed duration
请输入时长\tEnter a duration
延时方式\tDelay method
自动计算(日期)\tCalculate automatically (date)
选择触发器动作\tSelect trigger action
转交指定人员\tTransfer to specified users
自动驳回\tAutomatically reject
自动通过\tAutomatically approve
请输入备注\tEnter remarks
请输入组件名称\tEnter a component name
新增自定义组件\tAdd custom component
组件名称\tComponent name
优先级\tPriority
其他设置\tOther settings
站内消息推送\tIn-app notification
完成\tComplete
添加条件\tAdd condition
开始\tStart
只读\tRead-only
禁用\tDisabled
表单字段\tForm field
不自动周期重置\tNo automatic periodic reset
初始数值\tInitial value
固定字符\tFixed text
每年重置\tReset yearly
每日重置\tReset daily
每月重置\tReset monthly
每周重置\tReset weekly
请选择规则类型\tSelect a rule type
提交日期\tSubmission date
添加规则\tAdd rule
重置周期\tReset cycle
默认值类型\tDefault value type
请选择范围\tSelect a range
请选择模式\tSelect a mode
请选择默认值类型\tSelect a default value type
选择范围\tSelect range
选择固定值\tSelect fixed value
选择模式\tSelect mode
自定义人员或部门\tCustom users or departments
保存配置\tSave configuration
测试连接\tTest connection
密码认证\tPassword authentication
密钥认证\tKey authentication
请输入服务器IP或域名\tEnter the server IP or domain
请粘贴私钥内容或选择私钥文件\tPaste the private key or select a private-key file
私钥文件\tPrivate-key file
百分比\tPercentage
货币\tCurrency
数据格式\tData format
数值类型\tNumber type
小数\tDecimal
小数位数\tDecimal places
链接地址\tLink URL
链接设置\tLink settings
链接文字\tLink text
链接文字,不填为链接地址\tLink text; leave empty to use the URL
默认\tDefault
显示类型\tDisplay type
规则设置\tRule settings
添加/编辑规则\tAdd/edit rule
序列号规则\tSerial-number rules
自动计数\tAutomatic counter
模型标识\tModel identifier
模型图标\tModel icon
请输入模型标识\tEnter a model identifier
请输入模型名称\tEnter a model name
请先输入模型名称\tEnter the model name first
移动端显示\tShow on mobile
表单类型\tForm type
表单名称\tForm name
流式布局\tFlow layout
签批卡片式布局\tApproval-card layout
请输入表单名称\tEnter a form name
请先输入表单名称\tEnter the form name first
待处理\tPending
上报图像\tReported image
上传图像\tUpload image
事件列表\tEvent list
事件状态\tEvent status
事件ID\tEvent ID
关键词\tKeyword
类型描述\tType description
请输入关键词\tEnter a keyword
生成\tGenerate
字数限制\tCharacter limit
布局信息\tLayout information
服务器地址:\tServer URL:
服务状态:\tService status:
启用状态:\tEnabled status:
WebRTC配置\tWebRTC configuration
+ 添加标签\t+ Add tag
设备列表 > 编辑设备\tDevice list > Edit device
输入经度\tEnter longitude
输入类型：枪机、球机\tEnter type: bullet or PTZ camera
输入纬度\tEnter latitude
保存为组件\tSave as component
查看json\tView JSON
导出Vue文件\tExport Vue file
复制代码\tCopy code
清空\tClear
查看和管理你的云平台信息\tView and manage your cloud platform information
请先登录后查看已上传模型\tSign in to view uploaded models
我上传的模型\tMy uploaded models
用户信息\tUser information
正在准备登录...\tPreparing sign-in...
事件数据\tEvent data
高级搜索\tAdvanced search
批量操作\tBatch actions
下载模板\tDownload template
折叠按钮\tCollapse button
我的常用\tMy favorites
用户池搜索\tSearch user pool
组织架构\tOrganization
最近使用\tRecently used
保 存\tSave
查询类型\tQuery type
查询添加源码\tQuery source code
模拟运行\tSimulate
年\tYear
日期时间\tDate and time
月\tMonth
周\tWeek
发布到 Model Hub\tPublish to Model Hub
算法评估\tAlgorithm evaluation
添加\tAdd
下发到摄像机\tDeploy to camera
搜索分类\tSearch categories
算法分类\tAlgorithm categories
算法库\tAlgorithm library
所有分类\tAll categories
点击页面顶部“登录 OortCloud”，系统会自动选择账户中第一个启用的 API 令牌完成授权。\tClick Sign in to OortCloud at the top. The system automatically uses the first enabled API token in the account.
内置 OortCloud 开箱即用，也可配置其他 OpenAI 兼容中转站。\tBuilt-in OortCloud works out of the box; other OpenAI-compatible gateways can also be configured.
平台登录令牌过期后，已保存的模型授权仍可供后台复核使用。\tSaved model authorization remains available for background review after the platform sign-in token expires.
新增外部大模型\tAdd external LLM
反馈人员\tFeedback user
反馈位置\tFeedback location
图片/视频\tImage/video
养护详情\tMaintenance details
固件版本\tFirmware version
例如 1.0.1.14\tFor example, 1.0.1.14
上传固件\tUpload firmware
输入型号编码\tEnter model code
基础设置\tBasic settings
基础信息\tBasic information
系统基础配置\tBasic system configuration
系统配置与参数管理\tSystem configuration and parameter management
系统设置\tSystem settings
高级搜索组件测试\tAdvanced search component test
搜索结果\tSearch results
组件展示\tComponent preview
标签栏\tLabel panel
标注框数\tAnnotation boxes
添加标签\tAdd label
，仅用于说明任务区别。\t, for illustrating task differences only.
真实图像示例来自\tReal image examples from
Ultralytics 官方文档\tUltralytics official documentation
列表结构\tList structure
树形结构\tTree structure
展开/收起设备统计详情\tExpand/collapse device statistics
搜索设备\tSearch devices
新增同级\tAdd sibling
新增下级\tAdd child
个人\tPrivate
公开\tPublic
全选\tSelect all
清空联系人\tClear contacts
暂未选择\tNothing selected
当前组织\tCurrent organization
去认证\tVerify now
已认证\tVerified
服务目录\tService catalog
模型类型\tModel type
模型详情\tModel details
编辑模型\tEdit model
上传模型\tUpload model
暂无上传的模型\tNo uploaded models
复制\tCopy
配置和管理AI算法的组合编排\tConfigure and manage AI algorithm orchestration
新建编排\tNew orchestration
查看 YOLO 事件的二次判断、失败原因与人工复核记录。\tReview secondary decisions, failure reasons, and manual review records for YOLO events.
复核状态\tReview status
请输入算法 ID\tEnter an algorithm ID
场景列表\tScene list
请输入场景名称\tEnter a scene name
请选择分析类型\tSelect an analysis type
, 或联系管理员在\t, or contact an administrator to configure it in
更多自动派单\tMore automatic dispatch rules
统一工单中台配置\tUnified work-order center
处置记录\tProcessing history
流程图\tWorkflow diagram
权限\tPermissions
表单设计\tForm design
流程名称\tWorkflow name
流程设计\tWorkflow design
发现设备\tDiscover devices
搜索名称\tSearch name
ONVIF协议 16的设备可以使用Digest/WS,2.20版本使用WS\tONVIF Profile 16 devices can use Digest/WS; version 2.20 uses WS
正在连接 RTSP 流...\tConnecting to RTSP stream...
重新连接\tReconnect
断开连接\tDisconnect
输入命令...\tEnter a command...
请输入定位中点\tEnter the map center
设置定位中点\tSet map center
请输入数据类标识\tEnter a data-class identifier
数据类标识\tData-class identifier
请选择扫码类型\tSelect a scan type
扫码类型\tScan type
直接拖拽视频窗口可交换位置\tDrag video windows to swap positions
VLC播放\tVLC playback
&nbsp;基础信息\tBasic information
男\tMale
女\tFemale
工单办理\tProcess work order
巡查详情\tInspection details
菜单权限\tMenu permissions
权限配置\tPermission configuration
请输入租户名称\tEnter a tenant name
请输入租户ID\tEnter a tenant ID
搜索用户名或邮箱\tSearch username or email
系统用户账户管理与权限配置\tManage system user accounts and permissions
暂无可播放地址\tNo playable URL
请输入地址\tEnter an address
显隐列\tShow/hide columns
请选择标签\tSelect tags
请选择基础数据或者基础数据为空\tSelect base data or leave it empty
地区精度\tRegion precision
公式配置\tFormula configuration
地址本设置\tAddress-book settings
基础数据\tBase data
按钮文字\tButton text
计算公式\tCalculation formula
YYYY年MM月DD日\tYYYY-MM-DD
HH时mm分ss秒\tHH:mm:ss
字体粗细\tFont weight
组件高度\tComponent height
选择表单\tSelect form
关联查询\tRelated query
货币类型\tCurrency type
请输入上传地址，默认为系统文件上传地址\tEnter an upload URL, or leave empty to use the system default
打开 OortCloud\tOpen OortCloud
生成国标编码\tGenerate GB28181 code
选择行政区划\tSelect administrative area
选择虚拟组织\tSelect virtual organization
运 行\tRun
申请\tApply
图片预览\tImage preview
自动派单\tAutomatic dispatch
搜索摄像头名称\tSearch camera name
授权\tAuthorize
本地导入\tLocal import
标注格式\tAnnotation format
导入方式\tImport method
公开文件链接\tPublic file URL
恢复上传时重新选择同一原文件即可\tSelect the same source file to resume the upload
开始 / 继续上传\tStart / resume upload
数据标注状态\tAnnotation status
拖入文件或点击选择文件\tDrop files here or click to select
无标注信息\tNo annotation data
选择本地文件夹\tSelect local folder
有标注信息\tContains annotation data
暂停上传\tPause upload
支持 JPG/JPEG、PNG、BMP、MP4、MOV、ZIP。图片保持 4000 万像素上限。ZIP 支持 ZIP64，默认解压容量 32 GiB、最多 10000 个样本；文件上传成功后仍需完成格式和标注校验。\tSupports JPG/JPEG, PNG, BMP, MP4, MOV, and ZIP. Images are limited to 40 megapixels. ZIP64 is supported, with a default extracted size limit of 32 GiB and up to 10,000 samples. Uploaded files still require format and annotation validation.
支持单文件 ≤4 GiB 断点续传；更大的文件会继续尝试，受存储容量和文件内容限制。\tSupports resumable uploads for individual files up to 4 GiB. Larger files are attempted subject to storage capacity and file contents.
S3 对象存储\tS3 object storage
VLS 样本归档（vls-samples.json）\tVLS sample archive (vls-samples.json)
YOLO 检测框 ZIP\tYOLO bounding-box ZIP
从开始时间起按间隔取帧，结束时间不包含在内；时间范围超过视频时长时取到视频结尾。相同图片会跳过重复入库。\tExtract frames at intervals from the start time, excluding the end time. If the range exceeds the video duration, extraction stops at the end. Duplicate images are skipped.
后台处理，任务进度可在“导入记录”查看，关闭页面不会中断已提交的切图任务。\tProcessing runs in the background. Track progress under Import history; closing the page does not interrupt submitted frame-extraction tasks.
结束时间（秒）\tEnd time (seconds)
开始切图\tStart extraction
开始时间（秒）\tStart time (seconds)
留空直到视频结尾\tLeave empty to use the end of the video
切图间隔（秒）\tFrame interval (seconds)
视频切图\tExtract video frames
图片生成后自动加入当前数据集，可继续标注；原视频保留。\tGenerated images are added to the current dataset for annotation; the source video is retained.
张\timages
最多生成\tMaximum images
磁盘使用\tDisk usage
当前目录\tCurrent directory
列出文件\tList files
内存使用\tMemory usage
配置SSH连接信息，连接到远程服务器进行算法训练\tConfigure SSH credentials to connect to a remote server for algorithm training
请先配置SSH连接信息\tConfigure SSH connection information first
输入自定义命令\tEnter a custom command
执行\tRun
GPU状态\tGPU status
SSH远程连接\tSSH remote connection
标签名\tLabel name
导入图片\tImport images
加载更多\tLoad more
请输入标签名称\tEnter a label name
颜色\tColor
预览\tPreview
等待中\tPending
管理AI算法的训练任务和模型发布\tManage AI algorithm training tasks and model publishing
搜索训练任务\tSearch training tasks
新建训练任务\tNew training task
训练中\tTraining
已发布模型\tPublished models
发布为模型\tPublish as model
发起训练\tStart training
评估校验\tEvaluate and validate
新建\tNew
选择标注集\tSelect dataset
请输入任务名称\tEnter a task name
请选择状态\tSelect a status
配置外部 S3 对象存储或公开文件下载地址，导入的数据统一保存到当前存储中。\tConfigure external S3 object storage or public download URLs. Imported data is saved to the current storage.
搜索来源名称\tSearch source name
添加数据来源\tAdd data source
暂无数据来源，可添加外部 S3 或公开数据集链接\tNo data sources. Add an external S3 source or public dataset link.
请输入标注名称\tEnter an annotation name
请输入数据集路径\tEnter the dataset path
新增标注\tAdd annotation
查看\tView
下载\tDownload
查看与标注\tView and annotate
标签颜色\tLabel color
导入记录\tImport history
暂无导入任务\tNo import tasks
来源信息暂时无法加载\tSource information is temporarily unavailable
测 试\tTest
单端口\tSingle port
多端口\tMultiple ports
流IP\tStream IP
录像管理服务端口\tRecording service port
密钥\tSecret key
起始\tStart
请输入服务器绑定的 IP 地址\tEnter the server bind IP address
请输入媒体服务流IP\tEnter the media server stream IP
请输入媒体服务HOOK_IP\tEnter the media server HOOK_IP
请输入媒体服务HTTPS_PORT\tEnter the media server HTTPS_PORT
请输入媒体服务RTMP_PORT\tEnter the media server RTMP_PORT
请输入媒体服务RTMPS_PORT\tEnter the media server RTMPS_PORT
请输入媒体服务RTSP_PORT\tEnter the media server RTSP_PORT
请输入媒体服务RTSPS_PORT\tEnter the media server RTSPS_PORT
请输入媒体服务SDP_IP\tEnter the media server SDP_IP
请输入密钥\tEnter the secret key
请输入HTTP 协议端口\tEnter the HTTP port
取 消\tCancel
收流端口\tStream receiving port
收流端口模式\tStream receiving port mode
提 交\tSubmit
终止\tEnd
自动配置媒体服务\tConfigure media server automatically
开始巡航\tStart cruise
请选择预置点\tSelect a preset
删除巡航\tDelete cruise
设置巡航时间\tSet cruise duration
设置巡航速度\tSet cruise speed
添加巡航点\tAdd cruise point
停止巡航\tStop cruise
巡航速度\tCruise speed
巡航停留时间(秒)\tCruise dwell time (seconds)
巡航组号\tCruise group number
巡航组号:\tCruise group number:
编码\tCodec
采样率\tSample rate
持续时间\tDuration
丢包率\tPacket loss
分辨率\tResolution
概况\tOverview
观看人数\tViewers
视频信息\tVideo information
网络\tNetwork
音频信息\tAudio information
开始自动扫描\tStart auto scan
扫描组号\tScan group number
扫描组号:\tScan group number:
设置扫描速度\tSet scan speed
设置右边界\tSet right boundary
设置左边界\tSet left boundary
停止自动扫描\tStop auto scan
拉流代理\tStream proxy
请输入关键字\tEnter a keyword
推流设备\tPublishing device
未关联\tNot linked
已关联\tLinked
清空选择\tClear selection
未选\tNot selected
星期\\时间\tDay / time
已选\tSelected
关于分页问题自行查看国标文件9.7，能否分页取决于厂家是否支持分页功能\tSee section 9.7 of the GB28181 specification for pagination. Pagination depends on manufacturer support.
请通过高级筛选选择时间与类型\tUse advanced filters to select time and type
日期和时间不要选择太大要不然会很卡，解决方法：自行搭配el-table-v2\tAvoid selecting an excessively large date range because it can reduce performance. Use el-table-v2 for large results.
未启用\tDisabled
已启用\tEnabled
行政区划\tAdministrative area
业务分组\tBusiness group
平台信息\tPlatform information
搜索设备名称\tSearch device name
推流中\tPublishing
搜索设备编号\tSearch device number
预置位编号\tPreset number
开关编号\tSwitch number
更多地址\tMore URLs
返回\tBack
日期\tDate
点击下载\tClick to download
搜索节点\tSearch nodes
`

export const tableHeaderEnglish = Object.fromEntries(tableHeaderTsv.trim().split('\n').map(line => line.split('\t')))
