param(
    [string]$Version = "1.2.1"
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
Copy-Item (Join-Path $Root "deploy/release/README.md") (Join-Path $Package "README.md")

# Copy the sanitized complete schema and upgrade SQL in execution order.
Copy-Item (Join-Path $Backend "db/oortcloud_workflowforms_vls.sql") (Join-Path $Package "sql/init/10-oortcloud-workflowforms-vls.sql")
Copy-Item (Join-Path $Backend "doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql") (Join-Path $Package "sql/upgrade/10-blade-4.7.0-to-4.8.0.sql")
Copy-Item (Join-Path $Backend "db/2026-06-30-app-package.sql") (Join-Path $Package "sql/upgrade/20-app-package.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-source-priority-modules.sql") (Join-Path $Package "sql/upgrade/21-source-priority.sql")
Copy-Item (Join-Path $Backend "db/2026-07-15-vls-tag-management-audit-columns.sql") (Join-Path $Package "sql/upgrade/22-tag-audit.sql")
Copy-Item (Join-Path $Backend "db/2026-07-21-hisilicon-om-model-path.sql") (Join-Path $Package "sql/upgrade/23-hisilicon-om-model-path.sql")
Copy-Item (Join-Path $Backend "db/2026-07-22-base-algorithm-presets.sql") (Join-Path $Package "sql/upgrade/24-base-algorithm-presets.sql")

# Include the current Flyway migrations in execution order for release audit and offline verification.
$Migration = Join-Path $Backend "ruoyi-admin/src/main/resources/db/migration"
Copy-Item (Join-Path $Migration "V1_1_3_001__model_dispatch_protocol_v2.sql") (Join-Path $Package "sql/upgrade/30-v1.1.3-001-model-dispatch-protocol-v2.sql")
Copy-Item (Join-Path $Migration "V1_1_3_002__vls_device_event_media.sql") (Join-Path $Package "sql/upgrade/31-v1.1.3-002-vls-device-event-media.sql")
Copy-Item (Join-Path $Migration "V1_1_3_003__algorithm_training_conversion_status.sql") (Join-Path $Package "sql/upgrade/32-v1.1.3-003-algorithm-training-conversion-status.sql")
Copy-Item (Join-Path $Migration "V1_1_3_004__algorithm_library_seed.sql") (Join-Path $Package "sql/upgrade/33-v1.1.3-004-algorithm-library-seed.sql")
Copy-Item (Join-Path $Migration "V1_1_3_005__city_management_algorithm_seed.sql") (Join-Path $Package "sql/upgrade/34-v1.1.3-005-city-management-algorithm-seed.sql")
Copy-Item (Join-Path $Migration "V1_1_3_006__mqtt_device_and_stream.sql") (Join-Path $Package "sql/upgrade/35-v1.1.3-006-mqtt-device-and-stream.sql")
Copy-Item (Join-Path $Migration "V1_1_3_007__vls_device_firmware.sql") (Join-Path $Package "sql/upgrade/36-v1.1.3-007-vls-device-firmware.sql")
Copy-Item (Join-Path $Migration "V1_1_3_008__vls_device_firmware_ota.sql") (Join-Path $Package "sql/upgrade/37-v1.1.3-008-vls-device-firmware-ota.sql")
Copy-Item (Join-Path $Migration "V1_2_0_001__gpu_training_scheduler_metadata.sql") (Join-Path $Package "sql/upgrade/40-v1.2.0-001-gpu-training-scheduler-metadata.sql")
Copy-Item (Join-Path $Migration "V1_2_0_002__vls_rootfs_only_firmware.sql") (Join-Path $Package "sql/upgrade/41-v1.2.0-002-vls-rootfs-only-firmware.sql")
Copy-Item (Join-Path $Migration "V1_2_0_003__multi_tenant_shadow_identity.sql") (Join-Path $Package "sql/upgrade/42-v1.2.0-003-multi-tenant-shadow-identity.sql")
Copy-Item (Join-Path $Migration "V1_2_0_004__multi_tenant_event_report_outbox.sql") (Join-Path $Package "sql/upgrade/43-v1.2.0-004-multi-tenant-event-report-outbox.sql")

# Generate the ZIP and its SHA-256 checksum.
Compress-Archive -Path $Package -DestinationPath $Archive -CompressionLevel Optimal
$Hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Archive).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$Archive.sha256" -Value "$Hash  $(Split-Path -Leaf $Archive)" -Encoding ascii
Write-Output $Archive
