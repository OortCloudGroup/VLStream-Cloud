# 数据库自动升级说明

VLStream Cloud 使用 Flyway 管理 v1.1.2 之后的数据库结构变化。后端每次启动时会先检查数据库，只执行尚未执行过的升级脚本，然后才启动业务服务。

## v1.2.6 的双迁移历史

公开 v1.2.5 与开发主线曾为不同数据库历史使用相同的 Flyway 版本号。v1.2.6 暂时将它们放在两个互斥目录中：

- `db/migration/mainline/`：沿用当前 `origin/main` 的迁移文件名和版本，供 `dev`、`local` 等开发配置使用。
- `db/migration/release/`：从公开 v1.2.5 历史继续，供 `prod` 发布配置使用；v1.2.6 新增的生产迁移为 `V1_2_0_024` 至 `V1_2_0_028`。

`application.yml` 默认选择 `mainline`，`application-prod.yml` 显式选择 `release`。一个数据库只能使用其中一条历史，不能把两个目录合并，也不能在已有数据库上切换路径。已执行迁移的文件名和内容均保持不变。

在两条历史合并前，新数据库变更必须按实际目标环境添加到对应目录，并在各自 lineage 中保持版本唯一。发布分支需同时核对公开基线与主线基线：

```powershell
pwsh -File tools/check-database-migrations.ps1 -MainlineBaseRef origin/main -ReleaseBaseRef v1.2.5
```

## 开发时怎样新增 SQL

所有新的数据库升级脚本统一放在：

```text
VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/
```

文件名格式：

```text
V主版本_次版本_修订版本_序号__英文说明.sql
```

例如，v1.1.3 的第一、第二个数据库改动：

```text
V1_1_3_001__add_device_status.sql
V1_1_3_002__create_device_group.sql
```

版本号必须递增，两个下划线不能省略。脚本提交并随新版后端发布后，使用者只需启动或升级容器，后端便会自动执行它。

## 必须遵守的规则

1. 已经提交或发布的迁移文件禁止改名、修改或删除；需要修正时新增一个更高版本的文件。
2. 一个文件只处理一组相关的数据库变化，文件名使用英文。
3. 删除表、删除字段、批量修改业务数据等危险操作，发布前必须备份并在数据库副本上验证。
4. 不要再把新的增量 SQL 放进 `vls-stream/db/` 后期待容器自动执行；该目录中的旧文件只作为历史或手工操作参考。
5. Flyway 默认开启。只有排查紧急问题时才可临时设置 `FLYWAY_ENABLED=false`，正常发布不能关闭。

## Flyway 记录了什么

Flyway 会在业务数据库中创建 `flyway_schema_history` 表。里面记录脚本版本、文件名、校验值、执行时间和执行结果。已经成功执行的版本不会重复执行；旧文件被人修改或升级失败时，后端会停止启动，避免程序在数据库结构不完整的情况下继续运行。

v1.1.2 是本项目接入 Flyway 的基线。已有数据库第一次使用新版后端时会登记这条基线，再执行 v1.1.2 之后的迁移；全新的 Compose 数据库仍先由发布包的完整初始化 SQL 建库，再由 Flyway 补充后续变化。
