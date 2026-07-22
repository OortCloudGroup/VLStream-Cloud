#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.1.1}"
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
cp "$BACKEND/db/oortcloud_workflowforms_vls.sql" "$PACKAGE/sql/init/10-oortcloud-workflowforms-vls.sql"
cp "$BACKEND/doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql" "$PACKAGE/sql/upgrade/10-blade-4.7.0-to-4.8.0.sql"
cp "$BACKEND/db/2026-06-30-app-package.sql" "$PACKAGE/sql/upgrade/20-app-package.sql"
cp "$BACKEND/db/2026-07-15-vls-source-priority-modules.sql" "$PACKAGE/sql/upgrade/21-source-priority.sql"
cp "$BACKEND/db/2026-07-15-vls-tag-management-audit-columns.sql" "$PACKAGE/sql/upgrade/22-tag-audit.sql"
cp "$BACKEND/db/2026-07-21-hisilicon-om-model-path.sql" "$PACKAGE/sql/upgrade/23-hisilicon-om-model-path.sql"

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
