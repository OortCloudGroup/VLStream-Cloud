# VLStream Cloud v1.2.5

This release is built from `origin/main` and integrates with the independently
published VLStream WVP Lite v1.0.6.

## Highlights

- Added tenant-aware IPC remote-management control plane support with an optional,
  loopback-bound `tunnel-runtime` Compose profile and its Go gateway tests.
- Added tenant platform-logo management, smart annotation workflows, typed
  annotation payloads, and expanded multilingual console coverage.
- Added browser-session renewal and stricter device-tenant ownership checks for
  remote management and device operations.
- Moved platform credentials to environment-backed production configuration and
  removed plaintext production defaults from the release runtime configuration.
- Included immutable Flyway migrations V1_2_0_020 through V1_2_0_023 while
  preserving the previously released V1_2_0_019 OSS seed.

## Deployment

- VLStream images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.5` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.5`.
- WVP is deployed separately from the [VLStream WVP Lite v1.0.6 release](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.6),
  published as `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.6`, and must be
  started first.
- Set both `VLSTREAM_WVP_INTERNAL_BASE_URL` and
  `VLSTREAM_WVP_DEVICE_BASE_URL` to addresses reachable from the VLStream
  backend container. Set `WVP_UPSTREAM` for browser proxying.
- Use the same ZLMediaKit secret in both deployments. Set
  `VLSTREAM_ZLM_INTERNAL_URL` and `ZLM_UPSTREAM` to the WVP-owned ZLMediaKit
  service.
- The optional IPC tunnel profile is disabled by default. Configure its wildcard
  DNS/TLS and independent secrets before enabling `docker compose --profile tunnel`.
- Copy `.env.example` to `.env`, replace every example password and secret,
  and run `docker compose up -d`.

## Database

- New installations import the sanitized initialization schema, then run the
  immutable Flyway migrations included in the backend image.
- Existing installations only run pending Flyway migrations. Back up the
  database before upgrading and never edit a migration that has already run.
