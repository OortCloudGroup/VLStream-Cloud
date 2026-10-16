# AutoDL 训练后自动生成 Hi3519DV500 OM

## 适用范围

VLS 的可选 CPU 转换容器支持兼容 YOLOv8 Detect 头的目标检测 PT，输出用于 Hi3519DV500 SVP ACL 的 OM。分类、实例分割、语义分割、其他芯片以及不兼容的自定义检测头不在本期范围内。转换成功不代替板端加载、推理和精度验收。

AutoDL 只需要训练环境，不需要安装 ATC，也不需要访问开发用 P100。不启用转换容器时，原有 PT 训练、下载和模型保存照常使用。

## 用户操作

1. 接入 AutoDL 实例，选择目标检测数据集，正常开始训练。
2. PT 和类别文件回存 MinIO 后，启用转换服务的 VLS 自动创建 OM 转换任务。
3. 转换完成后，同一条模型版本补齐 OM 文件，在“算法模型 → 下载”中选择 **om**；无需重新训练或再新建模型。训练列表不单独增加 OM 转换列。
4. 失败时保留 PT 和具体错误；修复环境或模型问题后，可通过现有 `POST /vlsAlgorithmTraining/{id}/convert-model` 接口重试。旧任务、旧模型不会被替换。
5. 启用服务时，已完成但尚无 OM 的 AutoDL 目标检测任务也会自动进入队列。

## 私有化部署

前置条件：Linux x86_64 Docker 主机、建议至少 6 GiB 可用内存，以及合法取得的 **Hi3519DV500 SVP/ATC** 工具链。通用 Ascend ATC 不能代替本工具链。厂商文件不包含在开源仓库或镜像中。

已验证来源基线：SVP `V3.0.0.12`、ATC `6.10.t03spc010b050`。工具目录应包含：

```text
/srv/vls-svp/
  Ascend/ascend-toolkit/svp_latest/x86_64-linux/script/setenv.sh
  V3.0.0.12/sample/yolo/insert_op.cfg
```

保留工具包内部符号链接及动态库目录。可以一次性复制已有安装目录；运行时无需连接原服务器。

在仓库根目录构建公开程序镜像：

```bash
docker build -f deploy/om-converter/Dockerfile -t vlstream-om-converter:hi3519dv500 .
```

在部署目录 `.env` 增加厂商工具路径，并将本仓库 `deploy/release/compose.om.yaml` 放入部署目录：

```dotenv
VLSTREAM_SVP_DIR=/srv/vls-svp
OM_CONVERTER_IMAGE=vlstream-om-converter:hi3519dv500
```

启动时保留现有环境的所有 Compose 文件，仅追加转换组件：

```bash
docker compose -f compose.yaml -f compose.override.yaml -f compose.om.yaml up -d --no-deps backend om-converter
```

如果没有 `compose.override.yaml`，省略该参数。开发主线后端需包含 Flyway `V1_2_0_029__om_conversion_queue.sql`；v1.2.7 生产 Release 为延续公开 v1.2.6 历史，对应迁移编号是 `V1_2_0_030__om_conversion_queue.sql`。正常启动自动执行，不手工建表。转换默认关闭，叠加文件会设置 `VLSTREAM_OM_CONVERSION_ENABLED=true` 并为后端和转换容器挂载同一个 `om-work` 卷。

## 工作方式与故障恢复

- VLS 按租户校验任务及对象归属，固定本轮 PT、类别文件和训练数据包引用，从 MinIO 下载并核验哈希。
- 仅从该冻结包的训练集选择最多 20 张校准图片，不混入验证集；转换为固定 batch=1、640×640、UINT8 输入及海思 RPN 算子。
- 私有卷交付任务，容器没有对外端口、数据库或 MinIO 凭据；运行时关闭网络、限制资源，并使用受限权重加载。
- 持久队列和文件锁防止重复执行；结果原子落盘。网络/归档失败可重试，转换失败明确结束并可手动重试。
- OM 通过文件大小和 SHA-256 校验后回存 MinIO，再按原 PT 和训练版本匹配回填模型；不覆盖其他历史版本。
- 待转换期间保留数据集占用，避免源版本被删除或任务被重新训练。PT 不会因 OM 失败被删除。
- 转换日志位于 `om-work/jobs/<转换任务UUID>/conversion.log`。排队后长期不动应检查 `om-converter` 是否启动、厂商工具挂载及容器日志。
- 每次 ONNX 导出最多 15 分钟、ATC 最多 30 分钟；失败原因保留到任务。工作目录保留供排查，可在任务终态且 MinIO 归档成功后由管理员清理对应目录，不清理活动任务目录。
- 多后端副本必须访问同一工作卷和数据库；本期 Compose 面向单主机部署。转换服务停机不会丢失队列，也不会切回开发 GPU。

旧默认 GPU 的 SSH 转换链仍使用 `VLSTREAM_HISILICON_EXPORTER_SCRIPT` 等配置，不受此可选容器开关控制。本组件不增加 AutoDL ONNX/RKNN 自动转换。
