# VLStream Cloud v1.2.4

This release is built from `origin/main` and integrates with the independently
published VLStream WVP Lite v1.0.6.

## Highlights

- Added resumable dataset-source imports, video-frame origin tracking, ZIP annotation progress repair, and safer dataset cleanup.
- Added model-class files, model publication safeguards, device-model candidates, model-hub catalog APIs, and shared training-option metadata.
- Added EHome device management and player integration together with shared locale switching and language resources.
- Continued the multi-tenant platform-session, model-dispatch, device-state, and training-publication improvements from the previous release line.
- Included Flyway migrations V1_2_0_011 through V1_2_0_019 for seamless automated database upgrades.

## Deployment

- VLStream images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.4` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.4`.
- WVP is deployed separately from the [VLStream WVP Lite v1.0.6 release](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.6),
  published as `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.6`, and must be
  started first.
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
