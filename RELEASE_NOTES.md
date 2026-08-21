# VLStream Cloud v1.2.1

This release is built from `origin/main` and integrates with the independently
published APaaS WVP Server v1.0.1.

## Highlights

- Unified VLStream, GB28181, ONVIF, RTSP, ISUP, and Dahua device access through the WVP device center.
- Added tenant-aware token exchange, shadow identities, and event-delivery isolation.
- Added device classification across protocol sources and refreshed the device-management experience.
- Added firmware package management, deployment jobs, OTA status tracking, and rootfs-only delivery support.
- Improved MQTT device state handling, media upload validation, GPU training scheduling, and timeout recovery.
- Refined navigation, page layout, icons, tables, and algorithm-training workflows.

## Deployment

- VLStream images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.1` and `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.1`.
- WVP is deployed separately from `ghcr.io/oortcloudgroup/apaas-wvp-server:1.0.1` and must be started before VLStream.
- When both products run on the same host, set `WVP_HTTP_PORT=9080` in the WVP `.env`, then keep the VLStream WVP upstream at `http://host.docker.internal:9080`.
- Use the same ZLMediaKit secret in both deployments so VLStream can reuse the media server started by WVP.
- Extract the archive, copy `.env.example` to `.env`, replace every example password and secret, and run `docker compose up -d`.

## Database

- New installations import the sanitized VLStream initialization SQL.
- Existing installations receive immutable Flyway migrations for firmware management, GPU scheduler metadata, tenant shadow identities, and event-report delivery.
- Back up the database before upgrading and never edit a Flyway migration that has already run.
