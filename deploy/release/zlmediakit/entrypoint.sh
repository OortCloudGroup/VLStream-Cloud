#!/bin/sh
set -eu

config=/opt/media/conf/config.ini
temporary="${config}.vlstream.tmp"
hook_base_url="${ZLMEDIAKIT_HOOK_BASE_URL:-http://wvp-backend:9080/index/hook}"

awk \
  -v secret="${ZLMEDIAKIT_SECRET}" \
  -v hook_base_url="${hook_base_url}" '
    /^\[/ {
      section = $0
    }
    section == "[api]" && /^secret=/ {
      print "secret=" secret
      next
    }
    section == "[hook]" && /^enable=/ {
      print "enable=1"
      next
    }
    section == "[hook]" && /^alive_interval=/ {
      print "alive_interval=10.0"
      next
    }
    section == "[hook]" && /^on_server_started=/ {
      print "on_server_started=" hook_base_url "/on_server_started"
      next
    }
    section == "[hook]" && /^on_server_keepalive=/ {
      print "on_server_keepalive=" hook_base_url "/on_server_keepalive"
      next
    }
    { print }
  ' "${config}" > "${temporary}"

mv "${temporary}" "${config}"
exec ./MediaServer -s default.pem -c ../conf/config.ini -l 0
