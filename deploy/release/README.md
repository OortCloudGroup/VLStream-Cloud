# VLStream Cloud v1.2.0 Deployment Guide

Download and extract the GitHub Release archive. It contains the Compose files,
environment template, and the sanitized VLStream initialization SQL.

## One-command installation

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Before production startup, replace every `change-me` value in `.env`.

- Web: `http://localhost/bus/vls-ui/`
- Default account: `admin`
- Default password: `Codex@123456`

Change the application password immediately after the first sign-in.

The default stack starts MySQL, Redis, MinIO, WebRTC-streamer, ZLMediaKit, the
VLStream backend, the WVP backend, and the frontend. MySQL contains two separate
databases: `oortcloud_workflowforms_vls` and `ry-wvp`. WVP shares Redis but uses
database index `10`.

## Device and media networking

For access from another computer, set these values to the Docker host's
reachable LAN/public IP or hostname:

```dotenv
SIP_PUBLIC_IP=192.168.1.10
ZLMEDIAKIT_PUBLIC_HOST=192.168.1.10
```

Allow these ports in the host and cloud firewall:

- `8116/tcp` and `8116/udp`: GB28181 SIP
- `40000-40300/tcp` and `40000-40300/udp`: ZLMediaKit RTP
- `554/tcp`: RTSP
- `1935/tcp`: RTMP
- `8000/udp`: ZLMediaKit WebRTC
- `50000-50010/udp`: WebRTC-streamer media

Linux ISUP and Dahua native SDK services are disabled by default because their
vendor runtime libraries are not part of the public image.

## Existing MySQL and Redis

Create both databases and grant the configured users access before starting:

- `oortcloud_workflowforms_vls`
- `ry-wvp`

Fill the `EXTERNAL_*` values in `.env`, then run:

```powershell
docker compose -f compose.external.yaml up -d
```

## Database upgrades

MySQL imports `sql/init/*` only when the bundled MySQL volume is empty. The
VLStream and WVP backends then apply their own Flyway migrations automatically
on every startup. Never edit or manually rerun a migration that may already
have executed. Back up both databases before upgrading.

Update image tags in `.env`, then run:

```powershell
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f backend wvp-backend zlmediakit frontend
```

`docker compose down` keeps named volumes. Adding `-v` permanently deletes the
database, object-storage, and service data volumes.
