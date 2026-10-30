# VLStream Cloud v1.2.9

Built from `origin/main` at `9f52a515`. Production database upgrades continue
the public v1.2.7 Flyway history.

## Highlights

- Add cloud video analysis scenario and task workspaces with persisted runs and
  linked events.
- Improve model dispatch across selected devices, including saved-model
  selection, batch records, and deployment status visibility.
- Improve video square, event location, playback, and tenant-scoped event
  handling; strengthen session checks for long-lived IPC WebSocket connections.
- Require explicit identity-bound authorization before sending LLM review
  requests, and strengthen tenant and user identity checks in compute workflows.
- Add durable paid-run power-operation recovery and expand shared compute queue
  and billing safeguards. Payment, funding, and automatic power-control gates
  remain disabled by default; this release does not by itself enable live
  charging or external money movement.

## Compatibility and database upgrades

- Deploy [VLStream WVP Lite v1.0.10](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.10)
  separately. Its public image is
  `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.10`. WVP remains the device
  center and owns ZLMediaKit; this package does not start a second WVP stack.
- WVP tenant-scoped device sharing requires explicit assignment of the
  `vlstream:device:share:manage` and `vlstream:device:share:approve`
  permissions. The WVP migration does not grant these permissions to existing
  roles.
- Backend and frontend images:
  `ghcr.io/oortcloudgroup/vlstream-backend:1.2.9` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.9`.
- Production upgrades continue v1.2.7's release Flyway history and apply
  `V1_2_0_049` through `V1_2_0_060` in order. Development and local profiles
  retain the separate mainline history. Never change migration locations for a
  database that already has Flyway history.
- Fresh installations use the sanitized VLS and reviewed WVP bootstrap
  schemas; Flyway applies subsequent migrations. The package supports bundled
  MySQL and Redis or externally managed MySQL and Redis.

Paid compute and cloud video analysis still depend on tenant permissions,
operator configuration, provider access, network reachability, and the target
GPU runtime. Release checks do not establish real supplier operation, payment
activation, model quality, or compatibility with every GPU or device.
