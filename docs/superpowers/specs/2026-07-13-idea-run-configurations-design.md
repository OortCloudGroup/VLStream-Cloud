# IDEA 前后端启动配置设计

## 背景与根因

项目根目录同时容纳前端与后端源码。此前清理旧项目目录后，根目录 `.idea/misc.xml` 仍引用已经删除的 `VLStream-Cloud-Backend-Server/vls-server/pom.xml`，导致 IDEA 无法正确导入当前 Maven 工程。根项目也没有共享的运行配置，因此无法直接启动前端、后端或同时启动两者。

## 目标

- IDEA 将 `VLStream-Cloud-Backend-Server/vls-stream/pom.xml` 识别为后端 Maven 根工程。
- 后端使用 JDK 8，通过 `com.ruoyi.RuoYiApplication` 启动。
- 前端通过 `VLStream-Web/VLStream-ui/package.json` 的 `dev` 脚本启动。
- 提供一个 Compound 配置，同时启动前端和后端。
- 本次只保证 IDEA 能发起两个进程；数据库、Redis、内网 Maven 仓库等外部依赖错误不纳入本次修复。

## 方案

采用根目录共享 `.run` 配置：分别创建 Spring Boot 后端配置、npm 前端配置和引用二者的 Compound 配置。修正根 `.idea/misc.xml` 中的 Maven 工程路径，并清理只属于旧 `vls-server` 模块的编译器配置。仓库迁移后的实际后端目录是 `vls-stream`；先前候选目录 `apaas-workflowforms` 当前不存在，因此不作为运行目标。

不采用纯 Maven 命令配置，因为它不利于后端 Java 断点调试；不采用 PowerShell 启动脚本，因为目标是获得 IDEA 原生启动体验。

## 配置组成

1. Maven 导入配置指向当前 `vls-stream/pom.xml`，项目 SDK 保持 JDK 8。
2. 后端运行配置以 `ruoyi-admin` 为模块，主类为 `com.ruoyi.RuoYiApplication`，工作目录为后端 Maven 根目录。
3. 前端运行配置以 `VLStream-ui/package.json` 为包清单，执行 `dev` 脚本。
4. Compound 配置同时引用前端与后端配置，不复制二者参数。

## 错误边界

运行配置只解决 IDEA 工程识别和进程启动入口。若 Maven 依赖解析失败，应检查内网或 VPN；若 Spring Boot 启动后连接数据库或 Redis 失败，应作为独立环境问题继续诊断。配置中不写入密码、令牌或机器专属的外部服务凭据。

## 验证

在项目根目录 `codex` 下放置验证脚本和输出。验证脚本检查：

- Maven 路径存在且不再引用 `vls-server`。
- JDK 版本配置仍为 1.8。
- 后端主类、模块和工作目录正确。
- 前端配置引用正确的 `package.json` 和 `dev` 脚本。
- Compound 配置完整引用前端与后端配置。
- 所有新增或修改的 XML 均可解析。

先在旧配置上运行验证并确认失败，再实施最小配置修改并确认验证通过。最后分别从命令行进行有限的前端和后端启动检查，以区分运行配置问题与外部依赖问题。
