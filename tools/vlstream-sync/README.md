# VLStream同步扫描工具

该工具以单租户开源版本为业务变更源，只读扫描多租户版本，生成核心业务同步候选清单。第一阶段不会创建分支、复制文件或修改多租户仓库。

## 首次全量盘点

从 `VLStream-Cloud` 项目根目录执行：

```powershell
pwsh -NoProfile -File .\tools\vlstream-sync\Invoke-VlstreamSyncReport.ps1 -Inventory
```

报告生成到被 Git 忽略的 `codex/sync/` 目录。

## 日常增量扫描

确认 `sync-baseline.json` 中记录的提交已经完成多租户适配后，将 `confirmed` 改为 `true`，再执行：

```powershell
pwsh -NoProfile -File .\tools\vlstream-sync\Invoke-VlstreamSyncReport.ps1
```

也可以临时指定比较基线：

```powershell
pwsh -NoProfile -File .\tools\vlstream-sync\Invoke-VlstreamSyncReport.ps1 -BaseCommit <commit>
```

默认同时检查基线之后的提交和当前工作区变化。使用 `-CommittedOnly` 可忽略未提交变化。

## 配置说明

- `sync-manifest.json`：维护同步候选、明确排除、目标搜索路径和依赖风险规则。
- `sync-baseline.json`：记录单租户版本上一次已完成人工审核的提交。
- `Invoke-VlstreamSyncReport.ps1`：只读扫描两个仓库，在 `codex/sync/` 生成 Markdown 报告和 JSON 明细。

更新基线前，必须确认对应单租户提交中的核心功能已经在多租户版本完成适配和审核。
