param(
    [string]$Version = "1.2.2"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$Backend = Join-Path $Root "VLStream-Cloud-Backend-Server/vls-stream"
$Release = Join-Path $Root "deploy/release"
$Out = Join-Path $Root "codex/release-dist"
$Package = Join-Path $Out "VLStream-Cloud-v$Version"
$Archive = Join-Path $Out "VLStream-Cloud-v$Version.zip"
$Migration = Join-Path $Backend "ruoyi-admin/src/main/resources/db/migration"

if (Test-Path -LiteralPath $Out) {
    Remove-Item -LiteralPath $Out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $Package "sql/init"), (Join-Path $Package "sql/upgrade") | Out-Null

Copy-Item -LiteralPath (Join-Path $Release "compose.yaml") -Destination $Package
Copy-Item -LiteralPath (Join-Path $Release "compose.external.yaml") -Destination $Package
Copy-Item -LiteralPath (Join-Path $Release ".env.example") -Destination $Package
Copy-Item -LiteralPath (Join-Path $Release "README.md") -Destination $Package
Copy-Item -LiteralPath (Join-Path $Release "README.zh-CN.md") -Destination $Package
Copy-Item -LiteralPath (Join-Path $Release "sql/init/10-oortcloud-workflowforms-vls.sql") -Destination (Join-Path $Package "sql/init")

$migrations = Get-ChildItem -LiteralPath $Migration -File -Filter "*.sql" | Sort-Object Name
if ($migrations.Count -eq 0) {
    throw "No Flyway migrations were found: $Migration"
}
for ($index = 0; $index -lt $migrations.Count; $index++) {
    $destination = Join-Path $Package ("sql/upgrade/{0:D2}-{1}" -f (30 + $index), $migrations[$index].Name)
    Copy-Item -LiteralPath $migrations[$index].FullName -Destination $destination
}

Compress-Archive -Path $Package -DestinationPath $Archive -CompressionLevel Optimal
$Hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Archive).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$Archive.sha256" -Value "$Hash  $(Split-Path -Leaf $Archive)" -Encoding ascii
Write-Output $Archive
