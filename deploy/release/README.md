# VLStream Cloud Deployment Guide

This directory is the source template for the deployment package published with
each GitHub Release. For a one-command installation, download the release
archive because it also contains the complete database initialization SQL.

## Quick Start

```powershell
Copy-Item .env.example .env
docker compose up -d
```

- Web: `http://localhost/bus/vls-ui/`
- Placeholder public URL: `https://www.example.com/bus/vls-ui/`
- Default account: `admin`
- Default password: `Codex@123456`

Change every password in `.env` before startup and change the application
password immediately after the first sign-in.

WVP is a required dependency and the sole video-device center. Deploy
[VLStream WVP Lite v1.0.11](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.11)
first; its public image is `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.11`.
This package does not bundle a second WVP copy. On the same host, configure
WVP's HTTP host port as `9080`. The backend uses `VLSTREAM_WVP_DEVICE_BASE_URL`
for WVP device and tenant APIs. The frontend proxy destinations are configured
separately with `WVP_UPSTREAM` and `ZLM_UPSTREAM`; their defaults target host
ports 9080 and 8081. For a multi-host deployment, set addresses reachable from
the backend and frontend containers. WVP remains the owner of ZLMediaKit.
WVP v1.0.11 supports cross-protocol source grants used by Cloud AI and adds
tenant-scoped event and ONVIF controls. Device-sharing permissions remain
explicit: assign the dedicated
`vlstream:device:share:manage` and `vlstream:device:share:approve` permissions
to the intended roles; the WVP migration does not grant them to existing roles.

The default Compose file starts MySQL, Redis, MinIO, WebRTC-streamer, the
backend, and the frontend. To use existing MySQL and Redis services, provide
their connection variables and run:

```powershell
docker compose -f compose.external.yaml up -d
```

## Database Upgrades

MySQL imports `sql/init/*.sql` only when its data volume is empty. After the
initial installation, Flyway applies new migration files automatically whenever
the backend starts. Back up the database before upgrading, and never edit a
migration that has already run.

Update images in `.env`, then run:

```powershell
docker compose pull
docker compose up -d
```

Check status and logs with:

```powershell
docker compose ps
docker compose logs -f backend frontend
```

## Optional IPC Remote Management

For LAN acceptance from another computer, public configuration generation, the
credential-free client probe, and production prerequisites, see the
[IPC gateway deployment guide](../../docs/IPC_GATEWAY_DEPLOYMENT.md).
`.ipc.localhost` is for the developer machine; sharing a final page URL does not
transfer its browser session. Generating templates does not apply DNS, trust,
or runtime changes.

The `tunnel-runtime` profile combines a pinned rathole Server with a
host-isolated HTTP/WebSocket gateway. It is disabled by default.

Before enabling it:

1. Create wildcard DNS and a wildcard TLS certificate for a dedicated suffix,
   for example `*.ipc.example.com`.
2. Build the runtime and generate the rathole Noise key pair:

   ```powershell
   docker build -t vlstream/tunnel-runtime:local .\tunnel-runtime
   docker run --rm --entrypoint /usr/local/bin/rathole vlstream/tunnel-runtime:local --genkey
   ```

3. Generate three different random values of at least 32 bytes: the service
   signing secret, control API token, and gateway API token. Never reuse or
   commit them.
4. Set every `VLSTREAM_TUNNEL_*` variable in `.env`, including
   `VLSTREAM_TUNNEL_GATEWAY_BASE_URL=https://{sessionId}.ipc.example.com`.
5. Configure the wildcard TLS proxy using
   `VLStream-Web/VLStream-ui/nginx.conf.example`. Preserve the Host header and
   WebSocket Upgrade headers while proxying to host loopback port 8088.
6. Start the optional profile:

   ```powershell
   docker compose --profile tunnel up -d --build
   docker compose --profile tunnel ps
   ```

Only the rathole control port (default 2333) is public. The gateway port is
published on `127.0.0.1`, and per-device service ports 61000-61999 stay inside
the tunnel-runtime container. Never publish that service range.

The runtime stops rathole when it cannot refresh desired route state for two
minutes. A healthy container does not prove an IPC is reachable; enrollment,
Agent heartbeat, route `APPLIED`, browser login, and real device reconnect
still require end-to-end verification.

Browser access sessions expire after 10 hours without HTTP requests by default.
Initial bootstrap and subsequent HTTP requests renew the session after backend
validation, and refresh the gateway cookie. The bootstrap token remains single-use.
Revoked, expired or unavailable sessions cannot renew. Background HTTP polling
counts as activity; WebSocket upgrade renews once, subsequent frames do not.
`VLSTREAM_TUNNEL_ACCESS_SESSION_TTL_SECONDS=36000` accepts values from 60 to 36000
seconds. This does not change enrollment-code or IPC-login lifetimes. New
sessions use the updated value after the backend and gateway are updated together.
