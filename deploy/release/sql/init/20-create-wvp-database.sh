#!/bin/sh
set -eu

: "${WVP_MYSQL_DATABASE:=ry-wvp}"
: "${WVP_MYSQL_USER:=wvp}"
: "${WVP_MYSQL_PASSWORD:?WVP_MYSQL_PASSWORD must be set in .env}"

case "$WVP_MYSQL_DATABASE$WVP_MYSQL_USER$WVP_MYSQL_PASSWORD" in
  *[!A-Za-z0-9_-]*)
    echo "WVP database name, user, and password may contain only letters, digits, underscore, and hyphen." >&2
    exit 1
    ;;
esac

MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql --protocol=socket -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${WVP_MYSQL_DATABASE}\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${WVP_MYSQL_USER}'@'%' IDENTIFIED BY '${WVP_MYSQL_PASSWORD}';
ALTER USER '${WVP_MYSQL_USER}'@'%' IDENTIFIED BY '${WVP_MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${WVP_MYSQL_DATABASE}\`.* TO '${WVP_MYSQL_USER}'@'%';
GRANT SELECT ON performance_schema.user_variables_by_thread TO '${WVP_MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
