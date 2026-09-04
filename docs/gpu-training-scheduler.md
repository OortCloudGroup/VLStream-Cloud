# 单节点 GPU 训练调度

当前实现面向一台物理训练服务器、一张 NVIDIA GPU。算法训练不再直接在 SSH
会话中启动 Conda 进程，而是创建临时 Docker 容器：

1. 点击“开始训练”后写入持久化队列。
2. GPU 空闲时，最早排队的任务独占 GPU 0 并启动训练容器。
3. 训练数据、日志和模型目录通过 `/data/work` 挂载到宿主机。
4. 容器退出后自动删除；任务记录、日志和模型文件保留。
5. 下一个排队任务自动启动。

部署前执行：

```sql
source VLStream-Cloud-Backend-Server/vls-stream/doc/sql/2026-07-23-gpu-training-scheduler.sql;
```

GPU 服务器需要 Docker、NVIDIA 驱动和 NVIDIA Container Toolkit，并预先准备
`vlstream/yolo-training:8.3.240` 镜像。容器复用宿主机
`/data/work/anaconda3/envs/yolo8` 环境以及训练工作目录。

可配置环境变量：

```bash
VLSTREAM_TRAINING_CONTAINER_IMAGE=vlstream/yolo-training:8.3.240
VLSTREAM_TRAINING_GPU_INDEX=0
VLSTREAM_TRAINING_GPU_UUID=GPU-46808c9f-cd48-1feb-5304-5342999ca622
VLSTREAM_TRAINING_CPU_LIMIT=12
VLSTREAM_TRAINING_MEMORY_LIMIT=12g
VLSTREAM_TRAINING_HOST_DATA_DIR=/data/work
VLSTREAM_TRAINING_SCHEDULE_INTERVAL_MILLIS=3000
```

`AI算力调度 → 容器实例` 页面是训练容器任务看板，不提供人工创建容器。页面的
服务器、GPU 型号、显存、利用率、忙闲状态和排队数均来自当前 SSH 训练服务器。
