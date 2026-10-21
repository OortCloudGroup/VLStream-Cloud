# OA-env test frontend WebRTC signaling

Scope: `OA-env / zglt-1742612481 / Deployment vls-ui` only.
Previous image: `hb.oortcloudsmart.com/zglt-1742612481/vls-ui:261019160953`.
Previous Pod: `vls-ui-67cc777c-8pcl5`.

The test build uses:

```dotenv
VITE_ZLM_API_BASE_URL=/bus/vls-ui/zlm
VITE_ZLM_RTC_CAND_TCP=192.168.60.75:32742
```

No UDP candidate is configured. Production configuration must retain its existing
values. These are Vite build-time settings, not runtime container variables.

From the UI project root, run `npm run build:test`, then build with
`docker build -f deploy/test/Dockerfile -t <new-test-image> .`.
The Dockerfile-specific ignore file excludes source environment files and
unrelated build context. Its base is the previous test image, preserving existing
Nginx behavior and startup settings while adding one server-scoped include.

The gateway removes `/bus/vls-ui`: the live ZLM client asset exists under
`/opt/html/assets/` and not `/opt/html/bus/vls-ui/assets/`, and the live Nginx
configuration serves `/opt/html` without that prefix. Thus the public SDP URL
maps to the exact internal `/zlm/index/api/webrtc` location.

The proxy accepts only POST, `type=play`, `app=rtsp_preview`, and the random
RTSP/ONVIF stream format issued by the existing WVP preview service. It rebuilds
the upstream query with the validated stream and verified TCP candidate. Browser
cookies, Authorization, and platform identity headers are not forwarded. It does
not add API secrets or expose ZLM configuration or management endpoints. Existing
WVP create/renew/stop authorization and ZLM playback hooks remain responsible for
their original checks; this proxy does not replace those checks.

Validation completed locally:

- `npm run build:test`: passed (38.09 seconds; existing chunk-size warnings).
- `node tools/test-zlm-candidates.mjs`: passed.
- Built `ZLMRTCClient-fdd0b41a.js`: verified test proxy and TCP values; no UDP value.
- Downloaded live Nginx config MD5: `13af22b26b98edee8c973b662b0b0d33`.
- Nginx syntax and seven isolated forwarding/rejection checks: passed using the
  exact downloaded live config plus the proposed include. All test containers
  and their network were removed after the checks.

The first local syntax test caught include insertion into commented example
servers; the Dockerfile now matches exactly one active localhost server line.
No cluster configuration was applied during that failed local check.

Deployment is pending registry access. Pulling the previous image from
`hb.oortcloudsmart.com` failed with a TLS handshake timeout; a host-side HTTPS
probe also timed out. No network settings, DNS mappings, TLS validation, cluster
image, media server configuration, or heartbeat were changed to work around it.

After registry access is restored or an authorized existing build pipeline is
identified: build/push one unique test image, update only `vls-ui`, wait for Ready,
verify the new public asset hash and method/API restrictions, then use one new
owned preview session to verify the same-origin SDP answer, selected TCP
candidate, increasing decoded video frames, and successful owner-session DELETE.
Rollback is the recorded previous image; the exact prior Nginx config is backed
up separately. End-to-end playback and session release have not yet been tested.

The existing preview business flow was reviewed; this work changes only the
test environment signaling route and candidate configuration.
