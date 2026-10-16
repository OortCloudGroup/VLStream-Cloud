#!/usr/bin/env python3
"""Verify that public release SQL contains only required metadata and safe presets."""

from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path


ALLOWED_INSERT_TABLES = {
    "ACT_GE_PROPERTY",
    "FLW_EV_DATABASECHANGELOG",
    "FLW_EV_DATABASECHANGELOGLOCK",
    "VLS_ALGORITHM",
    "VLS_ALGORITHM_REPOSITORY",
}
REQUIRED_METADATA_TABLES = {
    "ACT_GE_PROPERTY",
    "FLW_EV_DATABASECHANGELOG",
    "FLW_EV_DATABASECHANGELOGLOCK",
    "VLS_ALGORITHM",
    "VLS_ALGORITHM_REPOSITORY",
}
ALLOWED_WVP_INSERT_TABLES = {
    "SYS_CONFIG",
    "SYS_DEPT",
    "SYS_DICT_DATA",
    "SYS_DICT_TYPE",
    "SYS_JOB",
    "SYS_MENU",
    "SYS_POST",
    "SYS_ROLE",
    "SYS_ROLE_DEPT",
    "SYS_ROLE_MENU",
    "SYS_USER",
    "SYS_USER_ROLE",
}
WVP_BOOTSTRAP_SHA256 = "04e1324f82cc91a29b9106fa49b3de775a007c91b1295a43d0c1579443bfa416"
INSERT_PATTERN = re.compile(r"^INSERT INTO `([^`]+)`", re.IGNORECASE)
VIEW_PATTERN = re.compile(r"^(?:DROP|CREATE).*\bVIEW\b", re.IGNORECASE)


def verify_sql(path: Path, kind: str = "vls") -> None:
    """Reject unexpected inserts and source-environment markers in release SQL."""
    if kind not in {"vls", "wvp"}:
        raise ValueError(f"unsupported SQL kind: {kind}")

    if kind == "wvp":
        normalized = path.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n")
        actual = hashlib.sha256(normalized).hexdigest()
        if actual != WVP_BOOTSTRAP_SHA256:
            raise ValueError("WVP bootstrap does not match the reviewed v1.0.8 public schema")

    inserted_tables: set[str] = set()
    allowed_tables = ALLOWED_WVP_INSERT_TABLES if kind == "wvp" else ALLOWED_INSERT_TABLES
    required_tables = ALLOWED_WVP_INSERT_TABLES if kind == "wvp" else REQUIRED_METADATA_TABLES
    forbidden_markers = (
        "Source Server",
        "Source Host",
        "apaas_admin_platform",
        "ap_admin_platform_app",
    )

    with path.open("r", encoding="utf-8") as sql_file:
        for line_number, line in enumerate(sql_file, start=1):
            if any(marker in line for marker in forbidden_markers):
                raise ValueError(f"source environment marker at line {line_number}")
            if VIEW_PATTERN.match(line):
                raise ValueError(f"database view at line {line_number}")

            match = INSERT_PATTERN.match(line)
            if not match:
                continue

            table = match.group(1).upper()
            if table not in allowed_tables:
                raise ValueError(f"data insert for {table} at line {line_number}")
            inserted_tables.add(table)

    missing = required_tables - inserted_tables
    if missing:
        raise ValueError(f"missing required {kind} bootstrap inserts: {sorted(missing)}")


def main() -> int:
    """Run release SQL validation for the path supplied by the caller."""
    if len(sys.argv) == 2:
        kind, sql_path = "vls", Path(sys.argv[1])
    elif len(sys.argv) == 4 and sys.argv[1] == "--kind" and sys.argv[2] in {"vls", "wvp"}:
        kind, sql_path = sys.argv[2], Path(sys.argv[3])
    else:
        print(f"usage: {Path(sys.argv[0]).name} [--kind vls|wvp] <sql-file>", file=sys.stderr)
        return 2

    try:
        verify_sql(sql_path, kind)
    except (OSError, ValueError) as error:
        print(f"release SQL validation failed: {error}", file=sys.stderr)
        return 1

    print(f"release SQL validation passed ({kind}): {sql_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
