# Build only with the output of npm run build:test.
ARG BASE_IMAGE=hb.oortcloudsmart.com/zglt-1742612481/vls-ui:261019160953
FROM ${BASE_IMAGE}
COPY deploy/test/nginx-webrtc-sdp.inc /etc/nginx/vls-test-webrtc-sdp.inc
RUN test "$(grep -c '^[[:space:]]*server_name[[:space:]]*localhost;[[:space:]]*$' /etc/nginx/nginx.conf)" = 1 \
    && cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.before-test-webrtc \
    && sed -i '/^[[:space:]]*server_name[[:space:]]*localhost;[[:space:]]*$/a\        include /etc/nginx/vls-test-webrtc-sdp.inc;' /etc/nginx/nginx.conf
COPY dist/ /opt/html/
