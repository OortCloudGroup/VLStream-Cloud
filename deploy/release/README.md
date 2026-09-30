# VLStream Cloud v1.2.6 Deployment Guide

This release archive contains the VLS application, its MySQL bootstrap schema,
and the separate WVP database schema used when sharing bundled MySQL. The WVP
and ZLMediaKit services are published and deployed separately.

## Before starting VLStream

Deploy [VLStream WVP Lite v1.0.8](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.8)
first. Its public image is `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.8`.
WVP remains the video-device center and owns the ZLMediaKit service; the VLStream
Compose project does not start duplicate WVP or ZLMediaKit services.

When both products use the same host, set `WVP_HTTP_PORT=9080` in the WVP
deployment because VLStream uses host port 8080. Set `ZLMEDIAKIT_SECRET` in the
VLStream `.env` to the exact `ZLM_SECRET` used by WVP. The default WVP and
ZLMediaKit browser/backend routes use host ports 9080 and 8081 respectively;
override `VLSTREAM_WVP_*`, `WVP_UPSTREAM`, `VLSTREAM_ZLM_INTERNAL_URL`, and
`ZLM_UPSTREAM` when your network topology differs.

## Fresh installation

```powershell
Copy-Item .env.example .env
# Edit .env and replace every example password and secret.
docker compose up -d
```

The default VLS Compose project starts MySQL, Redis, MinIO, WebRTC-streamer,
the backend, and the frontend. It initializes the VLS database and a separate
`ry-wvp` schema on the bundled MySQL volume. The WVP schema and its default
administrator are from the reviewed v1.0.8 bootstrap; WVP itself is not started
by this Compose project. If WVP uses this MySQL instance, configure its
`compose.external.yaml` with:

- `DB_HOST=host.docker.internal`, `DB_PORT=3306`, and `DB_NAME=ry-wvp`;
- `DB_USERNAME=wvp` and `DB_PASSWORD` equal to `WVP_MYSQL_PASSWORD` from VLS
  `.env`;
- `DB_MIGRATION_USERNAME=root` and `DB_MIGRATION_PASSWORD` equal to
  `MYSQL_ROOT_PASSWORD`;
- `WVP_HTTP_PORT=9080`.

Generate `WVP_MYSQL_PASSWORD` as a strong random alphanumeric value of at least
32 characters. You may instead run WVP with its own MySQL deployment. Change the
initial WVP administrator password immediately after first login.

VLStream defaults to the `prod` Spring profile. Keep
`SPRING_PROFILES_ACTIVE=prod` for released images. It selects the production
Flyway lineage used by public releases.
`UNIFIEDMESSAGINGSEND_URL` is optional; set it when workflow message
notifications must use an external messaging service. Blank leaves those
outbound notifications unconfigured.

- Web UI: `http://localhost/bus/vls-ui/`
- Default VLS account: `admin`
- Default VLS password: `Codex@123456` (change it after first login)

## Existing MySQL and Redis

Set the external database/cache variables in `.env`, then run:

```powershell
docker compose -f compose.external.yaml up -d
```

This Compose file does not initialize an external database. For a new VLS
database, create the database and import
`sql/init/10-oortcloud-workflowforms-vls.sql` once. For an existing public
v1.2.5 database, do not reimport the bootstrap schema; start the v1.2.6 backend
with the `prod` profile and allow Flyway to apply the pending release migrations.
If WVP shares this external MySQL instance, its `ry-wvp` database/user must also
be initialized by the database administrator or by the WVP deployment process.

## Upgrading

Back up MySQL before upgrading. Keep `SPRING_PROFILES_ACTIVE=prod`; the production
profile uses the public release history under `db/migration/release` and applies
v1.2.6 migrations `V1_2_0_024` through `V1_2_0_028`. The development history is
kept separately under `db/migration/mainline`; do not switch an existing
database between these locations.

Update images in `.env`, then run:

```powershell
docker compose pull
docker compose up -d
```

## IPC remote management

The tunnel control plane is enabled by default (`VLSTREAM_TUNNEL_ENABLED=true`).
This does not start the tunnel sidecar. The `tunnel` Compose profile remains
optional and must only be enabled after configuring its routing and secrets.
Set `VLSTREAM_TUNNEL_ENABLED=false` to disable the control plane.

Before enabling the sidecar:

1. Configure wildcard DNS and a TLS certificate for a dedicated suffix such as
   `*.ipc.example.com`.
2. Build the runtime and generate a rathole Noise key pair:

   ```powershell
   docker build -t vlstream/tunnel-runtime:local .\tunnel-runtime
   docker run --rm --entrypoint /usr/local/bin/rathole vlstream/tunnel-runtime:local --genkey
   ```

3. Generate separate random values of at least 32 bytes for the service signing
   secret, control API token, and gateway API token.
4. Set every `VLSTREAM_TUNNEL_*` value in `.env`, including
   `VLSTREAM_TUNNEL_GATEWAY_BASE_URL=https://{sessionId}.ipc.example.com`.
5. Configure the wildcard TLS proxy with
   `VLStream-Web/VLStream-ui/nginx.conf.example`. Preserve the Host and WebSocket
   Upgrade headers while proxying to loopback port 8088.
6. Start the optional profile:

   ```powershell
   docker compose --profile tunnel up -d --build
   docker compose --profile tunnel ps
   ```

Only the rathole control port (default 2333) should be public. The gateway binds
to `127.0.0.1`; per-device ports 61000-61999 stay inside the tunnel runtime
container and must not be published.

Browser access sessions expire after 10 hours without HTTP requests by default.
Validated HTTP requests renew the session and gateway cookie; the bootstrap
token remains single-use. Revoked, expired, or unavailable sessions cannot
renew. WebSocket upgrade renews once, and subsequent frames do not extend the
session. The runtime stops rathole if it cannot refresh desired route state for
two minutes.

A healthy container does not prove an IPC is reachable. Verify enrollment, Agent
heartbeat, route `APPLIED`, browser login, and device reconnect behavior in the
target environment.
