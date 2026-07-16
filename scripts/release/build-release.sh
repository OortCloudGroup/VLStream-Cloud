#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"
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
cp "$BACKEND/doc/sql/blade.mysql.all.create.sql" "$PACKAGE/sql/init/10-blade.sql"
cp "$BACKEND/doc/sql/vls_stream.sql" "$PACKAGE/sql/init/20-vls-stream.sql"
cp "$BACKEND/doc/sql/blade.mysql.upgrade.4.7.0.to.4.8.0.sql" "$PACKAGE/sql/upgrade/10-blade-4.7.0-to-4.8.0.sql"
cp "$BACKEND/db/2026-06-30-app-package.sql" "$PACKAGE/sql/upgrade/20-app-package.sql"
cp "$BACKEND/db/2026-07-15-vls-source-priority-modules.sql" "$PACKAGE/sql/upgrade/21-source-priority.sql"
cp "$BACKEND/db/2026-07-15-vls-tag-management-audit-columns.sql" "$PACKAGE/sql/upgrade/22-tag-audit.sql"

cd "$OUT"
zip -qr "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
sha256sum "VLStream-Cloud-v${VERSION}.zip" > "VLStream-Cloud-v${VERSION}.zip.sha256"
