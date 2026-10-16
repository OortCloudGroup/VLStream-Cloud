# 用户自接 SSH 算力与 AutoDL 接入

入口：**AI 算力调度 → 算力管理 → 接入服务器**。当前正常 VLS 登录用户可以自行登记个人已开机 GPU 服务器（AutoDL、其他云或自建），按当前租户和本人隔离，再在算法训练中选择该实际节点。

面向从新服务器开始配置的用户，请先阅读[用户自接 GPU 服务器指南](./NEW_GPU_ONBOARDING.md)，平台“接入指南”展示同一文档。按实际驱动与GPU选择配置并运行仓库中的
`tools/compute/bootstrap-autodl.sh`。P100 是项目团队开发测试设备，不是用户运行依赖。

本期范围为图像分类、物体检测、实例分割、语义分割训练，以及 PT 和类别 YAML 回存平台。普通实例通过 SSH 密码接入，不绑定开发者 Token、不自动同步实例。普通实例没有管理 API；PRO 的账号/API 接入是后续能力。官方依据：[普通与 PRO 对比](https://api.autodl.com/docs/instance_pro/)、[普通实例环境](https://api.autodl.com/docs/env/)、[PRO API](https://api.autodl.com/docs/instance_pro_api/)。

## 自接与市场权限边界（2026-10-16）

自接SSH不要求独立平台市场授权或厂商API Token。`/vlsCompute/my-nodes` 的创建/更新/环境检查/移除只面向当前已登录用户的私人节点；
新归属由后端从可信VLS用户与租户生成，客户端不能指定。只支持自助密码认证，拒绝公共节点/公共别名、公司共享节点和部署密钥创建。
管理者原`configure/publish/power/manage-all`权限继续用于原管理接口，普通用户不因此获得厂商API、公共发布或启停能力。
旧平台主体归属保留原证明路径，不自动回填无主记录；新私人训练使用当前租户及持久任务创建者访问路径，公共训练仍核验平台身份/占用/费用。

`保存并检查环境`只登记并探测，不安装依赖。自建服务器先按指南准备驱动、SSH/SFTP、Conda、独立Python3.10及四类模型缓存；
平台负责下发正式训练脚本、经后端上传训练包并取回PT/类别归档。默认仅普通公网SSH接入，通用私网接入尚无自助配置。
本地单元/隔离数据库与浏览器模拟验证见`codex/compute-onboarding-20261016/RESULTS.md`；源码修复不表示现有运行JVM已更新。

## 使用前准备

1. 更新后端和前端，按既有 Flyway 流程执行 `V1_2_0_025__autodl_ssh_compute.sql`。不要手工修改已执行的迁移。模型回存还依赖此前的模型存储迁移与可用 MinIO。
2. 新部署无需填写加密密钥。应用迁移028后，首次保存实例时自动生成密钥并持久保存；发布Compose已使用现有`backend-data`卷。已有显式密钥的部署继续保留原值，不要更换或删除。
3. 在 AutoDL 控制台有卡开机，按新接入指南选择 `standard` 或 `blackwell` 配置，执行初始化安装固定核心依赖并预下载四类基础权重。
4. 确认 VLS 后端可以访问实例的公网 SSH 地址及端口，实例目录可写、磁盘足够。Python 路径填写初始化脚本输出值（standard 默认为 `/root/autodl-tmp/vlstream/envs/vls-standard/bin/python`），默认工作目录为 `/root/autodl-tmp/vlstream`。AutoDL 容器内直接运行 Python，不启动嵌套 Docker。

## 接入和训练

### 凭据密钥自动管理

后端 `ruoyi-admin/src/main/resources/application.yml` 中已明确声明：

```yaml
vlstream:
  compute:
    encryption-key: ${VLSTREAM_COMPUTE_ENCRYPTION_KEY:}
    encryption-key-file: ${VLSTREAM_COMPUTE_ENCRYPTION_KEY_FILE:${user.dir}/data/security/compute-credential.key}
```

`encryption-key`默认留空即可。第一次使用线上算力凭据时自动生成32字节随机密钥，写入持久文件；以后重启或升级读取同一文件。
数据库的`vls_compute_key_registry`只保存SHA-256指纹，不保存密钥原文。密钥用于VLS后台加密SSH密码，不需要填到AutoDL实例。

- **标准Compose部署：**自动文件位于`/app/data/security/compute-credential.key`，由现有`backend-data`数据卷持久保存；用户不用生成或填写密钥。
- **直接运行JAR：**默认位于进程工作目录的`data/security/compute-credential.key`。升级时保留该目录，也可用文件路径配置指向已有持久目录。
- **多副本：**必须挂载同一个支持文件锁的持久目录。同一主机的发布Compose使用同一命名卷；跨主机部署需要共享卷，不能让各主机分别创建独立本地卷。数据库指纹会拒绝使用不同密钥的副本，避免写入无法互相解密的凭据。
- **已有部署：**原`VLSTREAM_COMPUTE_ENCRYPTION_KEY`或`vlstream.compute.encryption-key`优先使用（16/24/32字节），保持原值即可，不要求新增可写密钥文件。首次登记指纹前会校验已有凭据，原`v1:`密文格式保持兼容。
- **备份恢复：**自动模式同时备份数据库与密钥目录；显式配置模式保留原部署密钥。文件丢失、损坏或指纹不一致时拒绝生成替代密钥；需恢复原文件/配置，而不是清除记录后重试。

首次文件写入使用进程文件锁和原子替换，Linux新建密钥目录/文件权限分别为700/600。数据库登记指纹使用独立事务，后续保存实例失败不会撤销密钥指纹。
目录不可写、迁移028未应用或旧凭据缺少原密钥时，会提示管理员检查部署存储；普通租户无需管理这些参数。
源码和模板更新不会自动修改已部署服务，需部署含此功能的新后端并让Flyway正常升级。

### 操作步骤

1. 点击“接入服务器”，粘贴 `ssh -p 端口 用户名@主机` 或逐项填写，输入 SSH 密码、Python 路径、工作目录和 GPU 编号。
2. 保存后检查连接与环境。检查读取 CUDA/GPU、库版本和可用磁盘；通过后实例显示“可训练”。这是环境检查，不是训练成功证明。
3. 数据集先完成标注和训练/验证划分。线上训练不要求默认 GPU 主机上的 `dataset_path`，也不会改变原有数据集发布路径。
4. 创建训练任务，在训练配置中选择 AutoDL 实例和数据集。四种算法和标注类型必须匹配。提交后固定实例、参数和数据集版本快照，后台先将完整训练包保存并校验到 MinIO，再准备所选实例的本轮独立目录；重试使用同一固定包。该能力还需执行迁移 026，详见 `docs/dataset-import/TRAINING_DATASET_STORAGE.md`。
5. 后台按实例依次执行，日志和进度仍在算法训练页面查看。关闭浏览器不影响任务；原有默认 GPU 训练继续使用原路径。
6. 训练成功后取回**本轮** `output/weights/best.pt`，核对远端大小和 SHA-256，再写入 MinIO 并回读校验；类别文件来自同一冻结快照。两项都就绪才标记训练完成。按原“训练完成后保存到算法模型”选项自动登记模型，支持 PT 下载。

## 状态、恢复与边界

- `QUEUED → PREPARING → RUNNING → ARCHIVING → COMPLETED`；训练失败为 `FAILED`，确认取消为 `CANCELLED`。日志中的 `Training complete` 不直接决定平台完成状态。
- 后端使用每实例数据库锁，远端使用进程锁和 GPU 锁防止重复启动。GPU 已有其他计算进程时等待，不停止外部任务。首期每条记录选择一张 GPU，同一实例只执行一个平台任务。
- 连接中断时保留活动任务并重试，不推断远程进程已经停止。回存失败保持 `ARCHIVING`，复用已有归档状态重试；不会重新训练或悄悄换到默认 GPU。确认的训练失败需用户重新训练，生成新的任务目录。
- 停止操作记录取消意图；远端只对本任务创建的进程组执行停止，并核对 PID 与进程启动标识。未连接实例或未确认停止时，界面不会宣称已停止。
- 活动任务引用的实例不能修改或移除。平台移除实例记录不删除 AutoDL 实例、文件或账号；历史日志保留在任务记录中。
- SSH 密码使用 AES-GCM 加密；接口不回显密码/密文，请求日志排除算力接口参数。租户来自已认证的服务器上下文；实例、训练和产物访问均核对租户。只接受公网 SSH 地址；首次检查保存主机密钥，后续连接核对该密钥。
- 本期不包含云实例租用、开关机、释放、自动计费控制、线上智能标注、ONNX/OM/RKNN 转换。云实例费用和开关机在 AutoDL 控制台管理。默认 GPU 服务器的现有训练/智能标注/转换保持原行为。
- 云训练显式选择系统基础模型或算法指定模型；后者只允许当前租户 MinIO 已归档的 PT，下载校验后上传实例，不再回退到开发 GPU 读取历史绝对路径。

## 验证记录

2026-09-30真实实例验收：本地`localhost:3000`连接用户AutoDL vGPU-32GB（驱动识别RTX4080SUPER），完成初始化及环境检查、
四类数据集MinIO训练包、四类各1轮GPU训练、PT与类别回存、自动保存模型和两个入口下载校验；指定检测PT从MinIO取回再次训练通过。
排队取消、实际训练轮次开始后的停止及远端进程退出均确认。新增云模型“类别YAML”下载选项；原默认GPU不可用不再触发线上页的周期资源查询。
连接传输短暂失败后自动重试实际发生并恢复；未故意断开实例网络或在训练中重启后台。详细评估指标回写列表、线上智能标注、转换和设备部署不在通过范围。
图文证据：`codex/autodl-acceptance/AutoDL闭环测试图文记录.md`。下面记录保留此前各阶段验证历史。

2026-09-30 补充真实 CPU 验证：独立 Python 3.10、PyTorch 2.5.1 / Torchvision 0.20.1 CPU 版本、Ultralytics 8.3.240，
实际下载四类官方基础权重，分别完成 1 轮、batch=2、尺寸320的训练及 PT 重载推理。前端构建及新版指南页面检查通过。
证据位于 `codex/autodl-acceptance/`。这是训练代码及依赖验证，不代替 AutoDL GPU 或平台端到端验收。

本轮 Java 8 隔离编译与 53 项定向测试、6 项 Linux 进程测试、前端构建及浏览器模拟接口操作通过。隔离 MySQL 中实际执行了新迁移及队列事务；新增页面/API 的 ESLint 通过，既有页面未新增问题。证据保存于 `codex/autodl/RESULTS.md`。Linux 测试中四类训练函数使用可控替身，验证参数、进程、产物及状态协议，不代表真实 GPU 训练或模型精度。

未修改当前业务数据库、未重启用户业务后端、未连接或开机真实 AutoDL 实例。正式验收仍需在用户实例上完成：环境检查 → 真实数据上传 → 四类各一次小规模 GPU 训练 → 停止/断线恢复 → PT 和类别文件回存并下载核验。

### 2026-10-15 本地AutoDL Pro部署身份（源码已接入，默认关闭）

新增配置前缀vlstream.compute.autodl-local-ssh，默认enabled=false、environment-id为空、bindings为空。每条管理员绑定包含id、tenant-id、management-owner-subject、instance-id、host、port、username、host-key、public-key-fingerprint、private-key-file。只允许localhost环境、PRIVATE/EXTERNAL/INSTANCE_EXCLUSIVE、autodl/root，保留公网DNS和严格ED25519 host pin；后台身份模式不能提交密码，浏览器不能提交私钥或路径，公共别名/收费/共享/跨主体均拒绝。

如后续获准启用，本机VLS后端JSch必须读取已批准本地私钥到进程内存；仅文件引用并不免除持续使用授权。私钥不能上传VM、DB或仓库。此前允许给目标root安装SSH公钥，不代表允许启用本机后端持久认证。当前没有填写真实tenant/主体/路径绑定、开启enabled或登记节点；这些具体动作仍须单独批准。原P100固定身份和AutoDL厂商API凭据未更改。

此SSH身份供训练/探针/SFTP；厂商API token另用于Pro status/snapshot/list/电源接口，沿用既有后端加密凭据与总开关，本项不填token、不调用厂商接口或开关机，不改收费门禁。新增blackwell28版本及独立实机证据见NEW_GPU_ONBOARDING.md；完整行为和失败分支见根BUSINESS_PROCESSES.md。平台MinIO/模型/DB队列与重连尚未现场验收，不把离线替身当真实闭环。
