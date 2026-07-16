param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$Backend = Join-Path $Root "VLStream-Cloud-Backend-Server/vls-stream"
$Out = Join-Path $Root "codex/release-dist"
$Package = Join-Path $Out "VLStream-Cloud-v$Version"
$Archive = Join-Path $Out "VLStream-Cloud-v$Version.zip"

# 重新创建发布暂存区，防止旧版本文件混入附件。
if (Test-Path -LiteralPath $Out) {
    Remove-Item -LiteralPath $Out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $Package "sql/init"), (Join-Path $Package "sql/upgrade") | Out-Null

# 复制部署入口和文档。
Copy-Item (Join-Path $Root "deploy/release/compose.yaml") (Join-Path $Package "compose.yaml")
Copy-Item (Join-Path $Root "deploy/release/compose.external.yaml") (Join-Path $Package "compose.external.yaml")
Copy-Item (Join-Path $Root "deploy/release/.env.example") (Join-Path $Package ".env.example")
Copy-Item (Join-Path $Root "deploy/release/README.zh-CN.md") (Join-Path $Package "README.zh-CN.md")

# 按执行顺序复制全新初始化和升级 SQL。
Copy-Item (Join-Path $Backend "doc/sql/blade.mysql.all.create.sql") (Join-Path $Package "sql/init/10-blade.sql")
Copy-Item (Join-Path $Backend "doc/sql/vls_stream.sql") (Join-Path $Package "sql/init/20-vls-stream.sql")
Copy-Item (Join-Path $Backend "doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql") (Join-Path $Package "sql/upgrade/10-blade-4.7.0-to-4.8.0.sql")
Copy-Item (Join-Path $Backend "db/2026-06-30-app-package.sql") (Join-Path $Package "sql/upgrade/20-app-package.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-source-priority-modules.sql") (Join-Path $Package "sql/upgrade/21-source-priority.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-tag-management-audit-columns.sql") (Join-Path $Package "sql/upgrade/22-tag-audit.sql")

# 生成 ZIP 与 SHA-256 校验文件。
Compress-Archive -Path $Package -DestinationPath $Archive -CompressionLevel Optimal
$Hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Archive).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$Archive.sha256" -Value "$Hash  $(Split-Path -Leaf $Archive)" -Encoding ascii
Write-Output $Archive
