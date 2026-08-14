#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.2.0}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND="$ROOT/VLStream-Cloud-Backend-Server/vls-stream"
OUT="$ROOT/codex/release-dist"
PACKAGE="$OUT/VLStream-Cloud-v${VERSION}"

rm -rf "$OUT"
mkdir -p "$PACKAGE/sql/init" "$PACKAGE/sql/upgrade"
cp "$ROOT/deploy/release/compose.yaml" "$PACKAGE/compose.yaml"
cp "$ROOT/deploy/release/compose.external.yaml" "$PACKAGE/compose.external.yaml"
cp "$ROOT/deploy/release/.env.example" "$PACKAGE/.env.example"
cp "$ROOT/deploy/release/README.zh-CN.md" "$PACKAGE/README.zh-CN.md"
cp "$ROOT/deploy/release/README.md" "$PACKAGE/README.md"
cp -R "$ROOT/deploy/release/zlmediakit" "$PACKAGE/zlmediakit"
cp "$BACKEND/db/oortcloud_workflowforms_vls.sql" "$PACKAGE/sql/init/10-oortcloud-workflowforms-vls.sql"
cp "$ROOT/deploy/release/sql/init/20-create-wvp-database.sh" "$PACKAGE/sql/init/20-create-wvp-database.sh"
cp "$BACKEND/doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql" "$PACKAGE/sql/upgrade/10-blade-4.7.0-to-4.8.0.sql"
cp "$BACKEND/db/2026-06-30-app-package.sql" "$PACKAGE/sql/upgrade/20-app-package.sql"
cp "$BACKEND/db/2026-07-15-vls-source-priority-modules.sql" "$PACKAGE/sql/upgrade/21-source-priority.sql"
cp "$BACKEND/db/2026-07-15-vls-tag-management-audit-columns.sql" "$PACKAGE/sql/upgrade/22-tag-audit.sql"
cp "$BACKEND/db/2026-07-21-hisilicon-om-model-path.sql" "$PACKAGE/sql/upgrade/23-hisilicon-om-model-path.sql"
cp "$BACKEND/db/2026-07-22-base-algorithm-presets.sql" "$PACKAGE/sql/upgrade/24-base-algorithm-presets.sql"
MIGRATION="$BACKEND/ruoyi-admin/src/main/resources/db/migration"
cp "$MIGRATION/V1_1_3_001__model_dispatch_protocol_v2.sql" "$PACKAGE/sql/upgrade/30-v1.1.3-001-model-dispatch-protocol-v2.sql"
cp "$MIGRATION/V1_1_3_002__vls_device_event_media.sql" "$PACKAGE/sql/upgrade/31-v1.1.3-002-vls-device-event-media.sql"
cp "$MIGRATION/V1_1_3_003__algorithm_training_conversion_status.sql" "$PACKAGE/sql/upgrade/32-v1.1.3-003-algorithm-training-conversion-status.sql"
cp "$MIGRATION/V1_1_3_004__algorithm_library_seed.sql" "$PACKAGE/sql/upgrade/33-v1.1.3-004-algorithm-library-seed.sql"
cp "$MIGRATION/V1_1_3_005__city_management_algorithm_seed.sql" "$PACKAGE/sql/upgrade/34-v1.1.3-005-city-management-algorithm-seed.sql"
cp "$MIGRATION/V1_1_3_006__mqtt_device_and_stream.sql" "$PACKAGE/sql/upgrade/35-v1.1.3-006-mqtt-device-and-stream.sql"

cd "$OUT"
if command -v zip >/dev/null 2>&1; then
  zip -qr "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
elif command -v python3 >/dev/null 2>&1; then
  python3 -m zipfile -c "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
elif command -v python >/dev/null 2>&1; then
  python -m zipfile -c "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
else
  echo "zip or Python is required to build the release archive" >&2
  exit 1
fi
sha256sum "VLStream-Cloud-v${VERSION}.zip" > "VLStream-Cloud-v${VERSION}.zip.sha256"
