#!/usr/bin/env python3
"""Verify that public release SQL contains only required metadata and safe presets."""

from __future__ import annotations

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
INSERT_PATTERN = re.compile(r"^INSERT INTO `([^`]+)`", re.IGNORECASE)
VIEW_PATTERN = re.compile(r"^(?:DROP|CREATE).*\bVIEW\b", re.IGNORECASE)


def verify_sql(path: Path) -> None:
    """Reject unexpected inserts and source-environment markers in release SQL."""
    inserted_tables: set[str] = set()
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
            if table not in ALLOWED_INSERT_TABLES:
                raise ValueError(f"data insert for {table} at line {line_number}")
            inserted_tables.add(table)

    missing = REQUIRED_METADATA_TABLES - inserted_tables
    if missing:
        raise ValueError(f"missing Flowable metadata inserts: {sorted(missing)}")


def main() -> int:
    """Run release SQL validation for the path supplied by the caller."""
    if len(sys.argv) != 2:
        print(f"usage: {Path(sys.argv[0]).name} <sql-file>", file=sys.stderr)
        return 2

    sql_path = Path(sys.argv[1])
    try:
        verify_sql(sql_path)
    except (OSError, ValueError) as error:
        print(f"release SQL validation failed: {error}", file=sys.stderr)
        return 1

    print(f"release SQL validation passed: {sql_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
