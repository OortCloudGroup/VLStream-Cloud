# OA-env test frontend only. The gateway removes /bus/vls-ui before proxying.
# This endpoint only exchanges an SDP offer for an existing WVP preview stream.
location = /zlm/index/api/webrtc {
    if ($request_method != POST) { return 405; }
    if ($arg_type != play) { return 400; }
    if ($arg_app != rtsp_preview) { return 400; }
    if ($arg_stream !~ "^(rtsp|onvif)_device_[0-9]+_[0-9a-f]{32}$") { return 400; }

    client_max_body_size 64k;
    proxy_http_version 1.1;
    proxy_pass_request_headers off;
    proxy_set_header Host apaas-zlmediakit;
    proxy_set_header Content-Type "text/plain;charset=UTF-8";
    proxy_set_header Connection "";
    # Preserve the WVP-created random stream ID; never forward management credentials,
    # browser cookies, platform identity headers, or caller-selected API operations.
    set $args "app=rtsp_preview&stream=$arg_stream&type=play&cand_tcp=192.168.60.75:32742";
    proxy_pass http://apaas-zlmediakit:6080/index/api/webrtc;
    proxy_buffering off;
    proxy_read_timeout 30s;
    proxy_send_timeout 30s;
}

# The static SPA fallback must not turn other media API paths into routes.
location ^~ /zlm/ {
    return 404;
}
