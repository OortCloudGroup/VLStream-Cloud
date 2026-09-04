# VLStream Cloud v1.2.3

This release is built from `origin/main` and integrates with the independently
published VLStream WVP Lite v1.0.4.

## Highlights

- Added hierarchical algorithm catalog management supporting multi-level categories, tenant-isolated display preferences (tree and flat modes), and backend aggregated filtering.
- Implemented visual large language model (LLM) review for device AI structured events, including OortCloud tenant authorization, background worker execution, and OpenAI-compatible vision service integration.
- Repaired device event media pre-signed upload and preview URLs with consistent MinIO region handling and explicit public endpoint derivation.
- Added CameraRTC WebSocket proxy and standardized video playback across console views.
- Restored algorithm annotation task progress tracking and enhanced algorithm lifecycle management.
- Included Flyway migrations V1_2_0_005 through V1_2_0_010 for seamless automated database upgrades.

## Deployment

- VLStream images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.3` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.3`.
- WVP is deployed separately from
  `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.4` and must be started first.
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
