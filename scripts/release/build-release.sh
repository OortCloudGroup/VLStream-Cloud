#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.2.2}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND="$ROOT/VLStream-Cloud-Backend-Server/vls-stream"
RELEASE="$ROOT/deploy/release"
OUT="$ROOT/codex/release-dist"
PACKAGE="$OUT/VLStream-Cloud-v${VERSION}"
MIGRATION="$BACKEND/ruoyi-admin/src/main/resources/db/migration"

rm -rf "$OUT"
mkdir -p "$PACKAGE/sql/init" "$PACKAGE/sql/upgrade"
for file in compose.yaml compose.external.yaml .env.example README.md README.zh-CN.md; do
  cp "$RELEASE/$file" "$PACKAGE/$file"
done
cp "$RELEASE/sql/init/10-oortcloud-workflowforms-vls.sql" "$PACKAGE/sql/init/"

mapfile -t migrations < <(find "$MIGRATION" -maxdepth 1 -type f -name '*.sql' -printf '%f\n' | sort)
if [ "${#migrations[@]}" -eq 0 ]; then
  echo "No Flyway migrations were found: $MIGRATION" >&2
  exit 1
fi
for index in "${!migrations[@]}"; do
  printf -v order '%02d' "$((30 + index))"
  cp "$MIGRATION/${migrations[$index]}" "$PACKAGE/sql/upgrade/${order}-${migrations[$index]}"
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
