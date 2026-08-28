# VLStream Cloud v1.2.2

This release is built from `origin/main` and integrates with the independently
published APaaS WVP Server v1.0.3.

## Highlights

- Added tenant-aware platform header controls and improved Model Hub navigation.
- Routed model dispatch and protocol-device resolution through the WVP device
  center, including a separately configurable internal device-resolver URL.
- Refined authentication compatibility, administrator permission resolution,
  default single-tenant behavior, and MQTT runtime configuration.
- Restored reproducible release packaging with public image builds, deployment
  Compose files, a sanitized initialization schema, and immutable Flyway
  migrations included in the archive.

## Deployment

- VLStream images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.2` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.2`.
- WVP is deployed separately from
  `ghcr.io/oortcloudgroup/apaas-wvp-server:1.0.3` and must be started first.
- Set both `VLSTREAM_WVP_INTERNAL_BASE_URL` and
  `VLSTREAM_WVP_DEVICE_BASE_URL` to addresses reachable from the VLStream
  backend container. Set `WVP_UPSTREAM` for browser proxying.
- Use the same ZLMediaKit secret in both deployments. Set
  `VLSTREAM_ZLM_INTERNAL_URL` and `ZLM_UPSTREAM` to the WVP-owned ZLMediaKit
  service.
- Copy `.env.example` to `.env`, replace every example password and secret,
  and run `docker compose up -d`.

## Database

- New installations import the sanitized initialization schema, then run the
  immutable Flyway migrations included in the backend image.
- Existing installations only run pending Flyway migrations. Back up the
  database before upgrading and never edit a migration that has already run.
