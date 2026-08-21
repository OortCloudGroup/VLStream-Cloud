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
does not bundle a second WVP copy. Download and start
[APaaS WVP Server v1.0.1](https://github.com/OortCloudGroup/apaas-wvp-server/releases/tag/v1.0.1)
first. When both products run on the same host, set `WVP_HTTP_PORT=9080` in the
WVP `.env` to avoid the VLStream backend port, and set `ZLM_SECRET` to the same
value as VLStream's `ZLMEDIAKIT_SECRET`.

`VLSTREAM_WVP_INTERNAL_BASE_URL` and `WVP_UPSTREAM` must point to the WVP HTTP
service from the backend and frontend containers. `VLSTREAM_ZLM_INTERNAL_URL`
and `ZLM_UPSTREAM` must point to the ZLMediaKit HTTP service started by WVP. The
provided same-host defaults use `host.docker.internal:9080` and
`host.docker.internal:8081`.

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
