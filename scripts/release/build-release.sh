#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.2.6}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND="$ROOT/VLStream-Cloud-Backend-Server/vls-stream"
RELEASE="$ROOT/deploy/release"
OUT="${RELEASE_OUTPUT_DIR:-$ROOT/codex/release-dist}"
PACKAGE="$OUT/VLStream-Cloud-v${VERSION}"
MIGRATION="$BACKEND/ruoyi-admin/src/main/resources/db/migration"

if [[ -e "$PACKAGE" || -e "$OUT/VLStream-Cloud-v${VERSION}.zip" || -e "$OUT/VLStream-Cloud-v${VERSION}.zip.sha256" ]]; then
  echo "Release output already exists; choose a new RELEASE_OUTPUT_DIR to preserve previous evidence: $OUT" >&2
  exit 1
fi
mkdir -p "$PACKAGE/sql/init" "$PACKAGE/sql/upgrade"
for file in compose.yaml compose.external.yaml .env.example README.md README.zh-CN.md; do
  cp "$RELEASE/$file" "$PACKAGE/$file"
done
for file in 10-oortcloud-workflowforms-vls.sql 20-create-wvp-database.sh 21-import-ry-wvp.sh 21-ry-wvp.sql.inc; do
  cp "$RELEASE/sql/init/$file" "$PACKAGE/sql/init/"
done
if [ -d "$RELEASE/tunnel-runtime" ]; then
  cp -R "$RELEASE/tunnel-runtime" "$PACKAGE/tunnel-runtime"
fi

mapfile -t migrations < <(find "$MIGRATION/release" -maxdepth 1 -type f -name '*.sql' -printf '%f\n' | sort)
if [ "${#migrations[@]}" -eq 0 ]; then
  echo "No Flyway migrations were found: $MIGRATION" >&2
  exit 1
fi
for index in "${!migrations[@]}"; do
  printf -v order '%02d' "$((30 + index))"
  cp "$MIGRATION/release/${migrations[$index]}" "$PACKAGE/sql/upgrade/${order}-${migrations[$index]}"
done

cd "$OUT"
if command -v zip >/dev/null 2>&1; then
  zip -qr "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
elif command -v python3 >/dev/null 2>&1; then
  python3 -m zipfile -c "VLStream-Cloud-v${VERSION}.zip" "VLStream-Cloud-v${VERSION}"
else
  echo "zip or Python is required to build the release archive" >&2
  exit 1
fi
sha256sum "VLStream-Cloud-v${VERSION}.zip" > "VLStream-Cloud-v${VERSION}.zip.sha256"
