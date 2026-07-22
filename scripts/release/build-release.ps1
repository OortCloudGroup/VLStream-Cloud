param(
    [string]$Version = "1.1.1"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$Backend = Join-Path $Root "VLStream-Cloud-Backend-Server/vls-stream"
$Out = Join-Path $Root "codex/release-dist"
$Package = Join-Path $Out "VLStream-Cloud-v$Version"
$Archive = Join-Path $Out "VLStream-Cloud-v$Version.zip"

# Recreate the staging directory so stale release files cannot leak in.
if (Test-Path -LiteralPath $Out) {
    Remove-Item -LiteralPath $Out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $Package "sql/init"), (Join-Path $Package "sql/upgrade") | Out-Null

# Copy deployment entry points and documentation.
Copy-Item (Join-Path $Root "deploy/release/compose.yaml") (Join-Path $Package "compose.yaml")
Copy-Item (Join-Path $Root "deploy/release/compose.external.yaml") (Join-Path $Package "compose.external.yaml")
Copy-Item (Join-Path $Root "deploy/release/.env.example") (Join-Path $Package ".env.example")
Copy-Item (Join-Path $Root "deploy/release/README.zh-CN.md") (Join-Path $Package "README.zh-CN.md")

# Copy the sanitized complete schema and upgrade SQL in execution order.
Copy-Item (Join-Path $Backend "db/oortcloud_workflowforms_vls.sql") (Join-Path $Package "sql/init/10-oortcloud-workflowforms-vls.sql")
Copy-Item (Join-Path $Backend "doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql") (Join-Path $Package "sql/upgrade/10-blade-4.7.0-to-4.8.0.sql")
Copy-Item (Join-Path $Backend "db/2026-06-30-app-package.sql") (Join-Path $Package "sql/upgrade/20-app-package.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-source-priority-modules.sql") (Join-Path $Package "sql/upgrade/21-source-priority.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-tag-management-audit-columns.sql") (Join-Path $Package "sql/upgrade/22-tag-audit.sql")
Copy-Item (Join-Path $Backend "db/2026-07-21-hisilicon-om-model-path.sql") (Join-Path $Package "sql/upgrade/23-hisilicon-om-model-path.sql")

# Generate the ZIP and its SHA-256 checksum.
Compress-Archive -Path $Package -DestinationPath $Archive -CompressionLevel Optimal
$Hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Archive).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$Archive.sha256" -Value "$Hash  $(Split-Path -Leaf $Archive)" -Encoding ascii
Write-Output $Archive
