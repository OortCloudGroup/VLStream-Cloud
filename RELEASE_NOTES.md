# VLStream Cloud v1.1.3

This release packages the current `origin/main` after v1.1.2 with the latest device, video, model-dispatch, training, and frontend navigation updates.

## Highlights

- Added VLS-Protocol 2.2 model dispatch, signed model transfer, device media handling, and durable model conversion status tracking.
- Added model-training scheduler and container configuration support, together with algorithm-library and city-management seed migrations.
- Added V2 event-group management and unified active-safety navigation flows.
- Updated video playback and camera preview paths, including WebRTC snapshot fallback behavior.
- Grouped frontend routes into edge intelligence and cloud intelligence navigation sections.
- Added the core business and technical architecture reference with hardware lifecycle, video, messaging, storage, and dependency diagrams.

## Database

- New and existing installations use `sql/init/10-oortcloud-workflowforms-vls.sql` for the baseline schema.
- Flyway applies `V1_1_3_001` through `V1_1_3_005` automatically when the v1.1.3 backend starts.
- Back up existing databases before upgrading. Do not manually execute Flyway migration copies from `sql/upgrade`.

## Deployment

- Default images are `ghcr.io/oortcloudgroup/vlstream-backend:1.1.3` and `ghcr.io/oortcloudgroup/vlstream-frontend:1.1.3`.
- Extract the archive and run `docker compose up -d` for bundled MySQL, Redis, MinIO, WebRTC-streamer, backend, and frontend services.
- Use `docker compose -f compose.external.yaml up -d` to reuse externally managed MySQL and Redis services.
