#!/bin/sh
set -eu

: "${WVP_MYSQL_DATABASE:=ry-wvp}"

MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql --protocol=socket -uroot "${WVP_MYSQL_DATABASE}" \
  < /docker-entrypoint-initdb.d/21-ry-wvp.sql.inc
