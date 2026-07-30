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
