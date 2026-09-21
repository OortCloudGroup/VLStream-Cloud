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

WVP is a required dependency and the sole video-device center. This package
does not bundle a second WVP copy; deploy `apaas-wvp-server` first and set
`VLSTREAM_WVP_INTERNAL_BASE_URL` to an address reachable from the backend
container, such as `http://host.docker.internal:9080` for a separate service on
the same host. The read-only device lookup uses no additional shared secret;
keep the internal path reachable only through the backend service network.

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
