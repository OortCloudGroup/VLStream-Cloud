# GPU 算力实例统一训练环境初始化与磁盘容量清单

核实日期：2026-10-20。适用项目：VLStream-Cloud；适用业务：后续算力市场 GPU 实例执行相同的四类算法训练。

## 1. 结论与统计口径

6000D 已完成环境初始化及四类独立真实 GPU 小样本训练；这不等于 VM 发起训练、平台任务状态、MinIO 回存、模型保存和队列释放的全链路已经验收。后续所有 GPU 都准备相同业务能力，只按硬件架构选择兼容的软件组合。

- 一台 GPU 服务器通常只需一套适合其硬件的训练环境，不需为四类算法各装一套，也不需每张卡复制一套依赖。
- 6000D 完整独立任务环境建议预留 **12 GiB**；P100 的兼容环境按 **10～14 GiB** 估算。这里已包含 Python、Torch/Torchvision、CUDA 运行库及训练依赖，不含基础镜像、数据集、训练产物和下载缓存。
- 四类官方基础权重一套只有 **233,083,868 字节，约 222.29 MiB（0.217 GiB）**；模型权重不是主要的固定开销，环境和训练数据通常更大。
- 安装或升级时，下载包、缓存、临时解压和新旧环境可能共存；不能只按安装完成后的目录大小采购容量。
- **80GB 系统盘适合受控的小中型任务起步，不能保证任意数据集或长期保留多轮训练。** 建议保留至少 15%～20% 可用空间，并在每次任务开始前计算本轮容量。
- AutoDL Pro 当前只有系统盘；`/root/autodl-tmp` 是该盘上的保留目录，不是独立数据盘。其他供应商支持数据盘时，再采用单独的数据盘方案。

本文空间统一优先使用 GiB/MiB（1 GiB = 1,073,741,824 字节）。供应商“80GB”与 `df -h` 的 `G` 不一定是相同口径，容量判断以实例实际 `df -B1` 为准。

证据标记：**历史实测**指原服务器采集记录；**本地复核**指本次对已下载证据的文件大小/摘要重新核验；**估算/预算**指容量规划值。本次没有连接 GPU 实例、开机、训练、安装、扩容、清缓存或生成 SSH 密钥，没有今天的远端 `du/df` 实测。

## 2. 已完成到哪一步：6000D 状态证据

原始落盘报告日期为 **2026-10-15**，最终进程检查时间为 `2026-10-15T09:58:56.620844+00:00`。主聊天在 10 月 16 日和 18 日复述了这一完成状态，不能把复述日期当作新的训练运行日期。

| 核实项 | 结果 | 证据 |
| --- | --- | --- |
| 基础镜像 | Python 3.12.3、Torch 2.8.0+cu128、Torchvision 0.23.0+cu128 | `codex/autodl-pro-20261015/baseline.json` |
| 实际 GPU | NVIDIA RTX 6000D，85,651 MiB 显存，计算能力 12.0；驱动 595.71.05 | 同上；按实物读取，未仅凭商品名称推断 |
| 独立任务环境 | Python 3.10.22、Torch 2.8.0+cu128、Torchvision 0.23.0+cu128、Ultralytics 8.3.240；`pip check` 通过 | `downloaded-evidence/inventory-isolated.json`、`requirements-resolved-blackwell28.txt`、`RESULTS.md` |
| 环境可执行性 | GPU 前后向、Torchvision CUDA NMS、四类权重完整性及加载通过；原 base 包清单不变 | `probe-candidate.jsonl`、`base-packages-diff.txt`、`final-process-check.json` |
| 四类真实训练 | 四项 `SUCCEEDED`，PT 重载 GPU 推理、类别核对通过 | `downloaded-evidence/evidence/summary.json` |
| 收尾 | 训练/安装/传输进程退出，任务锁和 GPU 锁释放；当时无 GPU 计算进程 | `final-process-check.json` |
| 本地保存 | 证据 ZIP 231,879,273 字节；历史回执核验内部 163 文件。本次重新核对四个 PT 大小和 SHA-256，均与回执一致 | `local-transfer-receipt.json` |
| 平台全链路 | **未验收**；原证据明确 `platformComplete=false`、`minioArchived=false` | `final-process-check.json`、`local-transfer-receipt.json` |

上表 `downloaded-evidence/` 等相对位置均在 `codex/autodl-pro-20261015/` 下。本地独立环境为 `/root/vls-pro/envs/vls-blackwell28`，工作根为 `/root/vls-pro`；这是历史实际路径，新实例可选其他专用路径。

四类测试共享运行 ID：`99f905929db34ad9ab87543a5365a1b4`。每类 8 张合成图片（train 6 / val 2）、2 类、1 epoch、batch 2、输入 320；只证明功能和兼容性，不证明业务精度或性能。

| 训练类型 | 框架/基础模型 | 原记录 runner 用时 | 本次复核 best.pt 字节 | 结果 |
| --- | --- | ---: | ---: | --- |
| 图像分类 | Ultralytics YOLOv8n-cls | 8.18 秒 | 2,963,080 | 训练、GPU 重载推理通过 |
| 目标检测 | Ultralytics YOLOv8m | 18.19 秒 | 51,981,394 | 同上 |
| 实例分割 | Ultralytics YOLOv8n-seg，实例掩膜 | 12.18 秒 | 6,730,228 | 同上 |
| 语义分割 | Torchvision DeepLabV3-ResNet50，逐像素类别 | 5.23 秒 | 168,312,819 | 同上 |
| 合计 | 四个 best.pt | 不作性能比较 | **229,987,521（219.33 MiB）** | 四个摘要均匹配历史回执 |

原始证据 ZIP 的 SHA-256 为 `5fc02005a0ac5f445b05ad7e5ac447c729a524c54f6f41831bcdff40b5ff445a`（历史回执值；本次仅重新散列四个 PT）。

VM 到该 Pro 实例的后台身份、认证绑定及正式业务链路当时待配置/验收。后续出现公司供给展示记录、源码 profile 合入或服务健康，并不能补足真实平台训练证据。本次没有发起平台验收，故不把服务器独立训练改写为平台接入通过。

## 3. 同一业务的硬件兼容配置

| 实际硬件/条件 | 初始化 profile | Python | Torch / Torchvision | wheel CUDA 运行库 | 已有证据与边界 |
| --- | --- | --- | --- | --- | --- |
| 已验证 RTX 6000D，计算能力 12.0 | `blackwell28` | 3.10 | 2.8.0 / 0.23.0 | cu128 / 12.8 | 本文四类真实 GPU 训练通过 |
| 已验证 Tesla P100，计算能力 6.0 | `standard` | 3.10 | 2.5.1 / 0.20.1 | cu124 / 12.4 | `codex/gpu-nodes-20261013/173-STAGE-REPORT.md`、`88.99-TRAINING-READY.md` 有四类实训；wheel 实际含 sm_60 |
| 既有另一 Blackwell 组合 | `blackwell` | 3.10 | 2.7.1 / 0.22.1 | cu128 / 12.8 | 脚本保留的配置；本报告不替其承诺新实例验收通过 |
| 后续其他 GPU | 按架构、驱动和显存选兼容组合 | 依部署版本要求 | 成对固定 | 与所选 wheel 匹配 | 每台重新检查，并完成四类小样本验收 |

P100 **不能直接使用 Torch 2.8 官方 cu128 wheel**：NVIDIA 将 P100 列为计算能力 6.0；PyTorch 2.8 发布说明明确 cu128/cu129 构建取消 sm50～sm60。项目保持已验证的 2.5.1/cu124 组合，不因为官方存在其他可选构建就绕过项目版本检查。[NVIDIA 旧 GPU 架构表](https://developer.nvidia.com/cuda/gpus/legacy)、[PyTorch 2.8 CUDA 支持说明](https://github.com/pytorch/pytorch/releases/tag/v2.8.0)、[官方版本配对](https://pytorch.org/get-started/previous-versions/)。

6000D 购买镜像的 Python 3.12 与任务 Python 3.10 是两层：保留原 base，新建独立任务环境。不能把已存在 base 误写为无需另建环境，也不应把两套完整目录当成同一份空间。

## 4. 初始化必须具备的功能和依赖

实际入口为 `tools/compute/bootstrap-autodl.sh`；基础权重准备为 `tools/compute/prepare-presets.py`；运行时为 `ruoyi-vlstream/src/main/resources/training/four_task_runtime.py`，探针为同目录 `check_cloud_environment.py`。本次未找到一个可代替初始化脚本的统一静态 requirements 文件；应使用脚本固定核心版本，并保存安装完成后的 resolved 清单。

| 能力 | 必需内容 | 说明 |
| --- | --- | --- |
| GPU 访问 | 供应商/管理员提供的兼容 NVIDIA 驱动、容器 GPU 访问、`nvidia-smi` | 云容器通常复用宿主驱动，不在容器里重复安装内核驱动；驱动版本不等同 Torch CUDA 运行库 |
| SSH/SFTP 与进程管理 | OpenSSH 服务/SFTP、`nohup`、`flock`、coreutils、util-linux | 供后端上传数据/脚本、执行和确认进程身份、串行锁定 GPU；已有工具可复用 |
| 下载与系统动态库 | ca-certificates、curl、unzip、libgl1、libglib2.0-0；libgomp1 按实际依赖核查 | 不要求把 Jupyter、完整开发桌面或编译工具链作为训练前置 |
| 隔离 Python | Conda、独立 Python 3.10、pip；禁用用户 site 干扰 | 显式使用该环境绝对 Python 路径，保留原 base/旧业务环境 |
| 深度学习框架 | 按 profile 固定 Torch/Torchvision；Linux wheel 依赖的 Triton、NVIDIA 包 | 四类共用一套；当前业务未要求 torchaudio |
| 四类训练运行库 | `ultralytics==8.3.240`、`numpy==1.26.4`、`opencv-python==4.11.0.86` | 脚本直接固定的核心版本 |
| 传递依赖 | Pillow、SciPy、PyYAML、Matplotlib、Polars、psutil、requests、ultralytics-thop 等 | 由解析器安装；6000D 当时已冻结完整版本。新装不要声称所有传递依赖均已锁死；老 CPU 还需核对 Polars 指令集兼容 |
| 模型缓存 | 四类官方基础权重、`models/presets.json` SHA-256 清单 | 预下载并加载，避免首个业务任务等待下载 |
| 执行与交付 | 匹配部署版本的 runner、验证器、任务配置、输出 PT、classes.yaml、日志和状态文件 | 算法类型和训练包格式必须匹配；MinIO 归档在平台侧完成 |

6000D 当时 NVIDIA 包包括 cuBLAS、cuDNN、cuFFT、cuRAND、cuSOLVER、cuSPARSE、cuSPARSELt、NCCL、NVRTC、CUDA runtime/CUPTI、NVJitLink、NVTX、cuFile。它们是 Torch wheel 依赖的运行库，不需要另装一套系统 CUDA Toolkit 才能执行当前训练。

## 5. 分项空间：固定环境、缓存与可选导出

以下以 Linux x86_64、一套任务环境、一份模型缓存为单位。子项与“合计”不能再次相加。

| 项目/典型位置 | 稳态空间 | 安装/升级峰值或附加空间 | 依据和计数边界 |
| --- | ---: | ---: | --- |
| 供应商基础镜像、原 base、预装系统 Toolkit | **未取得分项目录实测**；记入实例原有占用 B | 取决于镜像/overlay 计量 | 复用，不能统一宣称 0GB 或固定 20GB；用初始化前 `df` 建基线 |
| 系统工具和动态库增量 | 0～0.5 GiB，估算 | 另 0.1～0.5 GiB 包缓存，估算 | 已有包增量可为 0；不含宿主驱动与原 base |
| Conda 管理器 base（已有则复用） | 新装约 0.5～1.5 GiB，估算 | 安装器/包缓存另计 | 不含下面独立任务环境；历史镜像已带管理器，无单独 du |
| 独立 Python 3.10、pip 和基础标准库 | 0.3～0.8 GiB，估算 | Conda 下载/解包见缓存行 | 任务 prefix 内，排除 Torch 等后续子项 |
| Torch + Torchvision，排除外置 NVIDIA 目录和 Triton | 1.5～2.5 GiB，6000D 估算 | 官方 Torch 下载约 889MB、vision 约 8.6MB（历史安装日志）；解包另占临时空间 | 不能拿 wheel 压缩下载大小当安装目录大小 |
| NVIDIA CUDA 运行库等 14 个包 | **4,386,028,882 字节，4.085 GiB，历史文件清单实测** | 若重新下载需额外压缩包及解包空间 | 368 文件；`cuda-cache-copy-manifest.json`。该值是文件逻辑字节，不是 du 分配块统计 |
| Triton | 0.3～0.8 GiB，估算 | 下载和编译缓存另计 | 与上行 NVIDIA 目录分开 |
| Ultralytics、NumPy、OpenCV、SciPy、Pillow、绘图/表格等其余依赖 | 0.6～1.5 GiB，估算 | wheel/解包另计 | 不重复计 Torch、NVIDIA、Triton |
| **6000D 任务环境合计（以上 Python 至其余依赖）** | **约 7～10 GiB，估算；规划 12 GiB** | 单套干净安装另留约 6～12 GiB 临时/缓存预算 | 只有 CUDA 子项精确；没有整套 prefix 的历史 du，不能把合计写成实测 |
| P100 standard 任务环境合计 | 约 10～14 GiB，估算 | 另留约 6～12 GiB 安装余量 | 是替代配置，不与 6000D 环境相加；未取得本次同口径 prefix du |
| 四类基础权重 `models/` | **0.217 GiB，历史文件大小实测**；建议分配 0.5～1 GiB | 下载中临时副本可接近另一份 | 一套 4 个；多工作区各复制一套就按份数计，不能按 SHA 相同假定物理去重 |
| runner/验证器、manifest、配置、字体 | 通常 <0.1 GiB，估算 | 按发布包内容增加 | 只分发精简训练资源；完整工程、前端 node_modules/Maven 缓存不是 GPU 必需 |
| pip 缓存、wheelhouse | 非必需长期保留；常见 3～8 GiB，估算 | 重复下载/多版本可远超此值 | 现行公共 bootstrap 未全局关闭 pip 缓存；历史验收脚本部分使用 `--no-cache-dir`，不能混同 |
| Conda `pkgs` 缓存 | 约 0.5～2 GiB，估算 | 多环境和版本继续增长 | 可能硬链接共享；实际物理计数应按同一统计口径，勿直接累加 apparent-size |
| pip 临时解包、`/tmp`、编译/绘图配置缓存 | 约 0.1～1 GiB 稳态，估算 | 安装时另留 3～6 GiB；与缓存合计约 6～12 GiB 起步 | 升级时若保留旧完整环境，还要再加一套环境空间 |
| ONNX 导出依赖（可选） | 同一兼容环境增装 ONNX 等约 0.1～0.5 GiB，估算 | 导出文件/临时模型另计 | 当前 GPU PT 训练初始化不要求安装；独立 CPU 导出环境约 1.5～4 GiB，作为替代方案，勿与前值重复 |
| OM 转换（可选独立服务） | CPU 镜像约 2～5 GiB + 厂商工具链约 3～10 GiB，均为估算 | 构建/多镜像可能 10～25 GiB；另留每任务约 0.5～2 GiB 工作区 | 无厂商工具目录实测；建议独立转换主机，不能直接塞入 GPU 训练必装预算 |

CUDA 防重复计数：若统计 `envs/vls-*/` 整个目录，其内部 `site-packages/nvidia` 已包含在内；不能再加 4.085 GiB。`/usr/local/cuda` 若实际存在，是系统 Toolkit 的另一份真实目录，可单列其占用，但对当前训练是可选；同一目录不能因名称分别写“CUDA”“cuDNN”“Torch 自带运行库”而重复累加。

历史 6000D 是从同机 base 精确核验后独立复制这 14 个包，没有链接到 base；因此新环境中确实存在额外副本。不能据此建议跨机器盲目复制 Python 3.12 的 Torch/Torchvision/Triton 到 Python 3.10。新实例默认按初始化脚本解析安装并验收。

导出依据：`deploy/om-converter/Dockerfile` 使用 Python 3.10、CPU Torch 2.5.1/Torchvision 0.20.1、Ultralytics 8.3.240、ONNX 1.17.0；厂商 SVP/ATC 另挂载。OM 当前仅覆盖已支持的 Hi3519DV500 YOLOv8 目标检测路径，不能推断四类均可转换。GPU 实例无需 ATC，也无需为 OM 访问 P100。见 [OM_CONVERSION.md](OM_CONVERSION.md)。

## 6. 数据集、检查点、日志：按任务增长的空间

| 内容 | 可核实示例或估算方法 | 规划方式 |
| --- | --- | --- |
| 训练包 ZIP | 每轮实际压缩包大小 Z | 上传/校验/解压期间至少考虑 Z 与解压结果 D 同时存在 |
| 解压图片、标签、掩膜 | 实际总字节 D；实例/语义分割需计掩膜，不只计图片 | 以冻结训练包实际解压大小为准；按文件数同时核对 inode |
| 数据版本与工作副本 | 每个仍保留版本/任务可能各占 D | 不按“只有一个项目”假定磁盘只存一份；并发、重试和历史轮次分别计算 |
| best/last/中间检查点 | 6000D 四个 best 合计 219.33 MiB；P100 历史另有 YOLO best+last、语义 best，共 7 个 checkpoint | 每轮初步预算 0.5～2 GiB，模型变大/保留更多轮时上调；训练中优化器等可使 checkpoint 大于最终精简 PT |
| 日志、CSV、曲线、预测图、类别和状态 | 小样本很小；长训练/保存预测按实际增长 | 每轮先留 0.1～1 GiB，估算；禁止无限保留详细日志和图片而不预算 |
| 本地归档 ZIP、下载副本 | 6000D 证据 ZIP 221.14 MiB，历史实测 | 归档 ZIP 与原目录共存时是额外空间；不能把归档当作原目录已被替换 |
| 平台/MinIO 原件与归档 | 数据集及模型属于平台存储，GPU 保存执行副本 | GPU 空间与平台存储分别计；远端回存成功不表示 GPU 副本已自动删除 |

本次对 6000D 已下载四类任务目录按文件 Length 统计：数据集合计 **1,516,634 字节（1.45 MiB）**，四个任务全部文件合计 **231,539,540 字节（220.81 MiB）**。这是本地证据副本的逻辑大小，训练证据包可能只收集所选产物，不能当作服务器原任务目录完整 `du`，更不能据 8 张合成图推断生产数据容量。

建议按以下式子估算单次任务的新增峰值：

```text
任务新增峰值 ≈ ZIP Z + 解压数据 D + 其他工作副本 D' + 检查点 C + 日志/图像 L + 暂存 T
磁盘需求 ≈ 已有真实占用 + 安装/升级新增峰值 + 同盘各任务新增峰值之和 + 安全余量
```

例：Z=4 GiB、D=12 GiB、没有额外完整副本、C=1 GiB、L=0.5 GiB、T=2 GiB，任务新增峰值约 **19.5 GiB**；若流程另复制整份数据，再加 12 GiB。JPEG/PNG 已压缩，不能一概假定 ZIP 解压倍率很大或很小。

## 7. 已有磁盘快照与稳态、峰值区别

| 历史采集点 | 原始记录 | 能说明什么 |
| --- | --- | --- |
| 6000D 安装期间基线 | overlay：总 79G、已用 2.4G、可用 76G；`/` 与 `/root/autodl-tmp` 相同 | 不是空镜像基线，不能从这里精确分摊初始 OS/base |
| 6000D 独立环境库存快照 | overlay：总 79G、已用 8.7G、可用 70G；`freeBytes=74,607,673,344`（69.48 GiB） | 是当时环境阶段的整文件系统快照，不是所有后续训练/归档后的最终 df，也不是安装峰值 |
| 共享内存 | `/dev/shm` 为 tmpfs，约 46G | 属内存资源，不额外算成系统盘已用 46GB |

来源：`baseline.json`、`downloaded-evidence/inventory-isolated.json`。历史报告“约71G可用”与原始 `df -h`/字节值有显示口径差别，本文以原始数值为准。

没有保存的精确值包括：各环境目录完整 du、pip/Conda 缓存目录 du、基础镜像层空间、安装全过程最大用量、四类训练后最终 df。不能用两个历史快照之差冒充各依赖实测占用。overlay 下镜像只读层和可写层也可能采用不同计量；`du /` 与供应商计费容量可能不一致。

稳态是保留环境、权重、当前数据和需要的产物后的用量。安装峰值还包含压缩下载与解压、缓存；升级若新旧环境并存需再加新环境。**现行脚本不承诺自动删除缓存、历史任务或检查点**，所以规划上的“稳态”不代表本次已清理得到这一用量。

## 8. 80GB 容量预算与存储策略

### 8.1 AutoDL Pro：单系统盘

供应商现行文档在本次只读核验时明确：Pro 暂无数据盘，只有可扩容系统盘；重置/更换镜像默认保留 `/root/autodl-tmp`。目录保留不等于独立磁盘、独立配额或外部备份；实例释放仍会丢数据。[AutoDL Pro 数据与磁盘说明](https://www.autodl.com/docs/instance_pro_data/)。

以下为约 **74 GiB 可用容量**的保守规划示例，不是 6000D 的真实目录测量；80GB 若按十进制约为 74.5 GiB，而既有 6000D 历史 df 显示约79G，实际必须重新读取容量。

| 预算项 | 示例 GiB | 说明 |
| --- | ---: | --- |
| 初始化前已有计量占用 B | 8 | 假设值，采购后先测；含原有系统/base 的已计量部分 |
| 新增独立任务环境及少量系统工具 | 12 | 6000D 规划；若 Conda 管理器需新装要从其他额度调整 |
| 基础权重与精简训练资源 | 1 | 实际四权重约0.217 GiB，其余是余量 |
| 下载/Conda/pip缓存额度 | 6 | 预算上限，实际脚本未强制限制 |
| 当前数据集 ZIP 暂存 | 8 | 按任务实际 Z 调整 |
| 当前解压数据/掩膜 | 15 | 按 D 调整，额外副本需另留空间 |
| 检查点、日志、待归档结果 | 5 | 控制历史轮数，不是所有模型的固定需求 |
| 安装/解包与任务临时空间 | 5 | 与缓存并存，避免安装中断 |
| 保持空闲的安全余量 | 14 | 接近19%；供增长和失败恢复 |
| 合计 | **74** | 任一项超预算需减少本轮数据/副本或经批准扩容 |

示例是从“初始化前 B”开始分配。**若实例已初始化，用当前 df 已用量作为新基线，不能再把其中已含的环境加一次。** 若 B 实测大于8 GiB，或准备两套环境、多个并发数据集/历史版本，80GB 很快不足。

Pro 新实例建议将持久训练根设为 `/root/autodl-tmp/vlstream`，其下统一 `envs/`、`models/`、`config/`、`cache/`、`runs/`；Conda/pip 的缓存和临时目录应在初始化前明确。它们仍消耗同一个系统盘。本次未移动历史 `/root/vls-pro`，也未改其缓存配置。

### 8.2 支持独立数据盘的平台/自建服务器

先通过 `findmnt`/`df` 确认实际挂载，再把工作根设置到数据盘；仅改目录名字不会得到独立容量。环境、模型、数据、runs 和缓存可统一放到数据盘专用目录；如果环境留系统盘，则分别核对两盘容量及 `/tmp` 所在盘。

数据盘按 `全部保留数据版本 + 当前任务 ZIP/解压/副本 + 产物保留周期 + 安装缓存 + 20%余量` 计算。约50 GiB活跃数据且多版本并存时，可从100～200 GiB档位评估，但必须按实测包大小和并发数计算，不能把该容量当作业务已实测要求。

原件及成功回读校验的模型归档到平台对象存储；GPU 本地作为执行副本。清理必须等任务终态、无活动引用、产物已回存且校验成功，并遵循保留策略。当前仅整理策略，没有执行删除、迁移或修改自动清理逻辑。

## 9. 新 GPU 初始化顺序

1. 记录供应商、实际 GPU 型号/计算能力、驱动、CPU 架构、显存、系统盘总量/可用量、inode 和实际挂载。记录初始化前磁盘基线，不为统计开启收费实例。
2. 按实际硬件选 `standard` 或 `blackwell28` 等兼容 profile；四类业务能力一致。确认已有 base/工具可复用，保留原环境。
3. 选定持久工作根、Conda prefix、模型/缓存/临时/任务目录；对 Pro 按单系统盘预算，其他平台核实数据盘。确认空间能够覆盖安装峰值和首个任务。
4. 由供应商/管理员准备驱动、SSH/SFTP、基础工具和动态库；取得与 VLS 部署版本一致的精简初始化资源。
5. 先检查脚本计划；在获得初始化执行授权且 GPU 空闲时，再执行现有 bootstrap。它会新建 Python3.10、固定核心框架、安装依赖、`pip check`、准备四类权重并生成 SHA 清单。
6. 保存 `requirements-resolved-<profile>.txt`、环境探针结果、版本/架构信息和初始化后空间统计。完整 requirements 应以当次解析结果为准，不能只写“最新版”。
7. 用后端实际网络和已批准身份登记并检查节点，确认每一项环境检查通过。维护 SSH 成功不能替代后台认证；不得挪用 P100 身份或新建未批准密钥。
8. 对每台新实例安排四类各一次小样本训练，核对 PT/类别、重载、日志、进程退出及锁释放；记录这一级为主机验收。
9. 再单独验收 VM/VLS 发起、冻结训练包、平台状态、PT/classes MinIO 回读摘要、模型保存及队列释放；未完成就保持“平台链路待验收”。

本次没有执行上述安装/运算/训练步骤。现有脚本 `--dry-run` 仅打印 profile 和路径；正式 bootstrap、环境探针都包含 GPU 运算/锁文件或输出文件写入，不属于下面的纯只读统计命令。

## 10. 验收清单

- [ ] 实物 GPU、计算能力与 wheel 编译架构相容；P100 含 sm_60，6000D 历史实例含 sm_120。
- [ ] Python3.10 的绝对路径、隔离 prefix、用户 site 禁用情况正确；原 base/旧业务环境保留。
- [ ] Torch/Torchvision/CUDA/Ultralytics 版本符合选定 profile；依赖完整且 `pip check` 无冲突。
- [ ] 四类基础权重存在、大小/摘要匹配 `presets.json`，各模型可加载。
- [ ] SSH/SFTP 由后台真实运行身份访问成功，主机指纹核验，凭据持久配置符合既有批准范围。
- [ ] 实际 GPU 前后向、CUDA NMS 及四类小样本训练通过，PT 能重载且类别一致。
- [ ] 训练包固定版本、train/val 非空且 SHA 不跨集合；掩膜和任务类型相符。
- [ ] 任务后进程退出、GPU/任务锁释放；平台任务状态和产物状态有实证。
- [ ] VM 发起训练、MinIO PT/classes 回读摘要、自动模型保存和队列释放单独通过，不能由主机实训代替。
- [ ] 初始化前/后、安装峰值、任务峰值有磁盘记录；总空间及 inode 都足够，至少保留15%～20%余量。
- [ ] 不在每个 GPU 上默认安装 ONNX/ATC/完整 CUDA Toolkit；确需导出时单独验版本与空间。
- [ ] 明确数据保留/实例释放政策、失败留证和归档条件；不自动删除活动任务或未归档产物。

## 11. 安全只读统计命令

仅在实例已经运行且现有批准 SSH 可用时执行；先核对目标。以下命令不安装、不训练、不清理、不读凭据。示例路径应替换为实际任务根；历史6000D为 `/root/vls-pro`，新 Pro 建议根为 `/root/autodl-tmp/vlstream`。

```bash
# 磁盘容量/可用字节、inode和实际挂载；不要用目录名判断是否数据盘
date -u
df -B1 / /root/autodl-tmp
df -i / /root/autodl-tmp
findmnt -T /root/autodl-tmp
df -h /dev/shm
nvidia-smi --query-gpu=name,driver_version,memory.total --format=csv

# 只统计已知训练目录，限制单次耗时；不从 / 全盘递归，不加 sudo
VLS_WORK=/root/vls-pro
VLS_ENV="$VLS_WORK/envs/vls-blackwell28"
du -x -B1 --max-depth=1 "$VLS_WORK"
timeout 30s du -x -B1 -s "$VLS_ENV" "$VLS_WORK/models" "$VLS_WORK/runs"

# 环境内部拆分：它们属于上面的环境总数，不能与总数相加
VLS_SITE="$VLS_ENV/lib/python3.10/site-packages"
timeout 30s du -x -B1 -s "$VLS_SITE/torch" "$VLS_SITE/torchvision" \
  "$VLS_SITE/nvidia" "$VLS_SITE/triton"

# 按实际存在的已知位置检查缓存/系统Toolkit；不存在时报错可跳过
# Pro新目录改变时同步替换路径。不可把 ~/.cache/pip 当作唯一实际缓存位置。
timeout 30s du -x -B1 -s /root/.cache/pip /root/miniconda3/pkgs \
  "$VLS_WORK/cache" /usr/local/cuda

# 不导入 torch/ultralytics，不执行GPU运算；只读已安装包元数据
PYTHONDONTWRITEBYTECODE=1 "$VLS_ENV/bin/python" -B -s - <<'PY'
import sys
from importlib.metadata import version
print('python', sys.version.split()[0], 'prefix', sys.prefix)
for name in ('torch', 'torchvision', 'ultralytics', 'numpy', 'opencv-python', 'triton'):
    print(name, version(name))
PY
```

第一条 `du --max-depth` 也可加 `timeout 30s`；超时即停止，不扩大扫描。`du` 为文件分配块统计（这里以字节显示），历史文件 Length/manifest 是逻辑字节；稀疏文件、硬链接、符号链接与 overlay 都可能使两者不同。按同一文件系统根统一统计硬链接，父目录总数与子目录只用于分解，不能累加；`/usr/local/cuda` 的符号链接目标应核实后只计实际目录一次。不要执行 `pip cache purge`、`conda clean`、`docker prune`、`rm` 或任何安装/训练命令来“测空间”。

## 12. 证据入口与剩余不确定项

项目内依据：

- [新GPU接入指南](NEW_GPU_ONBOARDING.md)、[AutoDL训练说明](AI_COMPUTE_AUTODL.md)、[OM转换范围](OM_CONVERSION.md)。
- 初始化源码：`tools/compute/bootstrap-autodl.sh`、`tools/compute/prepare-presets.py`；训练/探针：`VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/`。
- 本地历史6000D证据：`codex/autodl-pro-20261015/RESULTS.md`、`baseline.json`、`cuda-cache-copy-manifest.json`、`local-transfer-receipt.json`、`downloaded-evidence/inventory-isolated.json`、`downloaded-evidence/evidence/summary.json`、`downloaded-evidence/requirements-resolved-blackwell28.txt`。`codex/` 是本地忽略目录，外发本MD不会自动包含原证据包。
- P100兼容实训/模型清单：`codex/gpu-nodes-20261013/173-STAGE-REPORT.md`、`88.99-TRAINING-READY.md`、`PRESET_MODEL_INVENTORY.md`、`88.99-final-resource-audit.json`。
- 主聊天复述来源（由父会话提供）：`Sentinel_beaac19479f08191a68c858bf7d4ce2e`，2026-10-16 08:18:38 UTC；`Sentinel_18410e8e57f0819190aa839036cb332c`，2026-10-18 05:45:02 UTC。具体测试及大小以本机落盘证据为准。

尚缺今天的实例运行状态、远端分目录 du、安装全过程峰值、生产数据包大小/并发/保留周期，以及6000D VM平台全链路验收证据。因此本文提供的是**有历史实测支撑的初始化清单与可调整预算**，不是当前磁盘审计或所有新GPU已验收证明。无需为补测开机计费；下次已获授权的正常运行窗口再按上面的只读命令补齐即可。
