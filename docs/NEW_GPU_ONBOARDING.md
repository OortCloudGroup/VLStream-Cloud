# 从全新 AutoDL 普通容器实例接入 VLStream

本期仅对接 AutoDL 普通容器实例，不扩展到其他云算力平台。适用：自行私有化部署 VLStream 的用户，以及使用多租户平台、自己在 AutoDL 租用 GPU 的租户。
**项目团队的 P100 仅是开发测试设备。用户不需要、也不应连接它。** 新实例的地址、账号、环境和文件均属于用户自己。

## 先按这一条路线完成验收

本轮验收使用 **当前本地平台 `http://localhost:3000` + 用户购买的 AutoDL 实例**，实际探测报告为 RTX 4080 SUPER（计算能力8.9）。
平台不限定3090或其他单一型号；按GPU代际、驱动和所需显存选择合适环境，再通过检查及实际训练确认。型号示例不是白名单。
2026-09-30已在本地平台与用户的AutoDL vGPU-32GB实例完成本期四类训练、PT/类别回存下载、指定PT再训练及停止验收。
这不代表所有显卡已实测，也不包含线上智能标注、模型转换、设备部署或完整精度评估。

| 步骤 | 在哪里操作 | 做什么 | 完成标志 |
| --- | --- | --- | --- |
| 0. 平台准备 | VLS 管理员 | 更新前后端、执行所需 Flyway 迁移（含028）、确认 MinIO 和后端持久数据目录可用；新部署密钥自动生成 | 能打开线上算力页面，后台可正常保存实例；不能只看见页面就算完成 |
| 1. 购买并开机 | AutoDL 控制台 | 普通容器、按需选择GPU；兼容standard组合的实例可选 **PyTorch 2.5.1 / Python 3.12 / CUDA 12.4**，有卡开机；Blackwell等需选择对应组合 | 实例运行，复制得到 SSH 命令和密码 |
| 2. 初始化 | AutoDL 实例终端 | 上传与 VLS 匹配的初始化包或源码，运行下方 standard 命令 | 最后一条 `VLS_PROBE` 的 `ready` 为 `true`，记录输出的 Python 路径 |
| 3. 接入 | VLS → AI 算力调度 → 线上算力 | 接入实例，填写 SSH、Python、工作目录、GPU=0，保存并检查 | 每一项检查通过；保存记录或SSH连通本身不算训练就绪 |
| 4. 准备数据 | VLS → 算法仓库 → 数据集管理 | 先建一个“物体检测”测试数据集，导入图片、建类别、人工画框、保存标注，划分训练/验证集 | 两个集合都有有效标注，图片不能重复出现在两边 |
| 5. 建立算法任务 | VLS → 算法管理 / 算法训练 | 建立或选用“物体检测”算法，新建训练任务并关联相同类型数据集 | 任务和数据集类型匹配 |
| 6. 执行训练 | VLS → 算法训练 → 训练 | 算力选择刚接入的 **AutoDL实例**，起始模型选择“系统基础模型”；首轮用自定义参数：1轮、batch=2、输入尺寸320，开启完成后保存模型 | 请求入队，准备数据后出现真实训练日志；不要选“平台默认 GPU 服务器” |
| 7. 等待结果 | VLS → 算法训练 | 观察排队、训练、回存状态；可关闭页面再打开验证任务仍在运行 | PT和类别文件都校验回存后才显示完成，模型保存状态为已保存 |
| 8. 下载验收 | VLS → 算法模型 / 训练结果 | 下载PT，核对关联类别文件与本次数据；检查MinIO中对应产物 | 文件可读取、哈希一致、类别与本轮数据集对应 |
| 9. 继续覆盖其他类型 | 相同 VLS 流程 | 分别准备图像分类、实例分割、语义分割测试数据，再各跑一次；再测停止确认、重新训练、指定模型 | 每项有任务ID、日志、产物与结果记录，不用检测训练一次成功代替其他类型 |

首次联调建议每类至少准备 4 张训练图和 2 张验证图，确保内容不同；这只用于验证功能，不能评价模型效果。
分类需要整图类别；实例分割需要逐对象掩膜；语义分割需要完整像素类别，不能用检测框直接替代。
若希望先确认持久数据包，可在算法标注页点击“生成”并导出检查 MinIO ZIP；AutoDL 提交训练时也会固定版本并生成训练包。

本轮由开发侧执行联调时，用户购买并开机后提供 **SSH 登录命令和密码** 即可，开发侧负责上传初始化文件、执行检查和测试数据流程。
测试只新增专门的数据集/任务，不覆盖既有业务数据。完成后告知哪些已通过、哪些仍有问题，用户再决定是否关机；平台不会自动停止计费。

**本期“走完流程”的终点是模型 PT 和类别文件在平台可下载。** AutoDL 线上智能标注、ONNX/OM/RKNN转换和设备部署不在这一期的已接通范围，不能承诺买一张GPU就覆盖所有平台业务。

## 1. 购买时选择什么

购买页面的基础镜像与脚本建立的运行环境是两件事。AutoDL 当前官方列表中的 PyTorch 2.5.1 配的是 Python 3.12 / CUDA 12.4，
不是此前指南误写成的购买页 Python 3.10。初始化脚本会另外创建 Python 3.10 环境，因此选3.12基础镜像并不矛盾。

下面是**初始化后**的固定核心软件配置，不是购买页可选镜像清单，也不是已经逐款 GPU 实测认证的列表。
最终需环境检查和真实小样本训练验收；不要把购买页显示的框架版本直接当成已完成平台接入。

| 使用配置 | GPU 示例 | 框架名称 | 框架版本 | Python | CUDA | 配套 Torchvision |
| --- | --- | --- | --- | --- | --- | --- |
| `standard` | RTX 3090、4090、4090D | PyTorch | 2.5.1 | 3.10 | 12.4 | 0.20.1 |
| `blackwell` | RTX 5090、RTX PRO 6000 Blackwell | PyTorch | 2.7.1 | 3.10 | 12.8 | 0.22.1 |

两套都使用 **Ultralytics 8.3.240、NumPy 1.26.4、OpenCV 4.11.0.86**。实例分割依赖该 Ultralytics 版本内部接口。
型号名称相近不代表架构相同，例如 RTX PRO 6000 Blackwell 与旧 RTX 6000 不应混用判断。
其他 GPU、ARM、AMD、Windows 不是这份初始化脚本的标准适配范围，需另行验证；不要拿此表改动开发 P100 的现有环境。

如果 AutoDL 没有完全对应的四项组合，可选择支持该 GPU 的 Linux x86_64 Miniconda 镜像，再执行本项目初始化脚本。
脚本创建独立 Python 3.10 环境，安装明确 CUDA 构建的 PyTorch，不沿用基础镜像中不确定的框架版本。
AutoDL 宿主机驱动由服务商管理；这里选择 CUDA 运行环境，不在容器中安装或升级 NVIDIA 驱动。
`nvidia-smi` 显示的 CUDA 数字是驱动支持上限，不是当前 PyTorch 使用的 CUDA 版本。

依据：[PyTorch 官方组合](https://pytorch.org/get-started/previous-versions/)、
[Blackwell/CUDA 12.8 支持](https://pytorch.org/blog/pytorch-2-7/)、
[AutoDL Miniconda](https://api.autodl.com/docs/miniconda/)、[AutoDL CUDA 说明](https://www.autodl.com/docs/cuda/)。

## 2. 谁准备哪些东西

| 项目 | 由谁准备 | 是否依赖开发 P100 |
| --- | --- | --- |
| GPU、驱动、公网 SSH、有卡开机 | 用户与 AutoDL | 否 |
| Python、PyTorch/Torchvision、Ultralytics 和辅助库 | 用户在新实例执行初始化脚本 | 否 |
| 四类系统基础权重 | 初始化从官方来源下载到新实例并缓存 | 否 |
| 图片、标注、固定版本训练包 | VLS 存 MinIO，训练时上传工作副本 | 否 |
| 平台训练脚本 | VLS 从自身资源包上传 | 否 |
| 用户指定 PT | 必须先在当前租户 MinIO 中就绪，VLS 校验后上传 | 否，不回退旧 SSH |
| 训练结果 PT/类别文件 | 新实例产生，VLS 校验后回存 MinIO | 否 |
| OM/RKNN 等部署格式工具链 | 可选、单独的转换环境 | 不包含在本次初始化中 |

`ptModelFilePath` 是算法记录中 `pt_model_file_path` 的 Java/API 字段，**不是环境变量**。
历史配置可能写着 `/data/work/.../model.pt`，该路径不代表新实例也有文件。新云训练可显式选择“系统基础模型”，
即使算法历史路径非空也不会使用它；选择“算法指定模型”时，必须先在当前租户 MinIO 中找到 READY 对象。
仅仅在字段中填写文件名、旧服务器路径或导入模型记录，并不等于该算法已经绑定了可读取的起始权重。

系统基础模型当前为检测 `yolov8m.pt`、分类 `yolov8n-cls.pt`、实例分割 `yolov8n-seg.pt`，以及语义分割
DeepLabV3 ResNet50。用户自有模型必须能被对应运行时加载并与任务类型匹配；任意自定义网络结构不能仅凭 `.pt` 后缀直接训练。

## 3. 新实例的前置条件

- 有卡开机，Linux x86_64，能运行 `nvidia-smi`，SSH/SFTP 可从 **VLS 后端所在网络**访问。
  多租户用户自己的电脑能 SSH，不代表平台后端也能连通。
- 已有 Miniconda，AutoDL 通常位于 `/root/miniconda3/bin/conda`；具有写工作目录和创建环境的权限。
- 预留依赖、四类基础权重、数据集和训练输出空间。建议至少 30 GiB 空闲作为起点，真实需求随数据集增长；
  环境检查的 5 GiB 是最低拒绝线，不是训练容量承诺。
- 初始化期间需要 HTTPS 访问软件源、`download.pytorch.org` 和官方 YOLO 权重下载来源（GitHub/其重定向下载域名）。
  网络受限时先在同环境下载缓存，再搬运完整 `models/`；不要使用另一版本或不同来源的同名文件冒充。
- 环境需提供 `nohup`、`flock` 等标准 Linux 工具。若导入 OpenCV 报缺少 `libGL.so.1` 或 GLib，Ubuntu 管理员可安装
  `libgl1`、`libglib2.0-0` 后重试；脚本不隐式修改系统驱动或全局软件包。
- 私有化管理员先部署 VLS、MySQL、MinIO、相关迁移，并保留后端持久数据卷。新部署的算力密钥自动生成；已有显式密钥继续沿用。
  租户使用托管平台时不需要自己部署数据库、MinIO，也不需要把它们的密钥交给 GPU 实例。

## 4. 初始化命令

先从平台管理员取得**与平台部署版本一致的初始化包**，或使用包含本脚本的 VLStream 源码。
当前开发联调包由开发侧提供；这些改动发布前，不要假定旧版公开 Release 已包含脚本。

初始化包方式（不用把 VLS 后端、MySQL 或 MinIO 安装到 GPU 实例）：

1. 在 AutoDL 实例卡片打开 JupyterLab，进入 `/root/autodl-tmp`，上传 `vlstream-autodl-setup.zip`。
2. 打开该实例的终端，执行下方命令。也可通过 SSH/SFTP 上传到同一目录。

```bash
cd /root/autodl-tmp
/root/miniconda3/bin/python -m zipfile -e vlstream-autodl-setup.zip .
cd vlstream-autodl-setup
bash tools/compute/bootstrap-autodl.sh --profile standard
```

如果使用完整源码，则进入解压后的仓库根目录运行相同脚本。以下是可选的预览及配置命令：

```bash
# 先查看计划，不安装软件、不下载权重、不连接 GPU
bash tools/compute/bootstrap-autodl.sh --profile standard --dry-run

# 3090 / 4090 / 4090D
bash tools/compute/bootstrap-autodl.sh --profile standard

# 5090 / PRO 6000 Blackwell 则改用这一项，不需要把两项都执行
bash tools/compute/bootstrap-autodl.sh --profile blackwell
```

可用 `--work-dir /absolute/path`、`--conda /absolute/path/to/conda`、`--gpu 0` 覆盖位置及检查 GPU。
默认工作目录 `/root/autodl-tmp/vlstream`；两套环境分别放在其 `envs/vls-standard` 与 `envs/vls-blackwell` 中。
**不会覆盖基础镜像的 base 环境，不使用 `/data/work/anaconda3` 等开发机私有目录。**

重复运行会复用脚本管理的环境，保持核心版本约束、补齐依赖并检查权重；不删除已有数据/任务/模型。
非本脚本管理的同名环境目录会被拒绝。不同 GPU 配置用不同环境目录。首次 Conda 建环境中断且缺少 Python 时，
应排查报错后使用新的工作目录，脚本不会为恢复而删除未知目录。
核心版本固定，依赖解析出的完整版本保存在 `requirements-resolved-standard.txt` 或对应 blackwell 文件；该文件用于留档复现，
不要声称跨时间安装的所有间接依赖都由此脚本预先锁死。

首次安装会下载数GB软件包；PyTorch专用CUDA轮子从官方源获取，其余依赖使用实例配置的软件源。
网络较慢时，可由管理员提供与所选配置一致的Linux离线依赖包，校验SHA-256后安装，再重复运行初始化脚本。
不要把Windows依赖包直接放入Linux实例。首次训练还可能下载Ultralytics绘图字体，实例需能访问对应资源。

脚本会安装依赖、预下载并加载四类基础模型、记录 SHA-256 清单，然后检查 GPU 前后向运算和 Torchvision NMS。
检测到 GPU 正在计算时不停止别人的任务，需空闲后再执行。通过后输出供平台填写的路径，例如：

```text
Python: /root/autodl-tmp/vlstream/envs/vls-standard/bin/python
工作目录: /root/autodl-tmp/vlstream
```

基础模型放在 `models/`，Torchvision 缓存为 `models/torch/`；后续云训练使用同一缓存，不再每轮从开发机取模型。
离线检查可直接执行：

```bash
/root/autodl-tmp/vlstream/envs/vls-standard/bin/python \
  VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/check_cloud_environment.py \
  --work-dir /root/autodl-tmp/vlstream --gpu 0
```

## 5. 平台接入与验收

1. “AI 算力调度 → 线上算力 → 新 GPU 接入指南”可查看组合，再点击“接入实例”。
2. 填入 AutoDL 的 SSH 地址、端口、账号、密码，以及**初始化最后输出的 Python 路径和工作目录**。
3. 检查结果逐项显示系统/Python、版本组合、空间、GPU运算及四类权重。旧版只检查import的READY记录需重新检查。
4. 算法训练选择该实例，再选择“系统基础模型”或“算法指定模型（必须已存入 MinIO）”。
5. 用独立且正确划分的小数据集分别验收所需训练类型，确认日志、进度、停止确认、PT 和类别文件回存及下载。
   环境探测包含极小的张量运算，不等同真实训练验收，也不验证模型精度或最大可用 batch。

本期 AutoDL 提供四类正式训练。线上智能标注、自动租用/开关机以及 ONNX/OM/RKNN 转换仍不在此初始化范围；
不能因为依赖安装成功就宣称这些功能已接通。旧默认 GPU、旧模型归档和转换兼容路径依然存在，服务于原部署，
不是公共接入流程的必需条件。

2026-09-30验收：驱动识别RTX4080SUPER，Python3.10.21、Torch2.5.1+cu124、Torchvision0.20.1+cu124、Ultralytics8.3.240。
四类各1轮及指定检测PT再次训练均完成；PT/类别从训练与模型入口下载哈希一致，四类PT可重新加载；排队取消和运行中停止通过。
详细图文和日志位于本地`codex/autodl-acceptance/AutoDL闭环测试图文记录.md`。训练列表详细评估指标尚未接通本轮云训练回写，
合成小样本不证明业务精度；不把本次结果扩展为所有GPU/模型/故障场景都已验收。
