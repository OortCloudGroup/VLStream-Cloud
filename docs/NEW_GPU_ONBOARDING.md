# 用户自接 GPU 服务器指南

入口：**AI 算力调度 → 算力管理 → 接入服务器**。从本页“接入指南”也能进入同一配置表单。
你可以接入自己购买的 AutoDL、其他云 GPU 或自建服务器。正常 VLS 登录用户可登记自己的 SSH 节点；
节点归当前用户和当前租户，不要求先购买公共算力、授权平台市场账户或持有厂商 API Token。
厂商 API、公共供给与收费实例在“算力市场”另行管理；接入自有服务器不会向市场发布它。

## 1. 先准备什么

| 项目 | 用户需要准备 |
| --- | --- |
| 服务器 | 已开机的 Linux x86_64 + NVIDIA GPU；当前标准训练接入不支持 Windows、macOS、ARM 或 AMD/CPU 节点 |
| 驱动 | GPU 厂商支持的 NVIDIA 驱动，`nvidia-smi` 正常，容器内也能看到所选 GPU；平台不安装驱动 |
| SSH | 后端可达的主机名/IPv4、端口、账号和密码；账号可执行命令并使用 SFTP，无须是 root |
| 目录 | 专用、可读写的持久目录；建议空闲 30 GiB 起，另计数据集、依赖、临时副本和模型产物 |
| 环境 | Miniconda/Conda 可执行文件，初始化包或同平台版本的 VLStream 源码；脚本创建独立 Python 环境 |
| 网络 | 后端到 SSH/SFTP 的连通；安装时节点能访问 Conda/PyPI、PyTorch 和官方权重来源 |

在云控制台按实际 GPU 选择 Linux GPU 镜像、开机并取得 SSH 信息；自建服务器由服务器管理员安装 Linux、
NVIDIA 驱动和 OpenSSH Server，启用 SFTP，并仅向所需后端来源开放实际 SSH 端口。GPU 服务器不需要安装 VLS、
MySQL、Redis、MinIO、Java 或嵌套 Docker。平台不会因点击“保存并检查”而自动安装训练软件。

**网络边界：** 能从自己电脑 SSH 不等于 VLS 后端可以连接。请管理员从后端所在主机/容器网络验证。
普通自助接入只接受公网地址；任意内网、回环或云元数据地址不会因选择“自有服务器”就获得放行。
只有当前部署已单独批准的受信内网资产能走其专用规则，通用内网接入尚未提供自助配置。
如果你的服务器只有内网地址，先由部署管理员确认受支持的接入方案，不要关闭地址校验。

## 2. 核对 GPU、驱动和版本组合

在 GPU 服务器终端执行：

```bash
uname -srm
nvidia-smi
nvidia-smi --query-gpu=index,name,driver_version,memory.total --format=csv
command -v nohup flock
```

应看到 Linux、x86_64、真实 GPU/驱动/显存及命令路径。`nvidia-smi` 的“CUDA Version”表示驱动支持上限，
不是已安装 PyTorch 的 CUDA 运行库版本。平台探针还会实际执行 GPU 前后向和 Torchvision NMS，不能仅凭版本号判定可训练。

当前初始化脚本和环境检查接受以下组合，全部使用 **Python 3.10、Ultralytics 8.3.240**：

| 初始化配置 | PyTorch / Torchvision | PyTorch CUDA 运行库 | 选择依据 |
| --- | --- | --- | --- |
| `standard` | 2.5.1 / 0.20.1 | 12.4 (`cu124`) | 包含该 GPU 架构的较旧运行栈；已用于 P100、RTX 4080 SUPER 的独立环境，3090/4090 等仍须现场检查 |
| `blackwell28` | 2.8.0 / 0.23.0 | 12.8 (`cu128`) | 新 Blackwell GPU 的候选配置；已有独立真实 GPU 四类训练记录，全新安装仍须验证 |
| `blackwell` | 2.7.1 / 0.22.1 | 12.8 (`cu128`) | 保留的既有配置，按实际硬件/依赖验证后使用 |

“6000D”或“6000”商品名称不足以确定架构，先看真实型号及后续 `get_device_capability()`。
旧 P100 不应照搬 Blackwell 环境；已有旧 Python/CUDA 环境也不会自动满足此表，需另建兼容环境。
不要把所有卡统一升级到最新 Torch/CUDA，也不要把示例机型视为通用实测承诺。

驱动须同时支持 GPU 与选定运行库。NVIDIA 的 CUDA 12.4 GA 工具包对应 Linux 驱动为 550.54.14 起，
12.8 GA 对应 570.26 起；CUDA 12.x 的有限次版本兼容还可能允许部分旧驱动，但有功能和 PTX 限制，
不能将兼容性最低值当作所有新卡的驱动要求。云镜像优先使用供应商为该 GPU 验证的驱动。
PyTorch CUDA wheel 自带运行库，当前训练不要求另装系统 `nvcc`/CUDA Toolkit，安装 Toolkit 也不能替代驱动。

官方依据：[PyTorch 历史版本配对](https://pytorch.org/get-started/previous-versions/)、
[NVIDIA CUDA 12.8 驱动表](https://docs.nvidia.com/cuda/archive/12.8.0/cuda-toolkit-release-notes/index.html)、
[CUDA 次版本兼容限制](https://docs.nvidia.com/deploy/cuda-compatibility/minor-version-compatibility.html)。

## 3. 安装基础工具并选好工作目录

以下 Ubuntu/Debian 示例由你或服务器管理员在自己的服务器操作；云 GPU 镜像已有工具就跳过。
驱动应按供应商/NVIDIA 对实际 GPU 和系统的说明安装，勿盲目套用固定驱动包。

```bash
# 需要管理员权限；云容器没有 sudo 时请由供应商/管理员准备，已有包无需重装
sudo apt-get update
sudo apt-get install -y openssh-server ca-certificates curl unzip util-linux coreutils libgl1 libglib2.0-0
```

SSH 服务应支持当前账号的密码认证、非交互命令和 SFTP。可在**后端所在网络**测试（替换账号、端口和域名）：

```bash
ssh -p 22 trainer@gpu.example.com 'id; command -v nvidia-smi nohup flock'
sftp -P 22 trainer@gpu.example.com
```

SSH 应返回账号及命令路径，SFTP 应进入交互提示符，输入 `quit` 退出。首次连接先向服务器控制台核对主机指纹。
自助表单当前支持 SSH 密码，不支持浏览器上传私钥、SSH Agent、跳板机、交互 MFA 或任意 `-i` 参数。
仅密钥登录的机器需由部署管理员评估现有“管理员部署身份”能力；它只适用于明确批准、后端已绑定的特定资产，
默认关闭，不能在本页填一个私钥路径就启用。无需交出云厂商 Token。

若尚无 Conda，从 [Miniconda 官方下载页](https://repo.anaconda.com/miniconda/)取得 Linux x86_64 安装器并按该页校验 SHA-256，
用当前训练账号安装到自己的目录。例如已下载且校验过安装器后：

```bash
bash Miniconda3-latest-Linux-x86_64.sh -b -p "$HOME/miniconda3"
"$HOME/miniconda3/bin/conda" --version
mkdir -p "$HOME/vlstream"
test -w "$HOME/vlstream" && df -h "$HOME/vlstream"
```

应输出 Conda 版本、工作目录磁盘余量，且 `test` 成功。后续以当前账号的 `~/vlstream` 为例；命令会展开 `$HOME`，
**表单要填展开后的绝对路径**，不能填 `$HOME`、`~` 或相对路径。路径仅用英文字母、数字、`_`、`-`、`.`、`/`，
不含空格、`..` 或根目录 `/`。也可用管理员分配且归训练账号可写的持久数据盘目录。
30 GiB 只是起点，探针的 5 GiB 最低线不代表大数据集可训练；云临时盘的关机保留策略自行向供应商确认。

## 4. 准备四类训练环境和基础模型

从平台管理员取得**与部署版本一致**的 VLStream 初始化包或源码，上传并解压到 GPU 服务器。
若使用源码，进入仓库根目录；精简包至少应保留 `tools/compute/` 和训练资源目录的原相对结构。
旧版公开发布包未必包含当前三套配置，不要混用不同版本的初始化脚本与后端探针。

```bash
cd /path/to/VLStream-Cloud
# 先查看计划，不安装；路径改为你实际准备的目录和 Conda 路径
bash tools/compute/bootstrap-autodl.sh --profile standard \
  --conda "$HOME/miniconda3/bin/conda" --work-dir "$HOME/vlstream" --gpu 0 --dry-run
# 确认无误再执行。新 Blackwell 按上表将 standard 改为 blackwell28；三套选一套即可
bash tools/compute/bootstrap-autodl.sh --profile standard \
  --conda "$HOME/miniconda3/bin/conda" --work-dir "$HOME/vlstream" --gpu 0
```

脚本名称沿用 AutoDL，但通过 `--conda`、`--work-dir` 可用于满足条件的自建/其他云 Linux GPU 服务器。
不传参数的默认路径是 AutoDL 的 `/root/miniconda3/bin/conda`、`/root/autodl-tmp/vlstream`；普通账号不要照抄默认值。
脚本创建 `工作目录/envs/vls-配置名/`，不覆盖基础镜像的 base Python。
GPU 正在计算时会拒绝初始化，等待自己的任务完成，勿结束其他人的进程。

四类任务共用 PyTorch/Torchvision，分类、检测、实例分割使用 Ultralytics YOLOv8，语义分割使用 Torchvision DeepLabV3：

| 训练类型 | 运行框架 | 初始化准备的官方基础权重 |
| --- | --- | --- |
| 物体检测 | Ultralytics YOLOv8 | `models/yolov8m.pt` |
| 图像分类 | Ultralytics YOLOv8 | `models/yolov8n-cls.pt` |
| 实例分割 | Ultralytics YOLOv8 | `models/yolov8n-seg.pt` |
| 语义分割 | Torchvision DeepLabV3 ResNet50 | `models/torch/hub/checkpoints/deeplabv3_resnet50_coco-cd0a2569.pth` |

脚本固定 NumPy 1.26.4、opencv-python 4.11.0.86、Ultralytics 8.3.240 和所选 Torch 配对；
同时解析 Pillow、PyYAML、SciPy、Matplotlib、tqdm 等依赖。探针实际导入 `torch/torchvision/ultralytics/numpy/PIL/yaml/cv2/scipy`。
初始化会运行 `pip check`、下载并加载四类权重，保存 `models/presets.json` SHA-256 清单，
最后保存 `requirements-resolved-配置名.txt` 供复现。间接依赖并非全部预先锁死，不承诺任意时间/镜像完全一致。

网络需允许 Conda/PyPI 源、`download.pytorch.org`、`download.pytorch.org/models`、GitHub 官方 Ultralytics 权重与其重定向域名。
首次训练可能下载 Ultralytics 字体等资源；限制出网时需提前准备相同版本的资源。
离线环境由管理员在相同 Linux x86_64/Python/CUDA 配对环境准备完整依赖和模型缓存，核对 SHA-256 后搬运；
不能直接用 Windows wheel，也不能只复制三份 YOLO 权重而漏掉 DeepLab 权重和清单。
重复初始化只复用脚本管理的环境；未知同名目录会拒绝，失败后检查错误或另选工作目录，不删除已有用户数据。

**训练脚本由 VLS 提供。** 正式训练时后端通过 SSH/SFTP 下发本平台版本的训练执行器、任务配置及冻结数据包，
无需用户编写训练脚本或从公司测试机复制文件。用户自行准备的是服务器、依赖、权重缓存与 SSH 连接。
系统基础模型来自上述缓存；“算法指定模型”必须已在当前租户平台存储中就绪且与任务类型匹配，
仅填旧服务器的 `.pt` 路径不等于已准备模型，也不会回退到其他 GPU 读取文件。

## 5. 本机环境检查：看什么才算通过

以下仍在 GPU 服务器的源码/初始化包根目录执行，选择刚创建的 Python：

```bash
PY="$HOME/vlstream/envs/vls-standard/bin/python"
"$PY" -m pip check
"$PY" -c 'import torch,torchvision; print(torch.__version__,torchvision.__version__,torch.version.cuda); print(torch.cuda.is_available()); print(torch.cuda.get_arch_list()); print(torch.cuda.get_device_name(0),torch.cuda.get_device_capability(0))'
"$PY" VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/check_cloud_environment.py \
  --work-dir "$HOME/vlstream" --gpu 0
```

如使用 blackwell28，Python 路径相应改为 `envs/vls-blackwell28/bin/python`。
预期：`pip check` 无依赖冲突；CUDA 可用为 `True`；架构/真实型号与所选环境一致；
最后输出 `VLS_PROBE=...`，其中 `ready: true`，所有 `checks` 的 `passed` 均为 `true`。
探针验证系统/Python、工作目录/空间、依赖版本、GPU 前后向/NMS、四类权重完整性和加载，
不会安装软件，也不会取消 GPU 上已有任务。探针退出码为 0 仍需读 JSON，不能只看命令退出码。

## 6. 填写连接并保存

回到“算力管理 → 接入服务器”：

1. 填名称；按实际选择自有服务器、AutoDL 或阿里云。服务器标识/GPU型号是可选描述，真实资源以检查结果为准。
2. 填 SSH 主机名或 IPv4（不含 `ssh://`、端口或用户名）、实际端口、账号；也可粘贴 `ssh -p 端口 账号@主机`。
3. 选择“SSH 密码”并填写。保存后不回显，编辑留空保留原密码；不要把厂商 Token 填入此处。
4. 填初始化最后打印的 **Python path** 与 **Work directory**，选 `nvidia-smi` 中的 GPU 编号（0–15）。
5. 点击“保存并检查环境”。保存成功先登记节点，再进行检查；若检查失败，保留记录，可编辑修正后点“检查环境”。
6. 打开“检查结果”逐项确认，通过才显示“可训练”。修改地址、账号或环境后需重新检查。

当前用户只能查看、编辑、检查和移除自己的节点；跨租户仍拒绝。资源管理员的既有全租户管理范围由原权限控制，
这不会授予普通用户厂商 API 配置、公共发布、云端启停或收费能力。历史无主记录不会自动转到当前用户；
旧版以平台身份登记的记录保留原归属校验；记录未显示时请管理员核对原账户和归属，**新接入无需平台市场授权**。
移除只删除接入记录，不会删除第三方服务器或其文件；被活动任务引用的节点不能编辑/移除。

## 7. 训练、传输和结果验收

在“算法训练”选择已检查就绪的真实节点，选匹配算法类型的数据集，完成标注及训练/验证划分，
选择系统基础模型或已就绪的算法模型。首次用独立小样本、1 轮、batch=2、尺寸320验证所需任务类型；
显存不足再降低 batch/尺寸。环境检查并不证明模型精度或任意参数都能训练。

数据链路为：**平台对象存储 → VLS 后端 → SSH/SFTP 上传 GPU → 训练 → 后端 SFTP 取回 → 平台对象存储**。
VLS 后端必须能下载数据集和模型对象、读写临时目录并回存 MinIO/S3，GPU 节点无需配置平台存储密钥或回调入口。
完成依赖/模型/字体缓存后，核心训练数据与产物经 SSH 传输，不要求节点直接访问 MinIO 公网地址。
网络传输期间应保持实例开机和 SSH/SFTP 可达，并预留本轮独立目录及训练包解压空间。

查看排队、准备、训练、归档状态和日志，等待 PT 及类别文件回存并校验后才算完成。
下载 PT/类别文件验证可读和模型类型；按需保存到算法模型。浏览器关闭不结束任务。
回传失败可能停留“归档中”，恢复后端/SSH/存储后重试，不应因为日志写了训练完成就删除远端目录或重训。
费用、开关机和磁盘保留由你与服务器供应商管理，本页不会自动购买或启动收费实例。

## 8. 常见失败处理

| 现象 | 检查与处理 |
| --- | --- |
| SSH 超时/拒绝连接 | 核对已开机、地址/端口、安全组和防火墙；从后端网络测试；私网地址受当前接入策略限制 |
| SSH 登录或 SFTP 失败 | 用同一账号测试密码、命令和 SFTP；密钥/MFA/跳板认证不是自助表单能力，不降低服务器安全设置绕过 |
| 主机身份变化 | 向供应商控制台核对是否重建/换机；联系管理员确认，勿关闭主机验证或盲目重置指纹 |
| 找不到 Python/无权限 | 填初始化输出的绝对路径，用相同 SSH 账号执行该 Python；修正专用目录归属，勿改成全局777 |
| 版本组合不在标准内/NMS报错 | 同一独立环境中配套安装 Torch/Torchvision；勿混用 base、用户site或CPU wheel，按所选profile重新初始化 |
| `no kernel image`/CUDA不可用 | 检查真实GPU架构、驱动与wheel支持；P100与新Blackwell分别选兼容profile，不盲目安装最新版本 |
| GPU已占用/锁冲突 | 等当前任务完成后重试，不取消他人的训练或删除未知锁文件 |
| 权重缺失/校验失败 | 检查下载/磁盘，重新运行初始化的模型准备；离线包须包含四类权重和匹配清单 |
| `libGL.so.1`/GLib缺失 | 管理员准备 `libgl1`、`libglib2.0-0` 后重试导入OpenCV |
| 下载失败/证书错误 | 修复软件源、DNS、HTTPS代理或可信CA；按相同profile准备校验过的离线包，勿关闭TLS校验 |
| 磁盘不足/训练OOM | 清点自己的文件并扩容；5GiB只是环境最低线，显存不足则降低batch/尺寸 |
| 保存成功但检查未通过 | 连接记录已保留，打开检查结果；按失败项修复后重新检查，无需反复新增 |
| 训练完成但回存未完成 | 保持实例和SSH可达，检查后端MinIO权限/网络/临时盘及归档日志，待PT与类别都就绪 |

## 9. 训练与可选 OM 转换

这里准备的是四类训练环境，默认产物为 PT 和类别文件。OM 是面向指定设备/芯片的另一步转换，
需要平台管理员单独准备匹配的转换工具链、目标芯片支持和转换服务；它不是接入 GPU 或开始训练的前提。
当前可选 CPU OM 服务只覆盖已支持的 Hi3519DV500 目标检测路径，不能推断四类模型均可转 OM，
也不代表新接入 GPU 已接通 ONNX、OM 或 RKNN 全链路。不要把 OM/Ascend 工具安装成所有训练节点的必需项。

本指南依据仓库当前初始化脚本、环境探针和正式训练链路核对；新GPU、全新系统镜像及受限网络仍需现场检查和小样本验收。
