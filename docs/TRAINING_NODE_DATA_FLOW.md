# 训练节点数据链路核查（2026-10-10）

核查范围为本地“选择算力节点”的 CloudTraining 路径及其复用的数据集/模型对象存储。只读源码与本次隔离测试；未连接 GPU、MinIO、生产数据库或厂商账号，未删除任何业务文件。下列路径是代码中的规则，不表示某台线上服务器已经具备对应文件。

最新用户确认首版只卖 GPU 训练算力：GPU 磁盘为平台工作空间，关机磁盘成本平台承担；无用户存储收费、续费、欠费或人工客服。必要产物可靠回传并验证，再安全清理后才能复用下一用户；保障不足的节点须隔离。本次只核查事实，不删除任何现存文件。

## 已确认的数据流

设 `W = ComputeNode.workDir`，默认接入值为 `/root/autodl-tmp/vlstream`；`R = W/<SHA256(tenant)前16位>/<job UUID>`。每轮任务有新目录，数据库保存固定数据集版本、模型来源、训练参数及 R。目录按租户/任务组织，不能因此宣称已实现按用户的整厂商实例租用锁。

| 内容 | 当前来源、路径和持久性 | 代码证据 |
| --- | --- | --- |
| 原始样本/标注 | 样本图片通过 `DataMediaStorage.read(sample.localPath)` 从已配置 OSS 对象存储读取，`localPath` 在这里是规范化对象 key；冻结快照、版本和标注在平台数据库。 | `DataMediaStorage.java:23`；`TrainingDatasetArtifactService.java:242` |
| 冻结训练数据包 | 平台生成 ZIP，上传 `datasets/<tenantHash>/<datasetId>/<versionId>/<artifactId>.zip`，大小及 SHA-256 回读通过才 READY。不是从某用户 GPU 盘取唯一原始数据。 | `TrainingDatasetArtifactService.java:138`、`:149`、`:242`；`DataMediaStorage.java:23` |
| 数据集进入 GPU | 后端下载并校验 ZIP 到本地临时文件，检查全部路径/CRC/大小后经 SSH/SFTP逐文件写到 `R/dataset`，重写运行 YAML 的绝对根路径。GPU 不直接取得 MinIO 密钥；后端临时 ZIP 随后删除。 | `TrainingDatasetRuntimeService.java:88`；`CloudTrainingDatasetWriter.java:52` |
| 初始权重 | 系统模型从 `W/models` 预下载缓存读取，校验 `presets.json` 和每个文件摘要；指定算法权重须先在当前租户对象存储 READY，后端流式下载并校验后上传 `R/base.pt`。 | `CloudTrainingWorker.java:154`；`check_cloud_environment.py:28` |
| 训练过程 | `R/job.json`、`state.json`、`result.json`、`progress.json`、`run.lock`、`cancel.request` 和 `training.log/launcher.log` 在 GPU 本地；GPU 全局锁在 `/tmp/.vlstream-gpu-<index>.lock`。 | `cloud_training_runner.py:46`、`:98`、`:127`；`CloudTrainingWorker.java:102` |
| 模型/checkpoint与指标 | 输出在 `R/output`，最终目标固定 `output/weights/best.pt`，有 `args.yaml/vls-metrics.json`。YOLO 设置 save=True，但平台没有上传中间 checkpoint；语义分割只保存网络最佳权重，没有持久化优化器/随机状态恢复契约。 | `four_task_runtime.py:177`、`:191`、`:272` |
| 最终回存 | 从 GPU 取回本轮 best.pt，校验远端结果中的大小/摘要；类别文件由同一冻结快照生成。两项经过对象存储上传、回读及 READY 确认后才 COMPLETED。模型逻辑 key 是 `cloud-training/<nodeId>/<jobId>/weights/best.pt`，物理对象是 `models/<tenantHash>/<pathHash>/<sha256>/<fileName>`。 | `CloudTrainingWorker.java:169`；`ModelArtifactObjectStore.java:63`、`:83`；`CloudTrainingJob.java:25` |
| 日志与清理 | 轮询只把最后最多 32768 字节日志同步到 DB `log_tail`；没有全日志/checkpoint/指标包归档。成功、失败或取消只更新 DB 状态，未清理 GPU 的 R。平台临时文件清理不等于 GPU 清理。 | `cloud_training_runner.py:64`；`CloudTrainingWorker.java:118`、`:193` |

对象存储的实际桶/供应商由部署配置决定，本次没有读取配置密钥或验证线上桶。

## 失败、回存和恢复的真实边界

- SSH 断开或传输异常时保留任务活动状态并重试，不假定进程已死。服务重启后可以重读同一 R 下的 PID/进程起始标识、state/result 和日志；这是任务监控/回存恢复，不是训练 checkpoint 续训。
- SUCCEEDED 后先转 ARCHIVING。摘要不一致、对象存储暂不可用或某项未 READY 时留在 ARCHIVING，再从原 GPU 目录重试；不得在这一阶段释放/清理该目录。隔离测试已证明不一致字节不能完成，改成正确字节后才能 COMPLETED。
- 非 PREPARING 阶段若 `R/job.json` 消失，会报“本轮任务文件已缺失，无法恢复运行状态”。即使平台原数据包仍在，也不会自动把正在训练/回存任务搬到其他节点恢复。
- runner 若已记录状态但原进程死亡且无成功 result，会明确 FAILED，要求重新训练。`TrainingOptions` 只允许 mode/autoPublish，没有 resume；`four_task_runtime` 也没有 resume=True 或恢复优化器的分支。重新发起是新 UUID、新目录，从所选初始模型开始，不能声称自动断点续训。
- 已归档 best.pt 可作为以后训练的初始权重，但这不等同恢复原 epoch、优化器、数据顺序和完整训练状态。训练失败时已有本地最佳权重不会自动回存。
- `DatasetRemoteCleanup` 是另一条默认训练服务器上的项目数据集显式删除路径，不是 CloudTrainingWorker 成功后的清理步骤。本次没有调用它。

## 环境与用户任务产物的区别

`tools/compute/bootstrap-autodl.sh` 预置 `W/envs/vls-<profile>`、`W/models`、`W/models/torch`、`W/config/ultralytics`；缓存模型有摘要与固定版本校验。当前容器直接运行 Python，没有每任务独立 Docker 镜像或容器层销毁机制。

这些平台环境和公共基础权重可以复用，不属于某个用户的训练产物。`R/dataset`、`R/base.pt`、日志和输出属于本轮任务；同一工作目录下存在它们，不意味着必须向用户售卖长期磁盘。复用环境不能成为保留/转交上一用户产物的理由，源码目前也没有完成整实例重分配前的清理证明。

## 当前能称为什么，以及最小后续方案

当前已经做到“平台有可复用数据集包，成功模型经校验回存”；成功结果使用不再依赖 GPU 保留 best.pt。当前**未做到可随时销毁的无状态训练节点**：运行状态、回存源、失败中间结果和完整日志仍依赖原 GPU 盘；没有安全清理状态、全日志回存或断点 checkpoint 回存。

建议下一独立阶段先做“公司持有环境/缓存＋每任务临时工作区”的最小闭环，而不预设用户长期租盘：

1. 把成功必要产物与需要保留的日志明确归档，确认 READY 后将任务标记为可清理；回存失败继续保留工作区，不能误报可重分配。
2. 建立只针对本轮 R 的安全清理及证明，锁内核对无活跃任务、不跟随链接、不删 W/envs、W/models 或其他任务目录；该方案尚未实现，本次没有执行删除。
3. 首版故障明确重新训练、不中断点续训；若以后需要续训，再增加周期 checkpoint、优化器/随机状态、上传校验和恢复状态，避免把 best.pt 当完整 checkpoint。
4. 公司公共库存全平台可发现；公共库存阶段 `.039` 已持久化免费实例的跨卡可信 SSO 用户占用和用户级任务/模型访问校验，每卡队列保留，没有放宽全局租户隔离。用过工作区或历史任务无清理证明时隔离；当前没有安全清理执行器，付费占用拒绝。实际供应商库存同步仍待接入，未合成任何已购机器或未来卡数。
5. 按已确认方案不转收磁盘费。后续只需完成约定产物清单、回存自动重试/准确失败状态与节点隔离，不配置转人工客服；修复性补传、环境维护和清理由平台承担，不能冒用用户新扣款。支付同事可先做稳定个人余额/幂等扣款/查单/后台鉴权契约。

这些是后续建议，不代表本轮已提供清理、迁移、自动停机、收费或真实供应商联调。当前没有登记虚构的“20多台”实例或未来四张 P100，也没有读取用户提供的 AutoDL 凭据。

## 证据索引

源码根：`VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/`。

- [任务创建与固定目录](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/CloudTrainingService.java)
- [固定数据包与 GPU 上传](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/CloudTrainingDatasetWriter.java)
- [数据包生成/对象存储](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/data/TrainingDatasetArtifactService.java)
- [数据包校验/物化](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/data/TrainingDatasetRuntimeService.java)
- [任务轮询/模型回存](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/CloudTrainingWorker.java)
- [Python 进程及文件状态](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/cloud_training_runner.py)
- [四类训练/模型保存](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/four_task_runtime.py)
- [模型对象存储校验](../VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/service/ModelArtifactObjectStore.java)
- [初始化环境与缓存](../tools/compute/bootstrap-autodl.sh)

隔离 MySQL + SSH/对象存储 mock 的当前回归见 `codex/compute-market-reference/billing-policy-tests.log`。这是源码/受控测试证据，不是线上文件清单或真实 MinIO/GPU 验收。
