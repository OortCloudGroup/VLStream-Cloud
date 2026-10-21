# IPC 板端联调交付记录

## 2026-10-20 本机地址修复（后续记录）

本机联调入口现已迁移为 `https://192.168.75.11:8444`，Rathole 为 `192.168.75.11:2333`。
原 CA、Noise 身份及内部凭据保留，仅用原 CA/服务端私钥补发包含新 IP 的服务端证书；旧交付包的地址和激活码到期时间不能视作当前配置。
用户批准后已保持原运行 JAR/参数重启本机8080，启动 HTTPS/网关/Rathole；严格 CA/主机名验证、健康及匿名拒绝验证通过，没有消费激活码或替设备注册。
用户当前指定摄像机 `192.168.75.99:8000` 的页面实读设备编号为 `AETY-00-89NN-9AO4-00000005`，不是下文九月历史测试设备。
板端表单仍是旧平台地址，Agent未启动，尚未保存；后续须使用与当前设备编号匹配的激活码，并单独确认保存/注册。
备份、运行身份及验证限制见本地 `codex/ipc-lan-runtime/REPAIR-20261020.md`；本次不改变业务流程、租户归属或 MQTT 配置。

## 2026-09-29 历史冻结参数

2026-09-29 应设备端请求，冻结 Rathole v0.5.0 官方 AArch64 musl 制品：

- 固定提交：`ebb764ae53d7ffe4fcb45f83f7563bec5c74199d`。
- 官方下载：https://github.com/rathole-org/rathole/releases/download/v0.5.0/rathole-aarch64-unknown-linux-musl.zip
- 解压后二进制 SHA-256：`c95474d2ecdc031b3bb20c495595a0d6c6440c1ff7748f594d7ecd5315d36e8f`。
- 原始 ZIP SHA-256：`fa4a6fc63d86f8f1faa7c103a845e4715ce79a048455c0eec897b27237576564`。
- 二进制 1026216 字节，ELF64 AArch64，无动态解释器段。
- Docker ARM64 仿真执行 `--version` 成功，自报版本/提交/目标与以上信息一致，包含 `client` 和 `noise`。
- 交付文件在项目 `codex/ipc-board-handoff-20260929/`，含二进制、官方原包、SHA256SUMS 与交付说明。

用户已指定本机局域网和设备 `AETY-00-X6UN-XCWB-00000004`。实际冻结参数：

- Rathole Server：`192.168.88.45:2333`。
- Noise 公钥：`pSSsk76ZEcH36ZFu2n4t5JOTawCtt6mHSso+rc9dmnk=`。
- Agent HTTPS Base URL：`https://192.168.88.45:8444`。
- CA：交付包 `vls-ipc-lan-ca.pem`；DER SHA-256 指纹
  `C8:5B:65:79:E5:F4:D9:38:D7:00:12:B1:91:06:AA:93:43:0B:DC:7A:FD:31:9E:FC:B5:39:35:B8:43:B3:DB:BF`。
- 激活码：仅在 Git 忽略的设备专属包 `enrollment-code.json` 交付，不写入本文；2026-09-30 调整为默认有效 10 小时，注册成功立即消费。旧交付包仍以其记录的到期时间为准，需新版后端重新签发。

Docker 镜像拉取受网络超时影响，本次采用官方 Windows Rathole v0.5.0、原生编译 Go 网关及 nginx。
私钥和内部凭据保存在 `codex/ipc-lan-runtime/private/`，限制为本机用户和 SYSTEM 访问。
开发 profile 导入其中的配置，常规 IDEA 重启可继续加载；本次保留原服务 classpath 启动，关闭本次启动的
自动 Flyway 以避免顺带执行其他开发改动的迁移。未修改其他业务服务、Windows 信任库或防火墙设置。

已验证本机 LAN IP 的 HTTPS 证书/主机名校验、Agent 接口未认证返回 401、Rathole 监听及交付 ARM64
二进制的真实 Noise 控制通道。探测容器未调用 Agent 注册、未消费激活码，已停止。
设备当前状态为 `WAITING_FOR_AGENT`、路由 `APPLIED`，等待板端实际注册。
设备上报 IP `192.168.95.109` 与开发机网段不同，开发机到设备 HTTP 超时且路由经过 Mihomo，
实际板端到平台的连通性和原厂 Web 页面仍需联调。

浏览器会话域名暂用 `*.ipc.localhost:8444`，仅适用于开发机浏览器；板端 HTTPS API 使用上述固定 IP，
不依赖该域名。完整局域网多人浏览器访问需后续 DNS/CA 配置。本次不改变业务注册和租户归属规则。
